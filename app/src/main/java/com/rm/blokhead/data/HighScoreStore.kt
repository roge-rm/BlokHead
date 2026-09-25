package com.rm.blokhead.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.highScoreDataStore by preferencesDataStore(name = "high_scores")
private val ENTRIES_KEY = stringPreferencesKey("entries")

/**
 * DataStore-backed persistence for the high-score table, replacing the original's flat-file
 * save/load (highscore.c's saveScoreTable()/loadScoreTable(), selectHighScoreFile() picking a
 * per-platform config path) with Android's per-app Preferences storage. Ranking logic itself
 * lives in [HighScoreTable].
 */
class HighScoreStore(private val context: Context) : HighScoreRepository {

    override val entries: Flow<List<HighScoreEntry>> =
        context.highScoreDataStore.data.map { prefs -> HighScoreCodec.decode(prefs[ENTRIES_KEY] ?: "") }

    override suspend fun submit(name: String, score: Int): List<HighScoreEntry> {
        var updated = emptyList<HighScoreEntry>()
        context.highScoreDataStore.edit { prefs ->
            updated = HighScoreTable.insert(HighScoreCodec.decode(prefs[ENTRIES_KEY] ?: ""), name, score)
            prefs[ENTRIES_KEY] = HighScoreCodec.encode(updated)
        }
        return updated
    }
}
