package io.github.gilnetizen.aseh.core.content.runtime

import java.text.Normalizer

class ContentSearchInputException(message: String) : IllegalArgumentException(message)

internal object SafeFtsQuery {
    private const val MAX_QUERY_LENGTH = 1024
    private const val MAX_TERMS = 12
    private const val MAX_TERM_LENGTH = 64
    private val candidateTerm = Regex("[\\p{L}\\p{M}\\p{N}._:'’׳״-]+")
    private val searchableCharacter = Regex("[\\p{L}\\p{N}]")

    fun compile(query: String): String? {
        if (query.length > MAX_QUERY_LENGTH) {
            throw ContentSearchInputException("Search query exceeds $MAX_QUERY_LENGTH characters")
        }
        val normalized = Normalizer.normalize(query, Normalizer.Form.NFC)
        val terms = candidateTerm.findAll(normalized)
            .map { it.value }
            .filter { searchableCharacter.containsMatchIn(it) }
            .toList()
        if (terms.size > MAX_TERMS) {
            throw ContentSearchInputException("Search query exceeds $MAX_TERMS terms")
        }
        if (terms.any { it.length > MAX_TERM_LENGTH }) {
            throw ContentSearchInputException("Search term exceeds $MAX_TERM_LENGTH characters")
        }
        if (terms.isEmpty()) return null
        return terms.joinToString(" AND ") { term ->
            // candidateTerm excludes a double quote; quoting also prevents MATCH operators
            // such as OR, NEAR, column filters, and parentheses from becoming syntax.
            // FTS4 requires the prefix marker inside the quoted token. Keeping the
            // whole expression quoted means an operator-shaped term stays data.
            "\"$term*\""
        }
    }
}
