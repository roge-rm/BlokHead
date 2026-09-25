package com.rm.blokhead.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

/** Where [Settings] live: Android DataStore in :app, localStorage in the browser build. */
interface SettingsRepository {
    val settings: Flow<Settings>
    suspend fun save(settings: Settings)
}

/** Where [GamepadBindings] live; see [SettingsRepository]. */
interface GamepadBindingsRepository {
    val bindings: Flow<GamepadBindings>
    suspend fun save(bindings: GamepadBindings)
}

/** Where the high-score table lives; ranking itself is [HighScoreTable]'s. */
interface HighScoreRepository {
    val entries: Flow<List<HighScoreEntry>>
    suspend fun submit(name: String, score: Int): List<HighScoreEntry>
    suspend fun isHighScore(score: Int): Boolean = HighScoreTable.isHighScore(entries.first(), score)
}
