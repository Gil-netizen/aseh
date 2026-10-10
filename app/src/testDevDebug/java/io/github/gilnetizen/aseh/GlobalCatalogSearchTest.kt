package io.github.gilnetizen.aseh

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GlobalCatalogSearchTest {
  private val catalog = flavorContentCatalog()

  @Test
  fun blankSearchDoesNotDumpTheCatalog() {
    assertTrue(searchCatalog(catalog, "   ").isEmpty())
  }

  @Test
  fun searchFindsPracticeAndExactSourceContentOffline() {
    val accessible = searchCatalog(catalog, "accessible path")
    assertTrue(accessible.any { it.destination == GlobalSearchDestination.PRACTICE })

    val conductor = searchCatalog(catalog, "conductor transition")
    assertTrue(conductor.any { it.destination == GlobalSearchDestination.PRAYER })
    assertTrue(conductor.any { it.destination == GlobalSearchDestination.STUDY_SOURCE })
  }

  @Test
  fun searchHonorsTheResultLimit() {
    assertEquals(1, searchCatalog(catalog, "rehearsal", limit = 1).size)
    assertTrue(searchCatalog(catalog, "rehearsal", limit = 0).isEmpty())
  }
}
