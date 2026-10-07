package io.github.gilnetizen.aseh

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.AndroidComposeTestRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.gilnetizen.aseh.core.database.ManualPlaceContext
import io.github.gilnetizen.aseh.core.database.PlaceContextSource
import io.github.gilnetizen.aseh.core.model.CalendarRegion
import io.github.gilnetizen.aseh.core.model.DeviceUseMode
import io.github.gilnetizen.aseh.core.model.ExperienceState
import io.github.gilnetizen.aseh.core.model.ParticipantRole
import io.github.gilnetizen.aseh.core.model.ReadingPreparationStatus
import io.github.gilnetizen.aseh.core.model.WorkspaceKind
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Exercises the development demonstrator as one persisted, offline Shabbat workflow.
 *
 * This intentionally crosses feature boundaries instead of restating individual screen tests:
 * preparation feeds readiness, Build choices feed Prayer and the packet, conductor progress
 * resumes after recreation, and Ask ASEH resolves a claim to its exact installed source.
 */
@RunWith(AndroidJUnit4::class)
class ShabbatDemonstratorWorkflowTest {
  private val instrumentation = InstrumentationRegistry.getInstrumentation()
  private val activityRule = ActivityScenarioRule<MainActivity>(
    Intent(instrumentation.targetContext, MainActivity::class.java).apply {
      action = Intent.ACTION_MAIN
      addCategory(Intent.CATEGORY_LAUNCHER)
    },
  )

  @get:Rule
  val composeRule = AndroidComposeTestRule(activityRule) { rule ->
    lateinit var activity: MainActivity
    rule.scenario.onActivity { activity = it }
    activity
  }

  @Before
  fun resetPersistedWorkflow() {
    runBlocking(Dispatchers.IO) {
      withTimeout(5_000) {
        resetExperienceState()
        manualPlaceContextRepository().save(TEST_DEVICE_PLACE)
        experienceRepository().state.first(::isDefaultExperienceState)
      }
    }
    synchronizeSelectedDestination("now")
    composeRule.activityRule.scenario.recreate()
    waitUntilSelected("now")
  }

  @After
  fun clearPersistedWorkflow() {
    runBlocking(Dispatchers.IO) {
      withTimeout(5_000) {
        resetExperienceState()
        manualPlaceContextRepository().clear()
      }
    }
  }

  @Test
  fun preparationBuildPrayerStudyAndPacketFormOnePersistedWorkflow() {
    browsePracticeBeforeServiceSetupWithoutRecordingProgress()
    configureWorkspaceTeamReadingChoiceAndCharter()
    completePreparationChecklistAndVerifyItSurvivesNavigation()
    verifyShareChooserReceivesTheConfiguredPacket()
    completeAConductorStepAndResumeAfterRecreation()
    askAQuestionAndOpenItsExactInstalledSource()
  }

  private fun browsePracticeBeforeServiceSetupWithoutRecordingProgress() {
    selectDestination("practice")
    composeRule.onNodeWithTag("practice-catalog")
      .performScrollToNode(hasTestTag("practice-card-practice.rehearsal.team"))
    composeRule.onNodeWithTag("practice-card-practice.rehearsal.team")
      .assertIsDisplayed()
      .performClick()
    composeRule.onNodeWithTag("practice-detail")
      .performScrollToNode(hasTestTag("practice-progress-unavailable"))
    composeRule.onNodeWithTag("practice-progress-unavailable")
      .assertIsDisplayed()
    composeRule.onNodeWithTag("practice-detail")
      .performScrollToNode(hasTestTag("practice-step-practice.team.leader"))
    composeRule.onNodeWithTag("practice-step-practice.team.leader")
      .assertIsOff()
      .assertIsNotEnabled()
      .performTouchInput { click() }
    composeRule.waitForIdle()

    val state = runBlocking(Dispatchers.IO) { experienceRepository().state.first() }
    assertTrue(
      "Practice browsing without a service occurrence must not persist step progress",
      state.completedPracticeStepIds.isEmpty(),
    )

    pressSystemBack()
    composeRule.onNodeWithTag("practice-catalog").assertIsDisplayed()
  }

  private fun completePreparationChecklistAndVerifyItSurvivesNavigation() {
    selectDestination("practice")
    PREPARATION_CARDS.forEachIndexed { index, (cardId, stepIds) ->
      composeRule.onNodeWithTag("practice-catalog")
        .performScrollToNode(hasTestTag("practice-card-$cardId"))
      composeRule.onNodeWithTag("practice-card-$cardId")
        .performClick()
      stepIds.forEach { stepId ->
        composeRule.onNodeWithTag("practice-detail")
          .performScrollToNode(hasTestTag("practice-step-$stepId"))
        composeRule.onNodeWithTag("practice-step-$stepId")
          .assertIsOff()
          .assertIsEnabled()
          .performClick()
        waitForExperienceState("completed preparation step $stepId") {
          stepId in it.completedPracticeStepIds
        }
      }
      if (index < PREPARATION_CARDS.lastIndex) {
        pressSystemBack()
        composeRule.onNodeWithTag("practice-catalog").assertIsDisplayed()
      }
    }

    waitForExperienceState("the complete Shabbat preparation checklist") {
      it.completedPracticeStepIds.containsAll(PREPARATION_STEP_IDS)
    }

    selectDestination("study")
    selectDestination("practice")
    openTeamPreparationCard()
    composeRule.onNodeWithTag("practice-detail")
      .performScrollToNode(hasTestTag("practice-step-practice.team.leader"))
    composeRule.onNodeWithTag("practice-step-practice.team.leader")
      .assertIsOn()
  }

  private fun openTeamPreparationCard() {
    composeRule.onNodeWithTag("practice-card-practice.rehearsal.team")
      .performScrollTo()
      .assertIsDisplayed()
      .performClick()
    composeRule.waitUntil(timeoutMillis = 5_000) {
      composeRule.onAllNodesWithTag("practice-detail").fetchSemanticsNodes().isNotEmpty()
    }
  }

  private fun configureWorkspaceTeamReadingChoiceAndCharter() {
    selectDestination("build")
    composeRule.onNodeWithTag("workspace-open-service-setup")
      .assertIsDisplayed()
      .performClick()

    selectBuildStage("setup")
    composeRule.onNodeWithText("Household")
      .performScrollTo()
      .performClick()
    composeRule.onNodeWithTag("build-workspace-name")
      .performScrollTo()
      .performTextReplacement(WORKSPACE_NAME)
    composeRule.onNodeWithTag("build-save-workspace")
      .performScrollTo()
      .performClick()
    composeRule.onNodeWithTag("build-calendar-region-israel")
      .performScrollTo()
      .performClick()
    composeRule.onNodeWithTag("build-access-reviewed")
      .performScrollTo()
      .performClick()
    waitForExperienceState("reviewed participant access needs") { state ->
      state.accessibilityProfile.participantNeedsReviewed
    }
    composeRule.onNodeWithTag("build-access-movement")
      .performScrollTo()
      .performClick()
    waitForExperienceState("movement alternatives") { state ->
      state.accessibilityProfile.participantNeedsReviewed &&
        state.accessibilityProfile.useMovementAlternatives
    }
    composeRule.onNodeWithTag("build-access-visual-cues")
      .performScrollTo()
      .performClick()
    waitForExperienceState("visual voice and response cues") { state ->
      state.calendarRegion == CalendarRegion.ISRAEL &&
        state.serviceInstanceKey != null &&
      state.accessibilityProfile.participantNeedsReviewed &&
        state.accessibilityProfile.useMovementAlternatives &&
        state.accessibilityProfile.useVisualVoiceCues
    }

    selectBuildStage("team-readings")
    assignRole("leader", LEADER_NAME)
    assignRole("reader", READER_NAME)
    assignRole("gabbai", GABBAI_NAME)
    assignRole("host", HOST_NAME)

    READING_SLOTS.forEachIndexed { index, slot ->
      val slotId = slot.first
      val name = if (index == 0) READER_NAME else "Reader ${index + 1}"
      composeRule.onNodeWithTag("build-reading-$slotId")
        .performScrollTo()
        .performTextReplacement(name)
      composeRule.onNodeWithTag("build-reading-backup-$slotId")
        .performScrollTo()
        .performTextReplacement("Backup ${index + 1}")
      composeRule.onNodeWithTag("build-reading-status-$slotId-ready")
        .performScrollTo()
        .performClick()
      if (index == 0) {
        composeRule.onNodeWithTag("build-reading-manual-override-$slotId")
          .performScrollTo()
          .performClick()
        composeRule.onNodeWithTag("build-reading-portion-$slotId")
          .performScrollTo()
          .performTextReplacement(SYNTHETIC_PORTION)
        composeRule.onNodeWithTag("build-reading-locator-$slotId")
          .performScrollTo()
          .performTextReplacement(SYNTHETIC_LOCATOR)
        composeRule.onNodeWithTag("build-reading-range-$slotId")
          .performScrollTo()
          .performTextReplacement(SYNTHETIC_RANGE)
        composeRule.onNodeWithTag("build-reading-override-reason-$slotId")
          .performScrollTo()
          .performTextReplacement(SYNTHETIC_OVERRIDE_REASON)
      }
      composeRule.waitForIdle()
      composeRule.onNodeWithTag("build-reading-status-$slotId-ready")
        .assertIsSelected()
      composeRule.onNodeWithTag("build-reading-backup-$slotId")
        .assertTextContains("Backup ${index + 1}")
      composeRule.onNodeWithTag("build-save-reading-$slotId")
        .performScrollTo()
        .performClick()
      waitForExperienceState("the saved ${slot.second} plan") { state ->
        state.readingPlans[slotId]?.let { plan ->
          plan.assignee == name &&
            plan.backupAssignee == "Backup ${index + 1}" &&
            plan.preparationStatus == ReadingPreparationStatus.READY
        } == true
      }
    }

    selectBuildStage("community")
    composeRule.onNodeWithTag("build-charter-template")
      .performScrollTo()
      .performClick()
    composeRule.onNodeWithTag("build-charter-effective-date")
      .performScrollTo()
      .performTextReplacement(CHARTER_EFFECTIVE_DATE)
    composeRule.onNodeWithTag("build-charter-review-date")
      .performScrollTo()
      .performTextReplacement(CHARTER_REVIEW_DATE)
    composeRule.onNodeWithTag("build-save-charter")
      .performScrollTo()
      .performClick()
    waitForExperienceState("the saved charter draft") { state ->
      state.communityCharter.effectiveDate == CHARTER_EFFECTIVE_DATE &&
        state.communityCharter.reviewDate == CHARTER_REVIEW_DATE &&
        !state.communityCharter.adopted
    }
    composeRule.onNodeWithTag("build-adopt-charter")
      .performScrollTo()
      .performClick()
    waitForExperienceState("the adopted charter") { state ->
      state.communityCharter.adopted
    }
    composeRule.onNodeWithTag("build-adopt-charter")
      .performScrollTo()
      .assertIsOn()

    composeRule.onNodeWithText(COMMUNITY_OPTION_TITLE)
      .performScrollTo()
      .performClick()

    composeRule.onNodeWithTag("build-dossier-status")
      .performScrollTo()
      .assertTextContains("UNRESOLVED", substring = true)
    composeRule.onNodeWithTag("build-dossier-fact-1")
      .performScrollTo()
      .performClick()
    composeRule.onNodeWithTag("build-dossier-option-$DOSSIER_OPTION_ID")
      .performScrollTo()
      .performClick()
    composeRule.onNodeWithTag("build-dossier-adoption-scope")
      .performScrollTo()
      .performTextReplacement(WORKSPACE_NAME)
    composeRule.onNodeWithTag("build-dossier-effective-date")
      .performScrollTo()
      .performTextReplacement(CHARTER_EFFECTIVE_DATE)
    composeRule.onNodeWithTag("build-dossier-review-date")
      .performScrollTo()
      .performTextReplacement(CHARTER_REVIEW_DATE)
    composeRule.onNodeWithTag("build-dossier-recorded-by")
      .performScrollTo()
      .performTextReplacement(DOSSIER_RECORDED_BY)
    composeRule.onNodeWithTag("build-save-dossier-adoption")
      .performScrollTo()
      .performClick()

    selectBuildStage("packet-data")
    composeRule.onNodeWithTag("build-device-mode-print_only")
      .performScrollTo()
      .performClick()

    waitForExperienceState("the configured workspace") { state ->
      state.workspaceName == WORKSPACE_NAME &&
        state.workspaceKind == WorkspaceKind.HOUSEHOLD &&
        state.roleAssignments[ParticipantRole.LEADER] == LEADER_NAME &&
        state.roleAssignments[ParticipantRole.READER] == READER_NAME &&
        state.roleAssignments[ParticipantRole.GABBAI] == GABBAI_NAME &&
        state.readingAssignments[READING_SLOT_ID] == READER_NAME &&
        state.readingAssignments.size == 8 &&
        state.readingPlans.size == 8 &&
        state.readingPlans.values.all { plan ->
          plan.preparationStatus == ReadingPreparationStatus.READY
        } &&
        state.readingPlans[READING_SLOT_ID]?.backupAssignee == "Backup 1" &&
        state.readingPlans[READING_SLOT_ID]?.manualOverride == true &&
        state.deviceUseMode == DeviceUseMode.PRINT_ONLY &&
        state.calendarRegion == CalendarRegion.ISRAEL &&
        state.accessibilityProfile.participantNeedsReviewed &&
        state.accessibilityProfile.useMovementAlternatives &&
        state.accessibilityProfile.useVisualVoiceCues &&
        state.selectedCommunityOptionId == COMMUNITY_OPTION_ID &&
        state.communityCharter.adopted &&
        DOSSIER_FACT_ID in state.reviewedDossierFactIds &&
        state.disputedPracticeAdoption.optionId == DOSSIER_OPTION_ID &&
        state.disputedPracticeAdoption.scope == WORKSPACE_NAME
    }

    composeRule.activityRule.scenario.recreate()
    waitUntilSelected("build")
    waitForExperienceState("the configured workspace after recreation") { state ->
      state.workspaceName == WORKSPACE_NAME &&
        state.workspaceKind == WorkspaceKind.HOUSEHOLD &&
        state.roleAssignments[ParticipantRole.LEADER] == LEADER_NAME &&
        state.roleAssignments[ParticipantRole.GABBAI] == GABBAI_NAME &&
        state.readingAssignments[READING_SLOT_ID] == READER_NAME &&
        state.readingAssignments.size == 8 &&
        state.readingPlans.size == 8 &&
        state.readingPlans.values.all { plan ->
          plan.preparationStatus == ReadingPreparationStatus.READY
        } &&
        state.readingPlans[READING_SLOT_ID]?.backupAssignee == "Backup 1" &&
        state.readingPlans[READING_SLOT_ID]?.manualOverride == true &&
        state.deviceUseMode == DeviceUseMode.PRINT_ONLY &&
        state.calendarRegion == CalendarRegion.ISRAEL &&
        state.selectedCommunityOptionId == COMMUNITY_OPTION_ID &&
        state.communityCharter.adopted &&
        state.disputedPracticeAdoption.optionId == DOSSIER_OPTION_ID
    }

    selectBuildStage("setup")
    composeRule.onNodeWithTag("build-workspace-name")
      .performScrollTo()
      .assertTextContains(WORKSPACE_NAME)
    selectBuildStage("team-readings")
    composeRule.onNodeWithTag("build-reading-reading.aliyah.1")
      .performScrollTo()
      .assertTextContains(READER_NAME)
    composeRule.onNodeWithTag("build-reading-status-reading.aliyah.1-ready")
      .performScrollTo()
      .assertIsSelected()
    selectBuildStage("packet-data")
    composeRule.onNodeWithTag("build-device-mode-print_only")
      .performScrollTo()
      .assertIsDisplayed()
    selectBuildStage("community")
    composeRule.onNodeWithTag("build-adopt-charter")
      .performScrollTo()
      .assertIsOn()
    selectBuildStage("packet-data")
    composeRule.onNodeWithTag("build-print-packet")
      .performScrollTo()
      .assertIsDisplayed()
      .assertHasClickAction()
  }

  private fun verifyShareChooserReceivesTheConfiguredPacket() {
    selectDestination("build")
    if (composeRule.onAllNodesWithTag("workspace-open-service-setup").fetchSemanticsNodes().isNotEmpty()) {
      composeRule.onNodeWithTag("workspace-open-service-setup").performClick()
    }
    selectBuildStage("packet-data")
    composeRule.onNodeWithTag("build-print-packet")
      .performScrollTo()
      .performClick()
    composeRule.onNodeWithTag("build-export-preview-dialog").assertIsDisplayed()
    composeRule.onNodeWithTag("build-export-preview-accessible-equivalent")
      .assertIsDisplayed()
    composeRule.onNodeWithTag("build-export-preview-payload")
      .assertTextContains(WORKSPACE_NAME, substring = true)
      .assertTextContains(READER_NAME, substring = true)
    composeRule.onNodeWithTag("build-export-preview-cancel").performClick()
    composeRule.onNodeWithTag("build-export-preview-dialog").assertDoesNotExist()

    val chooserMonitor = CapturingChooserMonitor()
    instrumentation.addMonitor(chooserMonitor)
    try {
      composeRule.onNodeWithTag("build-share-packet")
        .performScrollTo()
        .performClick()
      composeRule.onNodeWithTag("build-export-preview-dialog").assertIsDisplayed()
      composeRule.onNodeWithTag("build-export-preview-payload")
        .assertTextContains("Device use: Print only", substring = true)
      composeRule.onNodeWithTag("build-export-preview-confirm").performClick()
      composeRule.waitUntil(timeoutMillis = 5_000) {
        chooserMonitor.chooserIntent != null
      }

      val chooserIntent = requireNotNull(chooserMonitor.chooserIntent)
      @Suppress("DEPRECATION")
      val sendIntent = chooserIntent.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)
      assertNotNull("The chooser must wrap the generated packet share intent", sendIntent)
      requireNotNull(sendIntent)
      assertEquals(Intent.ACTION_SEND, sendIntent.action)
      assertEquals("text/plain", sendIntent.type)

      val packet = sendIntent.getStringExtra(Intent.EXTRA_TEXT)
      assertNotNull("The share intent must include the generated packet", packet)
      requireNotNull(packet)
      assertTrue(packet.contains("Workspace: $WORKSPACE_NAME"))
      assertTrue(packet.contains("Status: BLOCKED"))
      assertTrue(packet.contains("Place: Current device location"))
      assertTrue(packet.contains("Time zone: Asia/Jerusalem"))
      assertTrue(packet.contains("Coordinates: 31.778, 35.235"))
      assertTrue(packet.contains("Prayer leader: $LEADER_NAME"))
      assertTrue(packet.contains("First aliyah (Aliyah): $READER_NAME"))
      assertTrue(packet.contains("Backup: Backup 1"))
      assertTrue(packet.contains("Preparation: Ready"))
      assertTrue(packet.contains("Portion: $SYNTHETIC_PORTION"))
      assertTrue(packet.contains("Manual local override; not verified by an installed source"))
      assertTrue(packet.contains("Device use: Print only"))
      assertTrue(packet.contains("because this workspace selected \"$COMMUNITY_OPTION_TITLE\""))
      assertTrue(packet.contains("Evidence status: Unresolved"))
      assertTrue(packet.contains("Option: Prepared-lighting precaution"))
      assertTrue(packet.contains("Adoption does not change the dossier's Unresolved evidence status"))
      assertTrue(packet.contains("Source text: UNAVAILABLE"))
      assertTrue(packet.contains("Edition ID: NOT SELECTED — LITURGY-001 open"))
      assertTrue(packet.contains("License: NOT ESTABLISHED — distribution blocked"))
      assertTrue(packet.contains("No prayer text is included in this installed catalog"))
    } finally {
      instrumentation.removeMonitor(chooserMonitor)
    }
  }

  private fun completeAConductorStepAndResumeAfterRecreation() {
    selectDestination("prayer")
    assertTaggedSubtreeContains("prayer-assembly-decision", COMMUNITY_OPTION_TITLE)
    composeRule.onNodeWithTag("prayer-role-leader")
      .performScrollTo()
      .performClick()
    waitForExperienceState("the selected prayer-leader role") {
      it.selectedRole == ParticipantRole.LEADER
    }

    composeRule.onNodeWithTag("prayer-readiness-readiness.preflight.incomplete")
      .performScrollTo()
      .assertIsDisplayed()
    composeRule.onNodeWithTag("prayer-start-resume")
      .performScrollTo()
      .assertIsNotEnabled()

    PREFLIGHT_IDS.forEach { id ->
      composeRule.onNodeWithTag("prayer-preflight-$id")
        .performScrollTo()
        .performClick()
    }
    waitForExperienceState("the completed rehearsal preflight") { state ->
      state.completedPreflightStepIds.containsAll(PREFLIGHT_IDS)
    }

    composeRule.onNodeWithTag("prayer-readiness-readiness.device.print-only")
      .performScrollTo()
      .assertIsDisplayed()

    composeRule.onNodeWithTag("prayer-start-resume")
      .performScrollTo()
      .assertIsEnabled()
      .performClick()
    composeRule.onNodeWithTag("prayer-focused-conductor").assertIsDisplayed()
    composeRule.onNodeWithTag("prayer-current-segment-title")
      .assertTextContains("Gather and orient")
    assertTaggedSubtreeContains("prayer-assigned-person", LEADER_NAME)
    assertTaggedSubtreeContains("prayer-movement-cue", "movement alternatives")
    composeRule.onNodeWithTag("prayer-complete-current")
      .performClick()
    waitForExperienceState("the completed first conductor segment") {
      "segment.gather" in it.completedServiceSegmentIds
    }

    composeRule.onNodeWithTag("prayer-open-overview").performClick()
    composeRule.onNodeWithTag("prayer-timeline-segment.reading")
      .performScrollTo()
      .performClick()
    composeRule.onNodeWithTag("prayer-focused-conductor").assertIsDisplayed()
    assertTaggedSubtreeContains("prayer-reading-primary-$READING_SLOT_ID", READER_NAME)
    assertTaggedSubtreeContains("prayer-reading-backup-$READING_SLOT_ID", "Backup 1")
    assertTaggedSubtreeContains("prayer-reading-preparation-$READING_SLOT_ID", "Ready")
    assertTaggedSubtreeContains("prayer-reading-portion-$READING_SLOT_ID", SYNTHETIC_PORTION)
    assertTaggedSubtreeContains("prayer-reading-locator-$READING_SLOT_ID", SYNTHETIC_LOCATOR)
    assertTaggedSubtreeContains("prayer-reading-range-$READING_SLOT_ID", SYNTHETIC_RANGE)
    composeRule.onNodeWithTag("prayer-reading-passage-unavailable-reading.aliyah.2")
      .performScrollTo()
      .assertIsDisplayed()
    assertTaggedSubtreeContains("prayer-source-text", "UNAVAILABLE")
    assertTaggedSubtreeContains("prayer-content-edition-id", "LITURGY-001")
    assertTaggedSubtreeContains("prayer-content-license", "distribution blocked")

    composeRule.onNodeWithTag("prayer-open-overview").performClick()
    composeRule.activityRule.scenario.recreate()
    waitUntilSelected("prayer")
    composeRule.onNodeWithTag("prayer-start-resume")
      .performScrollTo()
      .assertTextContains("Resume", substring = true)
      .performClick()
    composeRule.onNodeWithTag("prayer-current-segment-title")
      .assertTextContains("Confirm the Israel calendar profile")
  }

  private fun askAQuestionAndOpenItsExactInstalledSource() {
    selectDestination("study")
    waitForDevelopmentPackTerminalState()
    composeRule.onNodeWithText("Try an exact-source question")
      .performScrollTo()
      .performClick()
    composeRule.onNodeWithTag("study-ask-question")
      .assertTextContains("Who owns each active role before rehearsal?")
    composeRule.onNodeWithTag("study-ask-submit")
      .performScrollTo()
      .performClick()

    composeRule.onNodeWithTag("study-structured-answer")
      .performScrollTo()
      .assertIsDisplayed()
    composeRule.onNodeWithTag("study-answer-local-only")
      .performScrollTo()
      .assertTextContains("No AI model or network was used", substring = true)
    composeRule.onNodeWithTag("study-claim-short-answer-open-source.demo.role-readiness")
      .performScrollTo()
      .performClick()
    composeRule.onNodeWithTag("study-source-heading")
      .assertIsDisplayed()
      .assertTextContains("ASEH rehearsal protocol")
    composeRule.onNodeWithText("Development fixture 1:1")
      .assertIsDisplayed()
  }

  private fun waitForDevelopmentPackTerminalState() {
    val terminalTags = listOf(
      "study-development-pack-ready",
      "study-development-pack-unsupported",
      "study-development-pack-failed",
    )
    composeRule.waitUntil(timeoutMillis = 20_000) {
      terminalTags.any { tag ->
        composeRule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()
      }
    }
  }

  private suspend fun resetExperienceState() {
    experienceRepository().clearAllExperienceData()
  }

  private fun isDefaultExperienceState(state: ExperienceState): Boolean =
    state.copy(serviceInstanceId = null, serviceInstanceDate = null) == ExperienceState()

  private fun selectDestination(destinationId: String) {
    composeRule.onNodeWithTag("destination-$destinationId").performClick()
    waitUntilSelected(destinationId)
  }

  private fun synchronizeSelectedDestination(destinationId: String) {
    appGraph().setSelectedDestinationId(TEST_NAVIGATION_BARRIER_ID)
    waitUntilPersistedDestination(TEST_NAVIGATION_BARRIER_ID)
    appGraph().setSelectedDestinationId(destinationId)
    waitUntilPersistedDestination(destinationId)
  }

  private fun waitUntilPersistedDestination(destinationId: String) {
    composeRule.waitUntil(timeoutMillis = 5_000) {
      runBlocking(Dispatchers.IO) {
        withTimeoutOrNull(250) {
          preferencesRepository().preferences.first().selectedDestinationId == destinationId
        } == true
      }
    }
  }

  private fun pressSystemBack() {
    composeRule.runOnIdle {
      composeRule.activity.onBackPressedDispatcher.onBackPressed()
    }
  }

  private fun waitUntilSelected(destinationId: String) {
    composeRule.waitUntil(timeoutMillis = 5_000) {
      runCatching {
        composeRule.onNodeWithTag("destination-$destinationId").assertIsSelected()
      }.isSuccess
    }
    runBlocking(Dispatchers.IO) {
      withTimeout(5_000) {
        preferencesRepository().preferences.first {
          it.selectedDestinationId == destinationId
        }
      }
    }
  }

  private fun selectBuildStage(stageId: String) {
    composeRule.onNodeWithTag("build-stage-$stageId")
      .performScrollTo()
      .performClick()
    composeRule.onNodeWithTag("build-stage-$stageId").assertIsSelected()
  }

  private fun waitForExperienceState(
    description: String,
    predicate: (ExperienceState) -> Boolean,
  ) {
    // UI callbacks launch persistence work from the app's Compose scope. Poll
    // one snapshot at a time through the Compose rule so the app's main-scope
    // coroutine can resume between observations. Holding one blocking Flow
    // collection here starves that coroutine under the Compose test scheduler.
    composeRule.waitForIdle()
    try {
      composeRule.waitUntil(timeoutMillis = 10_000) {
        val snapshot = runBlocking(Dispatchers.IO) {
          experienceRepository().state.first()
        }
        predicate(snapshot)
      }
    } catch (error: Throwable) {
      val latest = runBlocking(Dispatchers.IO) { experienceRepository().state.first() }
      throw AssertionError("Timed out waiting for $description. Latest state: $latest", error)
    }
    composeRule.waitForIdle()
  }

  private fun assertTaggedSubtreeContains(tag: String, text: String) {
    composeRule.onNode(
      hasTestTag(tag).and(
        hasAnyDescendant(hasText(text, substring = true, ignoreCase = true)),
      ),
      useUnmergedTree = true,
    )
      .performScrollTo()
      .assertIsDisplayed()
  }

  private fun preferencesRepository() = appGraph().interfacePreferencesRepository

  private fun assignRole(roleId: String, name: String) {
    composeRule.onNodeWithTag("build-role-$roleId")
      .performScrollTo()
      .performTextReplacement(name)
    composeRule.onNodeWithTag("build-save-role-$roleId")
      .performScrollTo()
      .performClick()
  }

  private fun experienceRepository() = appGraph().experienceStateRepository

  private fun manualPlaceContextRepository() = appGraph().manualPlaceContextRepository

  private fun appGraph() =
    (instrumentation.targetContext.applicationContext as AsehApplication).appGraph

  private class CapturingChooserMonitor : Instrumentation.ActivityMonitor() {
    @Volatile
    var chooserIntent: Intent? = null
      private set

    override fun onStartActivity(intent: Intent): Instrumentation.ActivityResult? {
      if (intent.action != Intent.ACTION_CHOOSER) return null
      chooserIntent = Intent(intent)
      return Instrumentation.ActivityResult(Activity.RESULT_CANCELED, null)
    }
  }

  private companion object {
    const val TEST_NAVIGATION_BARRIER_ID = "test_sync"
    const val WORKSPACE_NAME = "Har Nof Shabbat Lab"
    const val LEADER_NAME = "Leah Test"
    const val READER_NAME = "Miriam Test"
    const val GABBAI_NAME = "Ari Test"
    const val HOST_NAME = "Noa Test"
    const val READING_SLOT_ID = "reading.aliyah.1"
    const val SYNTHETIC_PORTION = "Synthetic local portion"
    const val SYNTHETIC_LOCATOR = "Synthetic prepared copy"
    const val SYNTHETIC_RANGE = "Demo sections 1-8"
    const val SYNTHETIC_OVERRIDE_REASON =
      "Verified against this test's synthetic prepared copy"
    const val CHARTER_EFFECTIVE_DATE = "This rehearsal"
    const val CHARTER_REVIEW_DATE = "After the first rehearsal"
    const val COMMUNITY_OPTION_ID = "choice.teaching.before-close"
    const val COMMUNITY_OPTION_TITLE = "Before the closing review"
    const val DOSSIER_OPTION_ID = "adoption.lighting.prepared-precaution"
    const val DOSSIER_FACT_ID = "dossier.shabbat-electric-lighting.fact.1"
    const val DOSSIER_RECORDED_BY = "Workflow test coordinator"
    val PREFLIGHT_IDS = setOf(
      "preflight.roles",
      "preflight.packet",
      "preflight.access",
      "preflight.offline",
    )
    val PREPARATION_CARDS = listOf(
      "practice.rehearsal.team" to listOf(
        "practice.team.leader",
        "practice.team.reader",
        "practice.team.gabbai",
        "practice.team.backup",
      ),
      "practice.rehearsal.access" to listOf(
        "practice.access.path",
        "practice.access.seating",
        "practice.access.text",
        "practice.access.sound",
      ),
      "practice.rehearsal.offline" to listOf(
        "practice.offline.review",
        "practice.offline.choice",
        "practice.offline.export",
        "practice.offline.backup",
      ),
    )
    val PREPARATION_STEP_IDS = PREPARATION_CARDS.flatMap { (_, steps) -> steps }.toSet()
    val READING_SLOTS = listOf(
      "reading.aliyah.1" to "First aliyah",
      "reading.aliyah.2" to "Second aliyah",
      "reading.aliyah.3" to "Third aliyah",
      "reading.aliyah.4" to "Fourth aliyah",
      "reading.aliyah.5" to "Fifth aliyah",
      "reading.aliyah.6" to "Sixth aliyah",
      "reading.aliyah.7" to "Seventh aliyah",
      "reading.maftir" to "Maftir",
    )
    val TEST_DEVICE_PLACE = ManualPlaceContext(
      label = "",
      latitudeDegrees = 31.778,
      longitudeDegrees = 35.235,
      elevationMeters = 754.0,
      timeZoneId = "Asia/Jerusalem",
      source = PlaceContextSource.DEVICE,
      horizontalAccuracyMeters = 15.0,
    )
  }
}
