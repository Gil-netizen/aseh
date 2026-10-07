package io.github.gilnetizen.aseh.core.content

import java.util.Base64

object ContentSourceJson {
    fun encode(source: EditorialPackSource): ByteArray = CanonicalJson.encode(source.toJson())

    fun decode(bytes: ByteArray): EditorialPackSource = fromJson(CanonicalJson.parse(bytes))

    private fun EditorialPackSource.toJson(): JsonValue = jsonObject(
        "schemaVersion" to jsonInteger(schemaVersion),
        "identity" to identity.toJson(),
        "compatibility" to compatibility.toJson(),
        "dependencies" to jsonArray(dependencies.map(PackDependency::toJson)),
        "provenance" to provenance.toJson(),
        "review" to review.toJson(),
        "rights" to rights.toJson(),
        "editions" to jsonArray(editions.map(EditionMetadata::toJson)),
        "documents" to jsonArray(documents.map(ContentDocument::toJson)),
        "attributionText" to jsonString(attributionText),
        "licenseText" to jsonString(licenseText),
    )

    private fun fromJson(value: JsonValue): EditorialPackSource {
        val objectValue = value.asObject("content source")
        objectValue.strictKeys(
            "content source",
            setOf(
                "schemaVersion",
                "identity",
                "compatibility",
                "dependencies",
                "provenance",
                "review",
                "rights",
                "editions",
                "documents",
                "attributionText",
                "licenseText",
            ),
        )
        return EditorialPackSource(
            schemaVersion = objectValue.required("schemaVersion", "content source").asInt("schemaVersion"),
            identity = parseIdentity(objectValue.required("identity", "content source")),
            compatibility = parseCompatibility(objectValue.required("compatibility", "content source")),
            dependencies = objectValue.required("dependencies", "content source")
                .asArray("dependencies").map(::parseDependency),
            provenance = parseProvenance(objectValue.required("provenance", "content source")),
            review = parseReview(objectValue.required("review", "content source")),
            rights = parseRights(objectValue.required("rights", "content source")),
            editions = objectValue.required("editions", "content source")
                .asArray("editions").map(::parseEdition),
            documents = objectValue.required("documents", "content source")
                .asArray("documents").map(::parseDocument),
            attributionText = objectValue.required("attributionText", "content source").asString("attributionText"),
            licenseText = objectValue.required("licenseText", "content source").asString("licenseText"),
        )
    }
}

object PackManifestJson {
    fun encode(manifest: PackManifest): ByteArray = CanonicalJson.encode(manifest.toJson())

    fun decodeCanonical(bytes: ByteArray): PackManifest = fromJson(CanonicalJson.requireCanonical(bytes))

    private fun PackManifest.toJson(): JsonValue = jsonObject(
        "formatVersion" to jsonInteger(formatVersion),
        "identity" to identity.toJson(),
        "compatibility" to compatibility.toJson(),
        "signingKeyId" to jsonString(signingKeyId),
        "dependencies" to jsonArray(dependencies.map(PackDependency::toJson)),
        "provenance" to provenance.toJson(),
        "review" to review.toJson(),
        "rights" to rights.toJson(),
        "editions" to jsonArray(editions.map(EditionMetadata::toJson)),
        "files" to jsonArray(files.map(PackFileRecord::toJson)),
    )

    private fun fromJson(value: JsonValue): PackManifest {
        val objectValue = value.asObject("pack manifest")
        objectValue.strictKeys(
            "pack manifest",
            setOf(
                "formatVersion",
                "identity",
                "compatibility",
                "signingKeyId",
                "dependencies",
                "provenance",
                "review",
                "rights",
                "editions",
                "files",
            ),
        )
        return PackManifest(
            formatVersion = objectValue.required("formatVersion", "pack manifest").asInt("formatVersion"),
            identity = parseIdentity(objectValue.required("identity", "pack manifest")),
            compatibility = parseCompatibility(objectValue.required("compatibility", "pack manifest")),
            signingKeyId = objectValue.required("signingKeyId", "pack manifest").asString("signingKeyId"),
            dependencies = objectValue.required("dependencies", "pack manifest")
                .asArray("dependencies").map(::parseDependency),
            provenance = parseProvenance(objectValue.required("provenance", "pack manifest")),
            review = parseReview(objectValue.required("review", "pack manifest")),
            rights = parseRights(objectValue.required("rights", "pack manifest")),
            editions = objectValue.required("editions", "pack manifest")
                .asArray("editions").map(::parseEdition),
            files = objectValue.required("files", "pack manifest")
                .asArray("files").map(::parseFileRecord),
        )
    }
}

object PackSignatureJson {
    fun encode(signature: PackSignature): ByteArray = CanonicalJson.encode(
        jsonObject(
            "formatVersion" to jsonInteger(signature.formatVersion),
            "algorithm" to jsonString(signature.algorithm),
            "signingKeyId" to jsonString(signature.signingKeyId),
            "signature" to jsonString(Base64.getEncoder().encodeToString(signature.signature)),
        ),
    )

    fun decodeCanonical(bytes: ByteArray): PackSignature {
        val objectValue = CanonicalJson.requireCanonical(bytes).asObject("signature")
        objectValue.strictKeys(
            "signature",
            setOf("formatVersion", "algorithm", "signingKeyId", "signature"),
        )
        val encoded = objectValue.required("signature", "signature").asString("signature.signature")
        val decoded = try {
            Base64.getDecoder().decode(encoded)
        } catch (error: IllegalArgumentException) {
            throw IllegalArgumentException("signature.signature is not valid base64", error)
        }
        require(Base64.getEncoder().encodeToString(decoded) == encoded) { "signature.signature is not canonical base64" }
        return PackSignature(
            formatVersion = objectValue.required("formatVersion", "signature").asInt("signature.formatVersion"),
            algorithm = objectValue.required("algorithm", "signature").asString("signature.algorithm"),
            signingKeyId = objectValue.required("signingKeyId", "signature").asString("signature.signingKeyId"),
            signature = decoded,
        )
    }
}

private fun PackIdentity.toJson(): JsonValue = jsonObject(
    "packId" to jsonString(packId),
    "version" to jsonString(version.toString()),
    "packType" to jsonString(packType.value),
)

private fun parseIdentity(value: JsonValue): PackIdentity {
    val objectValue = value.asObject("identity")
    objectValue.strictKeys("identity", setOf("packId", "version", "packType"))
    return PackIdentity(
        packId = objectValue.required("packId", "identity").asString("identity.packId"),
        version = SemanticVersion.parse(objectValue.required("version", "identity").asString("identity.version")),
        packType = PackType.parse(objectValue.required("packType", "identity").asString("identity.packType")),
    )
}

private fun SchemaCompatibility.toJson(): JsonValue = jsonObject(
    "packSchema" to jsonInteger(packSchema),
    "contentSchema" to jsonInteger(contentSchema),
    "minAppSchema" to jsonInteger(minAppSchema),
    "maxAppSchema" to jsonInteger(maxAppSchema),
)

private fun parseCompatibility(value: JsonValue): SchemaCompatibility {
    val objectValue = value.asObject("compatibility")
    objectValue.strictKeys(
        "compatibility",
        setOf("packSchema", "contentSchema", "minAppSchema", "maxAppSchema"),
    )
    return SchemaCompatibility(
        packSchema = objectValue.required("packSchema", "compatibility").asInt("compatibility.packSchema"),
        contentSchema = objectValue.required("contentSchema", "compatibility").asInt("compatibility.contentSchema"),
        minAppSchema = objectValue.required("minAppSchema", "compatibility").asInt("compatibility.minAppSchema"),
        maxAppSchema = objectValue.required("maxAppSchema", "compatibility").asInt("compatibility.maxAppSchema"),
    )
}

private fun PackDependency.toJson(): JsonValue = jsonObject(
    "packId" to jsonString(packId),
    "minVersion" to jsonString(minVersion.toString()),
    "maxVersion" to jsonString(maxVersion.toString()),
)

private fun parseDependency(value: JsonValue): PackDependency {
    val objectValue = value.asObject("dependency")
    objectValue.strictKeys("dependency", setOf("packId", "minVersion", "maxVersion"))
    return PackDependency(
        packId = objectValue.required("packId", "dependency").asString("dependency.packId"),
        minVersion = SemanticVersion.parse(
            objectValue.required("minVersion", "dependency").asString("dependency.minVersion"),
        ),
        maxVersion = SemanticVersion.parse(
            objectValue.required("maxVersion", "dependency").asString("dependency.maxVersion"),
        ),
    )
}

private fun ProvenanceMetadata.toJson(): JsonValue = jsonObject(
    "sourceTreeSha256" to jsonString(sourceTreeSha256),
    "retrievedAt" to jsonString(retrievedAt),
    "transformations" to jsonArray(transformations.map(::jsonString)),
)

private fun parseProvenance(value: JsonValue): ProvenanceMetadata {
    val objectValue = value.asObject("provenance")
    objectValue.strictKeys("provenance", setOf("sourceTreeSha256", "retrievedAt", "transformations"))
    return ProvenanceMetadata(
        sourceTreeSha256 = objectValue.required("sourceTreeSha256", "provenance")
            .asString("provenance.sourceTreeSha256"),
        retrievedAt = objectValue.required("retrievedAt", "provenance").asString("provenance.retrievedAt"),
        transformations = objectValue.required("transformations", "provenance")
            .asArray("provenance.transformations").map { it.asString("transformation") },
    )
}

private fun ReviewMetadata.toJson(): JsonValue = jsonObject(
    "state" to jsonString(state.wireName),
    "reviewer" to (reviewer?.let(::jsonString) ?: JsonValue.NullValue),
    "reviewedAt" to (reviewedAt?.let(::jsonString) ?: JsonValue.NullValue),
)

private fun parseReview(value: JsonValue): ReviewMetadata {
    val objectValue = value.asObject("review")
    objectValue.strictKeys("review", setOf("state", "reviewer", "reviewedAt"))
    return ReviewMetadata(
        state = ReviewState.parse(objectValue.required("state", "review").asString("review.state")),
        reviewer = objectValue.nullableString("reviewer", "review"),
        reviewedAt = objectValue.nullableString("reviewedAt", "review"),
    )
}

private fun RightsMetadata.toJson(): JsonValue = jsonObject(
    "licenseLabel" to jsonString(licenseLabel),
    "licenseUrl" to jsonString(licenseUrl),
    "attribution" to jsonString(attribution),
    "notice" to jsonString(notice),
    "redistributionAllowed" to jsonBoolean(redistributionAllowed),
    "derivativesAllowed" to jsonBoolean(derivativesAllowed),
    "commercialUseAllowed" to jsonBoolean(commercialUseAllowed),
    "rightsReviewed" to jsonBoolean(rightsReviewed),
)

private fun parseRights(value: JsonValue): RightsMetadata {
    val objectValue = value.asObject("rights")
    objectValue.strictKeys(
        "rights",
        setOf(
            "licenseLabel",
            "licenseUrl",
            "attribution",
            "notice",
            "redistributionAllowed",
            "derivativesAllowed",
            "commercialUseAllowed",
            "rightsReviewed",
        ),
    )
    return RightsMetadata(
        licenseLabel = objectValue.required("licenseLabel", "rights").asString("rights.licenseLabel"),
        licenseUrl = objectValue.required("licenseUrl", "rights").asString("rights.licenseUrl"),
        attribution = objectValue.required("attribution", "rights").asString("rights.attribution"),
        notice = objectValue.required("notice", "rights").asString("rights.notice"),
        redistributionAllowed = objectValue.required("redistributionAllowed", "rights")
            .asBoolean("rights.redistributionAllowed"),
        derivativesAllowed = objectValue.required("derivativesAllowed", "rights")
            .asBoolean("rights.derivativesAllowed"),
        commercialUseAllowed = objectValue.required("commercialUseAllowed", "rights")
            .asBoolean("rights.commercialUseAllowed"),
        rightsReviewed = objectValue.required("rightsReviewed", "rights").asBoolean("rights.rightsReviewed"),
    )
}

private fun EditionMetadata.toJson(): JsonValue = jsonObject(
    "editionId" to jsonString(editionId),
    "workId" to jsonString(workId),
    "language" to jsonString(language),
    "script" to jsonString(script),
    "versionTitle" to jsonString(versionTitle),
    "editionTitle" to jsonString(editionTitle),
    "contributor" to jsonString(contributor),
    "sourceUrl" to jsonString(sourceUrl),
    "rawSha256" to jsonString(rawSha256),
    "rights" to rights.toJson(),
    "review" to review.toJson(),
)

private fun parseEdition(value: JsonValue): EditionMetadata {
    val objectValue = value.asObject("edition")
    objectValue.strictKeys(
        "edition",
        setOf(
            "editionId",
            "workId",
            "language",
            "script",
            "versionTitle",
            "editionTitle",
            "contributor",
            "sourceUrl",
            "rawSha256",
            "rights",
            "review",
        ),
    )
    return EditionMetadata(
        editionId = objectValue.required("editionId", "edition").asString("edition.editionId"),
        workId = objectValue.required("workId", "edition").asString("edition.workId"),
        language = objectValue.required("language", "edition").asString("edition.language"),
        script = objectValue.required("script", "edition").asString("edition.script"),
        versionTitle = objectValue.required("versionTitle", "edition").asString("edition.versionTitle"),
        editionTitle = objectValue.required("editionTitle", "edition").asString("edition.editionTitle"),
        contributor = objectValue.required("contributor", "edition").asString("edition.contributor"),
        sourceUrl = objectValue.required("sourceUrl", "edition").asString("edition.sourceUrl"),
        rawSha256 = objectValue.required("rawSha256", "edition").asString("edition.rawSha256"),
        rights = parseRights(objectValue.required("rights", "edition")),
        review = parseReview(objectValue.required("review", "edition")),
    )
}

private fun CitationRef.toJson(): JsonValue = jsonObject(
    "sourceUnitId" to jsonString(sourceUnitId),
    "relation" to jsonString(relation.wireName),
)

private fun parseCitation(value: JsonValue): CitationRef {
    val objectValue = value.asObject("citation")
    objectValue.strictKeys("citation", setOf("sourceUnitId", "relation"))
    return CitationRef(
        sourceUnitId = objectValue.required("sourceUnitId", "citation").asString("citation.sourceUnitId"),
        relation = CitationRelation.parse(
            objectValue.required("relation", "citation").asString("citation.relation"),
        ),
    )
}

private fun ContentClaim.toJson(): JsonValue = jsonObject(
    "claimId" to jsonString(claimId),
    "text" to jsonString(text),
    "conclusionStatus" to jsonString(conclusionStatus.wireName),
    "reviewState" to jsonString(reviewState.wireName),
    "citations" to jsonArray(citations.map(CitationRef::toJson)),
)

private fun parseClaim(value: JsonValue): ContentClaim {
    val objectValue = value.asObject("claim")
    objectValue.strictKeys(
        "claim",
        setOf("claimId", "text", "conclusionStatus", "reviewState", "citations"),
    )
    return ContentClaim(
        claimId = objectValue.required("claimId", "claim").asString("claim.claimId"),
        text = objectValue.required("text", "claim").asString("claim.text"),
        conclusionStatus = ConclusionStatus.parse(
            objectValue.required("conclusionStatus", "claim").asString("claim.conclusionStatus"),
        ),
        reviewState = ReviewState.parse(
            objectValue.required("reviewState", "claim").asString("claim.reviewState"),
        ),
        citations = objectValue.required("citations", "claim").asArray("claim.citations").map(::parseCitation),
    )
}

private fun ContentDocument.toJson(): JsonValue = jsonObject(
    "contentId" to jsonString(contentId),
    "kind" to jsonString(kind.wireName),
    "language" to jsonString(language),
    "script" to jsonString(script),
    "title" to jsonString(title),
    "body" to jsonString(body),
    "normalizedBody" to jsonString(normalizedBody),
    "editionId" to (editionId?.let(::jsonString) ?: JsonValue.NullValue),
    "locator" to (locator?.let(::jsonString) ?: JsonValue.NullValue),
    "reviewState" to jsonString(reviewState.wireName),
    "prerequisiteContentIds" to jsonArray(prerequisiteContentIds.map(::jsonString)),
    "claims" to jsonArray(claims.map(ContentClaim::toJson)),
)

private fun parseDocument(value: JsonValue): ContentDocument {
    val objectValue = value.asObject("document")
    objectValue.strictKeys(
        "document",
        setOf(
            "contentId",
            "kind",
            "language",
            "script",
            "title",
            "body",
            "normalizedBody",
            "editionId",
            "locator",
            "reviewState",
            "prerequisiteContentIds",
            "claims",
        ),
    )
    return ContentDocument(
        contentId = objectValue.required("contentId", "document").asString("document.contentId"),
        kind = ContentKind.parse(objectValue.required("kind", "document").asString("document.kind")),
        language = objectValue.required("language", "document").asString("document.language"),
        script = objectValue.required("script", "document").asString("document.script"),
        title = objectValue.required("title", "document").asString("document.title"),
        body = objectValue.required("body", "document").asString("document.body"),
        normalizedBody = objectValue.required("normalizedBody", "document").asString("document.normalizedBody"),
        editionId = objectValue.nullableString("editionId", "document"),
        locator = objectValue.nullableString("locator", "document"),
        reviewState = ReviewState.parse(
            objectValue.required("reviewState", "document").asString("document.reviewState"),
        ),
        prerequisiteContentIds = objectValue.required("prerequisiteContentIds", "document")
            .asArray("document.prerequisiteContentIds").map { it.asString("prerequisiteContentId") },
        claims = objectValue.required("claims", "document").asArray("document.claims").map(::parseClaim),
    )
}

private fun PackFileRecord.toJson(): JsonValue = jsonObject(
    "path" to jsonString(path),
    "size" to jsonLong(size),
    "sha256" to jsonString(sha256),
)

private fun parseFileRecord(value: JsonValue): PackFileRecord {
    val objectValue = value.asObject("file")
    objectValue.strictKeys("file", setOf("path", "size", "sha256"))
    return PackFileRecord(
        path = objectValue.required("path", "file").asString("file.path"),
        size = objectValue.required("size", "file").asLong("file.size"),
        sha256 = objectValue.required("sha256", "file").asString("file.sha256"),
    )
}
