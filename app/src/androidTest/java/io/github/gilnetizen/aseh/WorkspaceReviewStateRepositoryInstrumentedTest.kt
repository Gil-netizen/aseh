package io.github.gilnetizen.aseh

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.gilnetizen.aseh.domain.workspace.InventoryItemStatus
import io.github.gilnetizen.aseh.domain.workspace.SyntheticWorkspaceFixtures
import io.github.gilnetizen.aseh.domain.workspace.WorkspaceCommand
import io.github.gilnetizen.aseh.domain.workspace.WorkspaceId
import io.github.gilnetizen.aseh.domain.workspace.WorkspaceRecordId
import io.github.gilnetizen.aseh.domain.workspace.WorkspaceUpdateResult
import java.time.LocalDate
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WorkspaceReviewStateRepositoryInstrumentedTest {
  private val context: Context
    get() = InstrumentationRegistry.getInstrumentation().targetContext

  private val preferences
    get() = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

  @Before
  fun clearBefore() {
    assertTrue(preferences.edit().clear().commit())
  }

  @After
  fun clearAfter() {
    assertTrue(preferences.edit().clear().commit())
  }

  @Test
  fun persistedCommandsReplayAcrossRepositoryInstancesAndClearSafely() = runBlocking {
    val fixture = SyntheticWorkspaceFixtures.reviewScenario
    val first = WorkspaceReviewStateRepositoryFactory.create(context, fixture)
    val result = first.apply(outOfServiceCommand())
    assertTrue(result is WorkspaceUpdateResult.Applied)

    val reopened = WorkspaceReviewStateRepositoryFactory.create(context, fixture)
    assertEquals(InventoryItemStatus.OUT_OF_SERVICE, reopened.inventoryStatus())

    reopened.clear()
    val cleared = WorkspaceReviewStateRepositoryFactory.create(context, fixture)
    assertEquals(InventoryItemStatus.ACTIVE, cleared.inventoryStatus())
    assertNull(preferences.getString("command_log", null))
  }

  @Test
  fun corruptOrRejectedLogNeverChangesTheFixture() = runBlocking {
    val fixture = SyntheticWorkspaceFixtures.reviewScenario
    preferences.edit()
      .putString("fixture_id", fixture.id)
      .putString("command_log", "not-json")
      .commit()

    val recovered = WorkspaceReviewStateRepositoryFactory.create(context, fixture)
    assertEquals(InventoryItemStatus.ACTIVE, recovered.inventoryStatus())
    assertFalse(preferences.contains("command_log"))

    val rejected = recovered.apply(
      outOfServiceCommand().copy(target = InventoryItemStatus.ACTIVE),
    )
    assertTrue(rejected is WorkspaceUpdateResult.Rejected)
    val reopened = WorkspaceReviewStateRepositoryFactory.create(context, fixture)
    assertEquals(InventoryItemStatus.ACTIVE, reopened.inventoryStatus())
    assertNull(preferences.getString("command_log", null))
  }

  private fun outOfServiceCommand() = WorkspaceCommand.ChangeInventoryStatus(
    qahalId = WorkspaceId("dev.qahal.synthetic"),
    id = WorkspaceRecordId("dev.qahal.inventory.chairs"),
    target = InventoryItemStatus.OUT_OF_SERVICE,
    changedOn = LocalDate.parse("2026-10-07"),
  )

  private fun WorkspaceReviewStateRepository.inventoryStatus(): InventoryItemStatus =
    state.value.qahalWorkspaces.single().inventory.single().status

  private companion object {
    const val PREFERENCES_NAME = "aseh_workspace_review_state"
  }
}
