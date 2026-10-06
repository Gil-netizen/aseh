package io.github.gilnetizen.aseh.location

import kotlinx.coroutines.withTimeoutOrNull

internal enum class DeviceLocationPermission {
  NONE,
  APPROXIMATE,
  PRECISE,
}

internal data class DeviceLocationCandidate(
  val latitudeDegrees: Double,
  val longitudeDegrees: Double,
  val horizontalAccuracyMeters: Double?,
  val altitudeMeters: Double?,
  val elapsedRealtimeNanos: Long,
)

internal fun selectDeviceLocationProviders(
  permission: DeviceLocationPermission,
  enabledProviders: Set<DeviceLocationProvider>,
): List<DeviceLocationProvider> = when (permission) {
  DeviceLocationPermission.NONE -> emptyList()
  DeviceLocationPermission.APPROXIMATE -> listOf(
    DeviceLocationProvider.NETWORK,
    DeviceLocationProvider.GPS,
  )
  DeviceLocationPermission.PRECISE -> listOf(
    DeviceLocationProvider.GPS,
    DeviceLocationProvider.NETWORK,
  )
}.filter(enabledProviders::contains)

internal suspend fun acquireDeviceLocation(
  providers: List<DeviceLocationProvider>,
  isApproximate: Boolean,
  timeoutMillis: Long,
  maximumAgeNanos: Long,
  elapsedRealtimeNanos: () -> Long,
  request: suspend (DeviceLocationProvider) -> DeviceLocationCandidate?,
): DeviceLocationOutcome {
  if (providers.isEmpty()) {
    return DeviceLocationOutcome.Unavailable
  }

  return withTimeoutOrNull(timeoutMillis) {
    providers.forEachIndexed { index, provider ->
      val providersRemaining = providers.size - index
      val candidate = if (providersRemaining > 1) {
        withTimeoutOrNull((timeoutMillis / providersRemaining).coerceAtLeast(1L)) {
          request(provider)
        }
      } else {
        request(provider)
      }
      if (candidate != null) {
        val fix = validateDeviceLocation(
          candidate = candidate,
          provider = provider,
          isApproximate = isApproximate,
          observedAtElapsedRealtimeNanos = elapsedRealtimeNanos(),
          maximumAgeNanos = maximumAgeNanos,
        )
        if (fix != null) {
          return@withTimeoutOrNull DeviceLocationOutcome.Success(fix)
        }
      }
    }
    DeviceLocationOutcome.Unavailable
  } ?: DeviceLocationOutcome.Timeout
}

internal fun validateDeviceLocation(
  candidate: DeviceLocationCandidate,
  provider: DeviceLocationProvider,
  isApproximate: Boolean,
  observedAtElapsedRealtimeNanos: Long,
  maximumAgeNanos: Long,
): DeviceLocationFix? {
  if (!candidate.latitudeDegrees.isFinite() || candidate.latitudeDegrees !in -90.0..90.0) {
    return null
  }
  if (!candidate.longitudeDegrees.isFinite() || candidate.longitudeDegrees !in -180.0..180.0) {
    return null
  }
  if (
    candidate.horizontalAccuracyMeters != null &&
    (!candidate.horizontalAccuracyMeters.isFinite() || candidate.horizontalAccuracyMeters < 0.0)
  ) {
    return null
  }
  if (candidate.altitudeMeters != null && !candidate.altitudeMeters.isFinite()) {
    return null
  }
  if (
    candidate.elapsedRealtimeNanos <= 0L ||
    observedAtElapsedRealtimeNanos < candidate.elapsedRealtimeNanos ||
    observedAtElapsedRealtimeNanos - candidate.elapsedRealtimeNanos > maximumAgeNanos
  ) {
    return null
  }

  return DeviceLocationFix(
    latitudeDegrees = candidate.latitudeDegrees,
    longitudeDegrees = candidate.longitudeDegrees,
    horizontalAccuracyMeters = candidate.horizontalAccuracyMeters,
    altitudeMeters = candidate.altitudeMeters,
    isApproximate = isApproximate,
    provider = provider,
  )
}
