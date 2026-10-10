package io.github.gilnetizen.aseh.core.database

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import io.github.gilnetizen.aseh.core.model.CalendarRegion
import io.github.gilnetizen.aseh.core.model.DeviceUseMode
import io.github.gilnetizen.aseh.core.model.ParticipantRole
import io.github.gilnetizen.aseh.core.model.WorkspaceKind
import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class LegacyExperiencePreferencesTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun legacyOperationalValuesAreDecodedWithoutCollapsingAssignmentOnlyRecords() = runTest {
        val store = PreferenceDataStoreFactory.create(
            scope = backgroundScope,
            produceFile = { File(temporaryFolder.root, "legacy.preferences_pb") },
        )
        store.edit { values ->
            values[ExperiencePreferenceKeys.workspaceName] = "Local qahal"
            values[ExperiencePreferenceKeys.workspaceKind] = WorkspaceKind.QAHAL.id
            values[ExperiencePreferenceKeys.roleAssignment(ParticipantRole.LEADER)] = "Ari"
            values[ExperiencePreferenceKeys.readingAssignment("reading.aliyah.1")] = "Shira"
            values[ExperiencePreferenceKeys.completedPracticeSteps] = setOf("practice.one")
            values[ExperiencePreferenceKeys.bookmarkedSources] = setOf("source.one")
            values[ExperiencePreferenceKeys.selectedRole] = ParticipantRole.GABBAI.id
            values[ExperiencePreferenceKeys.selectedServicePlan] = "dev.service.weekday.morning@2026-10-07"
            values[ExperiencePreferenceKeys.deviceUseMode] = DeviceUseMode.PRINT_ONLY.id
            values[ExperiencePreferenceKeys.calendarRegion] = CalendarRegion.ISRAEL.name
            values[ExperiencePreferenceKeys.useLargeText] = true
        }

        val decoded = store.data.first().toExperienceStateFromLegacyPreferences()

        assertEquals("Local qahal", decoded.workspaceName)
        assertEquals("Ari", decoded.roleAssignments[ParticipantRole.LEADER])
        assertEquals("Shira", decoded.readingAssignments["reading.aliyah.1"])
        assertFalse(decoded.readingPlans.containsKey("reading.aliyah.1"))
        assertEquals(setOf("practice.one"), decoded.completedPracticeStepIds)
        assertEquals(setOf("source.one"), decoded.bookmarkedSourceIds)
        assertEquals(ParticipantRole.GABBAI, decoded.selectedRole)
        assertEquals(
            "dev.service.weekday.morning@2026-10-07",
            decoded.selectedServicePlanId,
        )
        assertEquals(DeviceUseMode.PRINT_ONLY, decoded.deviceUseMode)
        assertEquals(CalendarRegion.ISRAEL, decoded.calendarRegion)
        assertTrue(decoded.accessibilityProfile.useLargeText)
    }

    @Test
    fun cleanupRemovesOnlyMigratedOperationalKeys() = runTest {
        val store = PreferenceDataStoreFactory.create(
            scope = backgroundScope,
            produceFile = { File(temporaryFolder.root, "cleanup.preferences_pb") },
        )
        store.edit { values ->
            values[ExperiencePreferenceKeys.workspaceName] = "Household"
            values[ExperiencePreferenceKeys.roleAssignment(ParticipantRole.HOST)] = "Leah"
            values[ExperiencePreferenceKeys.readingAssignment("reading.maftir")] = "David"
            values[ExperiencePreferenceKeys.charterReviewDate] = "2027-01-07"
            values[ExperiencePreferenceKeys.selectedRole] = ParticipantRole.READER.id
            values[ExperiencePreferenceKeys.selectedServicePlan] = "dev.service.shabbat.morning@2026-10-10"
            values[ExperiencePreferenceKeys.deviceUseMode] = DeviceUseMode.PREPARED_DISPLAY_ONLY.id
            values[ExperiencePreferenceKeys.calendarRegion] = CalendarRegion.DIASPORA.name
            values[ExperiencePreferenceKeys.keepScreenAwake] = true
        }

        store.removeLegacyOperationalExperienceKeys()

        val remaining = store.data.first()
        assertFalse(remaining.asMap().keys.any { key ->
            key in ExperiencePreferenceKeys.operationalStaticKeys ||
                ExperiencePreferenceKeys.isDynamicOperationalKey(key.name)
        })
        assertEquals(ParticipantRole.READER.id, remaining[ExperiencePreferenceKeys.selectedRole])
        assertEquals(
            "dev.service.shabbat.morning@2026-10-10",
            remaining[ExperiencePreferenceKeys.selectedServicePlan],
        )
        assertEquals(
            DeviceUseMode.PREPARED_DISPLAY_ONLY.id,
            remaining[ExperiencePreferenceKeys.deviceUseMode],
        )
        assertEquals(CalendarRegion.DIASPORA.name, remaining[ExperiencePreferenceKeys.calendarRegion])
        assertTrue(remaining[ExperiencePreferenceKeys.keepScreenAwake] == true)
    }
}
