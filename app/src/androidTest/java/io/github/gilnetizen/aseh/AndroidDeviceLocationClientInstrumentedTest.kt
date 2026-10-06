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
 * The connected-test runner seeds Jerusalem coordinates before this suite starts.
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

    val outcome = try {
      if (Build.VERSION.SDK_INT >= 37) {
        refreshApi37TestProviderLocation()
      }
      AndroidDeviceLocationClient(context).getCurrentLocation()
    } finally {
      scenario.close()
    }

    assertTrue(
      "Expected a current emulator location, received $outcome",
      outcome is DeviceLocationOutcome.Success,
    )
    val fix = (outcome as DeviceLocationOutcome.Success).fix
    assertEquals(31.778, fix.latitudeDegrees, 0.02)
    assertEquals(35.235, fix.longitudeDegrees, 0.02)
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

  private fun refreshApi37TestProviderLocation() {
    val output = ParcelFileDescriptor.AutoCloseInputStream(
      InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(
        "cmd location providers set-test-provider-location gps " +
          "--location 31.778,35.235 --accuracy 5",
      ),
    ).bufferedReader().use { it.readText() }
    assertTrue("Unable to refresh the API 37 GPS test provider: $output", output.isBlank())
  }
}
