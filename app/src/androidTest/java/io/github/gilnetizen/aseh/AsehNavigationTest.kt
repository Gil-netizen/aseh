package io.github.gilnetizen.aseh

import android.content.Intent
import android.graphics.Bitmap
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.AndroidComposeTestRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.core.os.LocaleListCompat
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.platform.io.PlatformTestStorageRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlinx.coroutines.withTimeout

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
  fun resetSelectedDestination() {
    runBlocking {
      withTimeout(5_000) {
        preferencesRepository().setSelectedDestinationId("now")
        preferencesRepository().preferences.first { it.selectedDestinationId == "now" }
      }
    }
    waitUntilSelected("now")
  }

  @After
  fun restoreSystemLocales() {
    setApplicationLocales(LocaleListCompat.getEmptyLocaleList())
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

    captureRoot("english-ltr.png")
  }

  @Test
  fun hebrewLocaleMirrorsNavigationAndLabelsFallbackLanguage() {
    setApplicationLanguage("he")

    assertLogicalDestinationOrder(isRtl = true)
    composeRule.onNodeWithContentDescription("EN · Build", substring = true).performClick()
    composeRule.waitForIdle()

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
    (InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as AsehApplication)
      .appGraph
      .interfacePreferencesRepository

  private fun waitUntilSelected(destinationId: String) {
    composeRule.waitUntil(timeoutMillis = 5_000) {
      runCatching {
        composeRule.onNodeWithTag("destination-$destinationId").assertIsSelected()
      }.isSuccess
    }
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

  private companion object {
    val destinationIds = listOf("now", "practice", "prayer", "study", "build")
  }
}
