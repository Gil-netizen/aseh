package io.github.gilnetizen.aseh.core.content

import java.nio.charset.StandardCharsets
import java.security.MessageDigest

/** Immutable semantic version used for pack identities and dependency checks. */
data class SemanticVersion(
    val major: Int,
    val minor: Int,
    val patch: Int,
) : Comparable<SemanticVersion> {
    init {
        require(major >= 0 && minor >= 0 && patch >= 0) { "Version components must be non-negative" }
    }

    override fun compareTo(other: SemanticVersion): Int =
        compareValuesBy(this, other, SemanticVersion::major, SemanticVersion::minor, SemanticVersion::patch)

    override fun toString(): String = "$major.$minor.$patch"

    companion object {
        private val pattern = Regex("(?:0|[1-9][0-9]*)\\.(?:0|[1-9][0-9]*)\\.(?:0|[1-9][0-9]*)")

        fun parse(value: String): SemanticVersion {
            require(pattern.matches(value)) { "Pack versions must use canonical major.minor.patch syntax" }
            val parts = value.split('.').map(String::toInt)
            return SemanticVersion(parts[0], parts[1], parts[2])
        }
    }
}

/**
 * v0.1 pack type. Custom types must be namespaced so they cannot collide with
 * the project taxonomy.
 */
@JvmInline
value class PackType private constructor(val value: String) {
    companion object {
        private val canonical = setOf(
            "core-practice",
            "siddur-rambam",
            "primary-corpus-he",
            "translations-en",
            "geonica",
            "maimonidean-school",
            "visual-aids",
            "community-templates",
        )
        private val namespaced = Regex("[a-z][a-z0-9]*(?:[.-][a-z0-9]+)+/[a-z][a-z0-9-]*")

        fun parse(value: String): PackType {
            require(value in canonical || namespaced.matches(value)) {
                "Unknown pack type '$value'; custom types must be namespaced"
            }
            return PackType(value)
        }
    }
}

enum class ReviewState(val wireName: String) {
    PROPOSED("Proposed"),
    RESEARCHED("Researched"),
    DRAFTED("Drafted"),
    SOURCE_VERIFIED("SourceVerified"),
    HUMAN_REVIEWED("HumanReviewed"),
    APPROVED("Approved"),
    PUBLISHED("Published"),
    UNDER_REVIEW("UnderReview");

    companion object {
        fun parse(value: String): ReviewState =
            entries.singleOrNull { it.wireName == value }
                ?: throw IllegalArgumentException("Unknown review state '$value'")
    }
}

enum class ConclusionStatus(val wireName: String) {
    EXPLICIT("EXPLICIT"),
    INFERRED("INFERRED"),
    DISPUTED("DISPUTED"),
    UNRESOLVED("UNRESOLVED"),
    NOT_ESTABLISHED("NOT_ESTABLISHED");

    companion object {
        fun parse(value: String): ConclusionStatus =
            entries.singleOrNull { it.wireName == value }
                ?: throw IllegalArgumentException("Unknown conclusion status '$value'")
    }
}

enum class CitationRelation(val wireName: String) {
    EXPLICIT("explicit"),
    INFERRED("inferred"),
    CONTEXTUAL("contextual"),
    CONTRADICTING("contradicting"),
    BACKGROUND("background");

    companion object {
        fun parse(value: String): CitationRelation =
            entries.singleOrNull { it.wireName == value }
                ?: throw IllegalArgumentException("Unknown citation relation '$value'")
    }
}

enum class ContentKind(val wireName: String) {
    SOURCE_UNIT("source-unit"),
    PRACTICE_CARD("practice-card"),
    TEMPLATE("template"),
    DEVELOPMENT_FIXTURE("development-fixture");

    companion object {
        fun parse(value: String): ContentKind =
            entries.singleOrNull { it.wireName == value }
                ?: throw IllegalArgumentException("Unknown content kind '$value'")
    }
}

enum class PackBuildMode {
    DEVELOPMENT,
    PRODUCTION,
}

data class PackIdentity(
    val packId: String,
    val version: SemanticVersion,
    val packType: PackType,
)

data class SchemaCompatibility(
    val packSchema: Int,
    val contentSchema: Int,
    val minAppSchema: Int,
    val maxAppSchema: Int,
)

data class PackDependency(
    val packId: String,
    val minVersion: SemanticVersion,
    val maxVersion: SemanticVersion,
)

data class ProvenanceMetadata(
    val sourceTreeSha256: String,
    val retrievedAt: String,
    val transformations: List<String>,
)

data class ReviewMetadata(
    val state: ReviewState,
    val reviewer: String?,
    val reviewedAt: String?,
)

data class RightsMetadata(
    val licenseLabel: String,
    val licenseUrl: String,
    val attribution: String,
    val notice: String,
    val redistributionAllowed: Boolean,
    val derivativesAllowed: Boolean,
    val commercialUseAllowed: Boolean,
    val rightsReviewed: Boolean,
)

data class EditionMetadata(
    val editionId: String,
    val workId: String,
    val language: String,
    val script: String,
    val versionTitle: String,
    val editionTitle: String,
    val contributor: String,
    val sourceUrl: String,
    val rawSha256: String,
    val rights: RightsMetadata,
    val review: ReviewMetadata,
)

data class CitationRef(
    val sourceUnitId: String,
    val relation: CitationRelation,
)

data class ContentClaim(
    val claimId: String,
    val text: String,
    val conclusionStatus: ConclusionStatus,
    val reviewState: ReviewState,
    val citations: List<CitationRef>,
)

data class ContentDocument(
    val contentId: String,
    val kind: ContentKind,
    val language: String,
    val script: String,
    val title: String,
    val body: String,
    val normalizedBody: String,
    val editionId: String?,
    val locator: String?,
    val reviewState: ReviewState,
    val prerequisiteContentIds: List<String>,
    val claims: List<ContentClaim>,
)

/** Typed editorial input. The compiler never mutates or approves this data. */
data class EditorialPackSource(
    val schemaVersion: Int,
    val identity: PackIdentity,
    val compatibility: SchemaCompatibility,
    val dependencies: List<PackDependency>,
    val provenance: ProvenanceMetadata,
    val review: ReviewMetadata,
    val rights: RightsMetadata,
    val editions: List<EditionMetadata>,
    val documents: List<ContentDocument>,
    val attributionText: String,
    val licenseText: String,
)

data class PackFileRecord(
    val path: String,
    val size: Long,
    val sha256: String,
)

/** Exact signed manifest representation. */
data class PackManifest(
    val formatVersion: Int,
    val identity: PackIdentity,
    val compatibility: SchemaCompatibility,
    val signingKeyId: String,
    val dependencies: List<PackDependency>,
    val provenance: ProvenanceMetadata,
    val review: ReviewMetadata,
    val rights: RightsMetadata,
    val editions: List<EditionMetadata>,
    val files: List<PackFileRecord>,
)

data class PackSignature(
    val formatVersion: Int,
    val algorithm: String,
    val signingKeyId: String,
    val signature: ByteArray,
) {
    override fun equals(other: Any?): Boolean =
        other is PackSignature &&
            formatVersion == other.formatVersion &&
            algorithm == other.algorithm &&
            signingKeyId == other.signingKeyId &&
            signature.contentEquals(other.signature)

    override fun hashCode(): Int {
        var result = formatVersion
        result = 31 * result + algorithm.hashCode()
        result = 31 * result + signingKeyId.hashCode()
        return 31 * result + signature.contentHashCode()
    }
}

object ContentHashes {
    fun sha256(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256")
            .digest(bytes)
            .joinToString(separator = "") { byte -> "%02x".format(byte.toInt() and 0xff) }

    fun sourceTreeFingerprint(source: EditorialPackSource): String =
        sha256(ContentSourceJson.encode(source))

    fun utf8(value: String): ByteArray = value.toByteArray(StandardCharsets.UTF_8)
}
