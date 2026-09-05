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

    private var levelFactor: Float = levelFactorFor(startLevel)
    private var elapsedSinceSpawn: Float = 0f

    lateinit var currentBlock: Block
        private set

    init {
        spawnBlock()
    }

    private fun levelFactorFor(level: Int): Float = if (level < 5) level / 5f else level - 5f

    private fun spawnBlock() {
        val form = forms[random.nextInt(forms.size)]
        currentBlock = Block(form, fallSpeed = 0.1f + level * 0.3f)
        elapsedSinceSpawn = 0f
    }

    /** Advances the game by [deltaSeconds]. Call this from the render/game loop each frame. */
    fun update(deltaSeconds: Float) {
        if (isGameOver) return
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
        if (isGameOver) return
        Collision.tryTurnBlock(tube, currentBlock, axis, sign, elapsedSinceSpawn)
    }

    private fun tryMove(axis: Int, sign: Int) {
        if (isGameOver) return
        Collision.tryMoveBlock(tube, currentBlock, axis, sign, elapsedSinceSpawn)
    }

    /** Instantly speeds up the fall, ported from the space-bar handler in control.c. */
    fun hardDrop() {
        if (isGameOver) return
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
        spawnBlock()
    }

    private fun lockCurrentBlockAndAdvance() {
        val lockedBlock = currentBlock
        val timeNow = elapsedSinceSpawn

        tube.addBlock(lockedBlock)
        cubesDropped += lockedBlock.form.numCubes
        levelsDescended += tube.lastDrop

        val newLevel = cubesDropped / 70
        if (level < 10 && level < newLevel) {
            level = newLevel
            levelFactor = levelFactorFor(level)
        }

        val moveScore = (levelFactor * timeNow * 200 * 2.0.pow(tube.lastDrop) * lockedBlock.fallSpeed) /
            (tube.dimensions[2] - tube.height)
        score += (moveScore + 0.5).toInt()

        tube.lastDrop = 0

        if (tube.height < tube.dimensions[2] - 5) {
            spawnBlock()
        } else {
            isGameOver = true
        }
    }
}
