package com.rm.blokhead.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.highScoreDataStore by preferencesDataStore(name = "high_scores")
private val ENTRIES_KEY = stringPreferencesKey("entries")

/**
 * DataStore-backed persistence for the high-score table, replacing the original's flat-file
 * save/load (highscore.c's saveScoreTable()/loadScoreTable(), selectHighScoreFile() picking a
 * per-platform config path) with Android's per-app Preferences storage. Ranking logic itself
 * lives in [HighScoreTable].
 */
class HighScoreStore(private val context: Context) {

    val entries: Flow<List<HighScoreEntry>> =
        context.highScoreDataStore.data.map { prefs -> decode(prefs[ENTRIES_KEY] ?: "") }

    suspend fun isHighScore(score: Int): Boolean = HighScoreTable.isHighScore(entries.first(), score)

    suspend fun submit(name: String, score: Int): List<HighScoreEntry> {
        var updated = emptyList<HighScoreEntry>()
        context.highScoreDataStore.edit { prefs ->
            updated = HighScoreTable.insert(decode(prefs[ENTRIES_KEY] ?: ""), name, score)
            prefs[ENTRIES_KEY] = encode(updated)
        }
        return updated
    }

    companion object {
        // "|" can't appear in a name (stripped below) so it's a safe field separator; each entry
        // is one line.
        fun encode(entries: List<HighScoreEntry>): String =
            entries.joinToString("\n") { "${it.score}|${sanitize(it.name)}" }

        fun decode(raw: String): List<HighScoreEntry> {
            if (raw.isBlank()) return emptyList()
            return raw.lineSequence().mapNotNull { line ->
                val separator = line.indexOf('|')
                if (separator < 0) return@mapNotNull null
                val score = line.substring(0, separator).toIntOrNull() ?: return@mapNotNull null
                HighScoreEntry(line.substring(separator + 1), score)
            }.toList()
        }

        private fun sanitize(name: String): String = name.replace("|", "").replace("\n", "").trim()
    }
}
