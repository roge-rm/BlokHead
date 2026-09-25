package com.rm.blokhead.data

/** One row of the high-score table, ported from blokout's Score struct (highscore.h). */
data class HighScoreEntry(val name: String, val score: Int)

/**
 * Pure ranking logic ported from highscore.c's isHighScore()/addHighScore() — kept free of
 * DataStore/Android so it's directly unit-testable; [com.rm.blokhead.data.HighScoreStore] handles
 * persistence around it.
 */
object HighScoreTable {
    /** Matches the original's HIGHSCORE_LENGTH. */
    const val MAX_ENTRIES = 20

    fun isHighScore(entries: List<HighScoreEntry>, score: Int): Boolean =
        entries.size < MAX_ENTRIES || score > entries.minOf { it.score }

    /** Inserts [name]/[score], keeping the table sorted by score (highest first) and trimmed to
     *  [MAX_ENTRIES]. */
    fun insert(entries: List<HighScoreEntry>, name: String, score: Int): List<HighScoreEntry> =
        (entries + HighScoreEntry(name, score))
            .sortedByDescending { it.score }
            .take(MAX_ENTRIES)
}
