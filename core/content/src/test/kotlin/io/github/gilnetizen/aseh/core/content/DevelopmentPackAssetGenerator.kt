package io.github.gilnetizen.aseh.core.content

import java.nio.file.Files
import java.nio.file.Path
import java.security.KeyFactory
import java.security.KeyPair
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec

/** Build-time generator for the synthetic pack bundled only in the dev flavor. */
object DevelopmentPackAssetGenerator {
    private const val PRIVATE_KEY_PKCS8 =
        "302e020100300506032b657004220420" +
            "9d61b19deffd5a60ba844af492ec2cc4" +
            "4449c5697b326919703bac031cae7f60"
    private const val PUBLIC_KEY_X509 =
        "302a300506032b6570032100" +
            "d75a980182b10ab7d54bfed3c964073a" +
            "0ee172f3daa62325af021a68f707511a"

    /** RFC 8032 test vector 1. It is public test material, never a release signing key. */
    fun syntheticKeyPair(): KeyPair {
        val factory = KeyFactory.getInstance("Ed25519")
        return KeyPair(
            factory.generatePublic(X509EncodedKeySpec(PUBLIC_KEY_X509.hexBytes())),
            factory.generatePrivate(PKCS8EncodedKeySpec(PRIVATE_KEY_PKCS8.hexBytes())),
        )
    }

    @JvmStatic
    fun main(args: Array<String>) {
        require(args.size == 1) { "Expected one output path" }
        val output = Path.of(args.single()).toAbsolutePath()
        Files.createDirectories(output.parent)
        Files.deleteIfExists(output)
        val keyPair = syntheticKeyPair()
        DeterministicPackCompiler(JdbcDeterministicContentDatabaseCompiler()).compile(
            source = SyntheticPackFixture.source(),
            signer = JdkEd25519PackSigner.fromKeyPair(keyPair),
            output = output,
            mode = PackBuildMode.DEVELOPMENT,
        )
    }
}

private fun String.hexBytes(): ByteArray {
    require(length % 2 == 0)
    return ByteArray(length / 2) { index ->
        substring(index * 2, index * 2 + 2).toInt(16).toByte()
    }
}
