package io.github.gilnetizen.aseh.feature.workspace

import io.github.gilnetizen.aseh.domain.workspace.DueSoonKind
import io.github.gilnetizen.aseh.domain.workspace.LocalWorkspaceApi
import io.github.gilnetizen.aseh.domain.workspace.SyntheticWorkspaceFixtures
import io.github.gilnetizen.aseh.domain.workspace.WorkspaceCommand
import io.github.gilnetizen.aseh.domain.workspace.WorkspaceId
import io.github.gilnetizen.aseh.domain.workspace.WorkspaceIssueCode
import io.github.gilnetizen.aseh.domain.workspace.WorkspaceUpdateResult
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkspaceDashboardPresenterTest {
    private val snapshot = SyntheticWorkspaceFixtures.reviewScenario.snapshot
    private val asOf = LocalDate.parse("2026-10-07")

    @Test
    fun activeContextFiltersContentDueItemsAndActions() {
        val first = snapshot.households.single()
        val second = first.copy(
            id = WorkspaceId("dev.household.second"),
            label = "Synthetic second household",
        )
        val multiContext = snapshot.copy(households = listOf(first, second))

        val model = WorkspaceDashboardPresenter.present(
            snapshot = multiContext,
            activeContext = ActiveWorkspaceContext.Household(second.id),
            asOf = asOf,
        )

        val content = model.content as WorkspaceDashboardContent.Household
        assertEquals(second.id, content.workspace.id)
        assertEquals(second.label, model.contextLabel)
        assertTrue(model.dueSoonItems.isNotEmpty())
        assertTrue(model.dueSoonItems.all { it.workspaceId == second.id })
        assertTrue(
            model.actions.all { action ->
                when (val command = action.command) {
                    is WorkspaceCommand.ChangeResponsibilityStatus -> command.householdId == second.id
                    is WorkspaceCommand.ChangeCalendarItemStatus -> command.householdId == second.id
                    is WorkspaceCommand.ChangePreparationTaskStatus -> command.householdId == second.id
                    is WorkspaceCommand.ChangePreparationKitStatus -> command.householdId == second.id
                    else -> false
                }
            },
        )
    }

    @Test
    fun dueSoonRowsRemainInDomainDateOrder() {
        val household = snapshot.households.single()

        val model = WorkspaceDashboardPresenter.present(
            snapshot,
            ActiveWorkspaceContext.Household(household.id),
            asOf,
        )

        assertEquals(model.dueSoonItems.map { it.dueOn }.sorted(), model.dueSoonItems.map { it.dueOn })
        assertEquals(
            listOf(
                DueSoonKind.HOUSEHOLD_RESPONSIBILITY,
                DueSoonKind.PREPARATION_KIT_TASK,
                DueSoonKind.HOUSEHOLD_CALENDAR_ITEM,
                DueSoonKind.PREPARATION_KIT_TARGET,
            ),
            model.dueSoonItems.map { it.kind },
        )
    }

    @Test
    fun dashboardActionDispatchesTheExactTypedCommandOnce() {
        val self = snapshot.selfWorkspaces.single()
        val model = WorkspaceDashboardPresenter.present(
            snapshot,
            ActiveWorkspaceContext.Self(self.id),
            asOf,
        )
        val action = model.actions.single { it.label == WorkspaceActionLabel.PAUSE }
        val received = mutableListOf<WorkspaceCommand>()

        action.dispatch(received::add)

        assertEquals(1, received.size)
        assertSame(action.command, received.single())
        val command = received.single() as WorkspaceCommand.ChangePracticeStatus
        assertEquals(self.id, command.selfId)
        assertEquals(self.practiceAdoptions.single().id, command.id)
        assertEquals(asOf, command.changedOn)
    }

    @Test
    fun householdResponsibilityOffersCompletionAndReopenActions() {
        val household = snapshot.households.single()
        val responsibility = household.responsibilities.single()
        val initial = WorkspaceDashboardPresenter.present(
            snapshot,
            ActiveWorkspaceContext.Household(household.id),
            asOf,
        )
        val complete = initial.actionsFor(
            WorkspaceRecordKind.RESPONSIBILITY,
            responsibility.id,
        ).single { it.label == WorkspaceActionLabel.MARK_DONE }

        val completed = LocalWorkspaceApi.apply(snapshot, complete.command)
        assertTrue(completed is WorkspaceUpdateResult.Applied)
        val completedSnapshot = (completed as WorkspaceUpdateResult.Applied).snapshot
        val reopened = WorkspaceDashboardPresenter.present(
            completedSnapshot,
            ActiveWorkspaceContext.Household(household.id),
            asOf,
        ).actionsFor(WorkspaceRecordKind.RESPONSIBILITY, responsibility.id)

        assertTrue(reopened.any { it.label == WorkspaceActionLabel.REOPEN })
    }

    @Test
    fun invalidLifecycleActionsAreNotRendered() {
        val household = snapshot.households.single()
        val kit = household.preparationKits.single()

        val model = WorkspaceDashboardPresenter.present(
            snapshot,
            ActiveWorkspaceContext.Household(household.id),
            asOf,
        )
        val actions = model.actionsFor(WorkspaceRecordKind.PREPARATION_KIT, kit.id)

        assertFalse(actions.any { it.label == WorkspaceActionLabel.COMPLETE })
        assertTrue(actions.any { it.label == WorkspaceActionLabel.CANCEL })
    }

    @Test
    fun missingContextProducesClearFailClosedModel() {
        val model = WorkspaceDashboardPresenter.present(
            snapshot,
            ActiveWorkspaceContext.Household(WorkspaceId("missing-household")),
            asOf,
        )

        assertEquals(WorkspaceDashboardContent.Missing, model.content)
        assertTrue(model.dueSoonItems.isEmpty())
        assertTrue(model.actions.isEmpty())
        assertTrue(model.domainIssues.any { it.code == WorkspaceIssueCode.RECORD_NOT_FOUND })
    }

    @Test
    fun productionPresenterHasNoImplicitFixtureOrClockInput() {
        val present = WorkspaceDashboardPresenter::class.java.declaredMethods.single { method ->
            method.name == "present"
        }
        val parameterNames = present.parameterTypes.map { type -> type.simpleName }

        assertTrue(parameterNames.any { it == "WorkspaceSnapshot" })
        assertTrue(parameterNames.any { it == "ActiveWorkspaceContext" })
        assertTrue(parameterNames.any { it == "LocalDate" })
        assertFalse(parameterNames.any { it.contains("Fixture") || it.contains("Clock") })
    }
}
