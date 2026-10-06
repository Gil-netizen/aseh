package io.github.gilnetizen.aseh

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.gilnetizen.aseh.core.designsystem.AsehTheme
import io.github.gilnetizen.aseh.feature.now.NowPlaceContext
import io.github.gilnetizen.aseh.feature.now.NowScreen
import java.io.IOException
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NowScreenTest {
  @get:Rule
  val composeRule = createComposeRule()

  @Test
  fun refreshResamplesTheInjectedClockWithoutWaitingForWallTime() {
    val clock = MutableClock(
      currentInstant = Instant.parse("2026-10-06T12:04:05Z"),
      zoneId = ZoneId.of("UTC"),
    )

    composeRule.setContent {
      AsehTheme {
        NowScreen(clock = clock)
      }
    }

    composeRule.onNodeWithTag("now-date")
      .assertTextContains("October 6, 2026")
    composeRule.onNodeWithTag("now-weekday")
      .assertTextContains("Tuesday")
    composeRule.onNodeWithTag("now-time")
      .assertTextContains("12:04:05")
    composeRule.onNodeWithTag("now-time-zone")
      .assertTextContains("UTC")
    composeRule.onNodeWithTag("now-location-status").assertIsDisplayed()

    clock.currentInstant = Instant.parse("2026-10-07T01:02:03Z")
    composeRule.onNodeWithTag("now-refresh")
      .performScrollTo()
      .performClick()

    composeRule.onNodeWithTag("now-date")
      .assertTextContains("October 7, 2026")
    composeRule.onNodeWithTag("now-weekday")
      .assertTextContains("Wednesday")
    composeRule.onNodeWithTag("now-time")
      .assertTextContains("01:02:03")
  }

  @Test
  fun refreshResamplesTheDeviceTimeZone() {
    val clock = MutableClock(
      currentInstant = Instant.parse("2026-10-07T01:00:00Z"),
      zoneId = ZoneId.of("UTC"),
    )
    var deviceTimeZone = ZoneId.of("UTC")

    composeRule.setContent {
      AsehTheme {
        NowScreen(
          clock = clock,
          timeZone = { deviceTimeZone },
        )
      }
    }

    composeRule.onNodeWithTag("now-time-zone").assertTextContains("UTC")

    deviceTimeZone = ZoneId.of("America/Los_Angeles")
    composeRule.onNodeWithTag("now-refresh")
      .performScrollTo()
      .performClick()

    composeRule.onNodeWithTag("now-date").assertTextContains("October 6, 2026")
    composeRule.onNodeWithTag("now-time").assertTextContains("18:00:00")
    composeRule.onNodeWithTag("now-time-zone")
      .assertTextContains("America/Los_Angeles")
  }

  @Test
  fun invalidManualPlaceShowsInlineErrorsAndDoesNotSave() {
    var savedContext: NowPlaceContext? = null

    composeRule.setContent {
      AsehTheme {
        NowScreen(
          clock = fixedClock(),
          onSavePlaceContext = { savedContext = it },
        )
      }
    }

    openManualPlaceEditor()
    enterText("now-place-label-input", "Synthetic Ridge")
    enterText("now-place-latitude-input", "91")
    enterText("now-place-longitude-input", "-181")
    enterText("now-place-elevation-input", "not-a-number")
    composeRule.onNodeWithTag("now-save-context")
      .performScrollTo()
      .performClick()

    composeRule.runOnIdle { assertNull(savedContext) }
    composeRule.onNodeWithTag("now-place-latitude-input")
      .assertTextContains("Latitude must be from −90 to 90")
      .assertIsFocused()
    composeRule.onNodeWithTag("now-place-longitude-input")
      .assertTextContains("Longitude must be from −180 to 180")
    composeRule.onNodeWithTag("now-place-elevation-input")
      .assertTextContains("Enter a finite decimal number")
    composeRule.onNodeWithTag("now-place-error-summary")
      .assertTextContains(
        "Focus moved to the first error",
        substring = true,
        ignoreCase = true,
      )
    composeRule.onNodeWithTag("now-place-editor").assertIsDisplayed()
  }

  @Test
  fun loadingSavedContextDoesNotExposeABlankEditor() {
    val ready = androidx.compose.runtime.mutableStateOf(false)

    composeRule.setContent {
      AsehTheme {
        NowScreen(
          clock = fixedClock(),
          placeContextReady = ready.value,
        )
      }
    }

    composeRule.onNodeWithTag("now-location-status")
      .assertTextContains(
        "Checking this device for a saved place context",
        substring = true,
        ignoreCase = true,
      )
    composeRule.onAllNodesWithTag("now-change-context").assertCountEquals(0)

    composeRule.runOnIdle { ready.value = true }

    composeRule.onNodeWithTag("now-change-context")
      .performScrollTo()
      .assertIsDisplayed()
  }

  @Test
  fun failedSaveKeepsTheDraftAndReportsTheDeviceError() {
    var saveAttempts = 0

    composeRule.setContent {
      AsehTheme {
        NowScreen(
          clock = fixedClock(),
          onSavePlaceContext = {
            saveAttempts += 1
            throw IOException("Synthetic storage failure")
          },
        )
      }
    }

    openManualPlaceEditor()
    enterText("now-place-label-input", "Synthetic retry place")
    enterText("now-place-latitude-input", "12.25")
    enterText("now-place-longitude-input", "-34.5")
    composeRule.onNodeWithTag("now-save-context")
      .performScrollTo()
      .performClick()

    composeRule.waitForIdle()
    composeRule.runOnIdle { assertEquals(1, saveAttempts) }
    composeRule.onNodeWithTag("now-place-editor").assertIsDisplayed()
    composeRule.onNodeWithTag("now-place-label-input")
      .assertTextContains("Synthetic retry place")
    composeRule.onNodeWithTag("now-place-persistence-error")
      .assertTextContains(
        "could not be saved on this device",
        substring = true,
        ignoreCase = true,
      )
  }

  @Test
  fun validManualPlaceSavesNormalizedData() {
    var savedContext: NowPlaceContext? = null

    composeRule.setContent {
      AsehTheme {
        NowScreen(
          clock = fixedClock(),
          onSavePlaceContext = { savedContext = it },
        )
      }
    }

    openManualPlaceEditor()
    enterText("now-place-label-input", "  Synthetic Ridge  ")
    enterText("now-place-latitude-input", "12,5")
    enterText("now-place-longitude-input", "-45,25")
    enterText("now-place-elevation-input", "123,75")
    composeRule.onNodeWithTag("now-save-context")
      .performScrollTo()
      .performClick()

    composeRule.runOnIdle {
      assertEquals(
        NowPlaceContext(
          label = "Synthetic Ridge",
          latitudeDegrees = 12.5,
          longitudeDegrees = -45.25,
          elevationMeters = 123.75,
          timeZoneId = "UTC",
        ),
        savedContext,
      )
    }
    composeRule.onAllNodesWithTag("now-place-editor").assertCountEquals(0)
  }

  @Test
  fun activePlaceUsesItsZoneAcrossUtcDateBoundaryAndShowsInspectableValues() {
    val placeContext = NowPlaceContext(
      label = "Synthetic Pacific Point",
      latitudeDegrees = 12.5,
      longitudeDegrees = -45.25,
      elevationMeters = 123.75,
      timeZoneId = "America/Los_Angeles",
    )

    composeRule.setContent {
      AsehTheme {
        NowScreen(
          clock = Clock.fixed(
            Instant.parse("2026-01-01T00:30:00Z"),
            ZoneId.of("UTC"),
          ),
          placeContext = placeContext,
        )
      }
    }

    composeRule.onNodeWithTag("now-date")
      .assertTextContains("December 31, 2025")
    composeRule.onNodeWithTag("now-time")
      .assertTextContains("16:30:00")
    composeRule.onNodeWithTag("now-time-zone")
      .assertTextContains("America/Los_Angeles")
    composeRule.onNodeWithTag("now-place-name")
      .performScrollTo()
      .assertTextContains("Synthetic Pacific Point")
    composeRule.onNodeWithTag("now-place-coordinates")
      .performScrollTo()
      .assertTextContains("12.5, -45.25")
    composeRule.onNodeWithTag("now-place-elevation")
      .performScrollTo()
      .assertTextContains("123.75 meters")
  }

  @Test
  fun cancelClosesEditorWithoutSaving() {
    var savedContext: NowPlaceContext? = null

    composeRule.setContent {
      AsehTheme {
        NowScreen(
          clock = fixedClock(),
          onSavePlaceContext = { savedContext = it },
        )
      }
    }

    openManualPlaceEditor()
    enterText("now-place-label-input", "Unsaved Synthetic Place")
    composeRule.onNodeWithTag("now-cancel-context")
      .performScrollTo()
      .performClick()

    composeRule.runOnIdle { assertNull(savedContext) }
    composeRule.onAllNodesWithTag("now-place-editor").assertCountEquals(0)
    composeRule.onNodeWithTag("now-change-context")
      .performScrollTo()
      .assertIsDisplayed()
  }

  @Test
  fun activePlaceCanBeClearedExplicitly() {
    var clearCalled = false
    val placeContext = NowPlaceContext(
      label = "Synthetic Clearing",
      latitudeDegrees = 1.0,
      longitudeDegrees = 2.0,
      elevationMeters = null,
      timeZoneId = "UTC",
    )

    composeRule.setContent {
      AsehTheme {
        NowScreen(
          clock = fixedClock(),
          placeContext = placeContext,
          onClearPlaceContext = { clearCalled = true },
        )
      }
    }

    openManualPlaceEditor()
    composeRule.onNodeWithTag("now-clear-context")
      .performScrollTo()
      .performClick()

    composeRule.runOnIdle { assertEquals(true, clearCalled) }
    composeRule.onAllNodesWithTag("now-place-editor").assertCountEquals(0)
  }

  @Test
  fun offlineTimeZoneSearchSelectsAListedZone() {
    composeRule.setContent {
      AsehTheme {
        NowScreen(clock = fixedClock())
      }
    }

    openManualPlaceEditor()
    composeRule.onNodeWithTag("now-select-time-zone")
      .performScrollTo()
      .performClick()
    composeRule.onNodeWithTag("now-time-zone-dialog").assertIsDisplayed()
    composeRule.onNodeWithTag("now-time-zone-search")
      .performTextInput("Los_Angeles")
    composeRule.onNodeWithTag("now-time-zone-option-America/Los_Angeles")
      .performScrollTo()
      .performClick()

    composeRule.onAllNodesWithTag("now-time-zone-dialog").assertCountEquals(0)
    composeRule.onNodeWithTag("now-place-time-zone-input")
      .performScrollTo()
      .assertTextContains("America/Los_Angeles")
  }

  private fun openManualPlaceEditor() {
    composeRule.onNodeWithTag("now-change-context")
      .performScrollTo()
      .performClick()
    composeRule.onNodeWithTag("now-place-editor").assertIsDisplayed()
  }

  private fun enterText(testTag: String, value: String) {
    composeRule.onNodeWithTag(testTag)
      .performScrollTo()
      .performTextInput(value)
  }

  private fun fixedClock(): Clock = Clock.fixed(
    Instant.parse("2026-10-07T01:00:00Z"),
    ZoneId.of("UTC"),
  )

  private class MutableClock(
    var currentInstant: Instant,
    private val zoneId: ZoneId,
  ) : Clock() {
    override fun getZone(): ZoneId = zoneId

    override fun withZone(zone: ZoneId): Clock = MutableClock(currentInstant, zone)

    override fun instant(): Instant = currentInstant
  }
}
