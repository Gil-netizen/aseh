package io.github.gilnetizen.aseh.core.model

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExperienceModelsTest {
    @Test
    fun `conclusion evidence and editorial review use separate policy vocabularies`() {
        assertEquals(
            listOf(
                "Source explicit",
                "Authority explicit",
                "Strong synthesis",
                "Plausible synthesis",
                "Analogical application",
                "Editorial proposal",
                "Community enactment",
                "Personal discipline",
                "Disputed",
                "Unresolved",
            ),
            ConclusionStatus.entries.map(ConclusionStatus::label),
        )
        assertEquals(
            listOf(
                "Proposed",
                "Researched",
                "Drafted",
                "Source verified",
                "Human reviewed",
                "Approved",
                "Published",
                "Under review",
            ),
            EditorialReviewState.entries.map(EditorialReviewState::label),
        )
        assertEquals(ConclusionStatus.UNRESOLVED, ServiceTextProvenance().conclusionStatus)
        assertEquals(EditorialReviewState.PROPOSED, ServiceTextProvenance().reviewState)
    }

    @Test
    fun `next Shabbat keeps Saturday and advances other days`() {
        assertEquals(LocalDate.of(2026, 10, 10), nextShabbat(LocalDate.of(2026, 10, 6)))
        assertEquals(LocalDate.of(2026, 10, 10), nextShabbat(LocalDate.of(2026, 10, 10)))
    }

    @Test
    fun `service instance rollover fails closed without erasing durable workspace data`() {
        val previousDate = LocalDate.of(2026, 10, 10)
        val nextDate = LocalDate.of(2026, 10, 17)
        val state = ExperienceState(
            serviceInstanceDate = previousDate,
            completedPracticeStepIds = setOf("practice.weekly"),
            savedPracticeCardIds = setOf("practice.saved"),
            completedPreflightStepIds = setOf("preflight.weekly"),
            completedServiceSegmentIds = setOf("segment.weekly"),
            bookmarkedSourceIds = setOf("source.saved"),
            workspaceName = "Harimon",
            roleAssignments = mapOf(ParticipantRole.LEADER to "Ari"),
            readingAssignments = mapOf("reading.1" to "Miriam"),
            readingPlans = mapOf(
                "reading.1" to ReadingPlanEntry(
                    assignee = "Miriam",
                    preparationStatus = ReadingPreparationStatus.READY,
                ),
            ),
            communityCharter = CommunityCharter(purpose = "Durable charter draft"),
            reviewedDossierFactIds = setOf("dossier.fact.saved"),
        )

        assertEquals(state, state.forServiceInstance(previousDate))

        val rolled = state.forServiceInstance(nextDate)
        assertEquals(nextDate, rolled.serviceInstanceDate)
        assertTrue(rolled.completedPracticeStepIds.isEmpty())
        assertTrue(rolled.completedPreflightStepIds.isEmpty())
        assertTrue(rolled.completedServiceSegmentIds.isEmpty())
        assertTrue(rolled.roleAssignments.isEmpty())
        assertTrue(rolled.readingAssignments.isEmpty())
        assertTrue(rolled.readingPlans.isEmpty())
        assertEquals(setOf("practice.saved"), rolled.savedPracticeCardIds)
        assertEquals(setOf("source.saved"), rolled.bookmarkedSourceIds)
        assertEquals("Harimon", rolled.workspaceName)
        assertEquals("Durable charter draft", rolled.communityCharter.purpose)
        assertEquals(setOf("dossier.fact.saved"), rolled.reviewedDossierFactIds)
    }

    @Test
    fun `progress ignores stale record ids`() {
        val catalog = minimalCatalog()
        val state = ExperienceState(
            completedPracticeStepIds = setOf("step.known", "step.removed"),
            completedServiceSegmentIds = setOf("segment.known", "segment.removed"),
        )

        assertEquals(1 to 1, practiceProgress(catalog, state))
        assertEquals(1 to 1, serviceProgress(catalog, state))
    }

    @Test
    fun `assembled service progress ignores contextually omitted segments`() {
        val base = minimalCatalog()
        val conditional = base.service.segments.single().copy(
            id = "segment.contextual",
            inclusionPolicy = SegmentInclusionPolicy(
                condition = ServiceAssemblyCondition.CalendarRegionIn(setOf(CalendarRegion.ISRAEL)),
                includedReason = "Included for the Israel test context.",
                omittedReason = "Omitted outside the Israel test context.",
            ),
        )
        val catalog = base.copy(
            service = base.service.copy(
                segments = base.service.segments + conditional,
            ),
        )
        val state = ExperienceState(
            completedServiceSegmentIds = setOf("segment.contextual"),
        )
        val assembly = assembleService(
            catalog = catalog,
            state = state,
            dateLabel = "Saturday, October 10",
            locationLabel = "Test place",
        )

        assertEquals(listOf("segment.known"), assembly.segments.map { it.segment.id })
        assertEquals(0 to 1, serviceProgress(assembly, state))
    }

    @Test
    fun `service packet leaves unresolved assignments and choice visible`() {
        val catalog = minimalCatalog()

        val packet = buildServicePacket(
            catalog = catalog,
            state = ExperienceState(),
            dateLabel = "Saturday, October 10",
            locationLabel = "Test place",
        )

        assertTrue(packet.contains("Prayer leader: Unassigned"))
        assertTrue(packet.contains("Device use: Device permitted"))
        assertTrue(packet.contains("First aliyah (Aliyah): Open assignment"))
        assertTrue(packet.contains("Maftir (Maftir): Open assignment"))
        assertTrue(packet.contains("Service preflight: 0 of 1 complete"))
        assertTrue(packet.contains("Accessible option: Remain seated"))
        assertTrue(packet.contains("Provenance: Test provenance"))
        assertTrue(packet.contains("Local rehearsal choice: No option adopted"))
        assertTrue(packet.lowercase().contains("no prayer text is included"))
    }

    @Test
    fun `reading plan uses installed passage until an explicit local override is saved`() {
        val slot = ReadingSlot(
            id = "reading.aliyah.1",
            sequence = 1,
            label = "First aliyah",
            kind = ReadingSlotKind.ALIYAH,
            description = "Fixture",
            portionTitle = "Installed fixture portion",
            locator = "Fixture book",
            passageRange = "1-8",
            passageAvailability = ReadingPassageAvailability.AVAILABLE,
        )
        val installed = ExperienceState(
            readingAssignments = mapOf(slot.id to "Legacy reader"),
        ).readingPlanFor(slot)

        assertEquals("Installed fixture portion", installed.portionTitle)
        assertEquals("Fixture book", installed.locator)
        assertEquals("1-8", installed.passageRange)
        assertEquals("Legacy reader", installed.assignee)

        val manual = ExperienceState(
            readingAssignments = mapOf(slot.id to "Legacy reader"),
            readingPlans = mapOf(
                slot.id to ReadingPlanEntry(
                    portionTitle = "Local fixture portion",
                    locator = "Local prepared copy",
                    passageRange = "A-C",
                    assignee = "Primary reader",
                    backupAssignee = "Backup reader",
                    preparationStatus = ReadingPreparationStatus.READY,
                    manualOverride = true,
                    overrideReason = "Synthetic test verification",
                ),
            ),
        ).readingPlanFor(slot)

        assertEquals("Local fixture portion", manual.portionTitle)
        assertEquals("Primary reader", manual.assignee)
        assertEquals("Backup reader", manual.backupAssignee)
        assertEquals(ReadingPreparationStatus.READY, manual.preparationStatus)
    }

    @Test
    fun `packet renders available parallel text role response and provenance without fallback warning`() {
        val catalog = minimalCatalog().let { base ->
            base.copy(
                service = base.service.copy(
                    segments = base.service.segments.map { segment ->
                        segment.copy(
                            content = ServiceSegmentContent(
                                sourceText = ServiceTextField(
                                    availability = ServiceTextAvailability.AVAILABLE,
                                    text = "SYNTHETIC HEBREW FIXTURE",
                                    languageTag = "he",
                                    detail = "Synthetic test only",
                                ),
                                translation = ServiceTextField(
                                    availability = ServiceTextAvailability.AVAILABLE,
                                    text = "Synthetic English fixture",
                                    languageTag = "en",
                                    detail = "Synthetic test only",
                                ),
                                transliteration = ServiceTextField(
                                    availability = ServiceTextAvailability.UNAVAILABLE,
                                    detail = "No transliteration fixture installed.",
                                ),
                                roleTexts = mapOf(
                                    ParticipantRole.LEADER to ServiceTextField(
                                        availability = ServiceTextAvailability.AVAILABLE,
                                        text = "Synthetic leader fixture",
                                        languageTag = "en",
                                    ),
                                ),
                                responses = listOf(
                                    ServiceRoleResponse(
                                        speakerRole = ParticipantRole.LEADER,
                                        responderRole = ParticipantRole.CONGREGANT,
                                        prompt = ServiceTextField(
                                            availability = ServiceTextAvailability.AVAILABLE,
                                            text = "Synthetic prompt",
                                            languageTag = "en",
                                        ),
                                        response = ServiceTextField(
                                            availability = ServiceTextAvailability.AVAILABLE,
                                            text = "Synthetic response",
                                            languageTag = "en",
                                        ),
                                    ),
                                ),
                                provenance = ServiceTextProvenance(
                                    editionId = "edition.synthetic",
                                    editionTitle = "Synthetic test edition",
                                    sourceUnitId = "source-unit.synthetic",
                                    locator = "Fixture 1",
                                    provenance = "Generated test fixture",
                                    license = "CC0-1.0",
                                    conclusionStatus = ConclusionStatus.EDITORIAL_PROPOSAL,
                                    reviewState = EditorialReviewState.DRAFTED,
                                    editorialTreatment = "Synthetic test data",
                                    punctuationSource = "Synthetic test data",
                                    vocalizationSource = "Not applicable",
                                    variantNotes = "No variants",
                                ),
                            ),
                        )
                    },
                ),
            )
        }

        val packet = buildServicePacket(catalog, ExperienceState(), "Test date", "Test place")

        assertTrue(packet.contains("Source text (he): SYNTHETIC HEBREW FIXTURE"))
        assertTrue(packet.contains("Translation (en): Synthetic English fixture"))
        assertTrue(packet.contains("Transliteration: UNAVAILABLE"))
        assertTrue(packet.contains("Prayer leader text (en): Synthetic leader fixture"))
        assertTrue(packet.contains("Prompt (en): Synthetic prompt"))
        assertTrue(packet.contains("Response (en): Synthetic response"))
        assertTrue(packet.contains("Edition ID: edition.synthetic"))
        assertTrue(packet.contains("License: CC0-1.0"))
        assertTrue(packet.contains("Conclusion evidence status: Editorial proposal"))
        assertTrue(packet.contains("Review state: Drafted"))
        assertTrue(!packet.contains("WARNING: No prayer text is included"))
    }

    private fun minimalCatalog() = DemonstratorCatalog(
        label = "Development rehearsal",
        noticeTitle = "Synthetic development content",
        noticeBody = "Unreviewed and non-normative.",
        practiceCards = listOf(
            PracticeCard(
                id = "practice.known",
                topic = "Test",
                title = "Test practice",
                summary = "Summary",
                action = "Act",
                context = "Context",
                supplies = "None",
                steps = listOf(PracticeStep("step.known", "Known step", "Detail")),
                circumstancesDiffer = "Record differences",
                purpose = "Exercise the model",
                conclusionStatus = ConclusionStatus.EDITORIAL_PROPOSAL,
                reviewState = EditorialReviewState.DRAFTED,
                primarySourceIds = listOf("source.known"),
                reasoning = "Synthetic test reasoning",
                otherReadings = "None",
                confidence = "Test fixture",
                reviewDue = "2026-11-06",
            ),
        ),
        service = ServiceDefinition(
            id = "service.known",
            title = "Test service",
            subtitle = "Synthetic service",
            segments = listOf(
                ServiceSegment(
                    id = "segment.known",
                    phase = "Test",
                    title = "Known segment",
                    summary = "Summary",
                    movementCue = "No movement",
                    accessibleAlternative = "Remain seated",
                    voiceCue = "Speak",
                    roleCues = emptyMap(),
                    explanation = ExplanationTrace(
                        result = "Included",
                        facts = emptyList(),
                        rule = "Test rule",
                        sourceIds = listOf("source.known"),
                    ),
                ),
            ),
            preflightSteps = listOf(
                PracticeStep("preflight.known", "Known preflight", "Detail"),
            ),
            readingSlots = listOf(
                ReadingSlot(
                    id = "reading.aliyah.1",
                    sequence = 1,
                    label = "First aliyah",
                    kind = ReadingSlotKind.ALIYAH,
                    description = "Synthetic assignment slot.",
                ),
                ReadingSlot(
                    id = "reading.maftir",
                    sequence = 8,
                    label = "Maftir",
                    kind = ReadingSlotKind.MAFTIR,
                    description = "Synthetic assignment slot.",
                ),
            ),
        ),
        sources = listOf(
            SourceUnit(
                id = "source.known",
                title = "Test source",
                locator = "Fixture 1",
                language = "English",
                body = "Synthetic fixture",
                edition = "Test edition",
                provenance = "Test provenance",
                license = "CC0-1.0",
                conclusionStatus = ConclusionStatus.EDITORIAL_PROPOSAL,
                reviewState = EditorialReviewState.DRAFTED,
                relatedPracticeIds = listOf("practice.known"),
                relatedSegmentIds = listOf("segment.known"),
            ),
        ),
        communityChoice = CommunityChoice(
            id = "choice.known",
            title = "Local rehearsal choice",
            summary = "Synthetic choice",
            options = listOf(ChoiceOption("option.known", "Known option", "Test argument")),
            conclusionStatus = ConclusionStatus.UNRESOLVED,
            reviewState = EditorialReviewState.DRAFTED,
        ),
    )
}
