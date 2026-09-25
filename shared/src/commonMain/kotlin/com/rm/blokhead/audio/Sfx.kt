package com.rm.blokhead.audio

/** The game's short sound effects. Android plays the bundled WAVs through SoundPool (:app's
 *  SfxPlayer); the browser build plays the same files through Web Audio. */
interface Sfx {
    /** Gated by the Settings screen's sound toggle; set from the UI layer. */
    var muted: Boolean

    fun playMove()
    fun playRotate()
    fun playLock()
    fun playClear()
    fun playGameOver()
    fun playMenu()
    fun release()
}
