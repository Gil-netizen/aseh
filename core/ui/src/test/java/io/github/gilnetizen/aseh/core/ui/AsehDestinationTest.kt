package io.github.gilnetizen.aseh.core.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class AsehDestinationTest {
    @Test
    fun destinationOrderIsStable() {
        assertEquals(
            listOf(
                AsehDestination.NOW,
                AsehDestination.PRACTICE,
                AsehDestination.PRAYER,
                AsehDestination.STUDY,
                AsehDestination.BUILD,
            ),
            AsehDestination.entries,
        )
    }

    @Test
    fun persistedIdsAreUniqueAndRoundTrip() {
        val persistedIds = AsehDestination.entries.map(AsehDestination::persistedId)

        assertEquals(persistedIds.size, persistedIds.toSet().size)
        assertTrue(persistedIds.all(String::isNotBlank))
        AsehDestination.entries.forEach { destination ->
            assertSame(
                destination,
                AsehDestination.fromPersistedId(destination.persistedId),
            )
        }
    }

    @Test
    fun unknownPersistedIdRestoresToReachableDefault() {
        assertSame(AsehDestination.NOW, AsehDestination.fromPersistedId(null))
        assertSame(AsehDestination.NOW, AsehDestination.fromPersistedId("removed"))
    }
}
