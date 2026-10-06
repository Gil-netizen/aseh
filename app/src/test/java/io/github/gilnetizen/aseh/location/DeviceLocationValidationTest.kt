package io.github.gilnetizen.aseh.location

import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DeviceLocationValidationTest {
  @Test
  fun missingPermissionNeverSelectsProviders() {
    assertTrue(
      selectDeviceLocationProviders(
        DeviceLocationPermission.NONE,
        setOf(DeviceLocationProvider.GPS, DeviceLocationProvider.NETWORK),
      ).isEmpty(),
    )
  }

  @Test
  fun approximateAccessPrefersNetworkThenFallsBackToGps() {
    val enabled = setOf(DeviceLocationProvider.GPS, DeviceLocationProvider.NETWORK)

    assertEquals(
      listOf(DeviceLocationProvider.NETWORK, DeviceLocationProvider.GPS),
      selectDeviceLocationProviders(DeviceLocationPermission.APPROXIMATE, enabled),
    )
    assertEquals(
      listOf(DeviceLocationProvider.GPS),
      selectDeviceLocationProviders(
        DeviceLocationPermission.APPROXIMATE,
        setOf(DeviceLocationProvider.GPS),
      ),
    )
  }

  @Test
  fun preciseAccessPrefersGpsAndFallsBackToNetwork() {
    assertEquals(
      listOf(DeviceLocationProvider.GPS, DeviceLocationProvider.NETWORK),
      selectDeviceLocationProviders(
        DeviceLocationPermission.PRECISE,
        setOf(DeviceLocationProvider.GPS, DeviceLocationProvider.NETWORK),
      ),
    )
    assertEquals(
      listOf(DeviceLocationProvider.NETWORK),
      selectDeviceLocationProviders(
        DeviceLocationPermission.PRECISE,
        setOf(DeviceLocationProvider.NETWORK),
      ),
    )
  }

  @Test
  fun validatedFixRetainsAccuracyAltitudeAndApproximateMetadata() {
    val fix = validateDeviceLocation(
      candidate = validCandidate,
      provider = DeviceLocationProvider.NETWORK,
      isApproximate = true,
      observedAtElapsedRealtimeNanos = NOW_NANOS,
      maximumAgeNanos = MAXIMUM_AGE_NANOS,
    )

    requireNotNull(fix)
    assertEquals(31.778, fix.latitudeDegrees, 0.0)
    assertEquals(35.235, fix.longitudeDegrees, 0.0)
    assertEquals(24.5, fix.horizontalAccuracyMeters ?: -1.0, 0.0)
    assertEquals(754.0, fix.altitudeMeters ?: -1.0, 0.0)
    assertTrue(fix.isApproximate)
    assertEquals(DeviceLocationProvider.NETWORK, fix.provider)
  }

  @Test
  fun rejectsNonFiniteAndOutOfRangeCoordinates() {
    val invalidCandidates = listOf(
      validCandidate.copy(latitudeDegrees = Double.NaN),
      validCandidate.copy(latitudeDegrees = 90.000_001),
      validCandidate.copy(longitudeDegrees = Double.POSITIVE_INFINITY),
      validCandidate.copy(longitudeDegrees = -180.000_001),
    )

    invalidCandidates.forEach { candidate ->
      assertNull(candidate.validate())
    }
  }

  @Test
  fun acceptsInclusiveCoordinateBoundaries() {
    val northEast = validCandidate.copy(latitudeDegrees = 90.0, longitudeDegrees = 180.0)
    val southWest = validCandidate.copy(latitudeDegrees = -90.0, longitudeDegrees = -180.0)

    assertEquals(90.0, requireNotNull(northEast.validate()).latitudeDegrees, 0.0)
    assertEquals(-180.0, requireNotNull(southWest.validate()).longitudeDegrees, 0.0)
  }

  @Test
  fun rejectsInvalidOptionalMetadata() {
    assertNull(validCandidate.copy(horizontalAccuracyMeters = -0.1).validate())
    assertNull(validCandidate.copy(horizontalAccuracyMeters = Double.NaN).validate())
    assertNull(validCandidate.copy(altitudeMeters = Double.NEGATIVE_INFINITY).validate())
  }

  @Test
  fun rejectsStaleMissingAndFutureElapsedRealtime() {
    assertNull(validCandidate.copy(elapsedRealtimeNanos = 0L).validate())
    assertNull(
      validCandidate.copy(
        elapsedRealtimeNanos = NOW_NANOS - MAXIMUM_AGE_NANOS - 1L,
      ).validate(),
    )
    assertNull(validCandidate.copy(elapsedRealtimeNanos = NOW_NANOS + 1L).validate())
  }

  @Test
  fun preciseMetadataIsNotMarkedApproximate() {
    val fix = validateDeviceLocation(
      candidate = validCandidate.copy(horizontalAccuracyMeters = null, altitudeMeters = null),
      provider = DeviceLocationProvider.GPS,
      isApproximate = false,
      observedAtElapsedRealtimeNanos = NOW_NANOS,
      maximumAgeNanos = MAXIMUM_AGE_NANOS,
    )

    requireNotNull(fix)
    assertFalse(fix.isApproximate)
    assertNull(fix.horizontalAccuracyMeters)
    assertNull(fix.altitudeMeters)
  }

  @Test
  fun acquisitionTimesOutAndCancelsTheActiveRequest() = runBlocking {
    val requestWasCancelled = AtomicBoolean(false)

    val outcome = acquireDeviceLocation(
      providers = listOf(DeviceLocationProvider.GPS, DeviceLocationProvider.NETWORK),
      isApproximate = false,
      timeoutMillis = 25L,
      maximumAgeNanos = MAXIMUM_AGE_NANOS,
      elapsedRealtimeNanos = { NOW_NANOS },
    ) { provider ->
      if (provider == DeviceLocationProvider.GPS) {
        null
      } else {
        try {
          awaitCancellation()
        } finally {
          requestWasCancelled.set(true)
        }
      }
    }

    assertEquals(DeviceLocationOutcome.Timeout, outcome)
    assertTrue(requestWasCancelled.get())
  }

  @Test
  fun callerCancellationPropagatesIntoTheActiveRequest() = runBlocking {
    val requestStarted = CompletableDeferred<Unit>()
    val requestWasCancelled = AtomicBoolean(false)
    val job = launch {
      acquireDeviceLocation(
        providers = listOf(DeviceLocationProvider.GPS),
        isApproximate = false,
        timeoutMillis = 10_000L,
        maximumAgeNanos = MAXIMUM_AGE_NANOS,
        elapsedRealtimeNanos = { NOW_NANOS },
      ) { _ ->
        requestStarted.complete(Unit)
        try {
          awaitCancellation()
        } finally {
          requestWasCancelled.set(true)
        }
      }
    }

    requestStarted.await()
    job.cancelAndJoin()

    assertTrue(job.isCancelled)
    assertTrue(requestWasCancelled.get())
  }

  @Test
  fun nullPreferredProviderFallsBackToTheNextProvider() = runBlocking {
    val requestedProviders = mutableListOf<DeviceLocationProvider>()

    val outcome = acquireDeviceLocation(
      providers = listOf(DeviceLocationProvider.GPS, DeviceLocationProvider.NETWORK),
      isApproximate = false,
      timeoutMillis = 1_000L,
      maximumAgeNanos = MAXIMUM_AGE_NANOS,
      elapsedRealtimeNanos = { NOW_NANOS },
    ) { provider ->
      requestedProviders += provider
      validCandidate.takeIf { provider == DeviceLocationProvider.NETWORK }
    }

    assertEquals(
      listOf(DeviceLocationProvider.GPS, DeviceLocationProvider.NETWORK),
      requestedProviders,
    )
    assertEquals(
      DeviceLocationProvider.NETWORK,
      (outcome as DeviceLocationOutcome.Success).fix.provider,
    )
  }

  @Test
  fun unusablePreferredProviderFallsBackToTheNextProvider() = runBlocking {
    val requestedProviders = mutableListOf<DeviceLocationProvider>()

    val outcome = acquireDeviceLocation(
      providers = listOf(DeviceLocationProvider.NETWORK, DeviceLocationProvider.GPS),
      isApproximate = true,
      timeoutMillis = 1_000L,
      maximumAgeNanos = MAXIMUM_AGE_NANOS,
      elapsedRealtimeNanos = { NOW_NANOS },
    ) { provider ->
      requestedProviders += provider
      when (provider) {
        DeviceLocationProvider.NETWORK -> validCandidate.copy(latitudeDegrees = 91.0)
        DeviceLocationProvider.GPS -> validCandidate
      }
    }

    assertEquals(
      listOf(DeviceLocationProvider.NETWORK, DeviceLocationProvider.GPS),
      requestedProviders,
    )
    val fix = (outcome as DeviceLocationOutcome.Success).fix
    assertEquals(DeviceLocationProvider.GPS, fix.provider)
    assertTrue(fix.isApproximate)
  }

  @Test
  fun preferredProviderTimeoutStillLeavesTimeForFallback() = runBlocking {
    val firstRequestWasCancelled = AtomicBoolean(false)
    val requestedProviders = mutableListOf<DeviceLocationProvider>()

    val outcome = acquireDeviceLocation(
      providers = listOf(DeviceLocationProvider.GPS, DeviceLocationProvider.NETWORK),
      isApproximate = false,
      timeoutMillis = 200L,
      maximumAgeNanos = MAXIMUM_AGE_NANOS,
      elapsedRealtimeNanos = { NOW_NANOS },
    ) { provider ->
      requestedProviders += provider
      if (provider == DeviceLocationProvider.GPS) {
        try {
          delay(1_000L)
          null
        } finally {
          firstRequestWasCancelled.set(true)
        }
      } else {
        validCandidate
      }
    }

    assertTrue(firstRequestWasCancelled.get())
    assertEquals(
      listOf(DeviceLocationProvider.GPS, DeviceLocationProvider.NETWORK),
      requestedProviders,
    )
    assertEquals(
      DeviceLocationProvider.NETWORK,
      (outcome as DeviceLocationOutcome.Success).fix.provider,
    )
  }

  private fun DeviceLocationCandidate.validate(): DeviceLocationFix? = validateDeviceLocation(
    candidate = this,
    provider = DeviceLocationProvider.GPS,
    isApproximate = false,
    observedAtElapsedRealtimeNanos = NOW_NANOS,
    maximumAgeNanos = MAXIMUM_AGE_NANOS,
  )

  private companion object {
    const val NOW_NANOS = 100_000_000_000L
    const val MAXIMUM_AGE_NANOS = 30_000_000_000L

    val validCandidate = DeviceLocationCandidate(
      latitudeDegrees = 31.778,
      longitudeDegrees = 35.235,
      horizontalAccuracyMeters = 24.5,
      altitudeMeters = 754.0,
      elapsedRealtimeNanos = NOW_NANOS - 1_000_000_000L,
    )
  }
}
