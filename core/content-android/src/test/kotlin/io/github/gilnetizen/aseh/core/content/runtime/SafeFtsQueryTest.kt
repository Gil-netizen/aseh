package io.github.gilnetizen.aseh.core.content.runtime

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class SafeFtsQueryTest {
    @Test
    fun `terms become quoted prefix operands`() {
        assertEquals("\"rehearsal*\" AND \"roles*\"", SafeFtsQuery.compile("rehearsal roles"))
        assertEquals("\"תפקי*\"", SafeFtsQuery.compile("תפקי"))
    }

    @Test
    fun `MATCH operators and SQL punctuation never become syntax`() {
        val expression = requireNotNull(SafeFtsQuery.compile("x' OR 1=1 -- \" NEAR(test)"))

        assertEquals("\"x'*\" AND \"OR*\" AND \"1*\" AND \"1*\" AND \"NEAR*\" AND \"test*\"", expression)
        assertTrue(!expression.contains("--"))
        assertTrue(!expression.contains("="))
        assertTrue(!expression.contains("("))
    }

    @Test
    fun `empty and punctuation-only queries do not touch SQLite`() {
        assertNull(SafeFtsQuery.compile("  ( -- )  "))
    }

    @Test
    fun `oversized queries terms and result limits fail closed`() {
        assertThrows(ContentSearchInputException::class.java) {
            SafeFtsQuery.compile("x".repeat(1025))
        }
        assertThrows(ContentSearchInputException::class.java) {
            SafeFtsQuery.compile((1..13).joinToString(" ") { "term$it" })
        }
        assertThrows(ContentSearchInputException::class.java) {
            SafeFtsQuery.compile("x".repeat(65))
        }
    }
}
