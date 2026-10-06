package io.github.gilnetizen.aseh.core.database

/**
 * The immutable identity recorded after a content pack has passed verification.
 *
 * This is catalog metadata only. It deliberately contains no pack payload,
 * source passage, filesystem path, user-authored content, or install timestamp.
 */
data class InstalledPackIdentity(
    val packId: String,
    val immutableVersion: String,
    val manifestSha256: String,
    val schemaVersion: Int,
    val signingKeyId: String,
) {
    init {
        require(packId.isNotBlank()) { "packId must not be blank" }
        require(immutableVersion.isNotBlank()) { "immutableVersion must not be blank" }
        require(schemaVersion > 0) { "schemaVersion must be positive" }
        require(manifestSha256.isLowercaseSha256()) {
            "manifestSha256 must be a lowercase SHA-256 digest"
        }
        require(signingKeyId.isLowercaseSha256()) {
            "signingKeyId must be a lowercase SHA-256 digest"
        }
    }
}

data class InstalledPackCatalogEntry(
    val identity: InstalledPackIdentity,
    val isActive: Boolean,
)

/**
 * Operational catalog access for app-level orchestration.
 *
 * Features receive this interface when they need catalog state. The Room
 * database and DAO remain private to this module.
 */
interface InstalledPackCatalogRepository {
    suspend fun entries(): List<InstalledPackCatalogEntry>

    /** Records metadata for an already verified installed pack as inactive. */
    suspend fun recordInstalled(identity: InstalledPackIdentity)

    /**
     * Atomically selects one installed version for [packId].
     *
     * Returns false without changing the current selection when the requested
     * identity is not present.
     */
    suspend fun activate(packId: String, immutableVersion: String): Boolean

    suspend fun remove(packId: String, immutableVersion: String): Boolean
}

private fun String.isLowercaseSha256(): Boolean =
    length == SHA_256_HEX_LENGTH && all { it in '0'..'9' || it in 'a'..'f' }

private const val SHA_256_HEX_LENGTH = 64
