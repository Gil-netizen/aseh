package io.github.gilnetizen.aseh

import io.github.gilnetizen.aseh.core.model.ServiceDayKind
import io.github.gilnetizen.aseh.core.model.ServiceInstanceKey
import io.github.gilnetizen.aseh.domain.servicecatalog.ServiceKind
import io.github.gilnetizen.aseh.domain.servicecatalog.ServiceAvailabilityStatus
import io.github.gilnetizen.aseh.domain.servicecatalog.ServiceScheduleSelector
import io.github.gilnetizen.aseh.domain.servicecatalog.ServiceSelectionRequest
import io.github.gilnetizen.aseh.domain.servicecatalog.ServiceSelectionResult
import io.github.gilnetizen.aseh.domain.servicecatalog.ServiceTemporalState
import io.github.gilnetizen.aseh.domain.servicecatalog.SyntheticDevelopmentServiceCatalog
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class SelectedServiceBindingTest {
  private val selection = ServiceScheduleSelector(SyntheticDevelopmentServiceCatalog.catalog)
    .select(
      ServiceSelectionRequest(
        now = Instant.parse("2026-10-07T00:00:00Z"),
        zoneId = ZoneId.of("Asia/Jerusalem"),
        opinionProfileId = SyntheticDevelopmentServiceCatalog.ISRAEL_PROFILE_ID,
      ),
    ) as ServiceSelectionResult.Available

  @Test
  fun defaultSelectionPrefersTheInstalledShabbatMorningRehearsal() {
    val firstChronologicalService = selection.agenda.upcomingServices.first()
    val selected = requireNotNull(selectScheduledService(selection, requestedServiceId = null))

    assertFalse(firstChronologicalService.supportsShabbatMorningRehearsal())
    assertEquals(SHABBAT_MORNING_REHEARSAL_SERVICE_ID, selected.definition.id)
    assertEquals(LocalDate.parse("2026-10-10"), selected.serviceDate)
    assertTrue(selected.supportsShabbatMorningRehearsal())
    assertEquals(selected.id, selected.rehearsalServiceInstanceKeyOrNull()?.occurrenceId)
  }

  @Test
  fun explicitNonShabbatSelectionRemainsSelectedButCannotUseTheRehearsal() {
    val weekday = selection.agenda.upcomingServices.first { service ->
      service.definition.id == "dev.service.weekday.afternoon"
    }

    val selected = selectScheduledService(selection, weekday.id)

    assertSame(weekday, selected)
    assertFalse(requireNotNull(selected).supportsShabbatMorningRehearsal())
    assertEquals(null, selected.rehearsalServiceInstanceKeyOrNull())
    assertEquals(ServiceDayKind.WEEKDAY, selected.toCoreServiceDayKind())
  }

  @Test
  fun supportedOccurrenceSuppliesDateDayKindAndDisplayLabel() {
    val selected = requireNotNull(selectScheduledService(selection, requestedServiceId = null))

    assertEquals(LocalDate.parse("2026-10-10"), selected.serviceDate)
    assertEquals(ServiceDayKind.SHABBAT, selected.toCoreServiceDayKind())
    assertEquals(
      "Shabbat morning service · Saturday, October 10",
      selected.serviceDisplayLabel(),
    )
  }

  @Test
  fun supportCheckFailsClosedWhenTheExpectedDefinitionShapeChanges() {
    val selected = requireNotNull(selectScheduledService(selection, requestedServiceId = null))
    val wrongKind = selected.copy(
      definition = selected.definition.copy(serviceKind = ServiceKind.AFTERNOON),
    )

    assertFalse(wrongKind.supportsShabbatMorningRehearsal())
  }

  @Test
  fun supportCheckRequiresOperationalCapabilityAndAnOpenLifecycle() {
    val selected = requireNotNull(selectScheduledService(selection, requestedServiceId = null))
    val withoutCapability = selected.copy(
      definition = selected.definition.copy(operationalCapabilities = emptySet()),
    )

    assertEquals(ServiceAvailabilityStatus.PLANNING_ONLY, selected.availability.status)
    assertFalse(withoutCapability.supportsShabbatMorningRehearsal())
    assertFalse(
      selected.copy(temporalState = ServiceTemporalState.PASSED)
        .supportsShabbatMorningRehearsal(),
    )
    assertEquals(null, withoutCapability.rehearsalServiceInstanceKeyOrNull())
  }

  @Test
  fun unsupportedSelectionDoesNotDispatchAServiceScopedWrite() {
    var writes = 0

    val dispatched = dispatchServiceOccurrenceWrite(serviceInstance = null) { writes += 1 }

    assertFalse(dispatched)
    assertEquals(0, writes)
  }

  @Test
  fun selectedOccurrenceBecomesWritableOnlyAfterThatExactRoomOccurrenceIsActive() {
    val first = ServiceInstanceKey("service-a", LocalDate.parse("2026-10-10"))
    val second = ServiceInstanceKey("service-b", LocalDate.parse("2026-10-10"))

    assertEquals(first, writableServiceInstanceKey(selected = first, persistedActive = first))
    assertEquals(null, writableServiceInstanceKey(selected = second, persistedActive = first))
    assertEquals(null, writableServiceInstanceKey(selected = second, persistedActive = null))
    assertEquals(second, writableServiceInstanceKey(selected = second, persistedActive = second))
  }
}
