package io.github.gilnetizen.aseh

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.gilnetizen.aseh.core.designsystem.AsehTheme
import io.github.gilnetizen.aseh.feature.now.NowScreen
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
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
    composeRule.onNodeWithTag("now-refresh").performClick()

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
    composeRule.onNodeWithTag("now-refresh").performClick()

    composeRule.onNodeWithTag("now-date").assertTextContains("October 6, 2026")
    composeRule.onNodeWithTag("now-time").assertTextContains("18:00:00")
    composeRule.onNodeWithTag("now-time-zone")
      .assertTextContains("America/Los_Angeles")
  }

  private class MutableClock(
    var currentInstant: Instant,
    private val zoneId: ZoneId,
  ) : Clock() {
    override fun getZone(): ZoneId = zoneId

    override fun withZone(zone: ZoneId): Clock = MutableClock(currentInstant, zone)

    override fun instant(): Instant = currentInstant
  }
}
