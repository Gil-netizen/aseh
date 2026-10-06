package io.github.gilnetizen.aseh.core.testing

import org.junit.Assert.assertTrue
import org.junit.Test

class SyntheticUiFixturesTest {
  @Test
  fun mixedDirectionFixtureUsesUnicodeIsolation() {
    assertTrue(SyntheticUiFixtures.MIXED_DIRECTION_TEXT.contains('\u2066'))
    assertTrue(SyntheticUiFixtures.MIXED_DIRECTION_TEXT.contains('\u2067'))
    assertTrue(SyntheticUiFixtures.MIXED_DIRECTION_TEXT.contains('\u2069'))
  }
}
