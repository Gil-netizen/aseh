package io.github.gilnetizen.aseh.domain.workspace

import java.time.LocalDate

enum class FixtureAuthority {
    SYNTHETIC_NON_NORMATIVE,
}

data class WorkspaceFixture(
    val id: String,
    val authority: FixtureAuthority,
    val notice: String,
    val snapshot: WorkspaceSnapshot,
)

/**
 * Fixed development data for previews and tests. It contains no real person, community, ruling,
 * financial amount, sacred text, or claim of authority.
 */
object SyntheticWorkspaceFixtures {
    const val NOTICE =
        "Synthetic, non-normative development data. It does not establish a practice, qahal authority, or financial instruction."

    val reviewScenario = WorkspaceFixture(
        id = "dev.workspace.synthetic-review-v1",
        authority = FixtureAuthority.SYNTHETIC_NON_NORMATIVE,
        notice = NOTICE,
        snapshot = WorkspaceSnapshot(
            selfWorkspaces = listOf(
                SelfWorkspace(
                    id = WorkspaceId("dev.self.synthetic"),
                    label = "Synthetic self workspace",
                    practiceAdoptions = listOf(
                        PersonalPracticeAdoption(
                            id = record("dev.self.practice.preparation"),
                            practice = PracticeCardReference(
                                practiceCardId = "dev.practice.synthetic.preparation",
                                content = VerifiedContentReference(
                                    packId = "dev.pack.synthetic",
                                    packVersion = "1",
                                    editionId = "dev.edition.synthetic",
                                    sourceUnitId = "dev.source.synthetic-preparation",
                                ),
                            ),
                            label = "Synthetic personal preparation review",
                            status = PracticeAdoptionStatus.ADOPTED,
                            adoptedOn = date("2026-09-01"),
                            reviewOn = date("2026-10-08"),
                            statusChangedOn = date("2026-09-01"),
                        ),
                    ),
                ),
            ),
            households = listOf(
                HouseholdWorkspace(
                    id = WorkspaceId("dev.household.synthetic"),
                    label = "Synthetic household",
                    responsibilities = listOf(
                        HouseholdResponsibility(
                            id = record("dev.household.responsibility.table"),
                            label = "Synthetic shared-table preparation",
                            dueOn = date("2026-10-06"),
                            assignedTo = LocalPersonId("dev.person.household-a"),
                            status = WorkItemStatus.IN_PROGRESS,
                            statusChangedOn = date("2026-10-05"),
                        ),
                    ),
                    calendarItems = listOf(
                        HouseholdCalendarItem(
                            id = record("dev.household.calendar.study"),
                            label = "Synthetic household learning time",
                            kind = HouseholdCalendarKind.STUDY,
                            startsOn = date("2026-10-09"),
                            status = CalendarItemStatus.SCHEDULED,
                            statusChangedOn = date("2026-10-01"),
                        ),
                    ),
                    preparationKits = listOf(
                        HouseholdPreparationKit(
                            id = record("dev.household.kit.weekly"),
                            label = "Synthetic weekly preparation kit",
                            targetOn = date("2026-10-10"),
                            status = PreparationKitStatus.ACTIVE,
                            statusChangedOn = date("2026-10-04"),
                            tasks = listOf(
                                PreparationKitTask(
                                    id = record("dev.household.kit.task.access"),
                                    label = "Synthetic access-needs check",
                                    dueOn = date("2026-10-07"),
                                    status = WorkItemStatus.TODO,
                                    statusChangedOn = date("2026-10-04"),
                                ),
                                PreparationKitTask(
                                    id = record("dev.household.kit.task.supplies"),
                                    label = "Synthetic supplies check",
                                    dueOn = date("2026-10-06"),
                                    status = WorkItemStatus.DONE,
                                    statusChangedOn = date("2026-10-06"),
                                ),
                            ),
                        ),
                    ),
                ),
            ),
            qahalWorkspaces = listOf(
                QahalWorkspace(
                    id = WorkspaceId("dev.qahal.synthetic"),
                    label = "Synthetic qahal",
                    decisions = listOf(
                        QahalDecision(
                            id = record("dev.qahal.decision.access"),
                            label = "Synthetic access commitment review",
                            classification = DecisionClassification.POLICY,
                            publicSummary = "Review whether the synthetic access checklist was followed.",
                            authorityScope = "This synthetic workspace only.",
                            status = QahalDecisionStatus.ADOPTED,
                            effectiveOn = date("2026-09-01"),
                            reviewOn = date("2026-10-10"),
                            statusChangedOn = date("2026-09-01"),
                            dissentSummary = "No synthetic dissent recorded.",
                        ),
                    ),
                    volunteerRotations = listOf(
                        VolunteerRotation(
                            id = record("dev.qahal.rotation.welcome"),
                            label = "Synthetic welcome rota",
                            publicRole = PublicRoleId("dev.role.welcome"),
                            status = VolunteerRotationStatus.ACTIVE,
                            statusChangedOn = date("2026-10-01"),
                            slots = listOf(
                                VolunteerSlot(
                                    id = record("dev.qahal.slot.welcome-1"),
                                    serviceOn = date("2026-10-08"),
                                    status = VolunteerSlotStatus.OPEN,
                                    statusChangedOn = date("2026-10-01"),
                                ),
                                VolunteerSlot(
                                    id = record("dev.qahal.slot.welcome-2"),
                                    serviceOn = date("2026-10-09"),
                                    assignedTo = LocalPersonId("dev.person.qahal-a"),
                                    status = VolunteerSlotStatus.ASSIGNED,
                                    statusChangedOn = date("2026-10-02"),
                                ),
                            ),
                        ),
                    ),
                    inventory = listOf(
                        InventoryItem(
                            id = record("dev.qahal.inventory.chairs"),
                            label = "Synthetic accessible seating",
                            quantityOnHand = 1,
                            minimumDesired = 2,
                            nextCheckOn = date("2026-10-07"),
                            statusChangedOn = date("2026-10-01"),
                        ),
                    ),
                    financialControls = listOf(
                        FinancialControlChecklist(
                            id = record("dev.qahal.controls.monthly"),
                            label = "Synthetic monthly controls",
                            status = FinancialChecklistStatus.ACTIVE,
                            reviewOn = date("2026-10-14"),
                            statusChangedOn = date("2026-10-01"),
                            controls = listOf(
                                FinancialControlItem(
                                    id = record("dev.qahal.control.dual-approval"),
                                    kind = FinancialControlKind.DUAL_APPROVAL,
                                    dueOn = date("2026-10-08"),
                                    responsibleRole = PublicRoleId("dev.role.treasurer"),
                                    statusChangedOn = date("2026-10-01"),
                                ),
                                FinancialControlItem(
                                    id = record("dev.qahal.control.reporting"),
                                    kind = FinancialControlKind.MEMBER_REPORTING,
                                    status = FinancialControlStatus.VERIFIED,
                                    dueOn = date("2026-10-06"),
                                    responsibleRole = PublicRoleId("dev.role.secretary"),
                                    statusChangedOn = date("2026-10-06"),
                                ),
                            ),
                        ),
                    ),
                ),
            ),
        ),
    )

    private fun record(value: String) = WorkspaceRecordId(value)

    private fun date(value: String): LocalDate = LocalDate.parse(value)
}
