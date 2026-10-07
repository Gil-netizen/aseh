package io.github.gilnetizen.aseh

import android.Manifest
import android.content.Intent
import android.graphics.Bitmap
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.AndroidComposeTestRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.SemanticsMatcher
import androidx.core.os.LocaleListCompat
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.espresso.Espresso.pressBack
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.platform.io.PlatformTestStorageRegistry
import androidx.test.rule.GrantPermissionRule
import io.github.gilnetizen.aseh.core.database.ManualPlaceContext
import io.github.gilnetizen.aseh.core.model.WorkspaceKind
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AsehNavigationTest {
  private val activityRule = ActivityScenarioRule<MainActivity>(
    Intent(
      InstrumentationRegistry.getInstrumentation().targetContext,
      MainActivity::class.java,
    ).apply {
      action = Intent.ACTION_MAIN
      addCategory(Intent.CATEGORY_LAUNCHER)
    },
  )

  @get:Rule(order = 0)
  val locationPermissionRule: GrantPermissionRule =
    GrantPermissionRule.grant(Manifest.permission.ACCESS_COARSE_LOCATION)

  @get:Rule(order = 1)
  val composeRule = AndroidComposeTestRule(activityRule) { rule ->
    lateinit var activity: MainActivity
    rule.scenario.onActivity { activity = it }
    activity
  }

  @Before
  fun resetAppState() {
    runBlocking {
      withTimeout(5_000) {
        preferencesRepository().setSelectedDestinationId("now")
        manualPlaceContextRepository().clear()
        experienceStateRepository().setWorkspace("", WorkspaceKind.QAHAL)
        experienceStateRepository().setSelectedServicePlan(null)
        workspaceReviewStateRepository()?.clear()
        preferencesRepository().preferences.first { it.selectedDestinationId == "now" }
        manualPlaceContextRepository().context.first { it == null }
      }
    }
    waitUntilSelected("now")
    waitUntilManualPlaceCleared()
  }

  @After
  fun restoreAppState() {
    try {
      runBlocking {
        withTimeout(5_000) {
          manualPlaceContextRepository().clear()
          workspaceReviewStateRepository()?.clear()
          manualPlaceContextRepository().context.first { it == null }
        }
      }
    } finally {
      setApplicationLocales(LocaleListCompat.getEmptyLocaleList())
    }
  }

  @Test
  fun englishNavigationIsLtrAndEveryDestinationIsReachable() {
    setApplicationLanguage("en")

    composeRule.onNodeWithTag("destination-now")
      .assertIsDisplayed()
      .assertHasClickAction()
      .assertIsSelected()
    assertDestinationSemanticsAndTouchTargets()
    assertLogicalDestinationOrder(isRtl = false)

    listOf(
      "Practice" to "practice",
      "Pray" to "prayer",
      "Study" to "study",
      "Build" to "build",
      "Now" to "now",
    ).forEach { (label, destinationId) ->
      composeRule.onNodeWithContentDescription(label).performClick()
      composeRule.waitForIdle()
      composeRule.onNodeWithTag("destination-$destinationId").assertIsSelected()
    }

    assertNowContextIsUsable(isHebrewFallback = false)

    captureRoot("english-ltr.png")
  }

  @Test
  fun hebrewLocaleMirrorsNavigationAndLabelsFallbackLanguage() {
    setApplicationLanguage("he")

    assertLogicalDestinationOrder(isRtl = true)
    composeRule.onNodeWithContentDescription("EN · Build", substring = true).performClick()
    composeRule.onNodeWithContentDescription("EN · Now", substring = true).performClick()
    composeRule.waitForIdle()

    assertNowContextIsUsable(isHebrewFallback = true)

    captureRoot("hebrew-rtl.png")
  }

  @Test
  fun selectedDestinationSurvivesActivityRecreation() {
    composeRule.onNodeWithTag("destination-study").performClick()
    composeRule.activityRule.scenario.recreate()

    runBlocking {
      withTimeout(5_000) {
        preferencesRepository().preferences.first { it.selectedDestinationId == "study" }
      }
    }
    waitUntilSelected("study")
  }

  @Test
  fun latestRapidSelectionSurvivesActivityRecreation() {
    listOf("practice", "prayer", "study", "build").forEach { destinationId ->
      composeRule.onNodeWithTag("destination-$destinationId").performClick()
    }
    composeRule.activityRule.scenario.recreate()

    runBlocking {
      withTimeout(5_000) {
        preferencesRepository().preferences.first { it.selectedDestinationId == "build" }
      }
    }
    waitUntilSelected("build")
  }

  @Test
  fun globalSearchAndContextSwitchWorkFromEveryDestination() {
    setApplicationLanguage("en")

    composeRule.onNodeWithTag("app-global-search")
      .assertIsDisplayed()
      .performClick()
    composeRule.onNodeWithTag("app-global-search-input")
      .performTextInput("accessible path")
    composeRule.onNodeWithTag("app-global-search-result-practice.rehearsal.access")
      .assertIsDisplayed()
      .performClick()
    waitUntilSelected("practice")
    composeRule.onNodeWithTag("practice-detail").assertIsDisplayed()
    composeRule.onNodeWithText("Prepare an accessible path", substring = true)
      .assertIsDisplayed()

    composeRule.onNodeWithTag("app-global-context")
      .assertIsDisplayed()
      .performClick()
    composeRule.onNodeWithTag("app-context-household")
      .performClick()

    runBlocking {
      withTimeout(5_000) {
        experienceStateRepository().state.first { it.workspaceKind == WorkspaceKind.HOUSEHOLD }
      }
    }
    composeRule.onNodeWithTag("app-global-context")
      .assertTextContains("Household", substring = true)
  }

  @Test
  fun functionalReviewCardOpensTheInteractiveProductFlows() {
    setApplicationLanguage("en")

    composeRule.onNodeWithTag("now-functional-review")
      .performScrollTo()
      .assertIsDisplayed()
    composeRule.onNodeWithTag("now-functional-open-prayer")
      .performScrollTo()
      .assertHasClickAction()
      .performClick()
    waitUntilSelected("prayer")
    composeRule.onNodeWithTag("prayer-screen").assertIsDisplayed()

    pressBack()
    waitUntilSelected("now")
    composeRule.onNodeWithTag("now-functional-open-practice")
      .performScrollTo()
      .assertHasClickAction()
      .performClick()
    waitUntilSelected("practice")
    composeRule.onNodeWithTag("practice-catalog").assertIsDisplayed()

    pressBack()
    waitUntilSelected("now")
    composeRule.onNodeWithTag("now-functional-open-build")
      .performScrollTo()
      .assertHasClickAction()
      .performClick()
    waitUntilSelected("build")
    composeRule.onNodeWithTag("build-heading").assertIsDisplayed()
  }

  @Test
  fun workspaceContextsAreDistinctAndLifecycleChangesSurviveRecreation() {
    setApplicationLanguage("en")

    composeRule.onNodeWithTag("destination-build").performClick()
    composeRule.onNodeWithTag("workspace-dashboard").assertIsDisplayed()
    composeRule.onNodeWithTag("workspace-due-dev.qahal.inventory.chairs")
      .performScrollTo().assertIsDisplayed()

    composeRule.onNodeWithTag("app-global-context").performClick()
    composeRule.onNodeWithTag("app-context-household").performClick()
    runBlocking {
      withTimeout(5_000) {
        experienceStateRepository().state.first { it.workspaceKind == WorkspaceKind.HOUSEHOLD }
      }
    }
    composeRule.onNodeWithTag("workspace-due-dev.household.responsibility.table")
      .performScrollTo().assertIsDisplayed()
    val completeResponsibilityTag =
      "workspace-action-mark_done-dev.household.responsibility.table"
    composeRule.onNodeWithTag("workspace-dashboard")
      .performScrollToNode(hasTestTag(completeResponsibilityTag))
    composeRule.onNodeWithTag(completeResponsibilityTag).performClick()
    composeRule.waitUntil(timeoutMillis = 5_000) {
      composeRule.onAllNodesWithTag(
        "workspace-action-reopen-dev.household.responsibility.table",
      ).fetchSemanticsNodes().isNotEmpty()
    }
    val reopenResponsibilityTag =
      "workspace-action-reopen-dev.household.responsibility.table"
    composeRule.onNodeWithTag(reopenResponsibilityTag).assertIsDisplayed()

    composeRule.activityRule.scenario.recreate()
    waitUntilSelected("build")
    composeRule.onNodeWithTag("workspace-dashboard")
      .performScrollToNode(hasTestTag(reopenResponsibilityTag))
    composeRule.onNodeWithTag(reopenResponsibilityTag).assertIsDisplayed()

    composeRule.onNodeWithTag("app-global-context").performClick()
    composeRule.onNodeWithTag("app-context-self").performClick()
    runBlocking {
      withTimeout(5_000) {
        experienceStateRepository().state.first { it.workspaceKind == WorkspaceKind.SELF }
      }
    }
    composeRule.onNodeWithTag("workspace-due-dev.self.practice.preparation")
      .performScrollTo().assertIsDisplayed()
  }

  @Test
  fun systemBackUnwindsNestedViewsBeforeReturningToNow() {
    setApplicationLanguage("en")

    composeRule.onNodeWithTag("destination-practice").performClick()
    composeRule.onNodeWithTag("practice-card-practice.rehearsal.team")
      .performScrollTo()
      .performClick()
    composeRule.onNodeWithTag("practice-detail").assertIsDisplayed()
    pressBack()
    composeRule.onNodeWithTag("practice-catalog").assertIsDisplayed()

    pressBack()
    waitUntilSelected("now")

    composeRule.onNodeWithTag("destination-study").performClick()
    composeRule.onNodeWithTag("study-source-source.demo.role-readiness")
      .performScrollTo()
      .performClick()
    composeRule.onNodeWithTag("study-source-heading").assertIsDisplayed()
    pressBack()
    composeRule.onNodeWithTag("study-heading").assertIsDisplayed()

    pressBack()
    waitUntilSelected("now")

    composeRule.onNodeWithTag("destination-prayer").performClick()
    composeRule.onNodeWithTag("prayer-preview-with-blockers")
      .performScrollTo()
      .performClick()
    composeRule.onNodeWithTag("prayer-focused-conductor").assertIsDisplayed()
    pressBack()
    composeRule.onNodeWithTag("prayer-screen").assertIsDisplayed()

    pressBack()
    waitUntilSelected("now")
  }

  @Test
  fun missingPlaceIsActionableFromAnotherDestination() {
    setApplicationLanguage("en")

    composeRule.onNodeWithTag("destination-study").performClick()
    waitUntilSelected("study")

    composeRule.onNodeWithTag("app-place-context-bar").assertIsDisplayed()
    composeRule.onNodeWithTag("app-place-context-status", useUnmergedTree = true)
      .assertIsDisplayed()
      .assertTextContains("No place set", substring = true)
    composeRule.onNodeWithTag("app-place-context-action", useUnmergedTree = true)
      .assertIsDisplayed()
      .assertWidthIsAtLeast(48.dp)
      .assertHeightIsAtLeast(48.dp)
    composeRule.onNodeWithText("Set up place", useUnmergedTree = true)
      .assertIsDisplayed()
    composeRule.onNodeWithTag("app-place-context-bar")
      .assertHasClickAction()
      .performClick()

    waitUntilSelected("now")
    composeRule.onNodeWithTag("now-place-editor").assertIsDisplayed()
  }

  @Test
  fun savedManualPlaceSurvivesActivityRecreation() {
    setApplicationLanguage("en")

    composeRule.onNodeWithTag("now-set-up-place")
      .performScrollTo()
      .assertIsDisplayed()
      .performClick()
    openManualPlaceEditor()
    enterManualPlaceText("now-place-label-input", syntheticManualPlace.label)
    enterManualPlaceText("now-place-latitude-input", "12.25")
    enterManualPlaceText("now-place-longitude-input", "-34.5")
    enterManualPlaceText("now-place-elevation-input", "123.75")
    composeRule.onNodeWithTag("now-select-time-zone")
      .performScrollTo()
      .performClick()
    composeRule.onNodeWithTag("now-time-zone-search")
      .performTextInput("UTC")
    composeRule.onNodeWithTag("now-time-zone-option-UTC")
      .performScrollTo()
      .performClick()
    composeRule.onNodeWithTag("now-save-context")
      .performScrollTo()
      .performClick()

    waitUntilManualPlaceDisplayed()
    runBlocking {
      withTimeout(5_000) {
        manualPlaceContextRepository().context.first { it == syntheticManualPlace }
      }
    }

    composeRule.activityRule.scenario.recreate()

    waitUntilSelected("now")
    waitUntilManualPlaceDisplayed()
    assertSyntheticManualPlaceIsDisplayed()
  }

  @Test
  fun unsavedManualPlaceDraftIsDiscardedOnActivityRecreation() {
    setApplicationLanguage("en")

    composeRule.onNodeWithTag("now-set-up-place")
      .performScrollTo()
      .assertIsDisplayed()
      .performClick()
    openManualPlaceEditor()
    enterManualPlaceText("now-place-label-input", "Unsaved synthetic place")
    enterManualPlaceText("now-place-latitude-input", "1.25")
    enterManualPlaceText("now-place-longitude-input", "-2.5")

    composeRule.activityRule.scenario.recreate()

    waitUntilSelected("now")
    waitUntilManualPlaceCleared()
    composeRule.onAllNodesWithTag("now-place-editor").assertCountEquals(0)
  }

  @Test
  fun hebrewManualPlaceEditorKeepsEveryActionReachable() {
    setApplicationLanguage("he")

    composeRule.onNodeWithTag("now-set-up-place")
      .performScrollTo()
      .assertIsDisplayed()
      .performClick()
    openManualPlaceEditor()
    composeRule.onNodeWithTag("now-place-label-input")
      .performScrollTo()
      .assertIsDisplayed()
      .assertTextContains("EN · Place name", substring = true)
    composeRule.onNodeWithTag("now-save-context")
      .performScrollTo()
      .assertIsDisplayed()
      .assertHasClickAction()
      .assertWidthIsAtLeast(48.dp)
      .assertHeightIsAtLeast(48.dp)
    composeRule.onNodeWithTag("now-cancel-context")
      .performScrollTo()
      .assertIsDisplayed()
      .assertHasClickAction()
      .assertTextContains("EN · Cancel", substring = true)
      .assertWidthIsAtLeast(48.dp)
      .assertHeightIsAtLeast(48.dp)
      .performClick()
  }

  private fun setApplicationLanguage(languageTag: String) {
    setApplicationLocales(LocaleListCompat.forLanguageTags(languageTag))
  }

  private fun setApplicationLocales(locales: LocaleListCompat) {
    val oldActivity = composeRule.activity
    val localeChanged = AppCompatDelegate.getApplicationLocales() != locales
    composeRule.runOnIdle { AppCompatDelegate.setApplicationLocales(locales) }
    if (localeChanged) {
      composeRule.waitUntil(timeoutMillis = 5_000) {
        runCatching { composeRule.activity !== oldActivity }.getOrDefault(false)
      }
    }
    composeRule.waitForIdle()
  }

  private fun captureRoot(fileName: String) {
    PlatformTestStorageRegistry.getInstance().openOutputFile(fileName).use { output ->
      composeRule.onNodeWithTag("aseh-root", useUnmergedTree = false)
        .captureToImage()
        .asAndroidBitmap()
        .compress(Bitmap.CompressFormat.PNG, 100, output)
    }
  }

  private fun preferencesRepository() =
    appGraph().interfacePreferencesRepository

  private fun manualPlaceContextRepository() =
    appGraph().manualPlaceContextRepository

  private fun experienceStateRepository() =
    appGraph().experienceStateRepository

  private fun workspaceReviewStateRepository() =
    appGraph().workspaceReviewStateRepository

  private fun appGraph() =
    (InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as AsehApplication)
      .appGraph

  private fun waitUntilSelected(destinationId: String) {
    composeRule.waitUntil(timeoutMillis = 5_000) {
      runCatching {
        composeRule.onNodeWithTag("destination-$destinationId").assertIsSelected()
      }.isSuccess
    }
  }

  private fun enterManualPlaceText(testTag: String, value: String) {
    composeRule.onNodeWithTag(testTag)
      .performScrollTo()
      .performTextInput(value)
  }

  private fun openManualPlaceEditor() {
    composeRule.onNodeWithTag("now-enter-location-manually")
      .performScrollTo()
      .assertIsDisplayed()
      .performClick()
    composeRule.onNodeWithTag("now-place-label-input")
      .performScrollTo()
      .assertIsDisplayed()
  }

  private fun waitUntilManualPlaceCleared() {
    composeRule.waitUntil(timeoutMillis = 5_000) {
      runCatching {
        composeRule.onNodeWithTag("now-location-status")
          .assertTextContains("No location is set", substring = true)
        composeRule.onAllNodesWithTag("now-place-name").fetchSemanticsNodes().isEmpty()
      }.getOrDefault(false)
    }
  }

  private fun waitUntilManualPlaceDisplayed() {
    composeRule.waitUntil(timeoutMillis = 5_000) {
      runCatching {
        composeRule.onNodeWithTag("now-place-name")
          .assertTextContains(syntheticManualPlace.label, substring = true)
      }.isSuccess
    }
  }

  private fun assertSyntheticManualPlaceIsDisplayed() {
    composeRule.onNodeWithTag("now-location-status")
      .assertTextContains("manually entered location is active", substring = true)
    composeRule.onNodeWithTag("now-place-name")
      .assertTextContains(syntheticManualPlace.label, substring = true)
    composeRule.onNodeWithTag("now-place-coordinates")
      .assertTextContains("12.25, -34.5", substring = true)
    composeRule.onNodeWithTag("now-place-elevation")
      .assertTextContains("123.75 meters", substring = true)
    composeRule.onNodeWithTag("now-time-zone")
      .assertTextContains(syntheticManualPlace.timeZoneId, substring = true)
    composeRule.onNodeWithTag("now-change-context")
      .performScrollTo()
      .assertIsDisplayed()
      .assertHasClickAction()
      .assertTextContains("Change context", substring = true)
      .assertWidthIsAtLeast(48.dp)
      .assertHeightIsAtLeast(48.dp)
  }

  private fun assertLogicalDestinationOrder(isRtl: Boolean) {
    val destinationBounds = destinationIds.map { destinationId ->
      composeRule.onNodeWithTag("destination-$destinationId")
        .assertIsDisplayed()
        .fetchSemanticsNode().boundsInRoot
    }
    val railNodes = composeRule.onAllNodesWithTag(
      testTag = "navigation-rail",
      useUnmergedTree = true,
    ).fetchSemanticsNodes()

    if (railNodes.isEmpty()) {
      destinationBounds.zipWithNext().forEach { (current, next) ->
        if (isRtl) {
          assertTrue("Compact RTL destinations must follow logical start order", current.left > next.left)
        } else {
          assertTrue("Compact LTR destinations must follow logical start order", current.left < next.left)
        }
      }
      return
    }

    destinationBounds.zipWithNext().forEach { (current, next) ->
      assertTrue("Rail destinations must follow logical top-to-bottom order", current.top < next.top)
    }
    val railBounds = railNodes.single().boundsInRoot
    composeRule.onNodeWithTag(
      testTag = "navigation-rail",
      useUnmergedTree = true,
    )
      .assertWidthIsAtLeast(120.dp)
    val maximumRailWidthPx =
      200f * composeRule.activity.resources.displayMetrics.density
    assertTrue(
      "The navigation rail must not exceed 200 dp",
      railBounds.width <= maximumRailWidthPx + 0.5f,
    )
    val contentBounds = composeRule.onNodeWithTag(
      testTag = "navigation-content",
      useUnmergedTree = true,
    ).fetchSemanticsNode().boundsInRoot
    if (isRtl) {
      assertTrue("The navigation rail must occupy the RTL start edge", railBounds.left >= contentBounds.right)
    } else {
      assertTrue("The navigation rail must occupy the LTR start edge", railBounds.right <= contentBounds.left)
    }
  }

  private fun assertDestinationSemanticsAndTouchTargets() {
    destinationIds.forEach { destinationId ->
      val destination = composeRule.onNodeWithTag("destination-$destinationId")
        .assertIsDisplayed()
        .assertHasClickAction()
        .assertWidthIsAtLeast(48.dp)
        .assertHeightIsAtLeast(48.dp)
        .fetchSemanticsNode()
      assertEquals(Role.Tab, destination.config[SemanticsProperties.Role])
    }
  }

  private fun assertNowContextIsUsable(isHebrewFallback: Boolean) {
    val orderedTags = listOf(
      "now-heading",
      "now-summary",
      "now-location-status",
      "now-set-up-place",
      "now-date",
      "now-weekday",
      "now-time",
      "now-time-zone",
      "now-refresh",
    )
    listOf("now-heading", "now-summary").forEach { tag ->
      composeRule.onNodeWithTag(tag).assertIsDisplayed()
    }
    composeRule.onNodeWithTag("now-location-status")
      .performScrollTo()
      .assertIsDisplayed()
    composeRule.onNodeWithTag("now-set-up-place")
      .performScrollTo()
      .assertIsDisplayed()

    val headings = composeRule.onAllNodes(
      matcher = SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading),
      useUnmergedTree = true,
    ).fetchSemanticsNodes()
    val screenHeadingId = composeRule.onNodeWithTag(
      testTag = "now-heading",
      useUnmergedTree = true,
    ).fetchSemanticsNode().id
    assertTrue(
      "Now must expose its screen title as a semantic heading",
      headings.any { it.id == screenHeadingId },
    )

    val targetNodeIds = orderedTags.associateWith { tag ->
      composeRule.onNodeWithTag(tag).fetchSemanticsNode().id
    }
    val semanticsTreeOrder = mutableListOf<Int>()
    fun visit(node: SemanticsNode) {
      semanticsTreeOrder += node.id
      node.children.forEach(::visit)
    }
    visit(
      composeRule.onNodeWithTag(
        testTag = "aseh-root",
        useUnmergedTree = true,
      ).fetchSemanticsNode(),
    )
    val targetTreeIndices = orderedTags.map { tag ->
      semanticsTreeOrder.indexOf(targetNodeIds.getValue(tag))
    }
    assertTrue(
      "Every Now node must be present in the unmerged semantics tree",
      targetTreeIndices.all { it >= 0 },
    )
    targetTreeIndices.zipWithNext().forEach { (current, next) ->
      assertTrue("Now content must follow logical semantics-tree order", current < next)
    }

    val fallbackPrefix = if (isHebrewFallback) "EN · " else ""
    composeRule.onNodeWithTag("now-location-status")
      .assertTextContains("${fallbackPrefix}Location", substring = true)
    composeRule.onNodeWithTag("now-set-up-place")
      .performScrollTo()
      .assertIsDisplayed()
      .assertHasClickAction()
      .assertTextContains("${fallbackPrefix}Use my location", substring = true)
      .assertWidthIsAtLeast(48.dp)
      .assertHeightIsAtLeast(48.dp)

    composeRule.onNodeWithTag("now-date")
      .performScrollTo()
      .assertTextContains("${fallbackPrefix}Civil date", substring = true)
    composeRule.onNodeWithTag("now-weekday")
      .performScrollTo()
      .assertTextContains("${fallbackPrefix}Weekday", substring = true)
    composeRule.onNodeWithTag("now-time")
      .performScrollTo()
      .assertTextContains("${fallbackPrefix}Local time", substring = true)
    composeRule.onNodeWithTag("now-time-zone")
      .performScrollTo()
      .assertTextContains("${fallbackPrefix}Time zone", substring = true)

    composeRule.onNodeWithTag("now-refresh")
      .performScrollTo()
      .assertIsDisplayed()
      .assertHasClickAction()
      .assertTextContains("${fallbackPrefix}Refresh date and time", substring = true)
      .assertWidthIsAtLeast(48.dp)
      .assertHeightIsAtLeast(48.dp)

    composeRule.onNodeWithTag("now-set-up-place")
      .performScrollTo()
      .assertIsDisplayed()
  }

  private companion object {
    val destinationIds = listOf("now", "practice", "prayer", "study", "build")

    val syntheticManualPlace = ManualPlaceContext(
      label = "Synthetic Test Place",
      latitudeDegrees = 12.25,
      longitudeDegrees = -34.5,
      elevationMeters = 123.75,
      timeZoneId = "UTC",
    )
  }
}
