package io.github.gilnetizen.aseh.feature.study

import android.content.Context
import io.github.gilnetizen.aseh.core.content.PackInstallException
import io.github.gilnetizen.aseh.core.content.PackTrustMode
import io.github.gilnetizen.aseh.core.content.StaticPackTrustStore
import io.github.gilnetizen.aseh.core.content.TrustedPackKey
import io.github.gilnetizen.aseh.core.content.runtime.AndroidInstalledContentSearch
import io.github.gilnetizen.aseh.core.content.runtime.AndroidPackCryptoReadiness
import io.github.gilnetizen.aseh.core.content.runtime.androidVerifiedPackInstaller
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.KeyFactory
import java.security.spec.X509EncodedKeySpec
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

internal fun flavorDevelopmentContentPackRuntime(context: Context): DevelopmentContentPackRuntime =
    AndroidDevelopmentContentPackRuntime(context.applicationContext)

private class AndroidDevelopmentContentPackRuntime(
    private val context: Context,
) : DevelopmentContentPackRuntime {
    private val sessionMutex = Mutex()
    private var session: InstalledSession? = null

    override suspend fun load(query: String): DevelopmentContentPackUiState = withContext(Dispatchers.IO) {
        if (!AndroidPackCryptoReadiness.isEd25519Available()) {
            return@withContext DevelopmentContentPackUiState.Unsupported(
                "This Android version has no JCA Ed25519 provider. ASEH did not install or open the pack.",
            )
        }

        try {
            val installed = sessionMutex.withLock {
                session ?: installAndOpen().also { session = it }
            }
            val normalizedQuery = query.trim()
            val hits = if (normalizedQuery.isEmpty()) {
                emptyList()
            } else {
                installed.search.search(normalizedQuery).map { hit ->
                    DevelopmentContentSearchHit(
                        contentId = hit.contentId,
                        kind = hit.kind.wireName,
                        language = hit.language,
                        title = hit.title,
                        snippet = hit.snippet,
                        locator = hit.locator,
                    )
                }
            }
            DevelopmentContentPackUiState.Ready(
                packId = installed.packId,
                version = installed.version,
                manifestSha256 = installed.manifestSha256,
                signingKeyId = installed.signingKeyId,
                query = normalizedQuery,
                hits = hits,
            )
        } catch (error: CancellationException) {
            throw error
        } catch (error: PackInstallException) {
            DevelopmentContentPackUiState.Failed(
                "Pack verification rejected the development fixture (${error.reason.name}).",
            )
        } catch (_: Exception) {
            DevelopmentContentPackUiState.Failed(
                "The signed development fixture could not be verified and opened. It remains inactive.",
            )
        }
    }

    private fun installAndOpen(): InstalledSession {
        val publicKey = KeyFactory.getInstance("Ed25519").generatePublic(
            X509EncodedKeySpec(PUBLIC_KEY_X509.hexBytes()),
        )
        val installer = androidVerifiedPackInstaller(
            root = context.filesDir.toPath().resolve("development-content-packs"),
            trustStore = StaticPackTrustStore(
                listOf(
                    TrustedPackKey(
                        publicKey = publicKey,
                        allowedPackIds = setOf(PACK_ID),
                        productionAllowed = false,
                    ),
                ),
            ),
            trustMode = PackTrustMode.DEVELOPMENT,
            supportedAppSchema = 1,
        )
        val archive = copyBundledPackToPrivateCache()
        try {
            val result = installer.install(archive)
            val store = checkNotNull(installer.activeContentStore(PACK_ID)) {
                "Verified pack activation did not expose an active store"
            }
            return InstalledSession(
                packId = result.installed.packId,
                version = result.installed.version.toString(),
                manifestSha256 = result.installed.manifestSha256,
                signingKeyId = result.installed.signingKeyId,
                search = AndroidInstalledContentSearch(store),
            )
        } finally {
            Files.deleteIfExists(archive)
        }
    }

    private fun copyBundledPackToPrivateCache() =
        Files.createTempFile(context.cacheDir.toPath(), "aseh-dev-pack-", ".pending").let { pending ->
            try {
                context.assets.open(ASSET_NAME).use { input ->
                    Files.newOutputStream(pending).use(input::copyTo)
                }
                val archive = pending.resolveSibling("${pending.fileName}.asehpack")
                Files.move(pending, archive, StandardCopyOption.ATOMIC_MOVE)
                archive
            } catch (error: Exception) {
                Files.deleteIfExists(pending)
                throw error
            }
        }

    private data class InstalledSession(
        val packId: String,
        val version: String,
        val manifestSha256: String,
        val signingKeyId: String,
        val search: AndroidInstalledContentSearch,
    )

    private companion object {
        const val ASSET_NAME = "aseh-development-fixture.asehpack"
        const val PACK_ID = "aseh.synthetic.rehearsal"
        const val PUBLIC_KEY_X509 =
            "302a300506032b6570032100" +
                "d75a980182b10ab7d54bfed3c964073a" +
                "0ee172f3daa62325af021a68f707511a"
    }
}

private fun String.hexBytes(): ByteArray {
    require(length % 2 == 0)
    return ByteArray(length / 2) { index ->
        substring(index * 2, index * 2 + 2).toInt(16).toByte()
    }
}
