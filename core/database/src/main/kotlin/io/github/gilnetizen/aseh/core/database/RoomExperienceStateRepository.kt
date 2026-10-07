package io.github.gilnetizen.aseh.core.database

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
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
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Hybrid repository required by ADR-0003.
 *
 * Non-sensitive operational records live in Room. Small current-view and output preferences stay
 * in DataStore. Every read and write waits for the one-time legacy import, so observers never see
 * a mixture of pre-import preferences and an empty operational database.
 */
internal class RoomExperienceStateRepository(
    private val dao: ExperienceStateDao,
    private val preferenceStore: DataStore<Preferences>,
) : ExperienceStateRepository {
    private val operationMutex = Mutex()
    private var initialized = false

    override val state: Flow<ExperienceState> = flow {
        ensureInitialized()
        emitAll(
            combine(
                preferenceStore.safeExperiencePreferences()
                    .map(Preferences::toExperiencePreferenceState),
                dao.observeSnapshot(),
            ) { preferenceState, operationalSnapshot ->
                preferenceState.withOperationalState(
                    operationalSnapshot?.toExperienceState() ?: ExperienceState(),
                )
            },
        )
    }

    override suspend fun activateServiceInstance(serviceDate: LocalDate) = withInitialized {
        dao.activateServiceInstance(serviceDate.toString())
    }

    override suspend fun setPracticeStepCompleted(stepId: String, completed: Boolean) {
        setActiveServiceMarker(ExperienceRecordTypes.COMPLETED_PRACTICE_STEP, stepId, completed)
    }

    override suspend fun setPracticeStepCompleted(
        serviceDate: LocalDate,
        stepId: String,
        completed: Boolean,
    ) {
        setServiceMarker(serviceDate, ExperienceRecordTypes.COMPLETED_PRACTICE_STEP, stepId, completed)
    }

    override suspend fun setPracticeCardSaved(cardId: String, saved: Boolean) {
        setMarker(ExperienceRecordTypes.SAVED_PRACTICE_CARD, cardId, saved)
    }

    override suspend fun setPreflightStepCompleted(stepId: String, completed: Boolean) {
        setActiveServiceMarker(ExperienceRecordTypes.COMPLETED_PREFLIGHT_STEP, stepId, completed)
    }

    override suspend fun setPreflightStepCompleted(
        serviceDate: LocalDate,
        stepId: String,
        completed: Boolean,
    ) {
        setServiceMarker(serviceDate, ExperienceRecordTypes.COMPLETED_PREFLIGHT_STEP, stepId, completed)
    }

    override suspend fun setServiceSegmentCompleted(segmentId: String, completed: Boolean) {
        setActiveServiceMarker(ExperienceRecordTypes.COMPLETED_SERVICE_SEGMENT, segmentId, completed)
    }

    override suspend fun setServiceSegmentCompleted(
        serviceDate: LocalDate,
        segmentId: String,
        completed: Boolean,
    ) {
        setServiceMarker(
            serviceDate,
            ExperienceRecordTypes.COMPLETED_SERVICE_SEGMENT,
            segmentId,
            completed,
        )
    }

    override suspend fun setSelectedRole(role: ParticipantRole) {
        withInitialized {
            preferenceStore.edit { values -> values[ExperiencePreferenceKeys.selectedRole] = role.id }
        }
    }

    override suspend fun setSelectedServicePlan(servicePlanId: String?) {
        withInitialized {
            preferenceStore.edit { values ->
                val normalized = servicePlanId?.trim()?.takeIf(String::isNotBlank)
                if (normalized == null) {
                    values.remove(ExperiencePreferenceKeys.selectedServicePlan)
                } else {
                    require(normalized.length <= MAX_SERVICE_PLAN_ID_LENGTH) {
                        "Service plan IDs must be $MAX_SERVICE_PLAN_ID_LENGTH characters or fewer"
                    }
                    values[ExperiencePreferenceKeys.selectedServicePlan] = normalized
                }
            }
        }
    }

    override suspend fun setSourceBookmarked(sourceId: String, bookmarked: Boolean) {
        setMarker(ExperienceRecordTypes.BOOKMARKED_SOURCE, sourceId, bookmarked)
    }

    override suspend fun setWorkspace(name: String, kind: WorkspaceKind) = withInitialized {
        dao.upsertProfile(
            currentProfile().copy(
                workspaceName = name.normalizedExperienceLabel(),
                workspaceKind = kind.id,
            ),
        )
    }

    override suspend fun setRoleAssignment(role: ParticipantRole, name: String) = withInitialized {
        requireActiveServiceInstance()
        persistRoleAssignment(role, name)
    }

    override suspend fun setRoleAssignment(
        serviceDate: LocalDate,
        role: ParticipantRole,
        name: String,
    ) = withServiceInstanceForWrite(serviceDate) {
        persistRoleAssignment(role, name)
    }

    override suspend fun setReadingAssignment(slotId: String, name: String) = withInitialized {
        requireActiveServiceInstance()
        require(slotId.isNotBlank()) { "Reading slot IDs must not be blank" }
        val normalized = name.normalizedExperienceLabel()
        dao.setReadingAssignmentAndPlan(slotId, normalized)
    }

    override suspend fun setReadingAssignment(
        serviceDate: LocalDate,
        slotId: String,
        name: String,
    ) = withServiceInstanceForWrite(serviceDate) {
        require(slotId.isNotBlank()) { "Reading slot IDs must not be blank" }
        val normalized = name.normalizedExperienceLabel()
        dao.setReadingAssignmentAndPlan(slotId, normalized)
    }

    override suspend fun setReadingPlan(slotId: String, plan: ReadingPlanEntry) = withInitialized {
        requireActiveServiceInstance()
        require(slotId.isNotBlank()) { "Reading slot IDs must not be blank" }
        persistReadingPlan(slotId, plan)
    }

    override suspend fun setReadingPlan(
        serviceDate: LocalDate,
        slotId: String,
        plan: ReadingPlanEntry,
    ) = withServiceInstanceForWrite(serviceDate) {
        require(slotId.isNotBlank()) { "Reading slot IDs must not be blank" }
        persistReadingPlan(slotId, plan)
    }

    override suspend fun setDeviceUseMode(mode: DeviceUseMode) {
        withInitialized {
            preferenceStore.edit { values -> values[ExperiencePreferenceKeys.deviceUseMode] = mode.id }
        }
    }

    override suspend fun setCalendarRegion(region: CalendarRegion) {
        withInitialized {
            preferenceStore.edit { values -> values[ExperiencePreferenceKeys.calendarRegion] = region.name }
        }
    }

    override suspend fun updateAccessibilityProfile(
        transform: (ServiceAccessibilityProfile) -> ServiceAccessibilityProfile,
    ) {
        withInitialized {
            preferenceStore.edit { values ->
                val profile = transform(values.toExperiencePreferenceState().accessibilityProfile)
                values[ExperiencePreferenceKeys.accessNeedsReviewed] = profile.participantNeedsReviewed
                values[ExperiencePreferenceKeys.useMovementAlternatives] =
                    profile.useMovementAlternatives
                values[ExperiencePreferenceKeys.useVisualVoiceCues] = profile.useVisualVoiceCues
                values[ExperiencePreferenceKeys.useLargeText] = profile.useLargeText
                values[ExperiencePreferenceKeys.useHighContrast] = profile.useHighContrast
                values[ExperiencePreferenceKeys.reduceMotion] = profile.reduceMotion
                values[ExperiencePreferenceKeys.keepScreenAwake] = profile.keepScreenAwake
                values[ExperiencePreferenceKeys.lowLightMode] = profile.lowLightMode
            }
        }
    }

    override suspend fun setCommunityOption(optionId: String?) = withInitialized {
        dao.upsertProfile(
            currentProfile().copy(
                selectedCommunityOptionId = optionId?.takeIf(String::isNotBlank),
            ),
        )
    }

    override suspend fun setProvisionalCharterAdopted(adopted: Boolean) = withInitialized {
        dao.upsertProfile(currentProfile().copy(charterAdopted = adopted))
    }

    override suspend fun setCommunityCharter(charter: CommunityCharter) = withInitialized {
        dao.upsertProfile(
            currentProfile().copy(
                charterPurpose = charter.purpose.normalizedExperienceLongText(),
                charterParticipants = charter.participants.normalizedExperienceLongText(),
                charterAuthorityLimits = charter.authorityLimits.normalizedExperienceLongText(),
                charterDecisionProcess = charter.decisionProcess.normalizedExperienceLongText(),
                charterRoleTerms = charter.roleTerms.normalizedExperienceLongText(),
                charterAccessibilityCommitment =
                    charter.accessibilityCommitment.normalizedExperienceLongText(),
                charterEffectiveDate = charter.effectiveDate.normalizedExperienceLabel(),
                charterReviewDate = charter.reviewDate.normalizedExperienceLabel(),
                charterVersion = charter.version.normalizedExperienceLabel(),
                charterAdopted = charter.adopted,
            ),
        )
    }

    override suspend fun setDisputedPracticeAdoption(adoption: CommunityAdoption) =
        withInitialized {
            dao.upsertProfile(
                currentProfile().copy(
                    practiceAdoptionOptionId = adoption.optionId?.normalizedExperienceLabel()
                        ?.takeIf(String::isNotBlank),
                    practiceAdoptionScope = adoption.scope.normalizedExperienceLongText(),
                    practiceAdoptionEffectiveDate =
                        adoption.effectiveDate.normalizedExperienceLabel(),
                    practiceAdoptionReviewDate = adoption.reviewDate.normalizedExperienceLabel(),
                    practiceAdoptionRecordedBy = adoption.recordedBy.normalizedExperienceLabel(),
                ),
            )
        }

    override suspend fun setDossierFactReviewed(factId: String, reviewed: Boolean) {
        setMarker(ExperienceRecordTypes.REVIEWED_DOSSIER_FACT, factId, reviewed)
    }

    override suspend fun resetRehearsalProgress() = withInitialized {
        dao.deleteRecordMarkers(ExperienceRecordTypes.rehearsalProgress)
    }

    override suspend fun clearAllExperienceData() {
        withInitialized {
            dao.clearExperienceData()
            preferenceStore.edit { values -> values.clear() }
        }
    }

    private suspend fun persistRoleAssignment(role: ParticipantRole, name: String) {
        val normalized = name.normalizedExperienceLabel()
        if (normalized.isBlank()) {
            dao.deleteRoleAssignment(role.id)
        } else {
            dao.upsertRoleAssignment(
                ExperienceRoleAssignmentEntity(
                    roleId = role.id,
                    assigneeName = normalized,
                ),
            )
        }
    }

    private suspend fun persistReadingPlan(slotId: String, plan: ReadingPlanEntry) {
        val normalized = plan.normalizedForPersistence()
        val planEntity = normalized.takeUnless { it == ReadingPlanEntry() }?.toEntity(slotId)
        val assignmentEntity = normalized.assignee.takeIf(String::isNotBlank)?.let { assignee ->
            ExperienceReadingAssignmentEntity(slotId = slotId, assigneeName = assignee)
        }
        dao.setReadingPlanAndAssignment(slotId, planEntity, assignmentEntity)
    }

    private suspend fun setActiveServiceMarker(
        recordType: String,
        recordId: String,
        included: Boolean,
    ) = withInitialized {
        requireActiveServiceInstance()
        persistMarker(recordType, recordId, included)
    }

    private suspend fun setServiceMarker(
        serviceDate: LocalDate,
        recordType: String,
        recordId: String,
        included: Boolean,
    ) = withServiceInstanceForWrite(serviceDate) {
        persistMarker(recordType, recordId, included)
    }

    private suspend fun setMarker(recordType: String, recordId: String, included: Boolean) =
        withInitialized {
            persistMarker(recordType, recordId, included)
        }

    private suspend fun persistMarker(recordType: String, recordId: String, included: Boolean) {
        require(recordId.isNotBlank()) { "Persistent record IDs must not be blank" }
        if (included) {
            dao.insertRecordMarker(
                ExperienceRecordMarkerEntity(
                    recordType = recordType,
                    recordId = recordId,
                ),
            )
        } else {
            dao.deleteRecordMarker(recordType, recordId)
        }
    }

    private suspend fun requireActiveServiceInstance() {
        check(currentProfile().serviceInstanceDate != null) {
            "A dated service instance must be active before writing weekly state"
        }
    }

    private suspend fun currentProfile(): ExperienceProfileEntity =
        checkNotNull(dao.profile()) { "Experience profile must exist after initialization" }

    private suspend fun ensureInitialized() {
        operationMutex.withLock { ensureInitializedLocked() }
    }

    private suspend fun <T> withInitialized(block: suspend () -> T): T =
        operationMutex.withLock {
            ensureInitializedLocked()
            block()
        }

    private suspend fun withServiceInstanceForWrite(
        serviceDate: LocalDate,
        block: suspend () -> Unit,
    ) {
        operationMutex.withLock {
            ensureInitializedLocked()
            if (dao.prepareServiceInstanceForWrite(serviceDate.toString())) block()
        }
    }

    private suspend fun ensureInitializedLocked() {
        if (initialized) return

        val existing = dao.snapshot()
        if (existing?.profile?.legacyDataStoreMigrated != true) {
            val legacyState = preferenceStore.data.first()
                .toExperienceStateFromLegacyPreferences()
            dao.replaceSnapshot(legacyState.toOperationalSnapshot())
        }

        // If the process stopped after the Room transaction but before this edit, the Room marker
        // prevents a second import and this idempotent cleanup finishes on the next start.
        preferenceStore.removeLegacyOperationalExperienceKeys()
        initialized = true
    }
}

private fun ExperienceState.withOperationalState(operational: ExperienceState): ExperienceState =
    copy(
        serviceInstanceDate = operational.serviceInstanceDate,
        completedPracticeStepIds = operational.completedPracticeStepIds,
        savedPracticeCardIds = operational.savedPracticeCardIds,
        completedPreflightStepIds = operational.completedPreflightStepIds,
        completedServiceSegmentIds = operational.completedServiceSegmentIds,
        bookmarkedSourceIds = operational.bookmarkedSourceIds,
        workspaceName = operational.workspaceName,
        workspaceKind = operational.workspaceKind,
        roleAssignments = operational.roleAssignments,
        readingAssignments = operational.readingAssignments,
        readingPlans = operational.readingPlans,
        selectedCommunityOptionId = operational.selectedCommunityOptionId,
        provisionalCharterAdopted = operational.provisionalCharterAdopted,
        communityCharter = operational.communityCharter,
        disputedPracticeAdoption = operational.disputedPracticeAdoption,
        reviewedDossierFactIds = operational.reviewedDossierFactIds,
    )

private const val MAX_SERVICE_PLAN_ID_LENGTH = 256

private fun ExperienceOperationalSnapshotEntity.toExperienceState(): ExperienceState {
    val markerIds = recordMarkers.groupBy(
        keySelector = ExperienceRecordMarkerEntity::recordType,
        valueTransform = ExperienceRecordMarkerEntity::recordId,
    )
    val charter = CommunityCharter(
        purpose = profile.charterPurpose,
        participants = profile.charterParticipants,
        authorityLimits = profile.charterAuthorityLimits,
        decisionProcess = profile.charterDecisionProcess,
        roleTerms = profile.charterRoleTerms,
        accessibilityCommitment = profile.charterAccessibilityCommitment,
        effectiveDate = profile.charterEffectiveDate,
        reviewDate = profile.charterReviewDate,
        version = profile.charterVersion.ifBlank { "Draft 1" },
        adopted = profile.charterAdopted,
    )
    val serviceInstanceDate = profile.serviceInstanceDate?.let { storedDate ->
        runCatching { LocalDate.parse(storedDate) }.getOrNull()
    }
    val hasTrustedServiceInstance = serviceInstanceDate != null
    return ExperienceState(
        serviceInstanceDate = serviceInstanceDate,
        completedPracticeStepIds = if (hasTrustedServiceInstance) {
            markerIds.ids(ExperienceRecordTypes.COMPLETED_PRACTICE_STEP)
        } else {
            emptySet()
        },
        savedPracticeCardIds = markerIds.ids(ExperienceRecordTypes.SAVED_PRACTICE_CARD),
        completedPreflightStepIds = if (hasTrustedServiceInstance) {
            markerIds.ids(ExperienceRecordTypes.COMPLETED_PREFLIGHT_STEP)
        } else {
            emptySet()
        },
        completedServiceSegmentIds = if (hasTrustedServiceInstance) {
            markerIds.ids(ExperienceRecordTypes.COMPLETED_SERVICE_SEGMENT)
        } else {
            emptySet()
        },
        bookmarkedSourceIds = markerIds.ids(ExperienceRecordTypes.BOOKMARKED_SOURCE),
        workspaceName = profile.workspaceName,
        workspaceKind = WorkspaceKind.entries.firstOrNull { it.id == profile.workspaceKind }
            ?: WorkspaceKind.QAHAL,
        roleAssignments = if (hasTrustedServiceInstance) {
            roleAssignments.mapNotNull { assignment ->
                ParticipantRole.entries.firstOrNull { it.id == assignment.roleId }
                    ?.let { role -> role to assignment.assigneeName }
            }.toMap()
        } else {
            emptyMap()
        },
        readingAssignments = if (hasTrustedServiceInstance) {
            readingAssignments.associate { assignment ->
                assignment.slotId to assignment.assigneeName
            }
        } else {
            emptyMap()
        },
        readingPlans = if (hasTrustedServiceInstance) {
            readingPlans.associate { plan -> plan.slotId to plan.toReadingPlanEntry() }
        } else {
            emptyMap()
        },
        selectedCommunityOptionId = profile.selectedCommunityOptionId,
        provisionalCharterAdopted = profile.charterAdopted,
        communityCharter = charter,
        disputedPracticeAdoption = CommunityAdoption(
            optionId = profile.practiceAdoptionOptionId,
            scope = profile.practiceAdoptionScope,
            effectiveDate = profile.practiceAdoptionEffectiveDate,
            reviewDate = profile.practiceAdoptionReviewDate,
            recordedBy = profile.practiceAdoptionRecordedBy,
        ),
        reviewedDossierFactIds = markerIds.ids(ExperienceRecordTypes.REVIEWED_DOSSIER_FACT),
    )
}

private fun ExperienceState.toOperationalSnapshot(): ExperienceOperationalSnapshotEntity =
    ExperienceOperationalSnapshotEntity(
        profile = ExperienceProfileEntity(
            workspaceName = workspaceName,
            workspaceKind = workspaceKind.id,
            serviceInstanceDate = serviceInstanceDate?.toString(),
            selectedCommunityOptionId = selectedCommunityOptionId,
            charterPurpose = communityCharter.purpose,
            charterParticipants = communityCharter.participants,
            charterAuthorityLimits = communityCharter.authorityLimits,
            charterDecisionProcess = communityCharter.decisionProcess,
            charterRoleTerms = communityCharter.roleTerms,
            charterAccessibilityCommitment = communityCharter.accessibilityCommitment,
            charterEffectiveDate = communityCharter.effectiveDate,
            charterReviewDate = communityCharter.reviewDate,
            charterVersion = communityCharter.version,
            charterAdopted = communityCharter.adopted || provisionalCharterAdopted,
            practiceAdoptionOptionId = disputedPracticeAdoption.optionId,
            practiceAdoptionScope = disputedPracticeAdoption.scope,
            practiceAdoptionEffectiveDate = disputedPracticeAdoption.effectiveDate,
            practiceAdoptionReviewDate = disputedPracticeAdoption.reviewDate,
            practiceAdoptionRecordedBy = disputedPracticeAdoption.recordedBy,
            legacyDataStoreMigrated = true,
        ),
        roleAssignments = if (serviceInstanceDate == null) {
            emptyList()
        } else {
            roleAssignments.map { (role, assignee) ->
                ExperienceRoleAssignmentEntity(roleId = role.id, assigneeName = assignee)
            }
        },
        readingAssignments = if (serviceInstanceDate == null) {
            emptyList()
        } else {
            readingAssignments.map { (slotId, assignee) ->
                ExperienceReadingAssignmentEntity(slotId = slotId, assigneeName = assignee)
            }
        },
        readingPlans = if (serviceInstanceDate == null) {
            emptyList()
        } else {
            readingPlans.map { (slotId, plan) -> plan.toEntity(slotId) }
        },
        recordMarkers = buildList {
            addMarkers(ExperienceRecordTypes.SAVED_PRACTICE_CARD, savedPracticeCardIds)
            addMarkers(ExperienceRecordTypes.BOOKMARKED_SOURCE, bookmarkedSourceIds)
            addMarkers(ExperienceRecordTypes.REVIEWED_DOSSIER_FACT, reviewedDossierFactIds)
            if (serviceInstanceDate != null) {
                addMarkers(ExperienceRecordTypes.COMPLETED_PRACTICE_STEP, completedPracticeStepIds)
                addMarkers(ExperienceRecordTypes.COMPLETED_PREFLIGHT_STEP, completedPreflightStepIds)
                addMarkers(ExperienceRecordTypes.COMPLETED_SERVICE_SEGMENT, completedServiceSegmentIds)
            }
        },
    )

private fun MutableList<ExperienceRecordMarkerEntity>.addMarkers(
    recordType: String,
    recordIds: Set<String>,
) {
    recordIds.forEach { recordId ->
        add(ExperienceRecordMarkerEntity(recordType = recordType, recordId = recordId))
    }
}

private fun Map<String, List<String>>.ids(recordType: String): Set<String> =
    get(recordType).orEmpty().toSet()

private fun ReadingPlanEntry.normalizedForPersistence(): ReadingPlanEntry {
    val sourceFields = if (manualOverride) {
        this
    } else {
        copy(
            portionTitle = "",
            locator = "",
            passageRange = "",
            overrideReason = "",
        )
    }
    return sourceFields.copy(
        portionTitle = sourceFields.portionTitle.normalizedExperienceLabel(),
        locator = sourceFields.locator.normalizedExperienceLabel(),
        passageRange = sourceFields.passageRange.normalizedExperienceLabel(),
        assignee = assignee.normalizedExperienceLabel(),
        backupAssignee = backupAssignee.normalizedExperienceLabel(),
        overrideReason = sourceFields.overrideReason.normalizedExperienceLongText(),
    )
}

private fun ReadingPlanEntry.toEntity(slotId: String): ExperienceReadingPlanEntity =
    ExperienceReadingPlanEntity(
        slotId = slotId,
        portionTitle = portionTitle,
        locator = locator,
        passageRange = passageRange,
        assigneeName = assignee,
        backupAssigneeName = backupAssignee,
        preparationStatus = preparationStatus.name,
        manualOverride = manualOverride,
        overrideReason = overrideReason,
    )

private fun ExperienceReadingPlanEntity.toReadingPlanEntry(): ReadingPlanEntry = ReadingPlanEntry(
    portionTitle = portionTitle,
    locator = locator,
    passageRange = passageRange,
    assignee = assigneeName,
    backupAssignee = backupAssigneeName,
    preparationStatus = ReadingPreparationStatus.entries.firstOrNull { it.name == preparationStatus }
        ?: ReadingPreparationStatus.NOT_STARTED,
    manualOverride = manualOverride,
    overrideReason = overrideReason,
)
