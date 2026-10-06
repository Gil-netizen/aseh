package io.github.gilnetizen.aseh.feature.now

import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Test

class NowUiStateTest {
    @Test
    fun `formats a fixed instant in the supplied clock zone`() {
        val clock = Clock.fixed(
            Instant.parse("2026-10-06T12:04:05Z"),
            ZoneId.of("Asia/Jerusalem"),
        )

        assertEquals(
            NowUiState(
                civilDate = "October 6, 2026",
                weekday = "Tuesday",
                localTime = "15:04:05",
                timeZone = "Asia/Jerusalem",
            ),
            currentNowUiState(clock),
        )
    }

    @Test
    fun `the same instant follows the supplied civil time zone`() {
        val instant = Instant.parse("2026-01-01T00:30:45Z")

        assertEquals(
            NowUiState(
                civilDate = "December 31, 2025",
                weekday = "Wednesday",
                localTime = "16:30:45",
                timeZone = "America/Los_Angeles",
            ),
            formatNowUiState(
                instant = instant,
                zoneId = ZoneId.of("America/Los_Angeles"),
            ),
        )
    }

    @Test
    fun `utc output is stable and English`() {
        assertEquals(
            NowUiState(
                civilDate = "October 6, 2026",
                weekday = "Tuesday",
                localTime = "12:04:05",
                timeZone = "Z",
            ),
            formatNowUiState(
                instant = Instant.parse("2026-10-06T12:04:05Z"),
                zoneId = ZoneOffset.UTC,
            ),
        )
    }

    @Test
    fun `refresh resamples the injected clock without waiting`() {
        val clock = MutableClock(
            instant = Instant.parse("2026-10-06T20:59:59Z"),
            zone = ZoneId.of("Asia/Jerusalem"),
        )
        val state = NowScreenState(clock)

        assertEquals(
            NowUiState(
                civilDate = "October 6, 2026",
                weekday = "Tuesday",
                localTime = "23:59:59",
                timeZone = "Asia/Jerusalem",
            ),
            state.snapshot,
        )

        clock.advanceSeconds(2)
        state.refresh()

        assertEquals(
            NowUiState(
                civilDate = "October 7, 2026",
                weekday = "Wednesday",
                localTime = "00:00:01",
                timeZone = "Asia/Jerusalem",
            ),
            state.snapshot,
        )
    }

    @Test
    fun `refresh resamples the supplied device time zone`() {
        val clock = Clock.fixed(
            Instant.parse("2026-10-07T01:00:00Z"),
            ZoneOffset.UTC,
        )
        var zone = ZoneId.of("UTC")
        val state = NowScreenState(clock, timeZone = { zone })

        assertEquals("October 7, 2026", state.snapshot.civilDate)
        assertEquals("01:00:00", state.snapshot.localTime)
        assertEquals("UTC", state.snapshot.timeZone)

        zone = ZoneId.of("America/Los_Angeles")
        state.refresh()

        assertEquals("October 6, 2026", state.snapshot.civilDate)
        assertEquals("Tuesday", state.snapshot.weekday)
        assertEquals("18:00:00", state.snapshot.localTime)
        assertEquals("America/Los_Angeles", state.snapshot.timeZone)
    }

    private class MutableClock(
        private var instant: Instant,
        private val zone: ZoneId,
    ) : Clock() {
        override fun instant(): Instant = instant

        override fun getZone(): ZoneId = zone

        override fun withZone(zone: ZoneId): Clock = MutableClock(instant, zone)

        fun advanceSeconds(seconds: Long) {
            instant = instant.plusSeconds(seconds)
        }
    }
}
