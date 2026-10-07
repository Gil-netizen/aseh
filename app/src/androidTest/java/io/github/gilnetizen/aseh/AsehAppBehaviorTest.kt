package io.github.gilnetizen.aseh

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import io.github.gilnetizen.aseh.core.database.InterfacePreferences
import io.github.gilnetizen.aseh.core.database.InterfacePreferencesRepository
import io.github.gilnetizen.aseh.core.database.ManualPlaceContext
import io.github.gilnetizen.aseh.core.database.ManualPlaceContextRepository
import io.github.gilnetizen.aseh.core.designsystem.AsehTheme
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test

class AsehAppBehaviorTest {
  @get:Rule
  val composeRule = createComposeRule()

  @Test
  fun stalePersistedDestinationDoesNotClosePlaceEditor() {
    val preferencesRepository = FakeInterfacePreferencesRepository()
    val placeRepository = FakeManualPlaceContextRepository()

    composeRule.setContent {
      AsehApp(
        clock = Clock.fixed(Instant.parse("2026-10-06T12:00:00Z"), ZoneOffset.UTC),
        deviceTimeZone = { ZoneOffset.UTC },
        interfacePreferencesRepository = preferencesRepository,
        manualPlaceContextRepository = placeRepository,
      )
    }

    composeRule.onNodeWithTag("destination-study").performClick()
    composeRule.onNodeWithTag("destination-study").assertIsSelected()
    composeRule.onNodeWithTag("app-place-context-bar").performClick()
    composeRule.onNodeWithTag("destination-now").assertIsSelected()
    composeRule.onNodeWithTag("now-place-editor").assertIsDisplayed()

    // Simulate the delayed result of the earlier Study write arriving after
    // the newer global-bar request has already opened Now.
    composeRule.runOnIdle {
      preferencesRepository.state.value = InterfacePreferences(
        selectedDestinationId = "study",
      )
    }
    composeRule.waitForIdle()

    composeRule.onNodeWithTag("destination-now").assertIsSelected()
    composeRule.onNodeWithTag("now-place-editor").assertIsDisplayed()
  }

  @Test
  fun activePlaceBarAtTwoHundredPercentLeavesDestinationSpace() {
    val mixedLabel = ("ירושלים Test ").repeat(10).take(80)
    val placeContext = ManualPlaceContext(
      label = mixedLabel,
      latitudeDegrees = 31.778,
      longitudeDegrees = 35.235,
      elevationMeters = null,
      timeZoneId = "Asia/Jerusalem",
    )

    composeRule.setContent {
      val deviceDensity = LocalDensity.current
      CompositionLocalProvider(
        LocalDensity provides Density(
          density = deviceDensity.density,
          fontScale = 2f,
        ),
      ) {
        AsehTheme {
          Box(modifier = Modifier.fillMaxSize()) {
            Column(
              modifier = Modifier
                .width(360.dp)
                .height(320.dp),
            ) {
              PlaceContextBar(
                placeContext = placeContext,
                onEdit = {},
              )
              Box(
                modifier = Modifier
                  .fillMaxWidth()
                  .weight(1f)
                  .testTag("constrained-destination-space"),
              )
            }
          }
        }
      }
    }

    composeRule.onNodeWithTag("app-place-context-action", useUnmergedTree = true)
      .assertIsDisplayed()
      .assertHeightIsAtLeast(48.dp)
    composeRule.onNodeWithTag("constrained-destination-space")
      .assertIsDisplayed()
      .assertHeightIsAtLeast(48.dp)
    composeRule.onNodeWithTag("app-place-context-label", useUnmergedTree = true)
      .assertTextEquals(mixedLabel)
    composeRule.onNodeWithTag("app-place-context-time-zone", useUnmergedTree = true)
      .assertTextEquals("Asia/Jerusalem")
  }
}

private class FakeInterfacePreferencesRepository : InterfacePreferencesRepository {
  val state = MutableStateFlow(InterfacePreferences())

  override val preferences: Flow<InterfacePreferences> = state

  override suspend fun setSelectedDestinationId(destinationId: String) {
    state.value = state.value.copy(selectedDestinationId = destinationId)
  }

  override suspend fun setTextScale(textScale: Float) {
    state.value = state.value.copy(textScale = textScale)
  }

  override suspend fun setDyslexiaFriendlyLatinEnabled(enabled: Boolean) {
    state.value = state.value.copy(dyslexiaFriendlyLatinEnabled = enabled)
  }

  override suspend fun reset() {
    state.value = InterfacePreferences()
  }
}

private class FakeManualPlaceContextRepository : ManualPlaceContextRepository {
  private val state = MutableStateFlow<ManualPlaceContext?>(null)

  override val context: Flow<ManualPlaceContext?> = state

  override suspend fun save(context: ManualPlaceContext) {
    state.value = context
  }

  override suspend fun clear() {
    state.value = null
  }
}
