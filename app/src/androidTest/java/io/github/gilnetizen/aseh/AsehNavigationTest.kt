package io.github.gilnetizen.aseh

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
import androidx.compose.ui.test.junit4.v2.AndroidComposeTestRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.SemanticsMatcher
import androidx.core.os.LocaleListCompat
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.platform.io.PlatformTestStorageRegistry
import io.github.gilnetizen.aseh.core.database.ManualPlaceContext
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

  @get:Rule
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
  fun savedManualPlaceSurvivesActivityRecreation() {
    setApplicationLanguage("en")

    composeRule.onNodeWithTag("now-change-context")
      .performScrollTo()
      .performClick()
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

    composeRule.onNodeWithTag("now-change-context")
      .performScrollTo()
      .performClick()
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

    composeRule.onNodeWithTag("now-change-context")
      .performScrollTo()
      .performClick()
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
      .assertTextContains("manual place context is active", substring = true)
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
      "now-date",
      "now-weekday",
      "now-time",
      "now-time-zone",
      "now-location-status",
      "now-change-context",
      "now-refresh",
    )
    orderedTags.takeWhile { it != "now-change-context" }.forEach { tag ->
      composeRule.onNodeWithTag(tag).assertIsDisplayed()
    }

    val headings = composeRule.onAllNodes(
      matcher = SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading),
      useUnmergedTree = true,
    ).fetchSemanticsNodes()
    assertEquals("Now must expose exactly one semantic heading", 1, headings.size)

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
    composeRule.onNodeWithTag("now-date")
      .assertTextContains("${fallbackPrefix}Civil date", substring = true)
    composeRule.onNodeWithTag("now-weekday")
      .assertTextContains("${fallbackPrefix}Weekday", substring = true)
    composeRule.onNodeWithTag("now-time")
      .assertTextContains("${fallbackPrefix}Local time", substring = true)
    composeRule.onNodeWithTag("now-time-zone")
      .assertTextContains("${fallbackPrefix}Time zone", substring = true)
    composeRule.onNodeWithTag("now-location-status")
      .assertTextContains("${fallbackPrefix}Location", substring = true)

    composeRule.onNodeWithTag("now-change-context")
      .performScrollTo()
      .assertIsDisplayed()
      .assertHasClickAction()
      .assertTextContains("${fallbackPrefix}Change context", substring = true)
      .assertWidthIsAtLeast(48.dp)
      .assertHeightIsAtLeast(48.dp)

    composeRule.onNodeWithTag("now-refresh")
      .performScrollTo()
      .assertIsDisplayed()
      .assertHasClickAction()
      .assertTextContains("${fallbackPrefix}Refresh date and time", substring = true)
      .assertWidthIsAtLeast(48.dp)
      .assertHeightIsAtLeast(48.dp)
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
