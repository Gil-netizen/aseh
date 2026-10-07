package io.github.gilnetizen.aseh.core.database

import java.util.EnumMap
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

/**
 * Deletes every category of mutable user data currently owned by the alpha app.
 *
 * Installed public content packs are intentionally outside this boundary. A deletion
 * failure in one store does not prevent attempts against the other independent stores.
 */
class LocalUserDataDeletion(
    private val experienceStateRepository: ExperienceStateRepository?,
    private val manualPlaceContextRepository: ManualPlaceContextRepository,
    private val interfacePreferencesRepository: InterfacePreferencesRepository,
    private val workDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    // A confirmed deletion must finish even if navigation or rotation cancels the UI coroutine.
    // Keep DataStore and Room work off the caller's UI context: DataStore executes an edit with
    // context inherited from its caller, and a Compose test or frame context can otherwise stall
    // that edit while the UI is waiting for deletion to complete.
    suspend fun deleteAll(): LocalUserDataDeletionResult = withContext(NonCancellable) {
        withContext(workDispatcher) {
            val failures = EnumMap<LocalUserDataCategory, Throwable>(LocalUserDataCategory::class.java)

            attemptDeletion(LocalUserDataCategory.SAVED_PLACE, failures) {
                manualPlaceContextRepository.clear()
            }
            if (experienceStateRepository != null) {
                attemptDeletion(LocalUserDataCategory.EXPERIENCE, failures) {
                    experienceStateRepository.clearAllExperienceData()
                }
            }
            attemptDeletion(LocalUserDataCategory.INTERFACE_PREFERENCES, failures) {
                interfacePreferencesRepository.reset()
            }

            LocalUserDataDeletionResult(failures.toMap())
        }
    }
}

enum class LocalUserDataCategory {
    SAVED_PLACE,
    EXPERIENCE,
    INTERFACE_PREFERENCES,
}

class LocalUserDataDeletionResult internal constructor(
    val failures: Map<LocalUserDataCategory, Throwable>,
) {
    val isComplete: Boolean
        get() = failures.isEmpty()
}

private suspend fun attemptDeletion(
    category: LocalUserDataCategory,
    failures: MutableMap<LocalUserDataCategory, Throwable>,
    delete: suspend () -> Unit,
) {
    try {
        delete()
    } catch (error: CancellationException) {
        throw error
    } catch (error: Throwable) {
        failures[category] = error
    }
}
