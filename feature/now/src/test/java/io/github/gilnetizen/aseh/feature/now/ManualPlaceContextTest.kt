package io.github.gilnetizen.aseh.feature.now

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ManualPlaceContextTest {
    @Test
    fun validDraftNormalizesUserInput() {
        val result = validateManualPlaceDraft(
            ManualPlaceDraft(
                label = "  Sample East  ",
                latitude = "12,25",
                longitude = "-34.5",
                elevation = "123.75",
                timeZoneId = "UTC",
            ),
        )

        assertTrue(result.isValid)
        assertEquals(
            NowPlaceContext(
                label = "Sample East",
                latitudeDegrees = 12.25,
                longitudeDegrees = -34.5,
                elevationMeters = 123.75,
                timeZoneId = "UTC",
            ),
            result.context,
        )
    }

    @Test
    fun coordinateBoundariesAreInclusive() {
        val result = validateManualPlaceDraft(
            ManualPlaceDraft(
                label = "Boundary fixture",
                latitude = "-90",
                longitude = "180",
                elevation = "",
                timeZoneId = "UTC",
            ),
        )

        assertTrue(result.isValid)
        assertEquals(-90.0, result.context?.latitudeDegrees ?: 0.0, 0.0)
        assertEquals(180.0, result.context?.longitudeDegrees ?: 0.0, 0.0)
        assertNull(result.context?.elevationMeters)
    }

    @Test
    fun invalidFieldsAreReportedTogether() {
        val result = validateManualPlaceDraft(
            ManualPlaceDraft(
                label = " ",
                latitude = "90.01",
                longitude = "not a number",
                elevation = "Infinity",
                timeZoneId = "Synthetic/Unknown",
            ),
        )

        assertFalse(result.isValid)
        assertNull(result.context)
        assertEquals(ManualPlaceInputError.REQUIRED, result.labelError)
        assertEquals(ManualPlaceInputError.OUT_OF_RANGE, result.latitudeError)
        assertEquals(ManualPlaceInputError.INVALID_NUMBER, result.longitudeError)
        assertEquals(ManualPlaceInputError.INVALID_NUMBER, result.elevationError)
        assertEquals(ManualPlaceInputError.UNKNOWN_TIME_ZONE, result.timeZoneError)
    }

    @Test
    fun labelLengthIsBounded() {
        val result = validateManualPlaceDraft(
            ManualPlaceDraft(
                label = "x".repeat(MAX_PLACE_LABEL_LENGTH + 1),
                latitude = "0",
                longitude = "0",
                elevation = "",
                timeZoneId = "UTC",
            ),
        )

        assertEquals(ManualPlaceInputError.TOO_LONG, result.labelError)
        assertFalse(result.isValid)
    }

    @Test
    fun existingContextRoundTripsIntoEditableText() {
        val context = NowPlaceContext(
            label = "Sample West",
            latitudeDegrees = 34.0,
            longitudeDegrees = -118.25,
            elevationMeters = null,
            timeZoneId = "America/Los_Angeles",
        )

        val draft = ManualPlaceDraft.initial(context, defaultTimeZoneId = "UTC")

        assertEquals("Sample West", draft.label)
        assertEquals("34", draft.latitude)
        assertEquals("-118.25", draft.longitude)
        assertEquals("", draft.elevation)
        assertEquals("America/Los_Angeles", draft.timeZoneId)
        assertEquals(context, validateManualPlaceDraft(draft).context)
    }

    @Test
    fun veryLargeFiniteElevationRoundTripsWithoutClamping() {
        val context = NowPlaceContext(
            label = "Large elevation fixture",
            latitudeDegrees = 1.0,
            longitudeDegrees = 2.0,
            elevationMeters = Double.MAX_VALUE,
            timeZoneId = "UTC",
        )

        val draft = ManualPlaceDraft.initial(context, defaultTimeZoneId = "UTC")

        assertEquals(context, validateManualPlaceDraft(draft).context)
    }
}
