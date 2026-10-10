package io.github.gilnetizen.aseh.core.content

import java.io.BufferedOutputStream
import java.io.OutputStream
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.CRC32

/** Raw ZIP writer for the deliberately small deterministic STORED pack profile. */
internal object DeterministicStoredZipWriter {
    private const val UTF8_FLAG = 0x0800
    private const val STORED_METHOD = 0
    private const val DOS_TIME_MIDNIGHT = 0
    private const val DOS_DATE_1980_01_01 = 0x0021
    private const val VERSION_NEEDED = 10
    private const val VERSION_MADE_BY_UNIX = 0x0314
    private const val REGULAR_FILE_0644 = 0x81a40000L
    private const val UINT32_MAX = 0xffffffffL

    fun write(output: Path, entries: Map<String, ByteArray>) {
        require(entries.size <= 0xffff) { "ZIP entry count exceeds deterministic profile" }
        val ordered = entries.entries.sortedWith { left, right -> compareUtf8(left.key, right.key) }
        Files.newOutputStream(output).use { file ->
            CountingOutputStream(BufferedOutputStream(file)).use { archive ->
                val central = mutableListOf<CentralRecord>()
                ordered.forEach { (name, bytes) ->
                    val nameBytes = name.toByteArray(StandardCharsets.UTF_8)
                    require(nameBytes.size <= 0xffff) { "ZIP path is too long" }
                    require(bytes.size.toLong() <= UINT32_MAX) { "ZIP entry is too large" }
                    require(archive.written <= UINT32_MAX) { "ZIP local-header offset exceeds profile" }
                    val crc = CRC32().apply { update(bytes) }.value
                    val offset = archive.written
                    archive.writeUInt32(0x04034b50)
                    archive.writeUInt16(VERSION_NEEDED)
                    archive.writeUInt16(UTF8_FLAG)
                    archive.writeUInt16(STORED_METHOD)
                    archive.writeUInt16(DOS_TIME_MIDNIGHT)
                    archive.writeUInt16(DOS_DATE_1980_01_01)
                    archive.writeUInt32(crc)
                    archive.writeUInt32(bytes.size.toLong())
                    archive.writeUInt32(bytes.size.toLong())
                    archive.writeUInt16(nameBytes.size)
                    archive.writeUInt16(0)
                    archive.write(nameBytes)
                    archive.write(bytes)
                    central += CentralRecord(nameBytes, crc, bytes.size.toLong(), offset)
                }

                val centralOffset = archive.written
                central.forEach { entry ->
                    archive.writeUInt32(0x02014b50)
                    archive.writeUInt16(VERSION_MADE_BY_UNIX)
                    archive.writeUInt16(VERSION_NEEDED)
                    archive.writeUInt16(UTF8_FLAG)
                    archive.writeUInt16(STORED_METHOD)
                    archive.writeUInt16(DOS_TIME_MIDNIGHT)
                    archive.writeUInt16(DOS_DATE_1980_01_01)
                    archive.writeUInt32(entry.crc)
                    archive.writeUInt32(entry.size)
                    archive.writeUInt32(entry.size)
                    archive.writeUInt16(entry.name.size)
                    archive.writeUInt16(0)
                    archive.writeUInt16(0)
                    archive.writeUInt16(0)
                    archive.writeUInt16(0)
                    archive.writeUInt32(REGULAR_FILE_0644)
                    archive.writeUInt32(entry.localHeaderOffset)
                    archive.write(entry.name)
                }
                val centralSize = archive.written - centralOffset
                require(centralOffset <= UINT32_MAX && centralSize <= UINT32_MAX) {
                    "ZIP central directory exceeds deterministic profile"
                }
                archive.writeUInt32(0x06054b50)
                archive.writeUInt16(0)
                archive.writeUInt16(0)
                archive.writeUInt16(central.size)
                archive.writeUInt16(central.size)
                archive.writeUInt32(centralSize)
                archive.writeUInt32(centralOffset)
                archive.writeUInt16(0)
            }
        }
    }

    private fun compareUtf8(left: String, right: String): Int {
        val leftBytes = left.toByteArray(StandardCharsets.UTF_8)
        val rightBytes = right.toByteArray(StandardCharsets.UTF_8)
        val shared = minOf(leftBytes.size, rightBytes.size)
        for (index in 0 until shared) {
            val comparison = (leftBytes[index].toInt() and 0xff).compareTo(rightBytes[index].toInt() and 0xff)
            if (comparison != 0) return comparison
        }
        return leftBytes.size.compareTo(rightBytes.size)
    }

    private data class CentralRecord(
        val name: ByteArray,
        val crc: Long,
        val size: Long,
        val localHeaderOffset: Long,
    )

    private class CountingOutputStream(private val delegate: OutputStream) : OutputStream() {
        var written: Long = 0
            private set

        override fun write(value: Int) {
            delegate.write(value)
            written += 1
        }

        override fun write(bytes: ByteArray, offset: Int, length: Int) {
            delegate.write(bytes, offset, length)
            written += length
        }

        override fun flush() = delegate.flush()
        override fun close() = delegate.close()

        fun writeUInt16(value: Int) {
            require(value in 0..0xffff)
            write(value and 0xff)
            write((value ushr 8) and 0xff)
        }

        fun writeUInt32(value: Long) {
            require(value in 0..UINT32_MAX)
            write((value and 0xff).toInt())
            write(((value ushr 8) and 0xff).toInt())
            write(((value ushr 16) and 0xff).toInt())
            write(((value ushr 24) and 0xff).toInt())
        }
    }
}
