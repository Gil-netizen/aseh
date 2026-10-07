package io.github.gilnetizen.aseh.core.content

import java.io.ByteArrayOutputStream
import java.security.KeyPair
import java.security.PublicKey
import java.security.Signature

private val SIGNATURE_DOMAIN = ContentHashes.utf8("ASEH-PACK-SIGNATURE-v1")

fun packSigningMessage(canonicalManifest: ByteArray): ByteArray =
    ByteArrayOutputStream(SIGNATURE_DOMAIN.size + 1 + canonicalManifest.size).use { output ->
        output.write(SIGNATURE_DOMAIN)
        output.write(0)
        output.write(canonicalManifest)
        output.toByteArray()
    }

interface PackSigner {
    val keyId: String

    fun sign(message: ByteArray): ByteArray
}

class JdkEd25519PackSigner private constructor(
    private val keyPair: KeyPair,
    override val keyId: String,
) : PackSigner {
    override fun sign(message: ByteArray): ByteArray = Signature.getInstance("Ed25519").run {
        initSign(keyPair.private)
        update(message)
        sign()
    }

    companion object {
        fun fromKeyPair(keyPair: KeyPair): JdkEd25519PackSigner {
            require(keyPair.public.algorithm.equals("EdDSA", ignoreCase = true) ||
                keyPair.public.algorithm.equals("Ed25519", ignoreCase = true)) {
                "Pack signing requires an Ed25519 key pair"
            }
            return JdkEd25519PackSigner(keyPair, Ed25519Keys.keyId(keyPair.public))
        }
    }
}

interface PackSignatureVerifier {
    fun verify(publicKey: PublicKey, message: ByteArray, signature: ByteArray): Boolean
}

object JdkEd25519SignatureVerifier : PackSignatureVerifier {
    override fun verify(publicKey: PublicKey, message: ByteArray, signature: ByteArray): Boolean {
        if (signature.size != 64) return false
        return runCatching {
            Signature.getInstance("Ed25519").run {
                initVerify(publicKey)
                update(message)
                verify(signature)
            }
        }.getOrDefault(false)
    }
}

object Ed25519Keys {
    private val subjectPublicKeyInfoPrefix = byteArrayOf(
        0x30, 0x2a, 0x30, 0x05, 0x06, 0x03, 0x2b, 0x65, 0x70, 0x03, 0x21, 0x00,
    )

    fun rawPublicKey(publicKey: PublicKey): ByteArray {
        val encoded = publicKey.encoded
        require(encoded.size == subjectPublicKeyInfoPrefix.size + 32 &&
            encoded.copyOfRange(0, subjectPublicKeyInfoPrefix.size).contentEquals(subjectPublicKeyInfoPrefix)) {
            "Expected a standard raw Ed25519 SubjectPublicKeyInfo key"
        }
        return encoded.copyOfRange(subjectPublicKeyInfoPrefix.size, encoded.size)
    }

    fun keyId(publicKey: PublicKey): String = ContentHashes.sha256(rawPublicKey(publicKey))
}

enum class PackTrustMode {
    DEVELOPMENT,
    PRODUCTION,
}

data class TrustedPackKey(
    val publicKey: PublicKey,
    val allowedPackIds: Set<String> = emptySet(),
    val allowedPackTypes: Set<String> = emptySet(),
    val productionAllowed: Boolean = false,
) {
    val keyId: String = Ed25519Keys.keyId(publicKey)

    fun permits(identity: PackIdentity, mode: PackTrustMode): Boolean {
        if (mode == PackTrustMode.PRODUCTION && !productionAllowed) return false
        if (allowedPackIds.isNotEmpty() && identity.packId !in allowedPackIds) return false
        if (allowedPackTypes.isNotEmpty() && identity.packType.value !in allowedPackTypes) return false
        return true
    }
}

interface PackTrustStore {
    fun resolve(signingKeyId: String, identity: PackIdentity, mode: PackTrustMode): PublicKey?
}

class StaticPackTrustStore(keys: Iterable<TrustedPackKey>) : PackTrustStore {
    private val keyList = keys.toList()
    private val keysById = keyList.associateBy(TrustedPackKey::keyId)

    init {
        require(keysById.size == keyList.size) { "Trusted pack key IDs must be unique" }
    }

    override fun resolve(signingKeyId: String, identity: PackIdentity, mode: PackTrustMode): PublicKey? =
        keysById[signingKeyId]?.takeIf { it.permits(identity, mode) }?.publicKey
}
