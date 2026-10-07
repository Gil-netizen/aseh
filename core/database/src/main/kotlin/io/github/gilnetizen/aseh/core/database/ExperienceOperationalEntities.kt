package io.github.gilnetizen.aseh.core.database

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import androidx.room.Upsert
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "experience_profile")
internal data class ExperienceProfileEntity(
    @PrimaryKey
    @ColumnInfo(name = "profile_id")
    val profileId: Int = EXPERIENCE_PROFILE_ID,
    @ColumnInfo(name = "workspace_name") val workspaceName: String = "",
    @ColumnInfo(name = "workspace_kind") val workspaceKind: String = DEFAULT_WORKSPACE_KIND,
    @ColumnInfo(name = "service_instance_id") val serviceInstanceId: String? = null,
    @ColumnInfo(name = "service_instance_date") val serviceInstanceDate: String? = null,
    @ColumnInfo(name = "selected_community_option_id")
    val selectedCommunityOptionId: String? = null,
    @ColumnInfo(name = "charter_purpose") val charterPurpose: String = "",
    @ColumnInfo(name = "charter_participants") val charterParticipants: String = "",
    @ColumnInfo(name = "charter_authority_limits") val charterAuthorityLimits: String = "",
    @ColumnInfo(name = "charter_decision_process") val charterDecisionProcess: String = "",
    @ColumnInfo(name = "charter_role_terms") val charterRoleTerms: String = "",
    @ColumnInfo(name = "charter_accessibility_commitment")
    val charterAccessibilityCommitment: String = "",
    @ColumnInfo(name = "charter_effective_date") val charterEffectiveDate: String = "",
    @ColumnInfo(name = "charter_review_date") val charterReviewDate: String = "",
    @ColumnInfo(name = "charter_version") val charterVersion: String = DEFAULT_CHARTER_VERSION,
    @ColumnInfo(name = "charter_adopted") val charterAdopted: Boolean = false,
    @ColumnInfo(name = "practice_adoption_option_id") val practiceAdoptionOptionId: String? = null,
    @ColumnInfo(name = "practice_adoption_scope") val practiceAdoptionScope: String = "",
    @ColumnInfo(name = "practice_adoption_effective_date")
    val practiceAdoptionEffectiveDate: String = "",
    @ColumnInfo(name = "practice_adoption_review_date") val practiceAdoptionReviewDate: String = "",
    @ColumnInfo(name = "practice_adoption_recorded_by") val practiceAdoptionRecordedBy: String = "",
    @ColumnInfo(name = "legacy_datastore_migrated") val legacyDataStoreMigrated: Boolean = false,
)

@Entity(
    tableName = "experience_service_instance",
    primaryKeys = ["profile_id", "service_instance_id"],
    foreignKeys = [
        ForeignKey(
            entity = ExperienceProfileEntity::class,
            parentColumns = ["profile_id"],
            childColumns = ["profile_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
internal data class ExperienceServiceInstanceEntity(
    @ColumnInfo(name = "profile_id") val profileId: Int = EXPERIENCE_PROFILE_ID,
    @ColumnInfo(name = "service_instance_id") val serviceInstanceId: String,
    @ColumnInfo(name = "service_instance_date") val serviceInstanceDate: String,
)

@Entity(
    tableName = "experience_role_assignment",
    primaryKeys = ["profile_id", "service_instance_id", "role_id"],
    foreignKeys = [
        ForeignKey(
            entity = ExperienceServiceInstanceEntity::class,
            parentColumns = ["profile_id", "service_instance_id"],
            childColumns = ["profile_id", "service_instance_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
internal data class ExperienceRoleAssignmentEntity(
    @ColumnInfo(name = "profile_id") val profileId: Int = EXPERIENCE_PROFILE_ID,
    @ColumnInfo(name = "service_instance_id") val serviceInstanceId: String,
    @ColumnInfo(name = "role_id") val roleId: String,
    @ColumnInfo(name = "assignee_name") val assigneeName: String,
)

@Entity(
    tableName = "experience_reading_assignment",
    primaryKeys = ["profile_id", "service_instance_id", "slot_id"],
    foreignKeys = [
        ForeignKey(
            entity = ExperienceServiceInstanceEntity::class,
            parentColumns = ["profile_id", "service_instance_id"],
            childColumns = ["profile_id", "service_instance_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
internal data class ExperienceReadingAssignmentEntity(
    @ColumnInfo(name = "profile_id") val profileId: Int = EXPERIENCE_PROFILE_ID,
    @ColumnInfo(name = "service_instance_id") val serviceInstanceId: String,
    @ColumnInfo(name = "slot_id") val slotId: String,
    @ColumnInfo(name = "assignee_name") val assigneeName: String,
)

@Entity(
    tableName = "experience_reading_plan",
    primaryKeys = ["profile_id", "service_instance_id", "slot_id"],
    foreignKeys = [
        ForeignKey(
            entity = ExperienceServiceInstanceEntity::class,
            parentColumns = ["profile_id", "service_instance_id"],
            childColumns = ["profile_id", "service_instance_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
internal data class ExperienceReadingPlanEntity(
    @ColumnInfo(name = "profile_id") val profileId: Int = EXPERIENCE_PROFILE_ID,
    @ColumnInfo(name = "service_instance_id") val serviceInstanceId: String,
    @ColumnInfo(name = "slot_id") val slotId: String,
    @ColumnInfo(name = "portion_title") val portionTitle: String = "",
    @ColumnInfo(name = "locator") val locator: String = "",
    @ColumnInfo(name = "passage_range") val passageRange: String = "",
    @ColumnInfo(name = "assignee_name") val assigneeName: String = "",
    @ColumnInfo(name = "backup_assignee_name") val backupAssigneeName: String = "",
    @ColumnInfo(name = "preparation_status") val preparationStatus: String = DEFAULT_PREPARATION_STATUS,
    @ColumnInfo(name = "manual_override") val manualOverride: Boolean = false,
    @ColumnInfo(name = "override_reason") val overrideReason: String = "",
)

/** Durable records shared across service occurrences. */
@Entity(
    tableName = "experience_record_marker",
    primaryKeys = ["profile_id", "record_type", "record_id"],
    foreignKeys = [
        ForeignKey(
            entity = ExperienceProfileEntity::class,
            parentColumns = ["profile_id"],
            childColumns = ["profile_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
internal data class ExperienceRecordMarkerEntity(
    @ColumnInfo(name = "profile_id") val profileId: Int = EXPERIENCE_PROFILE_ID,
    @ColumnInfo(name = "record_type") val recordType: String,
    @ColumnInfo(name = "record_id") val recordId: String,
)

@Entity(
    tableName = "experience_service_record_marker",
    primaryKeys = ["profile_id", "service_instance_id", "record_type", "record_id"],
    foreignKeys = [
        ForeignKey(
            entity = ExperienceServiceInstanceEntity::class,
            parentColumns = ["profile_id", "service_instance_id"],
            childColumns = ["profile_id", "service_instance_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
internal data class ExperienceServiceRecordMarkerEntity(
    @ColumnInfo(name = "profile_id") val profileId: Int = EXPERIENCE_PROFILE_ID,
    @ColumnInfo(name = "service_instance_id") val serviceInstanceId: String,
    @ColumnInfo(name = "record_type") val recordType: String,
    @ColumnInfo(name = "record_id") val recordId: String,
)

internal data class ExperienceOperationalSnapshotEntity(
    @Embedded val profile: ExperienceProfileEntity,
    @Relation(parentColumn = "profile_id", entityColumn = "profile_id")
    val serviceInstances: List<ExperienceServiceInstanceEntity>,
    @Relation(parentColumn = "profile_id", entityColumn = "profile_id")
    val roleAssignments: List<ExperienceRoleAssignmentEntity>,
    @Relation(parentColumn = "profile_id", entityColumn = "profile_id")
    val readingAssignments: List<ExperienceReadingAssignmentEntity>,
    @Relation(parentColumn = "profile_id", entityColumn = "profile_id")
    val readingPlans: List<ExperienceReadingPlanEntity>,
    @Relation(parentColumn = "profile_id", entityColumn = "profile_id")
    val recordMarkers: List<ExperienceRecordMarkerEntity>,
    @Relation(parentColumn = "profile_id", entityColumn = "profile_id")
    val serviceRecordMarkers: List<ExperienceServiceRecordMarkerEntity>,
)

@Dao
internal abstract class ExperienceStateDao {
    @Transaction
    @Query("SELECT * FROM experience_profile WHERE profile_id = :profileId LIMIT 1")
    abstract fun observeSnapshot(
        profileId: Int = EXPERIENCE_PROFILE_ID,
    ): Flow<ExperienceOperationalSnapshotEntity?>

    @Transaction
    @Query("SELECT * FROM experience_profile WHERE profile_id = :profileId LIMIT 1")
    abstract suspend fun snapshot(
        profileId: Int = EXPERIENCE_PROFILE_ID,
    ): ExperienceOperationalSnapshotEntity?

    @Query("SELECT * FROM experience_profile WHERE profile_id = :profileId LIMIT 1")
    abstract suspend fun profile(profileId: Int = EXPERIENCE_PROFILE_ID): ExperienceProfileEntity?

    @Query(
        "UPDATE experience_profile SET service_instance_id = NULL, service_instance_date = NULL " +
            "WHERE profile_id = :profileId",
    )
    abstract suspend fun deactivateServiceInstance(profileId: Int = EXPERIENCE_PROFILE_ID)

    @Query(
        "SELECT service_instance_date FROM experience_service_instance " +
            "WHERE profile_id = :profileId AND service_instance_id = :serviceInstanceId LIMIT 1",
    )
    protected abstract suspend fun serviceInstanceDate(
        serviceInstanceId: String,
        profileId: Int = EXPERIENCE_PROFILE_ID,
    ): String?

    @Query(
        "SELECT * FROM experience_reading_plan WHERE profile_id = :profileId " +
            "AND service_instance_id = :serviceInstanceId AND slot_id = :slotId LIMIT 1",
    )
    abstract suspend fun readingPlan(
        serviceInstanceId: String,
        slotId: String,
        profileId: Int = EXPERIENCE_PROFILE_ID,
    ): ExperienceReadingPlanEntity?

    @Upsert
    abstract suspend fun upsertProfile(entity: ExperienceProfileEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    protected abstract suspend fun insertServiceInstance(entity: ExperienceServiceInstanceEntity): Long

    @Upsert
    abstract suspend fun upsertRoleAssignment(entity: ExperienceRoleAssignmentEntity)

    @Upsert
    abstract suspend fun upsertReadingAssignment(entity: ExperienceReadingAssignmentEntity)

    @Upsert
    abstract suspend fun upsertReadingPlan(entity: ExperienceReadingPlanEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insertRecordMarker(entity: ExperienceRecordMarkerEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insertServiceRecordMarker(entity: ExperienceServiceRecordMarkerEntity)

    @Query(
        "DELETE FROM experience_role_assignment WHERE profile_id = :profileId " +
            "AND service_instance_id = :serviceInstanceId AND role_id = :roleId",
    )
    abstract suspend fun deleteRoleAssignment(
        serviceInstanceId: String,
        roleId: String,
        profileId: Int = EXPERIENCE_PROFILE_ID,
    )

    @Query(
        "DELETE FROM experience_reading_assignment WHERE profile_id = :profileId " +
            "AND service_instance_id = :serviceInstanceId AND slot_id = :slotId",
    )
    abstract suspend fun deleteReadingAssignment(
        serviceInstanceId: String,
        slotId: String,
        profileId: Int = EXPERIENCE_PROFILE_ID,
    )

    @Query(
        "DELETE FROM experience_reading_plan WHERE profile_id = :profileId " +
            "AND service_instance_id = :serviceInstanceId AND slot_id = :slotId",
    )
    abstract suspend fun deleteReadingPlan(
        serviceInstanceId: String,
        slotId: String,
        profileId: Int = EXPERIENCE_PROFILE_ID,
    )

    @Query(
        "DELETE FROM experience_record_marker WHERE profile_id = :profileId " +
            "AND record_type = :recordType AND record_id = :recordId",
    )
    abstract suspend fun deleteRecordMarker(
        recordType: String,
        recordId: String,
        profileId: Int = EXPERIENCE_PROFILE_ID,
    )

    @Query(
        "DELETE FROM experience_service_record_marker WHERE profile_id = :profileId " +
            "AND service_instance_id = :serviceInstanceId AND record_type = :recordType " +
            "AND record_id = :recordId",
    )
    abstract suspend fun deleteServiceRecordMarker(
        serviceInstanceId: String,
        recordType: String,
        recordId: String,
        profileId: Int = EXPERIENCE_PROFILE_ID,
    )

    @Query(
        "DELETE FROM experience_service_record_marker WHERE profile_id = :profileId " +
            "AND service_instance_id = :serviceInstanceId AND record_type IN (:recordTypes)",
    )
    abstract suspend fun deleteServiceRecordMarkers(
        serviceInstanceId: String,
        recordTypes: List<String>,
        profileId: Int = EXPERIENCE_PROFILE_ID,
    )

    @Query("DELETE FROM experience_role_assignment WHERE profile_id = :profileId")
    protected abstract suspend fun deleteAllRoleAssignments(profileId: Int = EXPERIENCE_PROFILE_ID)

    @Query("DELETE FROM experience_reading_plan WHERE profile_id = :profileId")
    protected abstract suspend fun deleteAllReadingPlans(profileId: Int = EXPERIENCE_PROFILE_ID)

    @Query("DELETE FROM experience_reading_assignment WHERE profile_id = :profileId")
    protected abstract suspend fun deleteAllReadingAssignments(profileId: Int = EXPERIENCE_PROFILE_ID)

    @Query("DELETE FROM experience_service_record_marker WHERE profile_id = :profileId")
    protected abstract suspend fun deleteAllServiceRecordMarkers(profileId: Int = EXPERIENCE_PROFILE_ID)

    @Query("DELETE FROM experience_service_instance WHERE profile_id = :profileId")
    protected abstract suspend fun deleteAllServiceInstances(profileId: Int = EXPERIENCE_PROFILE_ID)

    @Query("DELETE FROM experience_record_marker WHERE profile_id = :profileId")
    protected abstract suspend fun deleteAllRecordMarkers(profileId: Int = EXPERIENCE_PROFILE_ID)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insertServiceInstances(
        entities: List<ExperienceServiceInstanceEntity>,
    )

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insertRoleAssignments(
        entities: List<ExperienceRoleAssignmentEntity>,
    )

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insertReadingAssignments(
        entities: List<ExperienceReadingAssignmentEntity>,
    )

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insertReadingPlans(
        entities: List<ExperienceReadingPlanEntity>,
    )

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insertRecordMarkers(
        entities: List<ExperienceRecordMarkerEntity>,
    )

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insertServiceRecordMarkers(
        entities: List<ExperienceServiceRecordMarkerEntity>,
    )

    @Transaction
    open suspend fun activateServiceInstance(serviceInstanceId: String, serviceDate: String) {
        require(serviceInstanceId.isNotBlank()) { "A service instance requires an occurrence ID" }
        require(LocalDate.parse(serviceDate).toString() == serviceDate) {
            "A service instance requires an ISO date"
        }
        val current = profile() ?: error("Experience profile must exist before service activation")
        val storedDate = serviceInstanceDate(serviceInstanceId)
        require(storedDate == null || storedDate == serviceDate) {
            "A service occurrence ID cannot be reused for another civil date"
        }
        if (storedDate == null) {
            insertServiceInstance(
                ExperienceServiceInstanceEntity(
                    serviceInstanceId = serviceInstanceId,
                    serviceInstanceDate = serviceDate,
                ),
            )
        }
        if (
            current.serviceInstanceId == serviceInstanceId &&
            current.serviceInstanceDate == serviceDate
        ) {
            return
        }
        upsertProfile(
            current.copy(
                serviceInstanceId = serviceInstanceId,
                serviceInstanceDate = serviceDate,
            ),
        )
    }

    /** Rejects a delayed callback rather than changing the active occurrence. */
    @Transaction
    open suspend fun prepareServiceInstanceForWrite(
        serviceInstanceId: String,
        serviceDate: String,
    ): Boolean {
        require(serviceInstanceId.isNotBlank()) { "A service instance requires an occurrence ID" }
        val current = profile() ?: error("Experience profile must exist before service activation")
        if (
            current.serviceInstanceId == serviceInstanceId &&
            current.serviceInstanceDate == serviceDate
        ) {
            return true
        }
        return false
    }

    @Transaction
    open suspend fun replaceSnapshot(snapshot: ExperienceOperationalSnapshotEntity) {
        upsertProfile(snapshot.profile)
        deleteAllRoleAssignments()
        deleteAllReadingAssignments()
        deleteAllReadingPlans()
        deleteAllServiceRecordMarkers()
        deleteAllServiceInstances()
        deleteAllRecordMarkers()
        if (snapshot.serviceInstances.isNotEmpty()) insertServiceInstances(snapshot.serviceInstances)
        if (snapshot.roleAssignments.isNotEmpty()) insertRoleAssignments(snapshot.roleAssignments)
        if (snapshot.readingAssignments.isNotEmpty()) {
            insertReadingAssignments(snapshot.readingAssignments)
        }
        if (snapshot.readingPlans.isNotEmpty()) insertReadingPlans(snapshot.readingPlans)
        if (snapshot.recordMarkers.isNotEmpty()) insertRecordMarkers(snapshot.recordMarkers)
        if (snapshot.serviceRecordMarkers.isNotEmpty()) {
            insertServiceRecordMarkers(snapshot.serviceRecordMarkers)
        }
    }

    @Transaction
    open suspend fun setReadingAssignmentAndPlan(
        serviceInstanceId: String,
        slotId: String,
        assigneeName: String,
    ) {
        if (assigneeName.isBlank()) {
            deleteReadingAssignment(serviceInstanceId, slotId)
        } else {
            upsertReadingAssignment(
                ExperienceReadingAssignmentEntity(
                    serviceInstanceId = serviceInstanceId,
                    slotId = slotId,
                    assigneeName = assigneeName,
                ),
            )
        }

        val updatedPlan = (
            readingPlan(serviceInstanceId, slotId)
                ?: ExperienceReadingPlanEntity(
                    serviceInstanceId = serviceInstanceId,
                    slotId = slotId,
                )
            ).copy(assigneeName = assigneeName)
        if (updatedPlan.isDefaultPlan()) {
            deleteReadingPlan(serviceInstanceId, slotId)
        } else {
            upsertReadingPlan(updatedPlan)
        }
    }

    @Transaction
    open suspend fun setReadingPlanAndAssignment(
        serviceInstanceId: String,
        slotId: String,
        plan: ExperienceReadingPlanEntity?,
        assignment: ExperienceReadingAssignmentEntity?,
    ) {
        if (plan == null) deleteReadingPlan(serviceInstanceId, slotId) else upsertReadingPlan(plan)
        if (assignment == null) {
            deleteReadingAssignment(serviceInstanceId, slotId)
        } else {
            upsertReadingAssignment(assignment)
        }
    }

    @Transaction
    open suspend fun clearExperienceData() {
        deleteAllRoleAssignments()
        deleteAllReadingAssignments()
        deleteAllReadingPlans()
        deleteAllServiceRecordMarkers()
        deleteAllServiceInstances()
        deleteAllRecordMarkers()
        upsertProfile(ExperienceProfileEntity(legacyDataStoreMigrated = true))
    }
}

private fun ExperienceReadingPlanEntity.isDefaultPlan(): Boolean =
    copy(serviceInstanceId = "", slotId = "") ==
        ExperienceReadingPlanEntity(serviceInstanceId = "", slotId = "")

internal object ExperienceRecordTypes {
    const val COMPLETED_PRACTICE_STEP = "completed_practice_step"
    const val SAVED_PRACTICE_CARD = "saved_practice_card"
    const val COMPLETED_PREFLIGHT_STEP = "completed_preflight_step"
    const val COMPLETED_SERVICE_SEGMENT = "completed_service_segment"
    const val BOOKMARKED_SOURCE = "bookmarked_source"
    const val REVIEWED_DOSSIER_FACT = "reviewed_dossier_fact"

    val serviceInstanceScoped = listOf(
        COMPLETED_PRACTICE_STEP,
        COMPLETED_PREFLIGHT_STEP,
        COMPLETED_SERVICE_SEGMENT,
    )

    val rehearsalProgress = serviceInstanceScoped
}

internal const val EXPERIENCE_PROFILE_ID = 1
private const val DEFAULT_WORKSPACE_KIND = "qahal"
private const val DEFAULT_CHARTER_VERSION = "Draft 1"
private const val DEFAULT_PREPARATION_STATUS = "NOT_STARTED"
