package io.github.gilnetizen.aseh.core.content

import java.nio.file.Files
import java.nio.file.Path
import java.security.KeyPair
import java.security.KeyPairGenerator

internal object SyntheticPackFixture {
    fun source(): EditorialPackSource {
        val bytes = checkNotNull(javaClass.getResourceAsStream("/fixtures/synthetic-pack/content-source.json")) {
            "Missing synthetic fixture"
        }.use { it.readAllBytes() }
        return ContentSourceJson.decode(bytes)
    }

    fun keyPair(): KeyPair = KeyPairGenerator.getInstance("Ed25519").generateKeyPair()

    fun signer(keyPair: KeyPair): JdkEd25519PackSigner = JdkEd25519PackSigner.fromKeyPair(keyPair)

    fun trustStore(keyPair: KeyPair): StaticPackTrustStore = StaticPackTrustStore(
        listOf(
            TrustedPackKey(
                publicKey = keyPair.public,
                allowedPackIds = setOf(source().identity.packId),
                productionAllowed = false,
            ),
        ),
    )

    fun compile(source: EditorialPackSource, keyPair: KeyPair, output: Path): PackCompilationResult =
        DeterministicPackCompiler(JdbcDeterministicContentDatabaseCompiler()).compile(
            source = source,
            signer = signer(keyPair),
            output = output,
            mode = PackBuildMode.DEVELOPMENT,
        )

    fun tempDirectory(prefix: String): Path = Files.createTempDirectory(prefix)
}
