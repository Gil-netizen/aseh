package io.github.gilnetizen.aseh.domain.workspace

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkspaceValidationTest {
    private val fixture = SyntheticWorkspaceFixtures.reviewScenario

    @Test
    fun syntheticFixtureIsValidAndUnambiguouslyNonNormative() {
        assertEquals(FixtureAuthority.SYNTHETIC_NON_NORMATIVE, fixture.authority)
        assertTrue(fixture.id.startsWith("dev.workspace.synthetic"))
        assertTrue(fixture.notice.contains("Synthetic, non-normative"))
        assertTrue(fixture.notice.contains("does not establish"))
        assertTrue(WorkspaceValidator.validate(fixture.snapshot).isEmpty())
        assertEquals(fixture, SyntheticWorkspaceFixtures.reviewScenario)
    }

    @Test
    fun validatorReportsStablePathsForDuplicateIdsBadDatesAndQuantities() {
        val household = fixture.snapshot.households.single()
        val firstResponsibility = household.responsibilities.single()
        val qahal = fixture.snapshot.qahalWorkspaces.single()
        val inventory = qahal.inventory.single()
        val candidate = fixture.snapshot.copy(
            households = listOf(
                household.copy(
                    responsibilities = listOf(firstResponsibility, firstResponsibility),
                    calendarItems = listOf(
                        household.calendarItems.single().copy(
                            startsOn = date("2026-10-10"),
                            endsOn = date("2026-10-09"),
                        ),
                    ),
                ),
            ),
            qahalWorkspaces = listOf(
                qahal.copy(inventory = listOf(inventory.copy(quantityOnHand = -1, minimumDesired = -2))),
            ),
        )

        val issues = WorkspaceValidator.validate(candidate)

        assertEquals(
            setOf(
                WorkspaceIssueCode.DUPLICATE_ID,
                WorkspaceIssueCode.INVALID_DATE_RANGE,
                WorkspaceIssueCode.INVALID_QUANTITY,
            ),
            issues.map(WorkspaceIssue::code).toSet(),
        )
        assertTrue(issues.any { it.path == "households[0].responsibilities" })
        assertTrue(issues.any { it.path == "households[0].calendarItems[0].endsOn" })
        assertEquals(2, issues.count { it.code == WorkspaceIssueCode.INVALID_QUANTITY })
        assertEquals(
            issues.sortedWith(compareBy(WorkspaceIssue::path, { it.code.name }, WorkspaceIssue::detail)),
            issues,
        )
    }

    @Test
    fun adoptedRecordsRequireDatesAndExplicitLocalAuthorityScope() {
        val self = fixture.snapshot.selfWorkspaces.single()
        val adoption = self.practiceAdoptions.single().copy(adoptedOn = null, reviewOn = null)
        val decision = fixture.snapshot.qahalWorkspaces.single().decisions.single().copy(
            publicSummary = "",
            authorityScope = "",
            effectiveOn = null,
            reviewOn = null,
        )
        val candidate = fixture.snapshot.copy(
            selfWorkspaces = listOf(self.copy(practiceAdoptions = listOf(adoption))),
            qahalWorkspaces = listOf(
                fixture.snapshot.qahalWorkspaces.single().copy(decisions = listOf(decision)),
            ),
        )

        val paths = WorkspaceValidator.validate(candidate).map(WorkspaceIssue::path)

        assertTrue("selfWorkspaces[0].practiceAdoptions[0].adoptedOn" in paths)
        assertTrue("selfWorkspaces[0].practiceAdoptions[0].reviewOn" in paths)
        assertTrue("qahalWorkspaces[0].decisions[0].publicSummary" in paths)
        assertTrue("qahalWorkspaces[0].decisions[0].authorityScope" in paths)
        assertTrue("qahalWorkspaces[0].decisions[0].effectiveOn" in paths)
        assertTrue("qahalWorkspaces[0].decisions[0].reviewOn" in paths)
    }

    @Test
    fun exactContentIdentityCannotBePartiallyBlank() {
        val self = fixture.snapshot.selfWorkspaces.single()
        val adoption = self.practiceAdoptions.single().copy(
            practice = PracticeCardReference(
                practiceCardId = "",
                content = VerifiedContentReference("", "", "", ""),
            ),
        )

        val issues = WorkspaceValidator.validate(
            WorkspaceSnapshot(selfWorkspaces = listOf(self.copy(practiceAdoptions = listOf(adoption)))),
        )

        assertEquals(5, issues.count { it.code == WorkspaceIssueCode.REQUIRED_FIELD })
        assertTrue(issues.all { it.path.startsWith("selfWorkspaces[0].practiceAdoptions[0].practice") })
    }

    @Test
    fun financialDomainHasControlsButNoFundStorageOrTransferSurface() {
        val forbiddenTerms = listOf(
            "amount",
            "balance",
            "account",
            "routing",
            "transfer",
            "payment",
            "disbursement",
            "currency",
        )
        val exposedFieldNames = listOf(
            FinancialControlChecklist::class.java,
            FinancialControlItem::class.java,
            QahalWorkspace::class.java,
        ).flatMap { type -> type.declaredFields.map { it.name.lowercase() } }

        assertFalse(exposedFieldNames.any { field -> forbiddenTerms.any(field::contains) })
        assertEquals(
            setOf(
                FinancialControlKind.DOCUMENTED_AUTHORITY,
                FinancialControlKind.DUAL_APPROVAL,
                FinancialControlKind.SEPARATION_OF_ROLES,
                FinancialControlKind.RECEIPT_RETENTION,
                FinancialControlKind.PERIODIC_RECONCILIATION,
                FinancialControlKind.INDEPENDENT_REVIEW,
                FinancialControlKind.CONFLICT_DISCLOSURE,
                FinancialControlKind.MEMBER_REPORTING,
            ),
            FinancialControlKind.entries.toSet(),
        )
    }

    @Test
    fun activeFinancialChecklistRejectsDuplicateControlKinds() {
        val qahal = fixture.snapshot.qahalWorkspaces.single()
        val checklist = qahal.financialControls.single()
        val duplicateKind = checklist.controls.first().copy(id = WorkspaceRecordId("another-id"))
        val candidate = fixture.snapshot.copy(
            qahalWorkspaces = listOf(
                qahal.copy(financialControls = listOf(checklist.copy(controls = checklist.controls + duplicateKind))),
            ),
        )

        val issues = WorkspaceValidator.validate(candidate)

        assertTrue(issues.any { it.code == WorkspaceIssueCode.DUPLICATE_CONTROL_KIND })
    }

    private fun date(value: String): LocalDate = LocalDate.parse(value)
}
