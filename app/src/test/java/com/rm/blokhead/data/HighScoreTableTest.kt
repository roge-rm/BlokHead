package com.rm.blokhead.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HighScoreTableTest {

    @Test
    fun `any score qualifies while the table has room`() {
        assertTrue(HighScoreTable.isHighScore(emptyList(), 0))
    }

    @Test
    fun `a full table only takes scores above its current lowest`() {
        val full = (1..HighScoreTable.MAX_ENTRIES).map { HighScoreEntry("p$it", it * 10) }
        assertFalse(HighScoreTable.isHighScore(full, 5))
        assertTrue(HighScoreTable.isHighScore(full, 15))
    }

    @Test
    fun `insert keeps the table sorted highest first`() {
        val entries = HighScoreTable.insert(
            HighScoreTable.insert(emptyList(), "Alice", 100),
            "Bob",
            250,
        )
        assertEquals(listOf(HighScoreEntry("Bob", 250), HighScoreEntry("Alice", 100)), entries)
    }

    @Test
    fun `insert trims beyond MAX_ENTRIES, dropping the lowest score`() {
        var entries = (1..HighScoreTable.MAX_ENTRIES).map { HighScoreEntry("p$it", it * 10) }
        entries = HighScoreTable.insert(entries, "new", 5) // lower than everything already there
        assertEquals(HighScoreTable.MAX_ENTRIES, entries.size)
        assertTrue(entries.none { it.name == "new" })
    }

    @Test
    fun `encode-decode round trip preserves entries`() {
        val entries = listOf(HighScoreEntry("Alice", 250), HighScoreEntry("Bob", 100))
        assertEquals(entries, HighScoreStore.decode(HighScoreStore.encode(entries)))
    }

    @Test
    fun `decode of blank input is an empty list`() {
        assertEquals(emptyList<HighScoreEntry>(), HighScoreStore.decode(""))
    }
}
