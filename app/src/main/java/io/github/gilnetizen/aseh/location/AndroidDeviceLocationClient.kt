package io.github.gilnetizen.aseh.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.CancellationSignal
import android.os.SystemClock
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import java.util.concurrent.Executor
import kotlin.coroutines.resume
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine

/** Android framework implementation with no Google Play Services or background collection. */
class AndroidDeviceLocationClient(
  context: Context,
  private val locationManager: LocationManager =
    context.applicationContext.getSystemService(LocationManager::class.java),
  private val callbackExecutor: Executor = ContextCompat.getMainExecutor(context.applicationContext),
  private val timeoutMillis: Long = DEFAULT_TIMEOUT_MILLIS,
  private val elapsedRealtimeNanos: () -> Long = SystemClock::elapsedRealtimeNanos,
) : DeviceLocationClient {
  private val applicationContext = context.applicationContext

  override suspend fun getCurrentLocation(): DeviceLocationOutcome {
    val permission = currentPermission()
    if (permission == DeviceLocationPermission.NONE) {
      return DeviceLocationOutcome.PermissionDenied
    }

    return try {
      if (!LocationManagerCompat.isLocationEnabled(locationManager)) {
        return DeviceLocationOutcome.LocationDisabled
      }

      val enabledProviders = enabledProviders()
      val providers = selectDeviceLocationProviders(permission, enabledProviders)
      acquireDeviceLocation(
        providers = providers,
        isApproximate = permission == DeviceLocationPermission.APPROXIMATE,
        timeoutMillis = timeoutMillis,
        maximumAgeNanos = MAXIMUM_ACCEPTED_FIX_AGE_NANOS,
        elapsedRealtimeNanos = elapsedRealtimeNanos,
      ) { provider ->
        requestOneLocation(provider).location?.toCandidate()
      }
    } catch (error: CancellationException) {
      throw error
    } catch (_: SecurityException) {
      DeviceLocationOutcome.PermissionDenied
    } catch (_: IllegalArgumentException) {
      DeviceLocationOutcome.Unavailable
    } catch (_: RuntimeException) {
      DeviceLocationOutcome.Failure
    }
  }

  private fun currentPermission(): DeviceLocationPermission = when {
    ContextCompat.checkSelfPermission(
      applicationContext,
      Manifest.permission.ACCESS_FINE_LOCATION,
    ) == PackageManager.PERMISSION_GRANTED -> DeviceLocationPermission.PRECISE
    ContextCompat.checkSelfPermission(
      applicationContext,
      Manifest.permission.ACCESS_COARSE_LOCATION,
    ) == PackageManager.PERMISSION_GRANTED -> DeviceLocationPermission.APPROXIMATE
    else -> DeviceLocationPermission.NONE
  }

  private fun enabledProviders(): Set<DeviceLocationProvider> = buildSet {
    if (isProviderEnabled(LocationManager.GPS_PROVIDER)) {
      add(DeviceLocationProvider.GPS)
    }
    if (isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
      add(DeviceLocationProvider.NETWORK)
    }
  }

  private fun isProviderEnabled(provider: String): Boolean =
    LocationManagerCompat.hasProvider(locationManager, provider) &&
      locationManager.isProviderEnabled(provider)

  @SuppressLint("MissingPermission")
  private suspend fun requestOneLocation(
    provider: DeviceLocationProvider,
  ): CurrentLocationCallbackResult = suspendCancellableCoroutine { continuation ->
    val cancellationSignal = CancellationSignal()
    continuation.invokeOnCancellation { cancellationSignal.cancel() }

    LocationManagerCompat.getCurrentLocation(
      locationManager,
      provider.androidName,
      cancellationSignal,
      callbackExecutor,
    ) { location ->
      if (continuation.isActive) {
        continuation.resume(CurrentLocationCallbackResult(location))
      }
    }
  }

  private fun Location.toCandidate(): DeviceLocationCandidate = toDeviceLocationCandidate()

  private val DeviceLocationProvider.androidName: String
    get() = when (this) {
      DeviceLocationProvider.GPS -> LocationManager.GPS_PROVIDER
      DeviceLocationProvider.NETWORK -> LocationManager.NETWORK_PROVIDER
    }

  private data class CurrentLocationCallbackResult(val location: Location?)

  private companion object {
    const val DEFAULT_TIMEOUT_MILLIS = 20_000L
    const val MAXIMUM_ACCEPTED_FIX_AGE_NANOS = 30_000_000_000L
  }
}

internal fun Location.toDeviceLocationCandidate(): DeviceLocationCandidate = DeviceLocationCandidate(
  latitudeDegrees = latitude,
  longitudeDegrees = longitude,
  horizontalAccuracyMeters = accuracy.toDouble().takeIf { hasAccuracy() },
  altitudeMeters = meanSeaLevelAltitudeMetersOrNull(),
  elapsedRealtimeNanos = elapsedRealtimeNanos,
)

private fun Location.meanSeaLevelAltitudeMetersOrNull(): Double? {
  return if (
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE &&
    hasMslAltitude()
  ) {
    mslAltitudeMeters
  } else {
    null
  }
}
