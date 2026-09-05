package com.rm.blokhead.game

import kotlin.math.pow
import kotlin.random.Random

/** Rotation axes, matching blokout's convention (0=X, 1=Y, 2=Z). */
object Axis {
    const val X = 0
    const val Y = 1
    const val Z = 2
}

/**
 * The game state machine, ported from blokout's game.c (gameIdle(), newBlock(), initGame()).
 * Pure Kotlin, no rendering/Android dependency, so it's directly unit-testable.
 *
 * Time in [update] is a delta in seconds; internally it's accumulated as time-since-the-current-
 * block-spawned, matching the original's per-block Timer that every Block/Collision function
 * expects (see [Block] doc).
 */
class GameEngine(
    private val forms: List<Form> = FormCatalog.allForms,
    private val startLevel: Int = 2,
    private val width: Int = 5,
    private val depth: Int = 5,
    private val height: Int = 16,
    private val random: Random = Random.Default,
) {
    var tube: Tube = Tube(width, depth, height)
        private set

    var level: Int = startLevel
        private set
    var score: Int = 0
        private set
    var cubesDropped: Int = 0
        private set
    var levelsDescended: Int = 0
        private set
    var isGameOver: Boolean = false
        private set
    var isPaused: Boolean = false
        private set

    /** World-Z layer indices currently flashing before they're cleared (see [update]); empty
     *  the rest of the time. The renderer uses this to highlight them instead of drawing the
     *  falling piece. */
    var pendingClearLayers: List<Int> = emptyList()
        private set

    /** 0f right when a layer clear starts, 1f right as it finishes — for the renderer to fade/
     *  pulse the flash. Meaningless (and unused) while [pendingClearLayers] is empty. */
    var clearFlashProgress: Float = 0f
        private set

    private var clearTimeRemaining = 0f

    /** True while input should be a no-op: game over, paused, or a completed layer is flashing
     *  (during which [currentBlock] is a stale reference — it already merged into the tube, and
     *  the next piece hasn't spawned — so acting on it would be meaningless). */
    private val isFrozen: Boolean
        get() = isGameOver || isPaused || pendingClearLayers.isNotEmpty()

    private var levelFactor: Float = levelFactorFor(startLevel)
    private var elapsedSinceSpawn: Float = 0f

    lateinit var currentBlock: Block
        private set

    init {
        spawnBlock()
    }

    /** How long a completed layer flashes before it's actually removed. Kept short so a clear
     *  doesn't interrupt the pace of play. */
    private val clearFlashDuration = 0.1f

    private fun levelFactorFor(level: Int): Float = if (level < 5) level / 5f else level - 5f

    private fun spawnBlock() {
        val form = forms[random.nextInt(forms.size)]
        currentBlock = Block(form, fallSpeed = 0.1f + level * 0.3f)
        elapsedSinceSpawn = 0f
    }

    /** Freezes/resumes the game in place: [update] becomes a no-op and every input method below
     *  is ignored until unpaused, without touching any game state (the piece resumes exactly
     *  where it left off). */
    fun setPaused(paused: Boolean) {
        isPaused = paused
    }

    /** Advances the game by [deltaSeconds]. Call this from the render/game loop each frame. */
    fun update(deltaSeconds: Float) {
        if (isGameOver || isPaused) return
        if (pendingClearLayers.isNotEmpty()) {
            clearTimeRemaining -= deltaSeconds
            clearFlashProgress = (1f - clearTimeRemaining / clearFlashDuration).coerceIn(0f, 1f)
            if (clearTimeRemaining <= 0f) finishClearingLayers()
            return // freeze the piece/spawn while a completed layer is flashing
        }
        elapsedSinceSpawn += deltaSeconds
        currentBlock.update(elapsedSinceSpawn)
        Collision.tryLowerBlock(tube, currentBlock, elapsedSinceSpawn)
        if (elapsedSinceSpawn - currentBlock.lastFall > 1f) {
            lockCurrentBlockAndAdvance()
        }
    }

    fun moveLeft() = tryMove(Axis.X, -1)
    fun moveRight() = tryMove(Axis.X, 1)
    fun moveForward() = tryMove(Axis.Y, 1)
    fun moveBackward() = tryMove(Axis.Y, -1)

    fun rotate(axis: Int, sign: Int) {
        if (isFrozen) return
        Collision.tryTurnBlock(tube, currentBlock, axis, sign, elapsedSinceSpawn)
    }

    private fun tryMove(axis: Int, sign: Int) {
        if (isFrozen) return
        Collision.tryMoveBlock(tube, currentBlock, axis, sign, elapsedSinceSpawn)
    }

    /** Instantly speeds up the fall, ported from the space-bar handler in control.c. */
    fun hardDrop() {
        if (isFrozen) return
        currentBlock.lastStop = elapsedSinceSpawn
        currentBlock.stopHeight = currentBlock.position[2]
        currentBlock.fallSpeed = tube.dimensions[2] / 2f
    }

    /** Resets the game to a fresh well and starting score/level, for a "play again" flow. */
    fun restart() {
        tube = Tube(width, depth, height)
        level = startLevel
        score = 0
        cubesDropped = 0
        levelsDescended = 0
        levelFactor = levelFactorFor(startLevel)
        isGameOver = false
        isPaused = false
        pendingClearLayers = emptyList()
        clearFlashProgress = 0f
        clearTimeRemaining = 0f
        spawnBlock()
    }

    private fun lockCurrentBlockAndAdvance() {
        val lockedBlock = currentBlock
        val timeNow = elapsedSinceSpawn

        val completedLayers = tube.placeBlock(lockedBlock)
        cubesDropped += lockedBlock.form.numCubes
        levelsDescended += completedLayers.size

        val newLevel = cubesDropped / 70
        if (level < 10 && level < newLevel) {
            level = newLevel
            levelFactor = levelFactorFor(level)
        }

        // tube.height reflects placement before any clearing; each cleared layer will drop it by
        // exactly one, so the post-clear height the original scored against is computed rather
        // than performed early — the actual clear (and the height change) waits for the flash.
        val postClearHeight = tube.height - completedLayers.size
        val moveScore = (levelFactor * timeNow * 200 * 2.0.pow(completedLayers.size) * lockedBlock.fallSpeed) /
            (tube.dimensions[2] - postClearHeight)
        score += (moveScore + 0.5).toInt()

        if (completedLayers.isEmpty()) {
            advanceAfterLock()
        } else {
            pendingClearLayers = completedLayers
            clearFlashProgress = 0f
            clearTimeRemaining = clearFlashDuration
        }
    }

    private fun finishClearingLayers() {
        tube.clearLayers(pendingClearLayers)
        pendingClearLayers = emptyList()
        advanceAfterLock()
    }

    private fun advanceAfterLock() {
        if (tube.height < tube.dimensions[2] - 5) {
            spawnBlock()
        } else {
            isGameOver = true
        }
    }
}
