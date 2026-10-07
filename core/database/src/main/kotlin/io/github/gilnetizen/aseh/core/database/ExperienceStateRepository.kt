package io.github.gilnetizen.aseh.core.database

import io.github.gilnetizen.aseh.core.model.CalendarRegion
import io.github.gilnetizen.aseh.core.model.CommunityAdoption
import io.github.gilnetizen.aseh.core.model.CommunityCharter
import io.github.gilnetizen.aseh.core.model.ExperienceState
import io.github.gilnetizen.aseh.core.model.DeviceUseMode
import io.github.gilnetizen.aseh.core.model.ParticipantRole
import io.github.gilnetizen.aseh.core.model.ReadingPlanEntry
import io.github.gilnetizen.aseh.core.model.ServiceInstanceKey
import io.github.gilnetizen.aseh.core.model.ServiceAccessibilityProfile
import io.github.gilnetizen.aseh.core.model.WorkspaceKind
import kotlinx.coroutines.flow.Flow

interface ExperienceStateRepository {
    val state: Flow<ExperienceState>

    /** Activates one scheduled occurrence while preserving records owned by other occurrences. */
    suspend fun activateServiceInstance(serviceInstance: ServiceInstanceKey) = Unit

    /** Clears only the active occurrence pointer; occurrence-owned records remain available. */
    suspend fun deactivateServiceInstance() = Unit

    suspend fun setPracticeStepCompleted(stepId: String, completed: Boolean)
    suspend fun setPracticeStepCompleted(
        serviceInstance: ServiceInstanceKey,
        stepId: String,
        completed: Boolean,
    ) {
        activateServiceInstance(serviceInstance)
        setPracticeStepCompleted(stepId, completed)
    }
    suspend fun setPracticeCardSaved(cardId: String, saved: Boolean)
    suspend fun setPreflightStepCompleted(stepId: String, completed: Boolean)
    suspend fun setPreflightStepCompleted(
        serviceInstance: ServiceInstanceKey,
        stepId: String,
        completed: Boolean,
    ) {
        activateServiceInstance(serviceInstance)
        setPreflightStepCompleted(stepId, completed)
    }
    suspend fun setServiceSegmentCompleted(segmentId: String, completed: Boolean)
    suspend fun setServiceSegmentCompleted(
        serviceInstance: ServiceInstanceKey,
        segmentId: String,
        completed: Boolean,
    ) {
        activateServiceInstance(serviceInstance)
        setServiceSegmentCompleted(segmentId, completed)
    }
    suspend fun setSelectedRole(role: ParticipantRole)
    suspend fun setSelectedServicePlan(servicePlanId: String?) = Unit
    suspend fun setSourceBookmarked(sourceId: String, bookmarked: Boolean)
    suspend fun setWorkspace(name: String, kind: WorkspaceKind)
    suspend fun setRoleAssignment(role: ParticipantRole, name: String)
    suspend fun setRoleAssignment(
        serviceInstance: ServiceInstanceKey,
        role: ParticipantRole,
        name: String,
    ) {
        activateServiceInstance(serviceInstance)
        setRoleAssignment(role, name)
    }
    suspend fun setReadingAssignment(slotId: String, name: String)
    suspend fun setReadingAssignment(
        serviceInstance: ServiceInstanceKey,
        slotId: String,
        name: String,
    ) {
        activateServiceInstance(serviceInstance)
        setReadingAssignment(slotId, name)
    }
    suspend fun setReadingPlan(slotId: String, plan: ReadingPlanEntry)
    suspend fun setReadingPlan(
        serviceInstance: ServiceInstanceKey,
        slotId: String,
        plan: ReadingPlanEntry,
    ) {
        activateServiceInstance(serviceInstance)
        setReadingPlan(slotId, plan)
    }
    suspend fun setDeviceUseMode(mode: DeviceUseMode)
    suspend fun setCalendarRegion(region: CalendarRegion)
    suspend fun updateAccessibilityProfile(
        transform: (ServiceAccessibilityProfile) -> ServiceAccessibilityProfile,
    )
    suspend fun setCommunityOption(optionId: String?)
    suspend fun setProvisionalCharterAdopted(adopted: Boolean)
    suspend fun setCommunityCharter(charter: CommunityCharter)
    suspend fun setDisputedPracticeAdoption(adoption: CommunityAdoption)
    suspend fun setDossierFactReviewed(factId: String, reviewed: Boolean)
    suspend fun resetRehearsalProgress()
    suspend fun clearAllExperienceData()
}
