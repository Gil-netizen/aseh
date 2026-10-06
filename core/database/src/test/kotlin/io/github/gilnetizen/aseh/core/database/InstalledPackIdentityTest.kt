package io.github.gilnetizen.aseh.core.database

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class InstalledPackIdentityTest {
    @Test
    fun acceptsCanonicalCatalogIdentity() {
        val identity = InstalledPackIdentity(
            packId = "aseh.core",
            immutableVersion = "1.0.0",
            manifestSha256 = "a".repeat(64),
            schemaVersion = 1,
            signingKeyId = "b".repeat(64),
        )

        assertEquals("aseh.core", identity.packId)
    }

    @Test
    fun rejectsNonCanonicalDigests() {
        assertThrows(IllegalArgumentException::class.java) {
            InstalledPackIdentity(
                packId = "aseh.core",
                immutableVersion = "1.0.0",
                manifestSha256 = "A".repeat(64),
                schemaVersion = 1,
                signingKeyId = "b".repeat(64),
            )
        }
    }
}
