package io.github.gilnetizen.aseh

import android.Manifest
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.test.rule.GrantPermissionRule
import io.github.gilnetizen.aseh.core.database.InterfacePreferences
import io.github.gilnetizen.aseh.core.database.InterfacePreferencesRepository
import io.github.gilnetizen.aseh.core.database.ManualPlaceContext
import io.github.gilnetizen.aseh.core.database.ManualPlaceContextRepository
import io.github.gilnetizen.aseh.core.database.PlaceContextSource
import io.github.gilnetizen.aseh.location.DeviceLocationClient
import io.github.gilnetizen.aseh.location.DeviceLocationFix
import io.github.gilnetizen.aseh.location.DeviceLocationOutcome
import io.github.gilnetizen.aseh.location.DeviceLocationProvider
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class DeviceLocationAsehAppIntegrationTest {
  @get:Rule(order = 0)
  val locationPermissionRule: GrantPermissionRule =
    GrantPermissionRule.grant(Manifest.permission.ACCESS_COARSE_LOCATION)

  @get:Rule(order = 1)
  val composeRule = createComposeRule()

  @Test
  fun confirmedDeviceFixPersistsItsSourceAccuracyAndDeviceTimeZone() {
    val placeRepository = RecordingPlaceContextRepository()
    val locationClient = ImmediateLocationClient(
      DeviceLocationOutcome.Success(
        DeviceLocationFix(
          latitudeDegrees = 31.778,
          longitudeDegrees = 35.235,
          horizontalAccuracyMeters = 125.0,
          altitudeMeters = 754.0,
          isApproximate = true,
          provider = DeviceLocationProvider.NETWORK,
        ),
      ),
    )

    composeRule.setContent {
      AsehApp(
        clock = fixedClock,
        deviceTimeZone = { ZoneId.of("Asia/Jerusalem") },
        interfacePreferencesRepository = StablePreferencesRepository(),
        manualPlaceContextRepository = placeRepository,
        deviceLocationClient = locationClient,
        onSelectedDestinationChanged = {},
      )
    }

    composeRule.onNodeWithTag("now-set-up-place").performClick()
    composeRule.onNodeWithTag("now-use-device-location").performClick()
    composeRule.onNodeWithTag("now-location-preview-kind").assertIsDisplayed()

    assertNull(placeRepository.saved.value)

    composeRule.onNodeWithTag("now-use-this-location")
      .performScrollTo()
      .performClick()
    composeRule.waitUntil(timeoutMillis = 5_000) {
      placeRepository.saved.value != null
    }

    val saved = requireNotNull(placeRepository.saved.value)
    assertEquals(PlaceContextSource.DEVICE, saved.source)
    assertEquals("", saved.label)
    assertEquals(31.778, saved.latitudeDegrees, 0.0)
    assertEquals(35.235, saved.longitudeDegrees, 0.0)
    assertEquals(754.0, saved.elevationMeters ?: -1.0, 0.0)
    assertEquals(125.0, saved.horizontalAccuracyMeters ?: -1.0, 0.0)
    assertEquals("Asia/Jerusalem", saved.timeZoneId)
    assertEquals(1, locationClient.requestCount)
  }

  @Test
  fun navigatingAwayCancelsAnActiveDeviceLocationRequest() {
    val locationClient = SuspendingLocationClient()

    composeRule.setContent {
      AsehApp(
        clock = fixedClock,
        deviceTimeZone = { ZoneOffset.UTC },
        interfacePreferencesRepository = StablePreferencesRepository(),
        manualPlaceContextRepository = RecordingPlaceContextRepository(),
        deviceLocationClient = locationClient,
        onSelectedDestinationChanged = {},
      )
    }

    composeRule.onNodeWithTag("now-set-up-place").performClick()
    composeRule.onNodeWithTag("now-use-device-location").performClick()
    composeRule.waitUntil(timeoutMillis = 5_000) { locationClient.started.isCompleted }

    composeRule.onNodeWithTag("destination-study").performClick()
    composeRule.waitUntil(timeoutMillis = 5_000) { locationClient.cancelled.get() }

    composeRule.onNodeWithTag("destination-now").performClick()
    composeRule.onNodeWithTag("now-set-up-place").performClick()
    composeRule.onNodeWithTag("now-use-device-location").assertIsDisplayed()
  }

  @Test
  fun stoppingTheRequestingLifecycleCancelsLocationAndRestoresIdleState() {
    val locationClient = SuspendingLocationClient()
    val lifecycleOwner = MutableLifecycleOwner()

    composeRule.setContent {
      CompositionLocalProvider(LocalLifecycleOwner provides lifecycleOwner) {
        AsehApp(
          clock = fixedClock,
          deviceTimeZone = { ZoneOffset.UTC },
          interfacePreferencesRepository = StablePreferencesRepository(),
          manualPlaceContextRepository = RecordingPlaceContextRepository(),
          deviceLocationClient = locationClient,
          onSelectedDestinationChanged = {},
        )
      }
    }
    composeRule.runOnIdle { lifecycleOwner.resume() }

    composeRule.onNodeWithTag("now-set-up-place").performClick()
    composeRule.onNodeWithTag("now-use-device-location").performClick()
    composeRule.waitUntil(timeoutMillis = 5_000) { locationClient.started.isCompleted }

    composeRule.runOnIdle { lifecycleOwner.stop() }
    composeRule.waitUntil(timeoutMillis = 5_000) { locationClient.cancelled.get() }
    composeRule.onNodeWithTag("now-use-device-location").assertIsDisplayed()
  }

  private companion object {
    val fixedClock: Clock = Clock.fixed(
      Instant.parse("2026-10-06T12:00:00Z"),
      ZoneOffset.UTC,
    )
  }
}

private class StablePreferencesRepository : InterfacePreferencesRepository {
  private val state = MutableStateFlow(InterfacePreferences())

  override val preferences: Flow<InterfacePreferences> = state

  override suspend fun setSelectedDestinationId(destinationId: String) {
    state.value = state.value.copy(selectedDestinationId = destinationId)
  }

  override suspend fun setTextScale(textScale: Float) = Unit

  override suspend fun setDyslexiaFriendlyLatinEnabled(enabled: Boolean) = Unit

  override suspend fun reset() = Unit
}

private class RecordingPlaceContextRepository : ManualPlaceContextRepository {
  val saved = MutableStateFlow<ManualPlaceContext?>(null)

  override val context: Flow<ManualPlaceContext?> = saved

  override suspend fun save(context: ManualPlaceContext) {
    saved.value = context
  }

  override suspend fun clear() {
    saved.value = null
  }
}

private class ImmediateLocationClient(
  private val outcome: DeviceLocationOutcome,
) : DeviceLocationClient {
  var requestCount: Int = 0
    private set

  override suspend fun getCurrentLocation(): DeviceLocationOutcome {
    requestCount += 1
    return outcome
  }
}

private class SuspendingLocationClient : DeviceLocationClient {
  val started = CompletableDeferred<Unit>()
  val cancelled = AtomicBoolean(false)

  override suspend fun getCurrentLocation(): DeviceLocationOutcome {
    started.complete(Unit)
    try {
      awaitCancellation()
    } finally {
      cancelled.set(true)
    }
  }
}

private class MutableLifecycleOwner : LifecycleOwner {
  private val registry = LifecycleRegistry(this)

  override val lifecycle: Lifecycle = registry

  fun resume() {
    registry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
    registry.handleLifecycleEvent(Lifecycle.Event.ON_START)
    registry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
  }

  fun stop() {
    registry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    registry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
  }
}
