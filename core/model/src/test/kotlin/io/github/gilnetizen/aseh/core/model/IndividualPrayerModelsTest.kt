package io.github.gilnetizen.aseh.core.model

import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Test

class IndividualPrayerModelsTest {
    @Test
    fun `local clock suggestion changes at documented UX boundaries`() {
        mapOf(
            LocalTime.MIDNIGHT to IndividualPrayerKind.ARVIT,
            LocalTime.of(3, 59, 59) to IndividualPrayerKind.ARVIT,
            LocalTime.of(4, 0) to IndividualPrayerKind.SHACHARIT,
            LocalTime.of(11, 59, 59) to IndividualPrayerKind.SHACHARIT,
            LocalTime.NOON to IndividualPrayerKind.MINCHA,
            LocalTime.of(17, 59, 59) to IndividualPrayerKind.MINCHA,
            LocalTime.of(18, 0) to IndividualPrayerKind.ARVIT,
            LocalTime.of(23, 59, 59) to IndividualPrayerKind.ARVIT,
        ).forEach { (time, expected) ->
            assertEquals("Unexpected suggestion at $time", expected, suggestedIndividualPrayerKind(time))
        }
    }
}
