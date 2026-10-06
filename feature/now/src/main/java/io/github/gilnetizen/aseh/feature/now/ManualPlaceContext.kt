package io.github.gilnetizen.aseh.feature.now

import java.math.BigDecimal
import java.time.ZoneId

data class NowPlaceContext(
    val label: String,
    val latitudeDegrees: Double,
    val longitudeDegrees: Double,
    val elevationMeters: Double?,
    val timeZoneId: String,
    val source: NowPlaceSource = NowPlaceSource.MANUAL,
    val horizontalAccuracyMeters: Double? = null,
) {
    init {
        require(
            label == label.trim() &&
                label.length <= MAX_PLACE_LABEL_LENGTH &&
                (source == NowPlaceSource.DEVICE || label.isNotEmpty()),
        )
        require(latitudeDegrees.isFinite() && latitudeDegrees in -90.0..90.0)
        require(longitudeDegrees.isFinite() && longitudeDegrees in -180.0..180.0)
        require(elevationMeters == null || elevationMeters.isFinite())
        require(
            horizontalAccuracyMeters == null ||
                (horizontalAccuracyMeters.isFinite() && horizontalAccuracyMeters >= 0.0),
        )
        require(timeZoneId in ZoneId.getAvailableZoneIds())
    }
}

enum class NowPlaceSource {
    MANUAL,
    DEVICE,
}

sealed interface DeviceLocationUiState {
    data object Idle : DeviceLocationUiState

    data object Locating : DeviceLocationUiState

    data class PermissionDenied(
        val openSettingsRequired: Boolean,
    ) : DeviceLocationUiState

    data object LocationDisabled : DeviceLocationUiState

    data object TimedOut : DeviceLocationUiState

    data object Unavailable : DeviceLocationUiState

    data class Preview(
        val context: NowPlaceContext,
        val isApproximate: Boolean,
    ) : DeviceLocationUiState {
        init {
            require(context.source == NowPlaceSource.DEVICE)
        }
    }
}

internal data class ManualPlaceDraft(
    val label: String,
    val latitude: String,
    val longitude: String,
    val elevation: String,
    val timeZoneId: String,
) {
    companion object {
        fun initial(
            existing: NowPlaceContext?,
            defaultTimeZoneId: String,
        ): ManualPlaceDraft = ManualPlaceDraft(
            label = existing?.label.orEmpty(),
            latitude = existing?.latitudeDegrees?.toPlainInput().orEmpty(),
            longitude = existing?.longitudeDegrees?.toPlainInput().orEmpty(),
            elevation = existing?.elevationMeters?.toPlainInput().orEmpty(),
            timeZoneId = existing?.timeZoneId ?: defaultTimeZoneId,
        )
    }
}

internal enum class ManualPlaceInputError {
    REQUIRED,
    TOO_LONG,
    INVALID_NUMBER,
    OUT_OF_RANGE,
    UNKNOWN_TIME_ZONE,
}

internal data class ManualPlaceValidation(
    val context: NowPlaceContext?,
    val labelError: ManualPlaceInputError? = null,
    val latitudeError: ManualPlaceInputError? = null,
    val longitudeError: ManualPlaceInputError? = null,
    val elevationError: ManualPlaceInputError? = null,
    val timeZoneError: ManualPlaceInputError? = null,
) {
    val isValid: Boolean
        get() = context != null
}

internal fun validateManualPlaceDraft(draft: ManualPlaceDraft): ManualPlaceValidation {
    val label = draft.label.trim()
    val labelError = when {
        label.isEmpty() -> ManualPlaceInputError.REQUIRED
        label.length > MAX_PLACE_LABEL_LENGTH -> ManualPlaceInputError.TOO_LONG
        else -> null
    }

    val latitude = draft.latitude.toCoordinateNumber()
    val latitudeError = when {
        draft.latitude.isBlank() -> ManualPlaceInputError.REQUIRED
        latitude == null || !latitude.isFinite() -> ManualPlaceInputError.INVALID_NUMBER
        latitude !in -90.0..90.0 -> ManualPlaceInputError.OUT_OF_RANGE
        else -> null
    }

    val longitude = draft.longitude.toCoordinateNumber()
    val longitudeError = when {
        draft.longitude.isBlank() -> ManualPlaceInputError.REQUIRED
        longitude == null || !longitude.isFinite() -> ManualPlaceInputError.INVALID_NUMBER
        longitude !in -180.0..180.0 -> ManualPlaceInputError.OUT_OF_RANGE
        else -> null
    }

    val elevation = draft.elevation.toOptionalNumber()
    val elevationError = when {
        draft.elevation.isBlank() -> null
        elevation == null || !elevation.isFinite() -> ManualPlaceInputError.INVALID_NUMBER
        else -> null
    }

    val timeZoneId = draft.timeZoneId.trim()
    val timeZoneError = when {
        timeZoneId.isEmpty() -> ManualPlaceInputError.REQUIRED
        timeZoneId !in ZoneId.getAvailableZoneIds() -> ManualPlaceInputError.UNKNOWN_TIME_ZONE
        else -> null
    }

    val hasError = listOf(
        labelError,
        latitudeError,
        longitudeError,
        elevationError,
        timeZoneError,
    ).any { it != null }

    return ManualPlaceValidation(
        context = if (hasError) {
            null
        } else {
            NowPlaceContext(
                label = label,
                latitudeDegrees = requireNotNull(latitude),
                longitudeDegrees = requireNotNull(longitude),
                elevationMeters = elevation,
                timeZoneId = timeZoneId,
            )
        },
        labelError = labelError,
        latitudeError = latitudeError,
        longitudeError = longitudeError,
        elevationError = elevationError,
        timeZoneError = timeZoneError,
    )
}

private fun String.toCoordinateNumber(): Double? =
    trim().replace(',', '.').toDoubleOrNull()

private fun String.toOptionalNumber(): Double? = when {
    isBlank() -> null
    else -> toCoordinateNumber()
}

private fun Double.toPlainInput(): String =
    BigDecimal.valueOf(this).stripTrailingZeros().toPlainString()

internal const val MAX_PLACE_LABEL_LENGTH = 80
