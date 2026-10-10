package io.github.gilnetizen.aseh.core.model

import java.time.LocalTime

enum class IndividualPrayerKind {
    SHACHARIT,
    MINCHA,
    ARVIT,
}

/**
 * Returns a clock-based UX suggestion for the prayer chooser.
 *
 * These fixed local-time bands are only a navigation convenience. They do not establish that a
 * service is halakhically available, and the user can always choose another installed service.
 */
fun suggestedIndividualPrayerKind(localTime: LocalTime): IndividualPrayerKind = when {
    localTime >= LocalTime.of(4, 0) && localTime < LocalTime.NOON ->
        IndividualPrayerKind.SHACHARIT
    localTime >= LocalTime.NOON && localTime < LocalTime.of(18, 0) ->
        IndividualPrayerKind.MINCHA
    else -> IndividualPrayerKind.ARVIT
}

/**
 * One offline individual-prayer service in its fixed editorial order.
 *
 * The service deliberately stays separate from [DemonstratorCatalog]. Individual prayer does not
 * require a community choice, assigned roles, a reading plan, or a rehearsal checklist.
 */
data class IndividualPrayerService(
    val id: String,
    val kind: IndividualPrayerKind,
    val title: String,
    val subtitle: String,
    val notice: String,
    val segments: List<ServiceSegment>,
    val sources: List<SourceUnit>,
) {
    init {
        require(id.isNotBlank()) { "An individual prayer service requires an ID." }
        require(title.isNotBlank()) { "An individual prayer service requires a title." }
        require(subtitle.isNotBlank()) { "An individual prayer service requires a subtitle." }
        require(notice.isNotBlank()) { "An individual prayer service requires an editorial notice." }
        require(segments.isNotEmpty()) { "An individual prayer service requires at least one segment." }
        requireUnique(segments.map(ServiceSegment::id), "segment")
        requireUnique(sources.map(SourceUnit::id), "source")
    }
}

/** Builds the accessible structured-text equivalent used by share and print actions. */
fun buildIndividualPrayerPacket(
    service: IndividualPrayerService,
    completedSegmentIds: Set<String>,
    dateLabel: String,
    locationLabel: String,
): String = buildString {
    appendLine(service.title)
    appendLine(service.subtitle)
    appendLine(service.notice)
    appendLine()
    appendLine("CONTEXT")
    appendLine("Date: ${dateLabel.ifBlank { "Not recorded" }}")
    appendLine("Place: ${locationLabel.ifBlank { "Not recorded" }}")
    appendLine(
        "Progress: ${service.segments.count { segment -> segment.id in completedSegmentIds }} " +
            "of ${service.segments.size} segments complete",
    )
    appendLine()
    appendLine("PRAYER ORDER")

    service.segments.forEachIndexed { index, segment ->
        val completion = if (segment.id in completedSegmentIds) "COMPLETE" else "NOT COMPLETE"
        appendLine()
        appendLine("${index + 1}. ${segment.title} [$completion]")
        appendLine(segment.phase)
        segment.summary.takeIf(String::isNotBlank)?.let(::appendLine)
        appendPrayerField("Hebrew text", segment.content.sourceText)
        appendPrayerField("Translation", segment.content.translation)
        appendPrayerField("Transliteration", segment.content.transliteration)
        appendLine("Movement: ${segment.movementCue}")
        appendLine("Accessible alternative: ${segment.accessibleAlternative}")
        appendLine("Voice: ${segment.voiceCue}")
        segment.roleCues[ParticipantRole.CONGREGANT]
            ?.takeIf(String::isNotBlank)
            ?.let { cue -> appendLine("Individual cue: $cue") }

        val provenance = segment.content.provenance
        appendLine("Edition: ${provenance.editionTitle} (${provenance.editionId})")
        appendLine("Source unit: ${provenance.sourceUnitId} · ${provenance.locator}")
        appendLine("Editorial treatment: ${provenance.editorialTreatment}")
        appendLine("Review: ${provenance.reviewState.label} · ${provenance.conclusionStatus.label}")
        appendLine("License: ${provenance.license}")

        val sourceIds = (
            segment.explanation.sourceIds +
                provenance.sourceUnitId.takeIf { id ->
                    id.isNotBlank() && id != "Not established"
                }.orEmpty()
            ).distinct()
        if (sourceIds.isNotEmpty()) {
            appendLine("Sources: ${sourceIds.joinToString()}")
        }
    }
}

private fun StringBuilder.appendPrayerField(label: String, field: ServiceTextField) {
    when (field.availability) {
        ServiceTextAvailability.AVAILABLE -> {
            appendLine("$label${field.languageTag.takeIf(String::isNotBlank)?.let { " ($it)" }.orEmpty()}:")
            appendLine(field.text)
        }

        ServiceTextAvailability.UNAVAILABLE -> appendLine("$label: UNAVAILABLE — ${field.detail}")
        ServiceTextAvailability.NOT_APPLICABLE -> Unit
    }
}

private fun requireUnique(ids: List<String>, kind: String) {
    require(ids.none(String::isBlank)) { "Individual prayer $kind IDs cannot be blank." }
    val duplicates = ids.groupingBy { id -> id }.eachCount().filterValues { count -> count > 1 }.keys
    require(duplicates.isEmpty()) {
        "Individual prayer ${kind}s require unique IDs: ${duplicates.sorted().joinToString()}."
    }
}
