package io.github.gilnetizen.aseh.core.content

import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets

/** Minimal strict JSON tree used to keep signed metadata dependency-free. */
sealed interface JsonValue {
    data class ObjectValue(val values: Map<String, JsonValue>) : JsonValue
    data class ArrayValue(val values: List<JsonValue>) : JsonValue
    data class StringValue(val value: String) : JsonValue
    data class IntegerValue(val value: Long) : JsonValue
    data class BooleanValue(val value: Boolean) : JsonValue
    data object NullValue : JsonValue
}

/**
 * Canonical JSON for the integer/string/boolean subset used by ASEH metadata.
 * Object keys use RFC 8785 UTF-16 ordering. Floats are deliberately excluded.
 */
object CanonicalJson {
    fun encode(value: JsonValue): ByteArray =
        buildString { appendValue(value) }.toByteArray(StandardCharsets.UTF_8)

    fun parse(bytes: ByteArray): JsonValue {
        val decoder = StandardCharsets.UTF_8.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
        val text = decoder.decode(ByteBuffer.wrap(bytes)).toString()
        return Parser(text).parse()
    }

    fun requireCanonical(bytes: ByteArray): JsonValue {
        val parsed = parse(bytes)
        require(bytes.contentEquals(encode(parsed))) { "JSON is not in canonical form" }
        return parsed
    }

    private fun StringBuilder.appendValue(value: JsonValue) {
        when (value) {
            is JsonValue.ObjectValue -> {
                append('{')
                value.values.entries.sortedBy { it.key }.forEachIndexed { index, entry ->
                    if (index > 0) append(',')
                    appendString(entry.key)
                    append(':')
                    appendValue(entry.value)
                }
                append('}')
            }
            is JsonValue.ArrayValue -> {
                append('[')
                value.values.forEachIndexed { index, item ->
                    if (index > 0) append(',')
                    appendValue(item)
                }
                append(']')
            }
            is JsonValue.StringValue -> appendString(value.value)
            is JsonValue.IntegerValue -> append(value.value)
            is JsonValue.BooleanValue -> append(if (value.value) "true" else "false")
            JsonValue.NullValue -> append("null")
        }
    }

    private fun StringBuilder.appendString(value: String) {
        append('"')
        var index = 0
        while (index < value.length) {
            val character = value[index]
            when (character) {
                '"' -> append("\\\"")
                '\\' -> append("\\\\")
                '\b' -> append("\\b")
                '\u000c' -> append("\\f")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> when {
                    character.code < 0x20 -> append("\\u%04x".format(character.code))
                    character.isHighSurrogate() -> {
                        require(index + 1 < value.length && value[index + 1].isLowSurrogate()) {
                            "Unpaired high surrogate in JSON string"
                        }
                        append(character)
                        index += 1
                        append(value[index])
                    }
                    character.isLowSurrogate() -> error("Unpaired low surrogate in JSON string")
                    else -> append(character)
                }
            }
            index += 1
        }
        append('"')
    }

    private class Parser(private val source: String) {
        private var position = 0
        private var depth = 0

        fun parse(): JsonValue {
            skipWhitespace()
            val result = parseValue()
            skipWhitespace()
            require(position == source.length) { "Unexpected trailing JSON at offset $position" }
            return result
        }

        private fun parseValue(): JsonValue {
            require(position < source.length) { "Unexpected end of JSON" }
            return when (source[position]) {
                '{' -> parseObject()
                '[' -> parseArray()
                '"' -> JsonValue.StringValue(parseString())
                't' -> parseLiteral("true", JsonValue.BooleanValue(true))
                'f' -> parseLiteral("false", JsonValue.BooleanValue(false))
                'n' -> parseLiteral("null", JsonValue.NullValue)
                '-', in '0'..'9' -> parseInteger()
                else -> error("Unexpected JSON token at offset $position")
            }
        }

        private fun parseObject(): JsonValue.ObjectValue = nested {
            expect('{')
            skipWhitespace()
            val values = linkedMapOf<String, JsonValue>()
            if (consume('}')) return@nested JsonValue.ObjectValue(values)
            while (true) {
                require(position < source.length && source[position] == '"') {
                    "Expected object key at offset $position"
                }
                val key = parseString()
                require(!values.containsKey(key)) { "Duplicate object key '$key'" }
                skipWhitespace()
                expect(':')
                skipWhitespace()
                values[key] = parseValue()
                skipWhitespace()
                if (consume('}')) break
                expect(',')
                skipWhitespace()
            }
            JsonValue.ObjectValue(values)
        }

        private fun parseArray(): JsonValue.ArrayValue = nested {
            expect('[')
            skipWhitespace()
            val values = mutableListOf<JsonValue>()
            if (consume(']')) return@nested JsonValue.ArrayValue(values)
            while (true) {
                values += parseValue()
                skipWhitespace()
                if (consume(']')) break
                expect(',')
                skipWhitespace()
            }
            JsonValue.ArrayValue(values)
        }

        private fun parseString(): String {
            expect('"')
            val result = StringBuilder()
            while (position < source.length) {
                val character = source[position++]
                when (character) {
                    '"' -> return result.toString()
                    '\\' -> {
                        require(position < source.length) { "Unterminated JSON escape" }
                        when (val escaped = source[position++]) {
                            '"', '\\', '/' -> result.append(escaped)
                            'b' -> result.append('\b')
                            'f' -> result.append('\u000c')
                            'n' -> result.append('\n')
                            'r' -> result.append('\r')
                            't' -> result.append('\t')
                            'u' -> result.append(parseUnicodeEscape())
                            else -> error("Invalid JSON escape \\$escaped")
                        }
                    }
                    else -> {
                        require(character.code >= 0x20) { "Control character in JSON string" }
                        when {
                            character.isHighSurrogate() -> {
                                require(position < source.length && source[position].isLowSurrogate()) {
                                    "Unpaired high surrogate in JSON string"
                                }
                                result.append(character)
                                result.append(source[position++])
                            }
                            character.isLowSurrogate() -> error("Unpaired low surrogate in JSON string")
                            else -> result.append(character)
                        }
                    }
                }
            }
            error("Unterminated JSON string")
        }

        private fun parseUnicodeEscape(): String {
            val first = parseHexCodeUnit()
            if (first in 0xd800..0xdbff) {
                require(position + 2 <= source.length && source.substring(position, position + 2) == "\\u") {
                    "High surrogate must be followed by a low surrogate"
                }
                position += 2
                val second = parseHexCodeUnit()
                require(second in 0xdc00..0xdfff) { "Invalid low surrogate" }
                return String(charArrayOf(first.toChar(), second.toChar()))
            }
            require(first !in 0xdc00..0xdfff) { "Unpaired low surrogate" }
            return first.toChar().toString()
        }

        private fun parseHexCodeUnit(): Int {
            require(position + 4 <= source.length) { "Incomplete unicode escape" }
            val value = source.substring(position, position + 4)
            require(value.all { it in '0'..'9' || it in 'a'..'f' || it in 'A'..'F' }) {
                "Invalid unicode escape"
            }
            position += 4
            return value.toInt(16)
        }

        private fun parseInteger(): JsonValue.IntegerValue {
            val start = position
            if (source[position] == '-') position += 1
            require(position < source.length) { "Incomplete number" }
            if (source[position] == '0') {
                position += 1
                require(position == source.length || !source[position].isDigit()) { "Leading zero in number" }
            } else {
                require(source[position] in '1'..'9') { "Invalid number" }
                while (position < source.length && source[position].isDigit()) position += 1
            }
            require(position == source.length || source[position] !in setOf('.', 'e', 'E')) {
                "Non-integer JSON numbers are not allowed in signed metadata"
            }
            return JsonValue.IntegerValue(source.substring(start, position).toLong())
        }

        private fun <T : JsonValue> parseLiteral(literal: String, value: T): T {
            require(source.startsWith(literal, position)) { "Invalid JSON literal at offset $position" }
            position += literal.length
            return value
        }

        private inline fun <T> nested(block: () -> T): T {
            depth += 1
            require(depth <= 64) { "JSON nesting exceeds limit" }
            return try {
                block()
            } finally {
                depth -= 1
            }
        }

        private fun skipWhitespace() {
            while (position < source.length && source[position] in setOf(' ', '\t', '\r', '\n')) {
                position += 1
            }
        }

        private fun consume(expected: Char): Boolean {
            if (position < source.length && source[position] == expected) {
                position += 1
                return true
            }
            return false
        }

        private fun expect(expected: Char) {
            require(consume(expected)) { "Expected '$expected' at offset $position" }
        }
    }
}

internal fun jsonObject(vararg pairs: Pair<String, JsonValue?>): JsonValue.ObjectValue =
    JsonValue.ObjectValue(pairs.mapNotNull { (key, value) -> value?.let { key to it } }.toMap())

internal fun jsonArray(values: Iterable<JsonValue>): JsonValue.ArrayValue = JsonValue.ArrayValue(values.toList())
internal fun jsonString(value: String): JsonValue.StringValue = JsonValue.StringValue(value)
internal fun jsonInteger(value: Int): JsonValue.IntegerValue = JsonValue.IntegerValue(value.toLong())
internal fun jsonLong(value: Long): JsonValue.IntegerValue = JsonValue.IntegerValue(value)
internal fun jsonBoolean(value: Boolean): JsonValue.BooleanValue = JsonValue.BooleanValue(value)

internal fun JsonValue.asObject(context: String): Map<String, JsonValue> =
    (this as? JsonValue.ObjectValue)?.values ?: error("$context must be an object")

internal fun JsonValue.asArray(context: String): List<JsonValue> =
    (this as? JsonValue.ArrayValue)?.values ?: error("$context must be an array")

internal fun JsonValue.asString(context: String): String =
    (this as? JsonValue.StringValue)?.value ?: error("$context must be a string")

internal fun JsonValue.asInt(context: String): Int {
    val number = (this as? JsonValue.IntegerValue)?.value ?: error("$context must be an integer")
    require(number in Int.MIN_VALUE..Int.MAX_VALUE) { "$context is outside integer range" }
    return number.toInt()
}

internal fun JsonValue.asLong(context: String): Long =
    (this as? JsonValue.IntegerValue)?.value ?: error("$context must be an integer")

internal fun JsonValue.asBoolean(context: String): Boolean =
    (this as? JsonValue.BooleanValue)?.value ?: error("$context must be a boolean")

internal fun Map<String, JsonValue>.strictKeys(context: String, required: Set<String>, optional: Set<String> = emptySet()) {
    val missing = required - keys
    require(missing.isEmpty()) { "$context is missing fields: ${missing.sorted().joinToString()}" }
    val unknown = keys - required - optional
    require(unknown.isEmpty()) { "$context contains unknown fields: ${unknown.sorted().joinToString()}" }
}

internal fun Map<String, JsonValue>.required(name: String, context: String): JsonValue =
    get(name) ?: error("$context is missing '$name'")

internal fun Map<String, JsonValue>.nullableString(name: String, context: String): String? =
    when (val value = get(name)) {
        null, JsonValue.NullValue -> null
        else -> value.asString("$context.$name")
    }
