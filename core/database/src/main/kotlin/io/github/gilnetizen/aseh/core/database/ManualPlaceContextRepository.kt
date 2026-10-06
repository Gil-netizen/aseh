package io.github.gilnetizen.aseh.core.database

import kotlinx.coroutines.flow.Flow

/**
 * One user-confirmed place used to interpret local calendar context.
 *
 * The model contains no Android or persistence types. Repository adapters
 * validate it before storage so feature modules can stay behind this narrow
 * boundary.
 */
data class ManualPlaceContext(
    val label: String,
    val latitudeDegrees: Double,
    val longitudeDegrees: Double,
    val elevationMeters: Double?,
    val timeZoneId: String,
    val source: PlaceContextSource = PlaceContextSource.MANUAL,
    val horizontalAccuracyMeters: Double? = null,
)

enum class PlaceContextSource {
    MANUAL,
    DEVICE,
}

interface ManualPlaceContextRepository {
    val context: Flow<ManualPlaceContext?>

    /** Saves the complete context atomically after validating every value. */
    suspend fun save(context: ManualPlaceContext)

    suspend fun clear()
}

const val MAX_MANUAL_PLACE_LABEL_LENGTH = 80
