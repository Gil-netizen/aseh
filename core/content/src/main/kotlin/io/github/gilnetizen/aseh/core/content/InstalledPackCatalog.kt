package io.github.gilnetizen.aseh.core.content

import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption

data class InstalledPackVersion(
    val packId: String,
    val version: SemanticVersion,
    val manifestSha256: String,
    val signingKeyId: String,
    val relativeDirectory: String,
)

data class InstalledPackState(
    val packId: String,
    val activeManifestSha256: String?,
    val installed: List<InstalledPackVersion>,
) {
    val active: InstalledPackVersion?
        get() = installed.singleOrNull { it.manifestSha256 == activeManifestSha256 }
}

data class InstalledPackCatalogState(
    val packs: List<InstalledPackState>,
) {
    fun pack(packId: String): InstalledPackState? = packs.singleOrNull { it.packId == packId }
}

class InstalledPackCatalog internal constructor(private val root: Path) {
    private val catalogFile = root.resolve("catalog.json")

    fun read(): InstalledPackCatalogState {
        if (!Files.exists(catalogFile)) return InstalledPackCatalogState(emptyList())
        val bytes = Files.readAllBytes(catalogFile)
        require(bytes.size <= MAX_CATALOG_BYTES) { "Installed pack catalog exceeds size limit" }
        return decode(bytes)
    }

    fun activate(record: InstalledPackVersion): InstalledPackCatalogState {
        val current = read()
        val previousPack = current.pack(record.packId)
        val installed = ((previousPack?.installed ?: emptyList()).filterNot {
            it.manifestSha256 == record.manifestSha256
        } + record).sortedWith(compareBy(InstalledPackVersion::version, InstalledPackVersion::manifestSha256))
        val updatedPack = InstalledPackState(
            packId = record.packId,
            activeManifestSha256 = record.manifestSha256,
            installed = installed,
        )
        val updated = InstalledPackCatalogState(
            packs = (current.packs.filterNot { it.packId == record.packId } + updatedPack).sortedBy { it.packId },
        )
        write(updated)
        return updated
    }

    fun activateExisting(packId: String, manifestSha256: String): InstalledPackCatalogState {
        val current = read()
        val pack = current.pack(packId) ?: error("Pack '$packId' is not installed")
        require(pack.installed.any { it.manifestSha256 == manifestSha256 }) {
            "Manifest '$manifestSha256' is not installed for '$packId'"
        }
        val updatedPack = pack.copy(activeManifestSha256 = manifestSha256)
        val updated = current.copy(
            packs = current.packs.map { if (it.packId == packId) updatedPack else it }.sortedBy { it.packId },
        )
        write(updated)
        return updated
    }

    private fun write(state: InstalledPackCatalogState) {
        Files.createDirectories(root)
        val bytes = encode(state)
        val pending = Files.createTempFile(root, ".catalog-", ".pending")
        try {
            FileChannel.open(pending, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING).use { channel ->
                var remaining = ByteBuffer.wrap(bytes)
                while (remaining.hasRemaining()) channel.write(remaining)
                channel.force(true)
            }
            try {
                Files.move(
                    pending,
                    catalogFile,
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING,
                )
            } catch (error: AtomicMoveNotSupportedException) {
                throw IllegalStateException("Pack catalog filesystem does not support atomic activation", error)
            }
        } finally {
            Files.deleteIfExists(pending)
        }
    }

    private fun encode(state: InstalledPackCatalogState): ByteArray = CanonicalJson.encode(
        jsonObject(
            "formatVersion" to jsonInteger(1),
            "packs" to jsonArray(
                state.packs.sortedBy { it.packId }.map { pack ->
                    jsonObject(
                        "packId" to jsonString(pack.packId),
                        "activeManifestSha256" to (
                            pack.activeManifestSha256?.let(::jsonString) ?: JsonValue.NullValue
                        ),
                        "installed" to jsonArray(
                            pack.installed.sortedWith(
                                compareBy(InstalledPackVersion::version, InstalledPackVersion::manifestSha256),
                            ).map { installed ->
                                jsonObject(
                                    "version" to jsonString(installed.version.toString()),
                                    "manifestSha256" to jsonString(installed.manifestSha256),
                                    "signingKeyId" to jsonString(installed.signingKeyId),
                                    "relativeDirectory" to jsonString(installed.relativeDirectory),
                                )
                            },
                        ),
                    )
                },
            ),
        ),
    )

    private fun decode(bytes: ByteArray): InstalledPackCatalogState {
        val rootObject = CanonicalJson.requireCanonical(bytes).asObject("installed pack catalog")
        rootObject.strictKeys("installed pack catalog", setOf("formatVersion", "packs"))
        require(rootObject.required("formatVersion", "catalog").asInt("catalog.formatVersion") == 1) {
            "Unsupported installed pack catalog format"
        }
        val packs = rootObject.required("packs", "catalog").asArray("catalog.packs").map { packValue ->
            val packObject = packValue.asObject("catalog pack")
            packObject.strictKeys("catalog pack", setOf("packId", "activeManifestSha256", "installed"))
            val packId = packObject.required("packId", "catalog pack").asString("catalog pack.packId")
            val installed = packObject.required("installed", "catalog pack").asArray("catalog pack.installed")
                .map { installedValue ->
                    val installedObject = installedValue.asObject("installed version")
                    installedObject.strictKeys(
                        "installed version",
                        setOf("version", "manifestSha256", "signingKeyId", "relativeDirectory"),
                    )
                    InstalledPackVersion(
                        packId = packId,
                        version = SemanticVersion.parse(
                            installedObject.required("version", "installed version")
                                .asString("installed version.version"),
                        ),
                        manifestSha256 = installedObject.required("manifestSha256", "installed version")
                            .asString("installed version.manifestSha256"),
                        signingKeyId = installedObject.required("signingKeyId", "installed version")
                            .asString("installed version.signingKeyId"),
                        relativeDirectory = installedObject.required("relativeDirectory", "installed version")
                            .asString("installed version.relativeDirectory"),
                    ).also { record -> validateCatalogRecord(record) }
                }
            val active = packObject.nullableString("activeManifestSha256", "catalog pack")
            require(installed.map { it.manifestSha256 }.distinct().size == installed.size) {
                "Catalog contains duplicate installed manifest identities"
            }
            require(installed.map { it.version }.distinct().size == installed.size) {
                "Catalog contains more than one immutable artifact for a pack version"
            }
            require(active == null || installed.any { it.manifestSha256 == active }) {
                "Catalog active version is not present in installed versions"
            }
            InstalledPackState(packId = packId, activeManifestSha256 = active, installed = installed)
        }
        require(packs.map { it.packId }.distinct().size == packs.size) { "Duplicate packs in installed catalog" }
        return InstalledPackCatalogState(packs)
    }

    private fun validateCatalogRecord(record: InstalledPackVersion) {
        require(Regex("[a-z][a-z0-9]*(?:[._-][a-z0-9]+)*").matches(record.packId)) { "Invalid catalog pack ID" }
        require(Regex("[0-9a-f]{64}").matches(record.manifestSha256)) { "Invalid catalog manifest digest" }
        require(Regex("[0-9a-f]{64}").matches(record.signingKeyId)) { "Invalid catalog signing key ID" }
        ContentPolicyValidator.validatePackPath(record.relativeDirectory)
        require(record.relativeDirectory.startsWith("packs/${record.packId}/")) { "Catalog directory escapes pack root" }
    }

    companion object {
        private const val MAX_CATALOG_BYTES = 2 * 1024 * 1024
    }
}
