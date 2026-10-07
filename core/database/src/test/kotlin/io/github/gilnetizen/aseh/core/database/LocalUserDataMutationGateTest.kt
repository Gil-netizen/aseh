package io.github.gilnetizen.aseh.core.database

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalUserDataMutationGateTest {
    @Test
    fun deletionDrainsEarlierWriteAndRejectsWriteRequestedDuringDeletion() = runTest {
        val gate = LocalUserDataMutationGate()
        val firstWriteStarted = CompletableDeferred<Unit>()
        val allowFirstWriteToFinish = CompletableDeferred<Unit>()
        val storedValues = mutableListOf("existing")

        val firstWrite = launch {
            gate.mutate {
                firstWriteStarted.complete(Unit)
                allowFirstWriteToFinish.await()
                storedValues += "earlier write"
            }
        }
        firstWriteStarted.await()

        val deletion = async {
            gate.deleteExclusively {
                storedValues.clear()
            }
        }
        while (!gate.isDeletionRequested) yield()

        val lateWriteAccepted = gate.mutate {
            storedValues += "late write"
        }
        assertFalse(lateWriteAccepted)

        allowFirstWriteToFinish.complete(Unit)
        firstWrite.join()
        deletion.await()

        assertTrue(storedValues.isEmpty())
        assertTrue(gate.mutate { storedValues += "post-delete write" })
        assertEquals(listOf("post-delete write"), storedValues)
    }
}
