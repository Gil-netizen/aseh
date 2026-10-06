package io.github.gilnetizen.aseh.location

/** Acquires one foreground-only device location when explicitly requested by the user. */
interface DeviceLocationClient {
  suspend fun getCurrentLocation(): DeviceLocationOutcome
}

sealed interface DeviceLocationOutcome {
  data class Success(val fix: DeviceLocationFix) : DeviceLocationOutcome

  data object PermissionDenied : DeviceLocationOutcome

  data object LocationDisabled : DeviceLocationOutcome

  data object Unavailable : DeviceLocationOutcome

  data object Timeout : DeviceLocationOutcome

  data object Failure : DeviceLocationOutcome
}

data class DeviceLocationFix(
  val latitudeDegrees: Double,
  val longitudeDegrees: Double,
  val horizontalAccuracyMeters: Double?,
  val altitudeMeters: Double?,
  val isApproximate: Boolean,
  val provider: DeviceLocationProvider,
)

enum class DeviceLocationProvider {
  GPS,
  NETWORK,
}
