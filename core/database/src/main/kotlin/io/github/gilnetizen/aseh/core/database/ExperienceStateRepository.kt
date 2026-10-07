package io.github.gilnetizen.aseh.core.database

import io.github.gilnetizen.aseh.core.model.CalendarRegion
import io.github.gilnetizen.aseh.core.model.CommunityAdoption
import io.github.gilnetizen.aseh.core.model.CommunityCharter
import io.github.gilnetizen.aseh.core.model.ExperienceState
import io.github.gilnetizen.aseh.core.model.DeviceUseMode
import io.github.gilnetizen.aseh.core.model.ParticipantRole
import io.github.gilnetizen.aseh.core.model.ReadingPlanEntry
import io.github.gilnetizen.aseh.core.model.ServiceAccessibilityProfile
import io.github.gilnetizen.aseh.core.model.WorkspaceKind
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

interface ExperienceStateRepository {
    val state: Flow<ExperienceState>

    /** Activates one explicit weekly service instance and clears prior week-scoped records. */
    suspend fun activateServiceInstance(serviceDate: LocalDate) = Unit

    suspend fun setPracticeStepCompleted(stepId: String, completed: Boolean)
    suspend fun setPracticeStepCompleted(
        serviceDate: LocalDate,
        stepId: String,
        completed: Boolean,
    ) {
        activateServiceInstance(serviceDate)
        setPracticeStepCompleted(stepId, completed)
    }
    suspend fun setPracticeCardSaved(cardId: String, saved: Boolean)
    suspend fun setPreflightStepCompleted(stepId: String, completed: Boolean)
    suspend fun setPreflightStepCompleted(
        serviceDate: LocalDate,
        stepId: String,
        completed: Boolean,
    ) {
        activateServiceInstance(serviceDate)
        setPreflightStepCompleted(stepId, completed)
    }
    suspend fun setServiceSegmentCompleted(segmentId: String, completed: Boolean)
    suspend fun setServiceSegmentCompleted(
        serviceDate: LocalDate,
        segmentId: String,
        completed: Boolean,
    ) {
        activateServiceInstance(serviceDate)
        setServiceSegmentCompleted(segmentId, completed)
    }
    suspend fun setSelectedRole(role: ParticipantRole)
    suspend fun setSelectedServicePlan(servicePlanId: String?) = Unit
    suspend fun setSourceBookmarked(sourceId: String, bookmarked: Boolean)
    suspend fun setWorkspace(name: String, kind: WorkspaceKind)
    suspend fun setRoleAssignment(role: ParticipantRole, name: String)
    suspend fun setRoleAssignment(
        serviceDate: LocalDate,
        role: ParticipantRole,
        name: String,
    ) {
        activateServiceInstance(serviceDate)
        setRoleAssignment(role, name)
    }
    suspend fun setReadingAssignment(slotId: String, name: String)
    suspend fun setReadingAssignment(serviceDate: LocalDate, slotId: String, name: String) {
        activateServiceInstance(serviceDate)
        setReadingAssignment(slotId, name)
    }
    suspend fun setReadingPlan(slotId: String, plan: ReadingPlanEntry)
    suspend fun setReadingPlan(
        serviceDate: LocalDate,
        slotId: String,
        plan: ReadingPlanEntry,
    ) {
        activateServiceInstance(serviceDate)
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
