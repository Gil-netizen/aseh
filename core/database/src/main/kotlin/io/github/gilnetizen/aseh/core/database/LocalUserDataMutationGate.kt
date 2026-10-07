package io.github.gilnetizen.aseh.core.database

import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Serializes local user-data writes with a confirmed delete-all operation.
 *
 * Once deletion is requested, new writes are rejected instead of waiting and
 * recreating data after the stores have been cleared. Writes already in flight
 * finish before deletion acquires the mutex.
 */
class LocalUserDataMutationGate {
    private val mutationMutex = Mutex()
    private val deletionRequested = AtomicBoolean(false)

    val isDeletionRequested: Boolean
        get() = deletionRequested.get()

    suspend fun mutate(block: suspend () -> Unit): Boolean {
        if (deletionRequested.get()) return false

        return mutationMutex.withLock {
            if (deletionRequested.get()) {
                false
            } else {
                block()
                true
            }
        }
    }

    suspend fun <T> deleteExclusively(block: suspend () -> T): T {
        check(deletionRequested.compareAndSet(false, true)) {
            "Local user-data deletion is already in progress"
        }

        return try {
            withContext(NonCancellable) {
                mutationMutex.withLock { block() }
            }
        } finally {
            deletionRequested.set(false)
        }
    }
}
