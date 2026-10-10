package io.github.gilnetizen.aseh

import io.github.gilnetizen.aseh.core.model.WorkspaceKind
import io.github.gilnetizen.aseh.domain.workspace.DecisionClassification
import io.github.gilnetizen.aseh.domain.workspace.HouseholdCalendarItem
import io.github.gilnetizen.aseh.domain.workspace.HouseholdCalendarKind
import io.github.gilnetizen.aseh.domain.workspace.HouseholdResponsibility
import io.github.gilnetizen.aseh.domain.workspace.HouseholdWorkspace
import io.github.gilnetizen.aseh.domain.workspace.InventoryItem
import io.github.gilnetizen.aseh.domain.workspace.LocalPersonId
import io.github.gilnetizen.aseh.domain.workspace.QahalDecision
import io.github.gilnetizen.aseh.domain.workspace.QahalWorkspace
import io.github.gilnetizen.aseh.domain.workspace.WorkspaceId
import io.github.gilnetizen.aseh.domain.workspace.WorkspaceRecordAddress
import io.github.gilnetizen.aseh.domain.workspace.WorkspaceRecordId
import io.github.gilnetizen.aseh.domain.workspace.WorkspaceRecordKind
import io.github.gilnetizen.aseh.domain.workspace.WorkspaceSnapshot
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkspaceSearchIndexTest {
  private val today = LocalDate.of(2026, 10, 7)
  private val snapshot = WorkspaceSnapshot(
    households = listOf(
      HouseholdWorkspace(
        id = WorkspaceId("household.home"),
        label = "Our home",
        responsibilities = listOf(
          HouseholdResponsibility(
            id = WorkspaceRecordId("responsibility.meal"),
            label = "Confirm Friday meal guests",
            dueOn = today.plusDays(2),
            assignedTo = LocalPersonId("Miriam"),
            statusChangedOn = today,
          ),
        ),
        calendarItems = listOf(
          HouseholdCalendarItem(
            id = WorkspaceRecordId("calendar.study"),
            label = "Family study hour",
            kind = HouseholdCalendarKind.STUDY,
            startsOn = today.plusDays(1),
            statusChangedOn = today,
          ),
        ),
      ),
    ),
    qahalWorkspaces = listOf(
      QahalWorkspace(
        id = WorkspaceId("qahal.local"),
        label = "Neighborhood qahal",
        decisions = listOf(
          QahalDecision(
            id = WorkspaceRecordId("decision.access"),
            label = "Step-free entrance policy",
            classification = DecisionClassification.POLICY,
            publicSummary = "Keep the east entrance clear for wheelchair access.",
            authorityScope = "Local facility operations",
            statusChangedOn = today,
          ),
        ),
        inventory = listOf(
          InventoryItem(
            id = WorkspaceRecordId("inventory.chairs"),
            label = "Folding chairs",
            quantityOnHand = 18,
            minimumDesired = 24,
            statusChangedOn = today,
          ),
        ),
      ),
    ),
  )

  @Test
  fun `household responsibility is searchable by assignee and words`() {
    val result = searchWorkspace(snapshot, "Miriam meal").single()

    assertEquals("Confirm Friday meal guests", result.title)
    assertEquals(WorkspaceKind.HOUSEHOLD, result.workspaceKind)
    assertEquals(
      WorkspaceRecordAddress(
        WorkspaceId("household.home"),
        WorkspaceRecordKind.HOUSEHOLD_RESPONSIBILITY,
        WorkspaceRecordId("responsibility.meal"),
      ),
      result.address,
    )
  }

  @Test
  fun `qahal decision is searchable by summary and scope`() {
    val result = searchWorkspace(snapshot, "wheelchair operations").single()

    assertEquals("Step-free entrance policy", result.title)
    assertEquals(WorkspaceKind.QAHAL, result.workspaceKind)
    assertEquals(
      WorkspaceRecordAddress(
        WorkspaceId("qahal.local"),
        WorkspaceRecordKind.QAHAL_DECISION,
        WorkspaceRecordId("decision.access"),
      ),
      result.address,
    )
  }

  @Test
  fun `inventory and calendar records are indexed`() {
    assertEquals("Folding chairs", searchWorkspace(snapshot, "chairs").single().title)
    assertEquals("Family study hour", searchWorkspace(snapshot, "study hour").single().title)
  }

  @Test
  fun `workspace result is searchable by name and workspace type`() {
    val result = searchWorkspace(snapshot, "Our home household workspace").single()

    assertEquals("Our home", result.title)
    assertEquals(WorkspaceKind.HOUSEHOLD, result.workspaceKind)
    assertEquals(
      WorkspaceRecordAddress(
        WorkspaceId("household.home"),
        WorkspaceRecordKind.WORKSPACE,
      ),
      result.address,
    )
  }

  @Test
  fun `unmatched and blank queries return no records`() {
    assertTrue(searchWorkspace(snapshot, "mikveh").isEmpty())
    assertTrue(searchWorkspace(snapshot, "   ").isEmpty())
  }

  @Test
  fun `colliding record ids retain their owning workspace and record kind`() {
    val collisionId = WorkspaceRecordId("shared.local.id")
    val withCollisions = snapshot.copy(
      households = snapshot.households + HouseholdWorkspace(
        id = WorkspaceId("household.second"),
        label = "Second home",
        responsibilities = listOf(
          HouseholdResponsibility(
            id = collisionId,
            label = "Collision target responsibility",
            dueOn = today,
            statusChangedOn = today,
          ),
        ),
        calendarItems = listOf(
          HouseholdCalendarItem(
            id = collisionId,
            label = "Collision target calendar",
            kind = HouseholdCalendarKind.OTHER,
            startsOn = today,
            statusChangedOn = today,
          ),
        ),
      ),
    )

    val results = searchWorkspace(withCollisions, "collision target")

    assertEquals(2, results.size)
    assertEquals(setOf(WorkspaceId("household.second")), results.map { it.address.workspaceId }.toSet())
    assertEquals(
      setOf(
        WorkspaceRecordKind.HOUSEHOLD_RESPONSIBILITY,
        WorkspaceRecordKind.HOUSEHOLD_CALENDAR_ITEM,
      ),
      results.map { it.address.kind }.toSet(),
    )
  }
}
