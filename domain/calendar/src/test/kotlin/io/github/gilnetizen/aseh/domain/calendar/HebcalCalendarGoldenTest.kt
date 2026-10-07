package io.github.gilnetizen.aseh.domain.calendar

import io.github.gilnetizen.aseh.domain.servicecatalog.CalendarRegion
import io.github.gilnetizen.aseh.domain.servicecatalog.ServiceDayKind
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

/**
 * Offline cross-implementation comparison against selected Hebcal API observations.
 * See ../../../../../resources and HEBCAL_CALENDAR_GOLDEN_PROVENANCE.md.
 */
class HebcalCalendarGoldenTest {
    private val engine = KosherJavaCalendarContextEngine()

    @Test
    fun pinnedKosherJavaMatchesFrozenHebcalMetadata() {
        val fixtures = loadFixtures()
        assertEquals(16, fixtures.size)

        fixtures.forEach { fixture ->
            val context = engine.resolve(fixture.request()).resolved()

            assertEquals("${fixture.id} Hebrew year", fixture.hebrewYear, context.jewishDate.year)
            assertEquals("${fixture.id} Hebrew month", fixture.hebrewMonth, context.jewishDate.month)
            assertEquals("${fixture.id} Hebrew day", fixture.hebrewDay, context.jewishDate.dayOfMonth)
            assertEquals("${fixture.id} day kind", fixture.serviceDayKind, context.serviceDayKind)
            assertEquals(
                "${fixture.id} work-restricted festival signal",
                fixture.workRestricted,
                context.details.rawSignals.isWorkRestrictedFestival,
            )
            assertEquals("${fixture.id} fast signal", fixture.fastDay, context.isFastDay)
            assertEquals("${fixture.id} Rosh Hodesh signal", fixture.roshHodesh, context.isRoshHodesh)
            assertEquals(
                "${fixture.id} special Shabbat signal",
                fixture.specialShabbat.takeUnless { it == "NONE" },
                context.details.rawSignals.kosherJavaSpecialShabbatName,
            )
            assertEquals(
                "${fixture.id} observances",
                fixture.hebcalObservanceIds + fixture.knownKosherJavaOnlyObservanceIds,
                context.observances.map { it.id.stableId },
            )
            assertEquals(setOf(fixture.region), context.serviceCatalogOverride.applicableRegions)
            assertFalse(context.observances.any { CalendarObservanceCategory.MODERN_OBSERVANCE in it.id.categories })
        }
    }

    private fun loadFixtures(): List<Fixture> {
        val lines = checkNotNull(javaClass.getResourceAsStream("/hebcal-calendar-6.13.1-4.2.2-golden.tsv")) {
            "Missing Hebcal calendar golden fixture."
        }.bufferedReader().use { reader ->
            reader.lineSequence()
                .filterNot { line -> line.isBlank() || line.startsWith('#') }
                .toList()
        }
        val header = lines.first().split('\t')
        assertEquals(EXPECTED_COLUMNS, header)
        return lines.drop(1).map { line ->
            val values = line.split('\t')
            assertEquals("fixture column count for $line", header.size, values.size)
            val row = header.zip(values).toMap()
            Fixture(
                id = row.getValue("id"),
                date = LocalDate.parse(row.getValue("date")),
                region = CalendarRegion.valueOf(row.getValue("region")),
                hebrewYear = row.getValue("hebrewYear").toInt(),
                hebrewMonth = JewishMonth.valueOf(row.getValue("hebrewMonth")),
                hebrewDay = row.getValue("hebrewDay").toInt(),
                serviceDayKind = ServiceDayKind.valueOf(row.getValue("serviceDayKind")),
                workRestricted = row.getValue("workRestricted").toBooleanStrict(),
                fastDay = row.getValue("fastDay").toBooleanStrict(),
                roshHodesh = row.getValue("roshHodesh").toBooleanStrict(),
                specialShabbat = row.getValue("specialShabbat"),
                hebcalObservanceIds = row.getValue("hebcalObservanceIds").ids(),
                knownKosherJavaOnlyObservanceIds = row
                    .getValue("knownKosherJavaOnlyObservanceIds")
                    .ids(),
            )
        }
    }

    private fun String.ids(): List<String> =
        takeUnless { value -> value == "NONE" }?.split(';').orEmpty()

    private fun CalendarContextResolution.resolved(): ResolvedCalendarContext {
        check(this is CalendarContextResolution.Resolved) { "Expected resolved context, got $this" }
        return context
    }

    private data class Fixture(
        val id: String,
        val date: LocalDate,
        val region: CalendarRegion,
        val hebrewYear: Int,
        val hebrewMonth: JewishMonth,
        val hebrewDay: Int,
        val serviceDayKind: ServiceDayKind,
        val workRestricted: Boolean,
        val fastDay: Boolean,
        val roshHodesh: Boolean,
        val specialShabbat: String,
        val hebcalObservanceIds: List<String>,
        /** Explicitly documented production-library metadata absent from the Hebcal response. */
        val knownKosherJavaOnlyObservanceIds: List<String>,
    ) {
        fun request() = CalendarContextRequest(date, region)
    }

    private companion object {
        val EXPECTED_COLUMNS = listOf(
            "id",
            "date",
            "region",
            "hebrewYear",
            "hebrewMonth",
            "hebrewDay",
            "serviceDayKind",
            "workRestricted",
            "fastDay",
            "roshHodesh",
            "specialShabbat",
            "hebcalObservanceIds",
            "knownKosherJavaOnlyObservanceIds",
        )
    }
}
