package io.github.gilnetizen.aseh

import io.github.gilnetizen.aseh.core.database.ManualPlaceContext
import io.github.gilnetizen.aseh.core.database.PlaceContextSource
import io.github.gilnetizen.aseh.core.model.ServiceHebrewDateContext
import io.github.gilnetizen.aseh.core.model.ServiceSolarEvent
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ServiceCalendarContextTest {
  @Test
  fun savedCoordinatesProduceLocalHebrewDateAndServiceDaySolarFacts() {
    val serviceDate = LocalDate.of(2026, 10, 10)

    val context = calculateServiceCalendarContext(
      place = ManualPlaceContext(
        label = "Jerusalem test fixture",
        latitudeDegrees = 31.778,
        longitudeDegrees = 35.235,
        elevationMeters = 754.5,
        timeZoneId = "Asia/Jerusalem",
        source = PlaceContextSource.MANUAL,
      ),
      serviceDate = serviceDate,
    )

    assertTrue(context.hebrewDate is ServiceHebrewDateContext.Available)
    assertEquals(serviceDate, context.zmanim.date)
    assertEquals("Asia/Jerusalem", context.zmanim.timeZoneId)
    assertEquals(3, context.zmanim.events.size)
    assertTrue(context.zmanim.events.all { event -> event is ServiceSolarEvent.Available })
    assertTrue(context.zmanim.methodLabel.contains("KosherJava Zmanim API"))
  }

  @Test
  fun missingPlaceKeepsCalendarAndSolarFactsExplicitlyUnavailable() {
    val context = calculateServiceCalendarContext(
      place = null,
      serviceDate = LocalDate.of(2026, 10, 10),
    )

    assertTrue(context.hebrewDate is ServiceHebrewDateContext.Unavailable)
    assertTrue(context.zmanim.events.all { event -> event is ServiceSolarEvent.Unavailable })
    assertTrue(context.zmanim.methodLabel.startsWith("Not calculated:"))
  }
}
