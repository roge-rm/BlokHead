package com.rm.blokhead.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.rm.blokhead.R

/**
 * Short synthesized retro-style sound effects (bundled as res/raw/sfx_*.wav — generated tones,
 * not licensed samples), played via [SoundPool] for low-latency, overlapping playback. The
 * original had no audio at all; this is new, not a port of anything.
 */
class SfxPlayer(context: Context) {
    private val soundPool = SoundPool.Builder()
        .setMaxStreams(4)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build(),
        )
        .build()

    private val moveId = soundPool.load(context, R.raw.sfx_move, 1)
    private val rotateId = soundPool.load(context, R.raw.sfx_rotate, 1)
    private val lockId = soundPool.load(context, R.raw.sfx_lock, 1)
    private val clearId = soundPool.load(context, R.raw.sfx_clear, 1)
    private val gameOverId = soundPool.load(context, R.raw.sfx_gameover, 1)
    private val menuId = soundPool.load(context, R.raw.sfx_menu, 1)

    fun playMove() = play(moveId)
    fun playRotate() = play(rotateId)
    fun playLock() = play(lockId)
    fun playClear() = play(clearId)
    fun playGameOver() = play(gameOverId)
    fun playMenu() = play(menuId)

    private fun play(soundId: Int) {
        soundPool.play(soundId, 1f, 1f, /* priority = */ 1, /* loop = */ 0, /* rate = */ 1f)
    }

    fun release() = soundPool.release()
}
