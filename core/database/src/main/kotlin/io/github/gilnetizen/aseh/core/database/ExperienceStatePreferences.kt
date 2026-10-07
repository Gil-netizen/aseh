package io.github.gilnetizen.aseh.core.database

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import io.github.gilnetizen.aseh.core.model.CalendarRegion
import io.github.gilnetizen.aseh.core.model.CommunityAdoption
import io.github.gilnetizen.aseh.core.model.CommunityCharter
import io.github.gilnetizen.aseh.core.model.DeviceUseMode
import io.github.gilnetizen.aseh.core.model.ExperienceState
import io.github.gilnetizen.aseh.core.model.ParticipantRole
import io.github.gilnetizen.aseh.core.model.ReadingPlanEntry
import io.github.gilnetizen.aseh.core.model.ReadingPreparationStatus
import io.github.gilnetizen.aseh.core.model.ServiceAccessibilityProfile
import io.github.gilnetizen.aseh.core.model.WorkspaceKind
import java.io.IOException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch

/**
 * Creates the existing experience preference store.
 *
 * Room owns operational records after migration. This DataStore then retains only the small
 * current-view and output preferences permitted by ADR-0003. The former operational keys remain
 * readable here solely to migrate an installed alpha without losing the user's local work.
 */
internal fun createExperiencePreferencesDataStore(
    context: Context,
    scope: CoroutineScope,
): DataStore<Preferences> {
    val appContext = context.applicationContext
    return PreferenceDataStoreFactory.create(
        corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
        scope = scope,
        produceFile = {
            appContext.preferencesDataStoreFile(EXPERIENCE_STATE_FILE_NAME)
        },
    )
}

internal fun DataStore<Preferences>.safeExperiencePreferences(): Flow<Preferences> = data
    .catch { error ->
        if (error is IOException) emit(emptyPreferences()) else throw error
    }

internal fun Preferences.toExperiencePreferenceState(): ExperienceState = ExperienceState(
    selectedServicePlanId = this[ExperiencePreferenceKeys.selectedServicePlan]
        ?.takeIf(String::isNotBlank),
    selectedRole = this[ExperiencePreferenceKeys.selectedRole]
        ?.let { id -> ParticipantRole.entries.firstOrNull { it.id == id } }
        ?: ParticipantRole.CONGREGANT,
    deviceUseMode = this[ExperiencePreferenceKeys.deviceUseMode]
        ?.let { id -> DeviceUseMode.entries.firstOrNull { it.id == id } }
        ?: DeviceUseMode.DEVICE_PERMITTED,
    calendarRegion = this[ExperiencePreferenceKeys.calendarRegion]
        ?.let { name -> CalendarRegion.entries.firstOrNull { it.name == name } }
        ?: CalendarRegion.UNSPECIFIED,
    accessibilityProfile = ServiceAccessibilityProfile(
        participantNeedsReviewed = this[ExperiencePreferenceKeys.accessNeedsReviewed] ?: false,
        useMovementAlternatives = this[ExperiencePreferenceKeys.useMovementAlternatives] ?: false,
        useVisualVoiceCues = this[ExperiencePreferenceKeys.useVisualVoiceCues] ?: false,
        useLargeText = this[ExperiencePreferenceKeys.useLargeText] ?: false,
        useHighContrast = this[ExperiencePreferenceKeys.useHighContrast] ?: false,
        reduceMotion = this[ExperiencePreferenceKeys.reduceMotion] ?: false,
        keepScreenAwake = this[ExperiencePreferenceKeys.keepScreenAwake] ?: false,
        lowLightMode = this[ExperiencePreferenceKeys.lowLightMode] ?: false,
    ),
)

internal fun Preferences.toExperienceStateFromLegacyPreferences(): ExperienceState {
    val role = this[ExperiencePreferenceKeys.selectedRole]
        ?.let { id -> ParticipantRole.entries.firstOrNull { it.id == id } }
        ?: ParticipantRole.CONGREGANT
    val kind = this[ExperiencePreferenceKeys.workspaceKind]
        ?.let { id -> WorkspaceKind.entries.firstOrNull { it.id == id } }
        ?: WorkspaceKind.QAHAL
    val assignments = ParticipantRole.entries.mapNotNull { participantRole ->
        this[ExperiencePreferenceKeys.roleAssignment(participantRole)]
            ?.takeIf(String::isNotBlank)
            ?.let { participantRole to it }
    }.toMap()
    val readingAssignments = asMap().mapNotNull { (key, value) ->
        if (!key.name.startsWith(ExperiencePreferenceKeys.READING_ASSIGNMENT_PREFIX)) {
            return@mapNotNull null
        }
        val slotId = key.name.removePrefix(ExperiencePreferenceKeys.READING_ASSIGNMENT_PREFIX)
        val assignee = value as? String
        if (slotId.isBlank() || assignee.isNullOrBlank()) null else slotId to assignee
    }.toMap()
    val readingPlanSlotIds = asMap().keys.mapNotNull { key ->
        ExperiencePreferenceKeys.readingPlanSlotId(key.name)
    }.toSet()
    val readingPlans = readingPlanSlotIds.associateWith { slotId ->
        ReadingPlanEntry(
            portionTitle = this[ExperiencePreferenceKeys.readingPlanPortion(slotId)].orEmpty(),
            locator = this[ExperiencePreferenceKeys.readingPlanLocator(slotId)].orEmpty(),
            passageRange = this[ExperiencePreferenceKeys.readingPlanRange(slotId)].orEmpty(),
            assignee = this[ExperiencePreferenceKeys.readingPlanAssignee(slotId)]
                .orEmpty()
                .ifBlank { readingAssignments[slotId].orEmpty() },
            backupAssignee = this[ExperiencePreferenceKeys.readingPlanBackup(slotId)].orEmpty(),
            preparationStatus = this[ExperiencePreferenceKeys.readingPlanPreparation(slotId)]
                ?.let { name -> ReadingPreparationStatus.entries.firstOrNull { it.name == name } }
                ?: ReadingPreparationStatus.NOT_STARTED,
            manualOverride = this[ExperiencePreferenceKeys.readingPlanManualOverride(slotId)] ?: false,
            overrideReason = this[ExperiencePreferenceKeys.readingPlanOverrideReason(slotId)].orEmpty(),
        )
    }
    val deviceUseMode = this[ExperiencePreferenceKeys.deviceUseMode]
        ?.let { id -> DeviceUseMode.entries.firstOrNull { it.id == id } }
        ?: DeviceUseMode.DEVICE_PERMITTED
    val calendarRegion = this[ExperiencePreferenceKeys.calendarRegion]
        ?.let { name -> CalendarRegion.entries.firstOrNull { it.name == name } }
        ?: CalendarRegion.UNSPECIFIED
    val accessibilityProfile = ServiceAccessibilityProfile(
        participantNeedsReviewed = this[ExperiencePreferenceKeys.accessNeedsReviewed] ?: false,
        useMovementAlternatives = this[ExperiencePreferenceKeys.useMovementAlternatives] ?: false,
        useVisualVoiceCues = this[ExperiencePreferenceKeys.useVisualVoiceCues] ?: false,
        useLargeText = this[ExperiencePreferenceKeys.useLargeText] ?: false,
        useHighContrast = this[ExperiencePreferenceKeys.useHighContrast] ?: false,
        reduceMotion = this[ExperiencePreferenceKeys.reduceMotion] ?: false,
        keepScreenAwake = this[ExperiencePreferenceKeys.keepScreenAwake] ?: false,
        lowLightMode = this[ExperiencePreferenceKeys.lowLightMode] ?: false,
    )
    val charterAdopted = this[ExperiencePreferenceKeys.charterAdopted]
        ?: this[ExperiencePreferenceKeys.provisionalCharterAdopted]
        ?: false
    val charter = CommunityCharter(
        purpose = this[ExperiencePreferenceKeys.charterPurpose].orEmpty(),
        participants = this[ExperiencePreferenceKeys.charterParticipants].orEmpty(),
        authorityLimits = this[ExperiencePreferenceKeys.charterAuthorityLimits].orEmpty(),
        decisionProcess = this[ExperiencePreferenceKeys.charterDecisionProcess].orEmpty(),
        roleTerms = this[ExperiencePreferenceKeys.charterRoleTerms].orEmpty(),
        accessibilityCommitment = this[ExperiencePreferenceKeys.charterAccessibility].orEmpty(),
        effectiveDate = this[ExperiencePreferenceKeys.charterEffectiveDate].orEmpty(),
        reviewDate = this[ExperiencePreferenceKeys.charterReviewDate].orEmpty(),
        version = this[ExperiencePreferenceKeys.charterVersion].orEmpty().ifBlank { "Draft 1" },
        adopted = charterAdopted,
    )
    val disputedPracticeAdoption = CommunityAdoption(
        optionId = this[ExperiencePreferenceKeys.dossierAdoptionOption],
        scope = this[ExperiencePreferenceKeys.dossierAdoptionScope].orEmpty(),
        effectiveDate = this[ExperiencePreferenceKeys.dossierAdoptionEffectiveDate].orEmpty(),
        reviewDate = this[ExperiencePreferenceKeys.dossierAdoptionReviewDate].orEmpty(),
        recordedBy = this[ExperiencePreferenceKeys.dossierAdoptionRecordedBy].orEmpty(),
    )

    return ExperienceState(
        selectedServicePlanId = this[ExperiencePreferenceKeys.selectedServicePlan]
            ?.takeIf(String::isNotBlank),
        completedPracticeStepIds = this[ExperiencePreferenceKeys.completedPracticeSteps].orEmpty(),
        savedPracticeCardIds = this[ExperiencePreferenceKeys.savedPracticeCards].orEmpty(),
        completedPreflightStepIds = this[ExperiencePreferenceKeys.completedPreflightSteps].orEmpty(),
        completedServiceSegmentIds = this[ExperiencePreferenceKeys.completedServiceSegments].orEmpty(),
        selectedRole = role,
        bookmarkedSourceIds = this[ExperiencePreferenceKeys.bookmarkedSources].orEmpty(),
        workspaceName = this[ExperiencePreferenceKeys.workspaceName].orEmpty(),
        workspaceKind = kind,
        roleAssignments = assignments,
        readingAssignments = readingAssignments,
        readingPlans = readingPlans,
        deviceUseMode = deviceUseMode,
        calendarRegion = calendarRegion,
        accessibilityProfile = accessibilityProfile,
        selectedCommunityOptionId = this[ExperiencePreferenceKeys.communityOption],
        provisionalCharterAdopted = charterAdopted,
        communityCharter = charter,
        disputedPracticeAdoption = disputedPracticeAdoption,
        reviewedDossierFactIds = this[ExperiencePreferenceKeys.reviewedDossierFacts].orEmpty(),
    )
}

internal suspend fun DataStore<Preferences>.removeLegacyOperationalExperienceKeys() {
    edit { values ->
        ExperiencePreferenceKeys.operationalStaticKeys.forEach(values::removeUntyped)
        values.asMap().keys
            .filter { key -> ExperiencePreferenceKeys.isDynamicOperationalKey(key.name) }
            .forEach(values::removeUntyped)
    }
}

@Suppress("UNCHECKED_CAST")
private fun MutablePreferences.removeUntyped(key: Preferences.Key<*>) {
    remove(key as Preferences.Key<Any>)
}

internal object ExperiencePreferenceKeys {
    val completedPracticeSteps = stringSetPreferencesKey("completed_practice_steps")
    val savedPracticeCards = stringSetPreferencesKey("saved_practice_cards")
    val completedPreflightSteps = stringSetPreferencesKey("completed_preflight_steps")
    val completedServiceSegments = stringSetPreferencesKey("completed_service_segments")
    val bookmarkedSources = stringSetPreferencesKey("bookmarked_sources")
    val selectedRole = stringPreferencesKey("selected_role")
    val selectedServicePlan = stringPreferencesKey("selected_service_plan")
    val workspaceName = stringPreferencesKey("workspace_name")
    val workspaceKind = stringPreferencesKey("workspace_kind")
    val communityOption = stringPreferencesKey("community_option")
    val provisionalCharterAdopted = booleanPreferencesKey("provisional_charter_adopted")
    val deviceUseMode = stringPreferencesKey("device_use_mode")
    val calendarRegion = stringPreferencesKey("calendar_region")
    val accessNeedsReviewed = booleanPreferencesKey("access_needs_reviewed")
    val useMovementAlternatives = booleanPreferencesKey("use_movement_alternatives")
    val useVisualVoiceCues = booleanPreferencesKey("use_visual_voice_cues")
    val useLargeText = booleanPreferencesKey("use_large_text")
    val useHighContrast = booleanPreferencesKey("use_high_contrast")
    val reduceMotion = booleanPreferencesKey("reduce_motion")
    val keepScreenAwake = booleanPreferencesKey("keep_screen_awake")
    val lowLightMode = booleanPreferencesKey("low_light_mode")
    val charterPurpose = stringPreferencesKey("charter_purpose")
    val charterParticipants = stringPreferencesKey("charter_participants")
    val charterAuthorityLimits = stringPreferencesKey("charter_authority_limits")
    val charterDecisionProcess = stringPreferencesKey("charter_decision_process")
    val charterRoleTerms = stringPreferencesKey("charter_role_terms")
    val charterAccessibility = stringPreferencesKey("charter_accessibility")
    val charterEffectiveDate = stringPreferencesKey("charter_effective_date")
    val charterReviewDate = stringPreferencesKey("charter_review_date")
    val charterVersion = stringPreferencesKey("charter_version")
    val charterAdopted = booleanPreferencesKey("charter_adopted")
    val dossierAdoptionOption = stringPreferencesKey("dossier_adoption_option")
    val dossierAdoptionScope = stringPreferencesKey("dossier_adoption_scope")
    val dossierAdoptionEffectiveDate = stringPreferencesKey("dossier_adoption_effective_date")
    val dossierAdoptionReviewDate = stringPreferencesKey("dossier_adoption_review_date")
    val dossierAdoptionRecordedBy = stringPreferencesKey("dossier_adoption_recorded_by")
    val reviewedDossierFacts = stringSetPreferencesKey("reviewed_dossier_facts")

    const val READING_ASSIGNMENT_PREFIX = "reading_assignment_"
    private const val ROLE_ASSIGNMENT_PREFIX = "role_assignment_"
    private const val READING_PLAN_PORTION_PREFIX = "reading_plan_portion_"
    private const val READING_PLAN_LOCATOR_PREFIX = "reading_plan_locator_"
    private const val READING_PLAN_RANGE_PREFIX = "reading_plan_range_"
    private const val READING_PLAN_ASSIGNEE_PREFIX = "reading_plan_assignee_"
    private const val READING_PLAN_BACKUP_PREFIX = "reading_plan_backup_"
    private const val READING_PLAN_PREPARATION_PREFIX = "reading_plan_preparation_"
    private const val READING_PLAN_MANUAL_OVERRIDE_PREFIX = "reading_plan_manual_override_"
    private const val READING_PLAN_OVERRIDE_REASON_PREFIX = "reading_plan_override_reason_"
    private val readingPlanPrefixes = listOf(
        READING_PLAN_PORTION_PREFIX,
        READING_PLAN_LOCATOR_PREFIX,
        READING_PLAN_RANGE_PREFIX,
        READING_PLAN_ASSIGNEE_PREFIX,
        READING_PLAN_BACKUP_PREFIX,
        READING_PLAN_PREPARATION_PREFIX,
        READING_PLAN_MANUAL_OVERRIDE_PREFIX,
        READING_PLAN_OVERRIDE_REASON_PREFIX,
    )
    private val dynamicOperationalPrefixes = buildList {
        add(ROLE_ASSIGNMENT_PREFIX)
        add(READING_ASSIGNMENT_PREFIX)
        addAll(readingPlanPrefixes)
    }

    val operationalStaticKeys: Set<Preferences.Key<*>> = setOf(
        completedPracticeSteps,
        savedPracticeCards,
        completedPreflightSteps,
        completedServiceSegments,
        bookmarkedSources,
        workspaceName,
        workspaceKind,
        communityOption,
        provisionalCharterAdopted,
        charterPurpose,
        charterParticipants,
        charterAuthorityLimits,
        charterDecisionProcess,
        charterRoleTerms,
        charterAccessibility,
        charterEffectiveDate,
        charterReviewDate,
        charterVersion,
        charterAdopted,
        dossierAdoptionOption,
        dossierAdoptionScope,
        dossierAdoptionEffectiveDate,
        dossierAdoptionReviewDate,
        dossierAdoptionRecordedBy,
        reviewedDossierFacts,
    )

    fun roleAssignment(role: ParticipantRole) =
        stringPreferencesKey("$ROLE_ASSIGNMENT_PREFIX${role.id}")

    fun readingAssignment(slotId: String) =
        stringPreferencesKey("$READING_ASSIGNMENT_PREFIX$slotId")

    fun readingPlanPortion(slotId: String) =
        stringPreferencesKey("$READING_PLAN_PORTION_PREFIX$slotId")

    fun readingPlanLocator(slotId: String) =
        stringPreferencesKey("$READING_PLAN_LOCATOR_PREFIX$slotId")

    fun readingPlanRange(slotId: String) =
        stringPreferencesKey("$READING_PLAN_RANGE_PREFIX$slotId")

    fun readingPlanAssignee(slotId: String) =
        stringPreferencesKey("$READING_PLAN_ASSIGNEE_PREFIX$slotId")

    fun readingPlanBackup(slotId: String) =
        stringPreferencesKey("$READING_PLAN_BACKUP_PREFIX$slotId")

    fun readingPlanPreparation(slotId: String) =
        stringPreferencesKey("$READING_PLAN_PREPARATION_PREFIX$slotId")

    fun readingPlanManualOverride(slotId: String) =
        booleanPreferencesKey("$READING_PLAN_MANUAL_OVERRIDE_PREFIX$slotId")

    fun readingPlanOverrideReason(slotId: String) =
        stringPreferencesKey("$READING_PLAN_OVERRIDE_REASON_PREFIX$slotId")

    fun readingPlanSlotId(keyName: String): String? = readingPlanPrefixes
        .firstOrNull(keyName::startsWith)
        ?.let(keyName::removePrefix)
        ?.takeIf(String::isNotBlank)

    fun isDynamicOperationalKey(keyName: String): Boolean =
        dynamicOperationalPrefixes.any(keyName::startsWith)
}

internal const val EXPERIENCE_STATE_FILE_NAME = "experience_state"
internal const val MAX_USER_LABEL_LENGTH = 80
internal const val MAX_USER_LONG_TEXT_LENGTH = 600

internal fun String.normalizedExperienceLabel(): String = trim().take(MAX_USER_LABEL_LENGTH)

internal fun String.normalizedExperienceLongText(): String = trim().take(MAX_USER_LONG_TEXT_LENGTH)
