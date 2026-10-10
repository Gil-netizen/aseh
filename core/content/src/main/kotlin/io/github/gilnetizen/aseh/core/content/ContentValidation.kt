package io.github.gilnetizen.aseh.core.content

import java.net.URI
import java.text.Normalizer
import java.time.Instant

class ContentValidationException(val violations: List<String>) :
    IllegalArgumentException(violations.joinToString(prefix = "Content validation failed: ", separator = "; "))

object ContentPolicyValidator {
    const val CURRENT_PACK_FORMAT = 1
    const val CURRENT_CONTENT_SCHEMA = 1

    private val idPattern = Regex("[a-z][a-z0-9]*(?:[._-][a-z0-9]+)*")
    private val sha256Pattern = Regex("[0-9a-f]{64}")
    private val languagePattern = Regex("[a-z]{2,3}(?:-[A-Za-z0-9]{2,8})*")
    private val scriptPattern = Regex("[A-Z][a-z]{3}")
    private val unsafeText = Regex(
        pattern = "(?:<\\s*script\\b|javascript\\s*:|data\\s*:\\s*text/html|file\\s*://|intent\\s*://)",
        option = RegexOption.IGNORE_CASE,
    )

    fun validate(source: EditorialPackSource, mode: PackBuildMode) {
        val errors = mutableListOf<String>()
        validateIdentity(source.identity, errors)
        validateCompatibility(source.compatibility, errors)
        if (source.schemaVersion != CURRENT_CONTENT_SCHEMA) {
            errors += "Unsupported editorial source schema ${source.schemaVersion}"
        }
        validateDependencies(source.dependencies, errors)
        validateProvenance(source.provenance, errors)
        validateReview(source.review, "pack review", mode, errors)
        validateRights(source.rights, "pack rights", mode, errors)
        requireText(source.attributionText, "attribution text", errors)
        requireText(source.licenseText, "license text", errors)

        val editions = source.editions.associateBy { it.editionId }
        if (editions.size != source.editions.size) errors += "Edition IDs must be unique"
        source.editions.forEach { edition -> validateEdition(edition, mode, errors) }

        val documents = source.documents.associateBy { it.contentId }
        if (documents.size != source.documents.size) errors += "Content IDs must be unique"
        if (documents.isEmpty()) errors += "At least one content document is required"
        val sourceUnits = source.documents.filter { it.kind == ContentKind.SOURCE_UNIT }.associateBy { it.contentId }
        val claimIds = mutableSetOf<String>()

        source.documents.forEach { document ->
            validateDocument(document, editions, documents, sourceUnits, claimIds, mode, errors)
        }
        validateAcyclicPrerequisites(documents, errors)

        if (errors.isNotEmpty()) throw ContentValidationException(errors.sorted())
    }

    fun validateManifest(manifest: PackManifest, production: Boolean) {
        val errors = mutableListOf<String>()
        if (manifest.formatVersion != CURRENT_PACK_FORMAT) errors += "Unsupported pack format ${manifest.formatVersion}"
        validateIdentity(manifest.identity, errors)
        validateCompatibility(manifest.compatibility, errors)
        validateSha(manifest.signingKeyId, "signing key ID", errors)
        validateDependencies(manifest.dependencies, errors)
        validateProvenance(manifest.provenance, errors)
        val mode = if (production) PackBuildMode.PRODUCTION else PackBuildMode.DEVELOPMENT
        validateReview(manifest.review, "pack review", mode, errors)
        validateRights(manifest.rights, "pack rights", mode, errors)
        manifest.editions.forEach { validateEdition(it, mode, errors) }
        if (manifest.editions.map { it.editionId }.distinct().size != manifest.editions.size) {
            errors += "Edition IDs must be unique"
        }
        if (manifest.files.isEmpty()) errors += "Manifest file inventory cannot be empty"
        if (manifest.files != manifest.files.sortedBy { it.path }) errors += "Manifest files must use lexicographic path order"
        if (manifest.files.map { it.path }.distinct().size != manifest.files.size) errors += "Manifest file paths must be unique"
        manifest.files.forEach { file ->
            validatePackPath(file.path, errors)
            if (file.path == "manifest.json" || file.path == "signature.json") {
                errors += "Manifest must not inventory ${file.path}"
            }
            if (file.size < 0) errors += "Negative file size for ${file.path}"
            validateSha(file.sha256, "SHA-256 for ${file.path}", errors)
        }
        if (errors.isNotEmpty()) throw ContentValidationException(errors.sorted())
    }

    fun validatePackPath(path: String) {
        val errors = mutableListOf<String>()
        validatePackPath(path, errors)
        if (errors.isNotEmpty()) throw ContentValidationException(errors)
    }

    private fun validatePackPath(path: String, errors: MutableList<String>) {
        if (path.isBlank()) errors += "Pack path cannot be blank"
        if (path.startsWith('/') || Regex("^[A-Za-z]:").containsMatchIn(path)) errors += "Absolute pack path '$path'"
        if ('\\' in path) errors += "Backslash in pack path '$path'"
        if (path.endsWith('/') || path.split('/').any { it.isBlank() || it == "." || it == ".." }) {
            errors += "Unsafe pack path '$path'"
        }
        if (Normalizer.normalize(path, Normalizer.Form.NFC) != path) errors += "Non-NFC pack path '$path'"
        if (path.any { it.code < 0x20 || it.code == 0x7f }) errors += "Control character in pack path"
    }

    private fun validateIdentity(identity: PackIdentity, errors: MutableList<String>) {
        if (!idPattern.matches(identity.packId)) errors += "Invalid pack ID '${identity.packId}'"
        runCatching { SemanticVersion.parse(identity.version.toString()) }
            .onFailure { errors += "Invalid semantic version ${identity.version}" }
        runCatching { PackType.parse(identity.packType.value) }
            .onFailure { errors += it.message ?: "Invalid pack type" }
    }

    private fun validateCompatibility(value: SchemaCompatibility, errors: MutableList<String>) {
        if (value.packSchema <= 0 || value.contentSchema <= 0 || value.minAppSchema <= 0 || value.maxAppSchema <= 0) {
            errors += "Schema compatibility values must be positive"
        }
        if (value.minAppSchema > value.maxAppSchema) errors += "minAppSchema must not exceed maxAppSchema"
    }

    private fun validateDependencies(values: List<PackDependency>, errors: MutableList<String>) {
        if (values.map { it.packId }.distinct().size != values.size) errors += "Pack dependencies must be unique"
        values.forEach { dependency ->
            if (!idPattern.matches(dependency.packId)) errors += "Invalid dependency pack ID '${dependency.packId}'"
            if (dependency.minVersion > dependency.maxVersion) {
                errors += "Dependency ${dependency.packId} has an inverted version range"
            }
        }
    }

    private fun validateProvenance(value: ProvenanceMetadata, errors: MutableList<String>) {
        validateSha(value.sourceTreeSha256, "source tree SHA-256", errors)
        if (runCatching { Instant.parse(value.retrievedAt) }.isFailure) errors += "retrievedAt must be an ISO-8601 instant"
        if (value.transformations.isEmpty() || value.transformations.any(String::isBlank)) {
            errors += "At least one named transformation is required"
        }
    }

    private fun validateEdition(
        edition: EditionMetadata,
        mode: PackBuildMode,
        errors: MutableList<String>,
    ) {
        if (!idPattern.matches(edition.editionId)) errors += "Invalid edition ID '${edition.editionId}'"
        if (!idPattern.matches(edition.workId)) errors += "Invalid work ID '${edition.workId}'"
        validateLocale(edition.language, edition.script, "edition ${edition.editionId}", errors)
        requireText(edition.versionTitle, "edition ${edition.editionId} version title", errors)
        requireText(edition.editionTitle, "edition ${edition.editionId} title", errors)
        requireText(edition.contributor, "edition ${edition.editionId} contributor", errors)
        validateHttpUrl(edition.sourceUrl, "edition ${edition.editionId} source URL", errors)
        validateSha(edition.rawSha256, "edition ${edition.editionId} raw SHA-256", errors)
        validateRights(edition.rights, "edition ${edition.editionId} rights", mode, errors)
        validateReview(edition.review, "edition ${edition.editionId} review", mode, errors)
    }

    private fun validateDocument(
        document: ContentDocument,
        editions: Map<String, EditionMetadata>,
        documents: Map<String, ContentDocument>,
        sourceUnits: Map<String, ContentDocument>,
        claimIds: MutableSet<String>,
        mode: PackBuildMode,
        errors: MutableList<String>,
    ) {
        if (!idPattern.matches(document.contentId)) errors += "Invalid content ID '${document.contentId}'"
        validateLocale(document.language, document.script, "content ${document.contentId}", errors)
        requireSafeText(document.title, "content ${document.contentId} title", errors)
        requireSafeText(document.body, "content ${document.contentId} body", errors)
        requireSafeText(document.normalizedBody, "content ${document.contentId} normalized body", errors)
        validateItemReview(document.reviewState, "content ${document.contentId}", mode, errors)

        if (document.kind == ContentKind.SOURCE_UNIT) {
            if (document.editionId == null || document.editionId !in editions) {
                errors += "Source unit ${document.contentId} must resolve to a declared edition"
            }
            if (document.locator.isNullOrBlank()) errors += "Source unit ${document.contentId} requires a locator"
        } else if (document.locator != null && document.editionId == null) {
            errors += "Content ${document.contentId} has a locator without an edition"
        }
        if (document.editionId != null && document.editionId !in editions) {
            errors += "Content ${document.contentId} references missing edition ${document.editionId}"
        }
        document.prerequisiteContentIds.forEach { prerequisite ->
            if (prerequisite !in documents) errors += "Content ${document.contentId} has missing prerequisite $prerequisite"
            if (prerequisite == document.contentId) errors += "Content ${document.contentId} cannot require itself"
        }
        if (document.prerequisiteContentIds.distinct().size != document.prerequisiteContentIds.size) {
            errors += "Content ${document.contentId} repeats a prerequisite"
        }

        document.claims.forEach { claim ->
            if (!idPattern.matches(claim.claimId)) errors += "Invalid claim ID '${claim.claimId}'"
            if (!claimIds.add(claim.claimId)) errors += "Claim ID ${claim.claimId} is duplicated"
            requireSafeText(claim.text, "claim ${claim.claimId}", errors)
            validateItemReview(claim.reviewState, "claim ${claim.claimId}", mode, errors)
            if (claim.conclusionStatus != ConclusionStatus.NOT_ESTABLISHED && claim.citations.isEmpty()) {
                errors += "Claim ${claim.claimId} has no supporting citation"
            }
            if (claim.citations.distinct().size != claim.citations.size) {
                errors += "Claim ${claim.claimId} repeats a citation"
            }
            claim.citations.forEach { citation ->
                if (citation.sourceUnitId !in sourceUnits) {
                    errors += "Claim ${claim.claimId} has dangling citation ${citation.sourceUnitId}"
                }
            }
        }
    }

    private fun validateAcyclicPrerequisites(
        documents: Map<String, ContentDocument>,
        errors: MutableList<String>,
    ) {
        val visiting = mutableSetOf<String>()
        val visited = mutableSetOf<String>()

        fun visit(id: String, path: List<String>) {
            if (id in visited || id !in documents) return
            if (!visiting.add(id)) {
                errors += "Cyclic content prerequisites: ${(path + id).joinToString(" -> ")}"
                return
            }
            documents.getValue(id).prerequisiteContentIds.sorted().forEach { dependency ->
                visit(dependency, path + id)
            }
            visiting -= id
            visited += id
        }

        documents.keys.sorted().forEach { visit(it, emptyList()) }
    }

    private fun validateReview(
        review: ReviewMetadata,
        context: String,
        mode: PackBuildMode,
        errors: MutableList<String>,
    ) {
        if (review.reviewer != null && review.reviewer.isBlank()) errors += "$context reviewer cannot be blank"
        if (review.reviewedAt != null && runCatching { Instant.parse(review.reviewedAt) }.isFailure) {
            errors += "$context reviewedAt must be an ISO-8601 instant"
        }
        val needsHumanRecord = review.state in setOf(
            ReviewState.HUMAN_REVIEWED,
            ReviewState.APPROVED,
            ReviewState.PUBLISHED,
            ReviewState.UNDER_REVIEW,
        )
        if (needsHumanRecord && (review.reviewer.isNullOrBlank() || review.reviewedAt.isNullOrBlank())) {
            errors += "$context state ${review.state.wireName} requires a named reviewer and review date"
        }
        if (mode == PackBuildMode.PRODUCTION && review.state !in setOf(ReviewState.APPROVED, ReviewState.PUBLISHED)) {
            errors += "$context must be Approved or Published for production"
        }
    }

    private fun validateItemReview(
        reviewState: ReviewState,
        context: String,
        mode: PackBuildMode,
        errors: MutableList<String>,
    ) {
        if (mode == PackBuildMode.PRODUCTION && reviewState !in setOf(ReviewState.APPROVED, ReviewState.PUBLISHED)) {
            errors += "$context must be Approved or Published for production"
        }
    }

    private fun validateRights(
        rights: RightsMetadata,
        context: String,
        mode: PackBuildMode,
        errors: MutableList<String>,
    ) {
        requireText(rights.licenseLabel, "$context license label", errors)
        validateHttpUrl(rights.licenseUrl, "$context license URL", errors)
        requireText(rights.attribution, "$context attribution", errors)
        requireText(rights.notice, "$context notice", errors)
        if (mode == PackBuildMode.PRODUCTION) {
            if (!rights.rightsReviewed) errors += "$context has not completed rights review"
            if (!rights.redistributionAllowed) errors += "$context does not allow redistribution"
            if (!rights.derivativesAllowed) errors += "$context does not allow generated pack derivatives"
            if (!rights.commercialUseAllowed) errors += "$context does not allow the configured production use"
        }
    }

    private fun validateLocale(language: String, script: String, context: String, errors: MutableList<String>) {
        if (!languagePattern.matches(language)) errors += "$context has invalid language tag '$language'"
        if (!scriptPattern.matches(script)) errors += "$context has invalid script '$script'"
        val base = language.substringBefore('-')
        if (base == "he" && script != "Hebr") errors += "$context Hebrew content must declare Hebr script"
        if (base == "ar" && script != "Arab") errors += "$context Arabic content must declare Arab script"
        if (base == "en" && script != "Latn") errors += "$context English content must declare Latn script"
    }

    private fun validateHttpUrl(value: String, context: String, errors: MutableList<String>) {
        val uri = runCatching { URI(value) }.getOrNull()
        if (uri == null || uri.scheme !in setOf("https", "http") || uri.host.isNullOrBlank() || uri.userInfo != null) {
            errors += "$context must be an absolute HTTP(S) URL without credentials"
        }
    }

    private fun validateSha(value: String, context: String, errors: MutableList<String>) {
        if (!sha256Pattern.matches(value)) errors += "$context must be 64 lowercase hexadecimal characters"
    }

    private fun requireSafeText(value: String, context: String, errors: MutableList<String>) {
        requireText(value, context, errors)
        if (unsafeText.containsMatchIn(value)) errors += "$context contains executable or unsafe content"
        if (value.any { it == '\u0000' || (it.code < 0x20 && it !in setOf('\t', '\n', '\r')) }) {
            errors += "$context contains forbidden control characters"
        }
    }

    private fun requireText(value: String, context: String, errors: MutableList<String>) {
        if (value.isBlank()) errors += "$context cannot be blank"
    }
}
