package io.github.gilnetizen.aseh.core.content

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class ContentSchemaValidationTest {
    @Test
    fun `synthetic fixture round trips through typed canonical source schema`() {
        val source = SyntheticPackFixture.source()

        ContentPolicyValidator.validate(source, PackBuildMode.DEVELOPMENT)
        val canonical = ContentSourceJson.encode(source)
        val decoded = ContentSourceJson.decode(canonical)

        assertEquals(source, decoded)
        assertTrue(canonical.contentEquals(ContentSourceJson.encode(decoded)))
        assertEquals(3, source.documents.size)
        assertTrue(source.rights.notice.contains("not a religious source"))
    }

    @Test
    fun `draft fixture cannot cross the production content gate`() {
        val error = assertThrows(ContentValidationException::class.java) {
            ContentPolicyValidator.validate(SyntheticPackFixture.source(), PackBuildMode.PRODUCTION)
        }

        assertTrue(error.violations.any { it.contains("must be Approved or Published") })
        assertTrue(error.violations.any { it.contains("rights review") })
    }

    @Test
    fun `unsupported and dangling claims fail closed`() {
        val source = SyntheticPackFixture.source()
        val guide = source.documents.single { it.contentId == "synthetic.guide.rehearsal" }
        val claim = guide.claims.single()
        val unsupported = source.copy(
            documents = source.documents.map {
                if (it.contentId == guide.contentId) it.copy(claims = listOf(claim.copy(citations = emptyList()))) else it
            },
        )
        val unsupportedError = assertThrows(ContentValidationException::class.java) {
            ContentPolicyValidator.validate(unsupported, PackBuildMode.DEVELOPMENT)
        }
        assertTrue(unsupportedError.violations.any { it.contains("no supporting citation") })

        val dangling = source.copy(
            documents = source.documents.map {
                if (it.contentId == guide.contentId) {
                    it.copy(
                        claims = listOf(
                            claim.copy(citations = listOf(CitationRef("synthetic.source.missing", CitationRelation.EXPLICIT))),
                        ),
                    )
                } else {
                    it
                }
            },
        )
        val danglingError = assertThrows(ContentValidationException::class.java) {
            ContentPolicyValidator.validate(dangling, PackBuildMode.DEVELOPMENT)
        }
        assertTrue(danglingError.violations.any { it.contains("dangling citation") })
    }

    @Test
    fun `cyclic prerequisites and executable content are rejected`() {
        val source = SyntheticPackFixture.source()
        val first = source.documents[0]
        val second = source.documents[1]
        val unsafe = source.copy(
            documents = source.documents.map { document ->
                when (document.contentId) {
                    first.contentId -> document.copy(prerequisiteContentIds = listOf(second.contentId))
                    second.contentId -> document.copy(
                        prerequisiteContentIds = listOf(first.contentId),
                        body = "<script>alert('fixture')</script>",
                    )
                    else -> document
                }
            },
        )

        val error = assertThrows(ContentValidationException::class.java) {
            ContentPolicyValidator.validate(unsafe, PackBuildMode.DEVELOPMENT)
        }

        assertTrue(error.violations.any { it.contains("Cyclic content prerequisites") })
        assertTrue(error.violations.any { it.contains("executable or unsafe content") })
    }

    @Test
    fun `strict parser rejects duplicate keys and unknown enum values`() {
        assertThrows(IllegalArgumentException::class.java) {
            CanonicalJson.parse("{\"a\":1,\"a\":2}".encodeToByteArray())
        }

        val sourceJson = ContentSourceJson.encode(SyntheticPackFixture.source()).decodeToString()
        val unknownKind = sourceJson.replaceFirst("\"development-fixture\"", "\"invented-kind\"")
        assertThrows(IllegalArgumentException::class.java) {
            ContentSourceJson.decode(unknownKind.encodeToByteArray())
        }
    }
}
