package io.github.gilnetizen.aseh.core.model

import java.time.Instant
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ServiceAssemblerTest {
    @Test
    fun `after-reading option places teaching immediately after reading`() {
        val catalog = catalog()

        val assembly = assembleService(
            catalog = catalog,
            state = ExperienceState(
                selectedCommunityOptionId = TEACHING_AFTER_READING_OPTION_ID,
            ),
            dateLabel = "Saturday, October 10",
            locationLabel = "Test place",
        )

        assertEquals(
            listOf("segment.opening", "segment.reading", "segment.teaching", "segment.reflect", "segment.close"),
            assembly.segments.map { assembled -> assembled.segment.id },
        )
        assertEquals(ServiceAssemblyResolution.LOCAL_OPTION_APPLIED, assembly.orderingDecision.resolution)
        assertEquals(ConclusionStatus.UNRESOLVED, assembly.orderingDecision.conclusionStatus)
        assertEquals(EditorialReviewState.DRAFTED, assembly.orderingDecision.reviewState)
        assertTrue(assembly.orderingDecision.orderChanged)
        assertTrue(assembly.orderingDecision.explanation.contains("immediately after the reading handoff"))
        assertTrue(assembly.orderingDecision.facts.contains("Placement anchor: segment.reading"))
    }

    @Test
    fun `before-close option places teaching immediately before close`() {
        val assembly = assembleService(
            catalog = catalog(),
            state = ExperienceState(
                selectedCommunityOptionId = TEACHING_BEFORE_CLOSE_OPTION_ID,
            ),
            dateLabel = "Saturday, October 10",
            locationLabel = "Test place",
        )

        assertEquals(
            listOf("segment.opening", "segment.reading", "segment.reflect", "segment.teaching", "segment.close"),
            assembly.segments.map { assembled -> assembled.segment.id },
        )
        assertTrue(assembly.orderingDecision.orderChanged)
        assertTrue(assembly.orderingDecision.explanation.contains("immediately before the closing review"))
        assertTrue(assembly.orderingDecision.facts.contains("Placement anchor: segment.close"))
    }

    @Test
    fun `no selection preserves catalog order as explicitly unresolved`() {
        val catalog = catalog()

        val assembly = assembleService(
            catalog = catalog,
            state = ExperienceState(),
            dateLabel = "Saturday, October 10",
            locationLabel = "Test place",
        )

        assertEquals(
            catalog.service.segments.map(ServiceSegment::id),
            assembly.segments.map { assembled -> assembled.segment.id },
        )
        assertEquals(ServiceAssemblyResolution.DEFAULT_UNRESOLVED, assembly.orderingDecision.resolution)
        assertFalse(assembly.orderingDecision.orderChanged)
        assertNull(assembly.orderingDecision.selectedOptionId)
        assertTrue(assembly.orderingDecision.explanation.contains("default segment order"))
    }

    @Test
    fun `unknown persisted selection does not silently adopt an option`() {
        val catalog = catalog()

        val assembly = assembleService(
            catalog = catalog,
            state = ExperienceState(selectedCommunityOptionId = "choice.removed"),
            dateLabel = "Saturday, October 10",
            locationLabel = "Test place",
        )

        assertEquals(
            catalog.service.segments.map(ServiceSegment::id),
            assembly.segments.map { assembled -> assembled.segment.id },
        )
        assertEquals(
            ServiceAssemblyResolution.DEFAULT_UNRECOGNIZED_OPTION,
            assembly.orderingDecision.resolution,
        )
        assertEquals("choice.removed", assembly.orderingDecision.selectedOptionId)
        assertEquals(ConclusionStatus.UNRESOLVED, assembly.orderingDecision.conclusionStatus)
        assertEquals(EditorialReviewState.DRAFTED, assembly.orderingDecision.reviewState)
        assertFalse(assembly.orderingDecision.orderChanged)
    }

    @Test
    fun `assembly exposes supplied context and exact installed source references`() {
        val assembly = assembleService(
            catalog = catalog(),
            state = ExperienceState(
                workspaceName = "West room",
                workspaceKind = WorkspaceKind.QAHAL,
                selectedRole = ParticipantRole.GABBAI,
                provisionalCharterAdopted = true,
            ),
            dateLabel = "Saturday, October 10",
            locationLabel = "Jerusalem",
        )

        assertTrue(assembly.contextFacts.contains(ServiceContextFact("Date", "Saturday, October 10")))
        assertTrue(assembly.contextFacts.contains(ServiceContextFact("Place", "Jerusalem")))
        assertTrue(assembly.contextFacts.contains(ServiceContextFact("Workspace", "West room")))
        assertTrue(assembly.contextFacts.contains(ServiceContextFact("View", "Gabbai")))
        val teaching = assembly.segments.single { assembled ->
            assembled.segment.id == TEACHING_SEGMENT_ID
        }
        assertEquals(
            listOf(ServiceSourceReference("source.choice", "Development fixture 4:1")),
            teaching.sourceReferences,
        )
    }

    @Test
    fun `packet renders assembled order context sources and development warning`() {
        val packet = buildServicePacket(
            catalog = catalog(),
            state = ExperienceState(
                workspaceName = "West room",
                selectedCommunityOptionId = TEACHING_BEFORE_CLOSE_OPTION_ID,
            ),
            dateLabel = "Saturday, October 10",
            locationLabel = "Jerusalem",
        )

        assertTrue(packet.contains("Date: Saturday, October 10"))
        assertTrue(packet.contains("Place: Jerusalem"))
        assertTrue(packet.contains("Workspace: West room"))
        assertTrue(packet.contains("Source: source.choice — Development fixture 4:1"))
        assertTrue(packet.contains("Conclusion evidence status: Unresolved"))
        assertTrue(packet.contains("Review state: Drafted"))
        assertTrue(packet.lowercase().contains("no prayer text is included"))
        assertTrue(packet.indexOf("Reflect") < packet.indexOf("Optional teaching"))
        assertTrue(packet.indexOf("Optional teaching") < packet.indexOf("Close"))
    }

    @Test
    fun `typed calendar policy explicitly includes or omits a segment with reasons`() {
        val base = catalog()
        val regional = base.copy(
            service = base.service.copy(
                segments = base.service.segments.map { segment ->
                    if (segment.id == TEACHING_SEGMENT_ID) {
                        segment.copy(
                            inclusionPolicy = SegmentInclusionPolicy(
                                condition = ServiceAssemblyCondition.CalendarRegionIn(
                                    setOf(CalendarRegion.ISRAEL),
                                ),
                                includedReason = "Included for the synthetic Israel test profile.",
                                omittedReason = "Omitted because the synthetic test profile is diaspora.",
                            ),
                        )
                    } else {
                        segment
                    }
                },
            ),
        )

        val israelAssembly = assembleService(
            catalog = regional,
            state = ExperienceState(),
            context = context(regional, CalendarRegion.ISRAEL),
        )
        val diasporaAssembly = assembleService(
            catalog = regional,
            state = ExperienceState(),
            context = context(regional, CalendarRegion.DIASPORA),
        )

        assertTrue(israelAssembly.segments.any { it.segment.id == TEACHING_SEGMENT_ID })
        assertFalse(diasporaAssembly.segments.any { it.segment.id == TEACHING_SEGMENT_ID })
        val included = israelAssembly.compositionDecisions.single {
            it.targetSegmentId == TEACHING_SEGMENT_ID &&
                it.action == ServiceCompositionAction.INCLUDE
        }
        val omitted = diasporaAssembly.compositionDecisions.single {
            it.targetSegmentId == TEACHING_SEGMENT_ID &&
                it.action == ServiceCompositionAction.OMIT
        }
        assertEquals("Included for the synthetic Israel test profile.", included.reason)
        assertEquals("Omitted because the synthetic test profile is diaspora.", omitted.reason)
        assertTrue(included.facts.single().contains("Calendar region: ISRAEL"))
        assertTrue(omitted.facts.single().contains("Calendar region: DIASPORA"))
        assertEquals(
            listOf(ServiceSourceReference("source.choice", "Development fixture 4:1")),
            omitted.sourceReferences,
        )
    }

    @Test
    fun `accessibility profile selects the equal-status movement alternative`() {
        val catalog = catalog()
        val context = context(catalog, CalendarRegion.ISRAEL).copy(
            accessibility = ServiceAccessibilityProfile(
                participantNeedsReviewed = true,
                useMovementAlternatives = true,
            ),
        )

        val assembly = assembleService(catalog, ExperienceState(), context)

        assertTrue(assembly.segments.all { it.effectiveMovementCue == "Remain seated" })
        assertTrue(
            assembly.segments.all {
                it.movementCueReason.contains("accessibility profile")
            },
        )
        assertTrue(
            assembly.contextFacts.contains(
                ServiceContextFact("Accessibility", "movement alternatives"),
            ),
        )
    }

    @Test
    fun `omitted teaching leaves requested reorder visibly unapplied`() {
        val base = catalog()
        val catalog = base.copy(
            service = base.service.copy(
                segments = base.service.segments.map { segment ->
                    if (segment.id == TEACHING_SEGMENT_ID) {
                        segment.copy(
                            inclusionPolicy = SegmentInclusionPolicy(
                                condition = ServiceAssemblyCondition.CalendarRegionIn(
                                    setOf(CalendarRegion.ISRAEL),
                                ),
                                includedReason = "Included in Israel fixture.",
                                omittedReason = "Omitted in diaspora fixture.",
                            ),
                        )
                    } else {
                        segment
                    }
                },
            ),
        )

        val assembly = assembleService(
            catalog = catalog,
            state = ExperienceState(
                selectedCommunityOptionId = TEACHING_AFTER_READING_OPTION_ID,
            ),
            context = context(catalog, CalendarRegion.DIASPORA),
        )

        assertEquals(ServiceAssemblyResolution.DEFAULT_UNAVAILABLE, assembly.orderingDecision.resolution)
        val reorder = assembly.compositionDecisions.single {
            it.action == ServiceCompositionAction.REORDER
        }
        assertFalse(reorder.applied)
        assertTrue(reorder.reason.contains("not included for this context"))
    }

    @Test
    fun `assembly preserves precise local calendar communal and seasonal context facts`() {
        val catalog = catalog()
        val context = context(catalog, CalendarRegion.ISRAEL).copy(
            date = ServiceDateContext(
                civilDate = LocalDate.of(2026, 10, 10),
                displayLabel = "Saturday, October 10",
                dayKind = ServiceDayKind.SHABBAT,
                hebrewDate = ServiceHebrewDateContext.Available(
                    transliteratedLabel = "29 Tishrei, 5787",
                    hebrewLabel = "כ״ט תשרי תשפ״ז",
                    boundaryLabel = "before sunset",
                ),
            ),
            place = ServicePlaceAvailability.Available(
                label = "Jerusalem",
                timeZoneId = "Asia/Jerusalem",
                coordinates = ServiceCoordinates(
                    latitudeDegrees = 31.778,
                    longitudeDegrees = 35.235,
                    elevationMeters = 754.5,
                    horizontalAccuracyMeters = 12.25,
                ),
            ),
            zmanim = ServiceZmanimContext(
                date = LocalDate.of(2026, 10, 10),
                timeZoneId = "Asia/Jerusalem",
                events = listOf(
                    ServiceSolarEvent.Available(
                        ServiceSolarEventKind.SUNRISE,
                        Instant.parse("2026-10-10T03:40:00Z"),
                    ),
                    ServiceSolarEvent.Unavailable(
                        ServiceSolarEventKind.SOLAR_NOON_CHATZOT,
                        "Synthetic unavailable fixture",
                    ),
                    ServiceSolarEvent.Available(
                        ServiceSolarEventKind.SUNSET,
                        Instant.parse("2026-10-10T15:14:00Z"),
                    ),
                ),
                methodLabel = "Synthetic deterministic test method",
            ),
            communalSetting = ServiceCommunalSetting.QAHAL_WORKSPACE,
            quorum = ServiceQuorumContext(
                status = ServiceQuorumStatus.CONFIRMED_PRESENT,
                countedParticipants = 12,
                requiredParticipants = 10,
                note = "Count recorded for this rehearsal only",
            ),
            practiceProfile = ServicePracticeProfileContext(
                profileId = "profile.local.test",
                label = "Local test profile",
                adopted = true,
                adoptionScope = "Synthetic test workspace",
            ),
            seasonal = ServiceSeasonalContext(
                seasonLabel = "Synthetic test season",
                additions = listOf("Synthetic addition"),
                established = true,
            ),
        )

        val assembly = assembleService(catalog, ExperienceState(), context)

        assertTrue(assembly.contextFacts.contains(ServiceContextFact("Coordinates", "31.778, 35.235")))
        assertTrue(assembly.contextFacts.contains(ServiceContextFact("Elevation", "754.5 m")))
        assertTrue(
            assembly.contextFacts.contains(ServiceContextFact("Horizontal accuracy", "12.25 m")),
        )
        assertTrue(
            assembly.contextFacts.any { fact ->
                fact.label == "Hebrew date" && fact.value.contains("כ״ט תשרי תשפ״ז")
            },
        )
        assertTrue(
            assembly.contextFacts.any { fact ->
                fact.label == "Sunrise" && fact.value.contains("Oct 10, 2026")
            },
        )
        assertTrue(assembly.contextFacts.contains(ServiceContextFact("Communal setting", "Qahal workspace")))
        assertTrue(
            assembly.contextFacts.any { fact ->
                fact.label == "Quorum" && fact.value.contains("counted: 12")
            },
        )
        assertTrue(
            assembly.contextFacts.any { fact ->
                fact.label == "Practice profile" && fact.value.contains("adopted locally")
            },
        )
        assertTrue(
            assembly.contextFacts.any { fact ->
                fact.label == "Seasonal context" && fact.value.contains("Synthetic addition")
            },
        )
    }

    @Test
    fun `each assembled segment retains its evaluated inclusion trace and packet renders it`() {
        val base = catalog()
        val traced = base.copy(
            service = base.service.copy(
                segments = base.service.segments.map { segment ->
                    if (segment.id == "segment.opening") {
                        segment.copy(
                            inclusionPolicy = SegmentInclusionPolicy(
                                condition = ServiceAssemblyCondition.CommunalSettingIn(
                                    setOf(ServiceCommunalSetting.QAHAL_WORKSPACE),
                                ),
                                includedReason = "Included for the synthetic qahal fixture.",
                                omittedReason = "Omitted outside the synthetic qahal fixture.",
                            ),
                        )
                    } else {
                        segment
                    }
                },
            ),
        )
        val state = ExperienceState(workspaceKind = WorkspaceKind.QAHAL)
        val context = context(traced, CalendarRegion.ISRAEL).copy(
            communalSetting = ServiceCommunalSetting.QAHAL_WORKSPACE,
        )

        val assembly = assembleService(traced, state, context)
        val opening = assembly.segments.single { it.segment.id == "segment.opening" }
        val openingDecision = assembly.compositionDecisions.single {
            it.targetSegmentId == "segment.opening" && it.action == ServiceCompositionAction.INCLUDE
        }

        assertEquals(openingDecision.reason, opening.inclusionReason)
        assertEquals(openingDecision.facts, opening.inclusionFacts)
        assertTrue(opening.inclusionFacts.single().contains("QAHAL_WORKSPACE"))
        val packet = buildServicePacket(traced, state, context)
        assertTrue(packet.contains("Inclusion: Included for the synthetic qahal fixture."))
        assertTrue(packet.contains("Evaluated fact: Communal setting: QAHAL_WORKSPACE"))
    }

    private fun context(
        catalog: DemonstratorCatalog,
        region: CalendarRegion,
    ): ServiceAssemblyContext = serviceAssemblyContext(
        catalog = catalog,
        state = ExperienceState(),
        date = ServiceDateContext(
            civilDate = LocalDate.of(2026, 10, 10),
            displayLabel = "Saturday, October 10",
            dayKind = ServiceDayKind.SHABBAT,
        ),
        calendarRegion = region,
        place = ServicePlaceAvailability.Available("Test place"),
        accessibility = ServiceAccessibilityProfile(participantNeedsReviewed = true),
        requiredRoles = emptySet(),
    )

    private fun catalog(): DemonstratorCatalog = DemonstratorCatalog(
        label = "Development rehearsal",
        noticeTitle = "Synthetic development content",
        noticeBody = "Unreviewed and non-normative.",
        practiceCards = emptyList(),
        service = ServiceDefinition(
            id = "service.test",
            title = "Test service",
            subtitle = "Synthetic service",
            segments = listOf(
                segment("segment.opening", "Opening", "source.order"),
                segment(TEACHING_SEGMENT_ID, "Optional teaching", "source.choice"),
                segment("segment.reading", "Reading", "source.order"),
                segment("segment.reflect", "Reflect", "source.order"),
                segment("segment.close", "Close", "source.order"),
            ),
            preflightSteps = emptyList(),
        ),
        sources = listOf(
            source("source.order", "Development fixture 1:1"),
            source("source.choice", "Development fixture 4:1"),
        ),
        communityChoice = CommunityChoice(
            id = "choice.teaching-placement",
            title = "Placement of the teaching pause",
            summary = "Synthetic options only.",
            options = listOf(
                ChoiceOption(
                    id = TEACHING_AFTER_READING_OPTION_ID,
                    title = "After the reading handoff",
                    argument = "Synthetic argument.",
                ),
                ChoiceOption(
                    id = TEACHING_BEFORE_CLOSE_OPTION_ID,
                    title = "Before the closing review",
                    argument = "Synthetic argument.",
                ),
            ),
            conclusionStatus = ConclusionStatus.UNRESOLVED,
            reviewState = EditorialReviewState.DRAFTED,
        ),
    )

    private fun segment(id: String, title: String, sourceId: String) = ServiceSegment(
        id = id,
        phase = "Test",
        title = title,
        summary = "Summary",
        movementCue = "No movement",
        accessibleAlternative = "Remain seated",
        voiceCue = "Speak",
        roleCues = emptyMap(),
        explanation = ExplanationTrace(
            result = "Included",
            facts = emptyList(),
            rule = "Synthetic rule",
            sourceIds = listOf(sourceId),
        ),
    )

    private fun source(id: String, locator: String) = SourceUnit(
        id = id,
        title = "Test source",
        locator = locator,
        language = "English",
        body = "Synthetic fixture",
        edition = "Test edition",
        provenance = "Test provenance",
        license = "CC0-1.0",
        conclusionStatus = ConclusionStatus.EDITORIAL_PROPOSAL,
        reviewState = EditorialReviewState.DRAFTED,
        relatedPracticeIds = emptyList(),
        relatedSegmentIds = emptyList(),
    )
}
