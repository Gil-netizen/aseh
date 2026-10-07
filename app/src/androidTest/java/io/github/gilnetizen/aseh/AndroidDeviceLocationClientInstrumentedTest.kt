package io.github.gilnetizen.aseh

import android.Manifest
import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import io.github.gilnetizen.aseh.location.AndroidDeviceLocationClient
import io.github.gilnetizen.aseh.location.DeviceLocationOutcome
import io.github.gilnetizen.aseh.location.toDeviceLocationCandidate
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Exercises the framework location adapter against the emulator's seeded GPS fix.
 * API 37 installs a shell-owned deterministic provider here. Older emulator images expose no
 * shell location-provider API, so this verifies their currently available foreground GPS fix;
 * the connected-test runner may separately seed a known coordinate while a listener is active.
 */
@RunWith(AndroidJUnit4::class)
class AndroidDeviceLocationClientInstrumentedTest {
  @get:Rule
  val locationPermissionRule: GrantPermissionRule = GrantPermissionRule.grant(
    Manifest.permission.ACCESS_COARSE_LOCATION,
    Manifest.permission.ACCESS_FINE_LOCATION,
  )

  @Test
  fun readsTheCurrentSeededGpsFix() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val scenario = ActivityScenario.launch(MainActivity::class.java)
    val usesApi37TestProvider = Build.VERSION.SDK_INT >= 37

    val outcome = try {
      if (usesApi37TestProvider) {
        installApi37TestProviderLocation()
      }
      AndroidDeviceLocationClient(context).getCurrentLocation()
    } finally {
      if (usesApi37TestProvider) {
        removeApi37TestProvider()
      }
      scenario.close()
    }

    assertTrue(
      "Expected a current emulator location, received $outcome",
      outcome is DeviceLocationOutcome.Success,
    )
    val fix = (outcome as DeviceLocationOutcome.Success).fix
    if (usesApi37TestProvider) {
      assertEquals(31.778, fix.latitudeDegrees, 0.02)
      assertEquals(35.235, fix.longitudeDegrees, 0.02)
    } else {
      assertTrue(fix.latitudeDegrees.isFinite())
      assertTrue(fix.latitudeDegrees in -90.0..90.0)
      assertTrue(fix.longitudeDegrees.isFinite())
      assertTrue(fix.longitudeDegrees in -180.0..180.0)
    }
  }

  @Test
  fun ellipsoidAltitudeAloneIsNeverExposedAsElevation() {
    val location = syntheticLocation().apply {
      altitude = 812.0
    }

    assertTrue(location.hasAltitude())
    assertNull(location.toDeviceLocationCandidate().altitudeMeters)
  }

  @Test
  @SdkSuppress(minSdkVersion = 34)
  fun api34MeanSeaLevelAltitudeIsExposedAsElevation() {
    val location = syntheticLocation().apply {
      altitude = 812.0
      mslAltitudeMeters = 754.0
    }

    assertEquals(
      754.0,
      location.toDeviceLocationCandidate().altitudeMeters ?: Double.NaN,
      0.0,
    )
  }

  private fun syntheticLocation(): Location = Location(LocationManager.GPS_PROVIDER).apply {
    latitude = 31.778
    longitude = 35.235
    elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos()
  }

  private fun installApi37TestProviderLocation() {
    runShellCommand("appops set 2000 android:mock_location allow")
    runShellCommand("cmd location providers remove-test-provider gps")
    runShellCommand(
      "cmd location providers add-test-provider gps " +
        "--requiresSatellite --supportsAltitude",
    )
    runShellCommand("cmd location providers set-test-provider-enabled gps true")
    runShellCommand(
      "cmd location providers set-test-provider-location gps " +
        "--location 31.778,35.235 --accuracy 5",
    )
  }

  private fun removeApi37TestProvider() {
    runShellCommand("cmd location providers remove-test-provider gps", assertSuccess = false)
    runShellCommand("appops set 2000 android:mock_location default", assertSuccess = false)
  }

  private fun runShellCommand(command: String, assertSuccess: Boolean = true) {
    val output = ParcelFileDescriptor.AutoCloseInputStream(
      InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(command),
    ).bufferedReader().use { it.readText() }
    if (assertSuccess) {
      assertTrue("Shell command failed: $command\n$output", output.isBlank())
    }
  }
}
