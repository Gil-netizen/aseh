package io.github.gilnetizen.aseh.core.content.runtime

import io.github.gilnetizen.aseh.core.content.JdkEd25519SignatureVerifier
import io.github.gilnetizen.aseh.core.content.PackInstallLimits
import io.github.gilnetizen.aseh.core.content.PackSignatureVerifier
import io.github.gilnetizen.aseh.core.content.PackTrustMode
import io.github.gilnetizen.aseh.core.content.PackTrustStore
import io.github.gilnetizen.aseh.core.content.VerifiedPackInstaller
import java.nio.file.Path
import java.security.KeyFactory
import java.security.PublicKey
import java.security.Signature

class PackCryptoUnavailableException(message: String, cause: Throwable? = null) :
    IllegalStateException(message, cause)

object AndroidPackCryptoReadiness {
    fun isEd25519Available(): Boolean = runCatching {
        Signature.getInstance("Ed25519")
        KeyFactory.getInstance("Ed25519")
    }.isSuccess

    fun requireEd25519() {
        try {
            Signature.getInstance("Ed25519")
            KeyFactory.getInstance("Ed25519")
        } catch (error: Exception) {
            throw PackCryptoUnavailableException(
                "This Android runtime has no JCA Ed25519 provider; signed content-pack installation is disabled",
                error,
            )
        }
    }
}

/** Preserves the Ed25519 algorithm and fails explicitly when the platform cannot provide it. */
object AndroidJcaEd25519SignatureVerifier : PackSignatureVerifier {
    override fun verify(publicKey: PublicKey, message: ByteArray, signature: ByteArray): Boolean {
        AndroidPackCryptoReadiness.requireEd25519()
        return JdkEd25519SignatureVerifier.verify(publicKey, message, signature)
    }
}

/**
 * Constructs the Android installer only when its cryptographic and SQLite
 * verification boundaries are available. API 26 currently fails here closed.
 */
fun androidVerifiedPackInstaller(
    root: Path,
    trustStore: PackTrustStore,
    trustMode: PackTrustMode,
    supportedAppSchema: Int,
    limits: PackInstallLimits = PackInstallLimits(),
): VerifiedPackInstaller {
    AndroidPackCryptoReadiness.requireEd25519()
    return VerifiedPackInstaller(
        root = root,
        trustStore = trustStore,
        trustMode = trustMode,
        supportedAppSchema = supportedAppSchema,
        limits = limits,
        signatureVerifier = AndroidJcaEd25519SignatureVerifier,
        contentDatabaseVerifier = AndroidInstalledContentDatabaseVerifier,
    )
}
