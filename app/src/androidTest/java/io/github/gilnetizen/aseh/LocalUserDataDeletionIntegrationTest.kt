package io.github.gilnetizen.aseh

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import io.github.gilnetizen.aseh.core.database.InterfacePreferences
import io.github.gilnetizen.aseh.core.database.LocalUserDataDeletion
import io.github.gilnetizen.aseh.core.database.ManualPlaceContext
import io.github.gilnetizen.aseh.core.database.PlaceContextSource
import io.github.gilnetizen.aseh.core.model.ExperienceState
import io.github.gilnetizen.aseh.core.model.WorkspaceKind
import io.github.gilnetizen.aseh.domain.workspace.InventoryItemStatus
import io.github.gilnetizen.aseh.domain.workspace.WorkspaceCommand
import io.github.gilnetizen.aseh.domain.workspace.WorkspaceId
import io.github.gilnetizen.aseh.domain.workspace.WorkspaceRecordId
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class LocalUserDataDeletionIntegrationTest {
  @get:Rule
  val composeRule = createAndroidComposeRule<MainActivity>()

  @Before
  fun seedAllLocalUserDataCategories() {
    runBlocking(Dispatchers.IO) {
      deletion().deleteAll()
      graph.workspaceReviewStateRepository?.clear()
      graph.experienceStateRepository.setWorkspace(
        name = "Private rehearsal",
        kind = WorkspaceKind.HOUSEHOLD,
      )
      graph.experienceStateRepository.setPracticeCardSaved("practice.private", true)
      graph.manualPlaceContextRepository.save(
        ManualPlaceContext(
          label = "",
          latitudeDegrees = 31.778,
          longitudeDegrees = 35.235,
          elevationMeters = null,
          timeZoneId = "Asia/Jerusalem",
          source = PlaceContextSource.DEVICE,
          horizontalAccuracyMeters = 9.0,
        ),
      )
      graph.interfacePreferencesRepository.setTextScale(1.4f)
      graph.interfacePreferencesRepository.setDyslexiaFriendlyLatinEnabled(true)
      graph.workspaceReviewStateRepository?.apply(
        WorkspaceCommand.ChangeInventoryStatus(
          qahalId = WorkspaceId("dev.qahal.synthetic"),
          id = WorkspaceRecordId("dev.qahal.inventory.chairs"),
          target = InventoryItemStatus.OUT_OF_SERVICE,
          changedOn = LocalDate.parse("2026-10-07"),
        ),
      )
    }
  }

  @After
  fun removeTestData() {
    runBlocking(Dispatchers.IO) {
      deletion().deleteAll()
      graph.workspaceReviewStateRepository?.clear()
    }
  }

  @Test
  fun confirmedBuildDeleteRemovesPlaceExperienceAndInterfacePreferences() {
    composeRule.onNodeWithTag("destination-build").performClick()
    composeRule.onNodeWithTag("workspace-open-service-setup").performClick()
    composeRule.onNodeWithTag("build-stage-packet-data")
      .performScrollTo()
      .performClick()

    composeRule.onNodeWithTag("build-delete-local-data")
      .performScrollTo()
      .performClick()
    composeRule.onNodeWithTag("build-delete-local-data-dialog")
      .assertIsDisplayed()
    composeRule.onNodeWithTag("build-confirm-delete-local-data")
      .performClick()

    composeRule.waitUntil(timeoutMillis = 10_000) {
      runBlocking(Dispatchers.IO) {
        graph.manualPlaceContextRepository.context.first() == null &&
          graph.experienceStateRepository.state.first().withoutDerivedServiceDate() == ExperienceState() &&
          graph.interfacePreferencesRepository.preferences.first() == InterfacePreferences()
          && graph.workspaceReviewStateRepository?.state?.first()
            ?.qahalWorkspaces?.single()?.inventory?.single()?.status == InventoryItemStatus.ACTIVE
      }
    }
    runBlocking(Dispatchers.IO) {
      assertNull(graph.manualPlaceContextRepository.context.first())
      assertEquals(
        ExperienceState(),
        graph.experienceStateRepository.state.first().withoutDerivedServiceDate(),
      )
      assertEquals(
        InterfacePreferences(),
        graph.interfacePreferencesRepository.preferences.first(),
      )
      assertEquals(
        InventoryItemStatus.ACTIVE,
        graph.workspaceReviewStateRepository?.state?.first()
          ?.qahalWorkspaces?.single()?.inventory?.single()?.status,
      )
    }

    composeRule.waitForIdle()
    composeRule.onNodeWithTag("destination-now").assertIsSelected()
  }

  private val graph: AppGraph
    get() = (composeRule.activity.application as AsehApplication).appGraph

  private fun deletion() = LocalUserDataDeletion(
    experienceStateRepository = graph.experienceStateRepository,
    manualPlaceContextRepository = graph.manualPlaceContextRepository,
    interfacePreferencesRepository = graph.interfacePreferencesRepository,
  )

  private fun ExperienceState.withoutDerivedServiceDate(): ExperienceState =
    copy(serviceInstanceDate = null)
}
