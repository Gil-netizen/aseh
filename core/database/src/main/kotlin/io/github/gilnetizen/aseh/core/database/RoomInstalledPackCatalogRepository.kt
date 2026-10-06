package io.github.gilnetizen.aseh.core.database

internal class RoomInstalledPackCatalogRepository(
    private val dao: InstalledPackCatalogDao,
) : InstalledPackCatalogRepository {
    override suspend fun entries(): List<InstalledPackCatalogEntry> =
        dao.entries().map(InstalledPackCatalogEntity::toExternalModel)

    override suspend fun recordInstalled(identity: InstalledPackIdentity) {
        dao.recordInstalled(identity.toEntity())
    }

    override suspend fun activate(packId: String, immutableVersion: String): Boolean {
        require(packId.isNotBlank()) { "packId must not be blank" }
        require(immutableVersion.isNotBlank()) { "immutableVersion must not be blank" }
        return dao.activate(packId, immutableVersion)
    }

    override suspend fun remove(packId: String, immutableVersion: String): Boolean {
        require(packId.isNotBlank()) { "packId must not be blank" }
        require(immutableVersion.isNotBlank()) { "immutableVersion must not be blank" }
        return dao.remove(packId, immutableVersion) == 1
    }
}

private fun InstalledPackIdentity.toEntity(): InstalledPackCatalogEntity =
    InstalledPackCatalogEntity(
        packId = packId,
        immutableVersion = immutableVersion,
        manifestSha256 = manifestSha256,
        schemaVersion = schemaVersion,
        signingKeyId = signingKeyId,
        isActive = false,
    )

private fun InstalledPackCatalogEntity.toExternalModel(): InstalledPackCatalogEntry =
    InstalledPackCatalogEntry(
        identity = InstalledPackIdentity(
            packId = packId,
            immutableVersion = immutableVersion,
            manifestSha256 = manifestSha256,
            schemaVersion = schemaVersion,
            signingKeyId = signingKeyId,
        ),
        isActive = isActive,
    )
