package com.rm.blokhead

import android.content.res.Configuration
import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.rm.blokhead.audio.SfxPlayer
import com.rm.blokhead.data.GamepadBindings
import com.rm.blokhead.data.GamepadBindingsStore
import com.rm.blokhead.data.HighScoreEntry
import com.rm.blokhead.data.HighScoreStore
import com.rm.blokhead.data.Settings
import com.rm.blokhead.data.SettingsStore
import com.rm.blokhead.game.Axis
import com.rm.blokhead.game.FormCatalog
import com.rm.blokhead.game.GameEngine
import com.rm.blokhead.game.resolveGamepadAction
import com.rm.blokhead.data.GamepadAction
import com.rm.blokhead.render.BlokoutSurfaceView
import com.rm.blokhead.render.wellBackgroundColor
import com.rm.blokhead.ui.AppScreen
import com.rm.blokhead.ui.GameControls
import com.rm.blokhead.ui.GameHud
import com.rm.blokhead.ui.HudStat
import com.rm.blokhead.ui.GameOverOverlay
import com.rm.blokhead.ui.GamepadBindingsScreen
import com.rm.blokhead.ui.HighScoreScreen
import com.rm.blokhead.ui.HudSnapshot
import com.rm.blokhead.ui.MenuScreen
import com.rm.blokhead.ui.MoveDPad
import com.rm.blokhead.ui.NameEntryOverlay
import com.rm.blokhead.ui.PausedOverlay
import com.rm.blokhead.ui.RotateCluster
import com.rm.blokhead.ui.SettingsScreen
import com.rm.blokhead.ui.gamepadFocusable
import com.rm.blokhead.ui.theme.BlokHeadTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Each control cluster's fixed footprint in [GameControls]'s [MoveDPad]/[RotateCluster] (3
 *  columns of 48.dp cells with 6.dp gaps) — used only to keep the landscape layout's centered
 *  grid from ever overlapping the clusters beside it; not imported directly since those are
 *  private constants of a different file's internal layout. */
private val LANDSCAPE_CLUSTER_WIDTH = 156.dp

/** Matches [GameControls]'s private `CELL` — a single button's width at 100% Button Scale. In
 *  landscape, the control clusters sit right against the screen's physical left/right edges,
 *  which on a real device can land right under a camera cutout or the gesture-nav area; insetting
 *  by a full button width (on top of the existing small gap) keeps them clear of either by
 *  default — scaled further by [Settings.landscapeButtonInset] for devices that need more or
 *  less. */
private val LANDSCAPE_EDGE_INSET = 48.dp

class MainActivity : ComponentActivity() {
    // Activity-level field (not `remember`-ed — a composable can't be reached from
    // dispatchKeyEvent) that every gamepad-aware composable installs/clears itself into as it
    // enters/leaves composition. See GamepadInputRouter's doc.
    private val gamepadRouter = GamepadInputRouter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BlokHeadTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    BlokHeadApp(gamepadRouter)
                }
            }
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean =
        gamepadRouter.handle(event) ?: super.dispatchKeyEvent(event)
}

@Composable
private fun BlokHeadApp(gamepadRouter: GamepadInputRouter) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val highScoreStore = remember { HighScoreStore(context.applicationContext) }
    val settingsStore = remember { SettingsStore(context.applicationContext) }
    val settings by settingsStore.settings.collectAsState(initial = Settings())
    val gamepadBindingsStore = remember { GamepadBindingsStore(context.applicationContext) }
    val gamepadBindings by gamepadBindingsStore.bindings.collectAsState(initial = GamepadBindings())
    val sfx = remember { SfxPlayer(context.applicationContext) }
    DisposableEffect(sfx) { onDispose { sfx.release() } }
    LaunchedEffect(settings.soundEnabled) { sfx.muted = !settings.soundEnabled }

    var screen by remember { mutableStateOf(AppScreen.MENU) }
    // Bumped each "Start Game", so GameScreen's remember(sessionId) below starts a fresh
    // GameEngine/GLSurfaceView per playthrough instead of reusing one across menu visits.
    var gameSessionId by remember { mutableIntStateOf(0) }

    // The fixed "B" button always means Back, wherever that leads for the screen currently
    // shown — except AppScreen.GAME, which owns its own backHandler for the exit-confirm dialog
    // (see GameScreen's DisposableEffect) since Back means something different while playing.
    DisposableEffect(screen) {
        if (screen != AppScreen.GAME) {
            gamepadRouter.backHandler = when (screen) {
                AppScreen.MENU -> null
                AppScreen.HIGH_SCORES -> { { sfx.playMenu(); screen = AppScreen.MENU } }
                AppScreen.SETTINGS -> { { sfx.playMenu(); screen = AppScreen.MENU } }
                AppScreen.CONTROLLER -> { { sfx.playMenu(); screen = AppScreen.SETTINGS } }
                AppScreen.GAME -> null
            }
        }
        onDispose { if (screen != AppScreen.GAME) gamepadRouter.backHandler = null }
    }

    when (screen) {
        AppScreen.MENU -> MenuScreen(
            onStartGame = {
                sfx.playMenu()
                gameSessionId++
                screen = AppScreen.GAME
            },
            onShowHighScores = {
                sfx.playMenu()
                screen = AppScreen.HIGH_SCORES
            },
            onShowSettings = {
                sfx.playMenu()
                screen = AppScreen.SETTINGS
            },
        )

        AppScreen.GAME -> GameScreen(
            sessionId = gameSessionId,
            settings = settings,
            gamepadBindings = gamepadBindings,
            gamepadRouter = gamepadRouter,
            highScoreStore = highScoreStore,
            sfx = sfx,
            onExitToMenu = { screen = AppScreen.MENU },
        )

        AppScreen.HIGH_SCORES -> {
            var entries by remember { mutableStateOf(emptyList<HighScoreEntry>()) }
            LaunchedEffect(Unit) { highScoreStore.entries.collect { entries = it } }
            HighScoreScreen(entries = entries, onBack = { sfx.playMenu(); screen = AppScreen.MENU })
        }

        AppScreen.SETTINGS -> SettingsScreen(
            settings = settings,
            onSettingsChange = { updated -> coroutineScope.launch { settingsStore.save(updated) } },
            onBack = { sfx.playMenu(); screen = AppScreen.MENU },
            onShowGamepadBindings = { sfx.playMenu(); screen = AppScreen.CONTROLLER },
        )

        AppScreen.CONTROLLER -> GamepadBindingsScreen(
            gamepadRouter = gamepadRouter,
            bindings = gamepadBindings,
            onBindingsChange = { updated -> coroutineScope.launch { gamepadBindingsStore.save(updated) } },
            onBack = { sfx.playMenu(); screen = AppScreen.SETTINGS },
        )
    }
}

@Composable
private fun GameScreen(
    sessionId: Int,
    settings: Settings,
    gamepadBindings: GamepadBindings,
    gamepadRouter: GamepadInputRouter,
    highScoreStore: HighScoreStore,
    sfx: SfxPlayer,
    onExitToMenu: () -> Unit,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    // Captured once per session (sessionId only bumps on "Start Game"), so settings changed
    // later from the menu never affect a game already in progress.
    val engine = remember(sessionId) {
        GameEngine(
            forms = FormCatalog.formsFor(settings.blockSet),
            startLevel = settings.startingDifficulty,
            width = settings.wellSize,
            depth = settings.wellSize,
            height = settings.wellHeight,
        )
    }
    val surfaceView = remember(sessionId) { BlokoutSurfaceView(context, engine) }

    // Nothing else in this app matches the Activity's own lifecycle to the game/GL surface's —
    // without this, backgrounding only *looks* paused because Android happens to tear down the
    // GL surface for an invisible window (stopping onDrawFrame, which is what drives
    // engine.update()); that's incidental, not guaranteed (multi-window/some launchers' recent-
    // apps preview can keep the surface alive), and GLSurfaceView's own docs call for onPause()/
    // onResume() regardless. ON_STOP pauses the game the same way tapping the grid does (so it
    // resumes exactly where it was, PAUSED overlay and all, never auto-unpausing on return) and
    // suspends the GL thread; ON_START only resumes the GL thread, leaving the game paused until
    // the player explicitly taps to continue.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, surfaceView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> {
                    surfaceView.enqueue { setPaused(true) }
                    surfaceView.onPause()
                }
                Lifecycle.Event.ON_START -> surfaceView.onResume()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // GameEngine's mutable state is owned by the GL thread (see BlokoutRenderer); this polls the
    // plain Int/Boolean fields on a timer for display rather than wiring a proper state stream,
    // since a HUD is fine lagging one tick behind and this avoids adding cross-thread
    // synchronization to the ported game logic just for readouts. The same poll also diffs
    // cubesDropped/levelsDescended/isGameOver against their previous values to trigger sound
    // effects for lock/layer-clear/game-over — those are engine-state transitions, not direct
    // taps, so there's no single call site to hang a sfx.play...() off of otherwise.
    var hud by remember(sessionId) { mutableStateOf(HudSnapshot(0, 0, 0, false, false)) }
    LaunchedEffect(engine) {
        var previousCubesDropped = engine.cubesDropped
        var previousLayersCleared = engine.levelsDescended
        var previousGameOver = engine.isGameOver
        while (true) {
            val cubesDropped = engine.cubesDropped
            val layersCleared = engine.levelsDescended
            val gameOver = engine.isGameOver
            if (cubesDropped != previousCubesDropped) sfx.playLock()
            if (layersCleared != previousLayersCleared) sfx.playClear()
            if (gameOver && !previousGameOver) sfx.playGameOver()
            previousCubesDropped = cubesDropped
            previousLayersCleared = layersCleared
            previousGameOver = gameOver

            hud = HudSnapshot(engine.score, engine.level, cubesDropped, gameOver, engine.isPaused)
            delay(100)
        }
    }

    // Once game-over fires, check the high-score table exactly once for this playthrough: either
    // the score qualifies (show NameEntryOverlay) or it doesn't (go straight to GameOverOverlay).
    var highScoreQualified by remember(sessionId) { mutableStateOf<Boolean?>(null) }
    LaunchedEffect(hud.isGameOver) {
        if (hud.isGameOver && highScoreQualified == null) {
            highScoreQualified = highScoreStore.isHighScore(hud.score)
        }
    }

    var showExitConfirm by remember(sessionId) { mutableStateOf(false) }

    // Installs the gamepad gameplay handler for exactly as long as this session is playable —
    // reads showExitConfirm/hud/gamepadBindings live on every call (Compose state read by
    // reference, not captured by value), so it doesn't need to be reinstalled when any of those
    // change. While a dialog/overlay is covering the game, it defers entirely to Back/Compose's
    // own key handling instead of acting on the piece underneath.
    DisposableEffect(sessionId) {
        gamepadRouter.gameplayHandler = { event ->
            val action = if (showExitConfirm || hud.isGameOver) {
                null
            } else {
                resolveGamepadAction(event.keyCode, gamepadBindings.keyCodes)
            }
            when (action) {
                GamepadAction.MoveLeft -> { sfx.playMove(); surfaceView.enqueue { moveLeft() } }
                GamepadAction.MoveRight -> { sfx.playMove(); surfaceView.enqueue { moveRight() } }
                GamepadAction.MoveForward -> { sfx.playMove(); surfaceView.enqueue { moveForward() } }
                GamepadAction.MoveBackward -> { sfx.playMove(); surfaceView.enqueue { moveBackward() } }
                GamepadAction.RotateXPositive -> { sfx.playRotate(); surfaceView.enqueue { rotate(Axis.X, 1) } }
                GamepadAction.RotateXNegative -> { sfx.playRotate(); surfaceView.enqueue { rotate(Axis.X, -1) } }
                GamepadAction.RotateYPositive -> { sfx.playRotate(); surfaceView.enqueue { rotate(Axis.Y, 1) } }
                GamepadAction.RotateYNegative -> { sfx.playRotate(); surfaceView.enqueue { rotate(Axis.Y, -1) } }
                GamepadAction.RotateZPositive -> { sfx.playRotate(); surfaceView.enqueue { rotate(Axis.Z, 1) } }
                GamepadAction.RotateZNegative -> { sfx.playRotate(); surfaceView.enqueue { rotate(Axis.Z, -1) } }
                GamepadAction.HardDrop -> surfaceView.enqueue { hardDrop() }
                GamepadAction.Pause -> surfaceView.enqueue { setPaused(!isPaused) }
                null -> {}
            }
            action != null
        }
        gamepadRouter.backHandler = { if (showExitConfirm) showExitConfirm = false }
        onDispose {
            gamepadRouter.gameplayHandler = null
            gamepadRouter.backHandler = null
        }
    }

    // Shared by both orientation branches below, so the move/rotate/drop wiring exists exactly
    // once regardless of which layout is currently shown.
    val onMove: (Int, Int) -> Unit = { axis, sign ->
        sfx.playMove()
        surfaceView.enqueue {
            when (axis) {
                Axis.X -> if (sign < 0) moveLeft() else moveRight()
                else -> if (sign < 0) moveBackward() else moveForward()
            }
        }
    }
    val onDiagonalMove: (Int, Int) -> Unit = { xSign, ySign ->
        sfx.playMove()
        surfaceView.enqueue {
            if (xSign < 0) moveLeft() else moveRight()
            if (ySign < 0) moveBackward() else moveForward()
        }
    }
    val onRotateAction: (Int, Int) -> Unit = { axis, sign ->
        sfx.playRotate()
        surfaceView.enqueue { rotate(axis, sign) }
    }
    val onHardDropAction: () -> Unit = { surfaceView.enqueue { hardDrop() } }

    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(wellBackgroundColor),
    ) {
        if (isLandscape) {
            // Pillarboxed: the grid (HUD + well) sits centered at a portrait-like aspect ratio
            // with dark margins on both sides, and the two control clusters are vertically
            // centered in those margins — a fundamentally different shape than portrait's
            // Column-with-spacer layout below, not a tweak of the same formula, since portrait's
            // aspect/spacer math assumes a container taller than it is wide and produces
            // negative/invalid values once it isn't.
            // The well's near opening is square (footprint width == depth), and the renderer's
            // camera always projects it as a square image whose rendered size equals
            // min(box width, box height) — handing it a box taller than it is wide (as an
            // earlier version of this code did, matching the device's own portrait aspect)
            // doesn't make that square any bigger, it just adds unused margin above/below equal
            // to the box's extra height. A square box (width == height == the full available
            // height) is what actually makes the rendered well as large as possible with zero
            // dead space top or bottom — landscape has width to spare for this, unlike portrait.
            val edgeInset = 8.dp + LANDSCAPE_EDGE_INSET * settings.buttonScale * settings.landscapeButtonInset
            val clusterClearance = (LANDSCAPE_CLUSTER_WIDTH * settings.buttonScale + edgeInset + 8.dp) * 2
            val gridWidth = minOf(maxHeight, maxWidth - clusterClearance).coerceAtLeast(0.dp)

            // The grid claims the full container height on its own now — SCORE/LEVEL/CUBES no
            // longer sit in a bar above it (that ate noticeably into how large the well could
            // render); they're laid out below instead, tucked into the side margins' otherwise
            // unused space above the vertically-centered control clusters. With no HUD bar left
            // to tap, the grid itself is the pause target.
            AndroidView(
                modifier = Modifier
                    .align(Alignment.Center)
                    .width(gridWidth)
                    .fillMaxHeight()
                    .pointerInput(engine) {
                        detectTapGestures {
                            if (!hud.isGameOver) surfaceView.enqueue { setPaused(!isPaused) }
                        }
                    },
                factory = { surfaceView },
            )

            // Button Position only makes sense for portrait's below-the-grid stack; landscape
            // always centers both clusters vertically in the side margins instead.
            val leftModifier = Modifier.align(Alignment.CenterStart).padding(horizontal = edgeInset)
            val rightModifier = Modifier.align(Alignment.CenterEnd).padding(horizontal = edgeInset)

            Column(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .statusBarsPadding()
                    .padding(horizontal = edgeInset, vertical = 8.dp),
            ) {
                HudStat("SCORE", hud.score.toString())
            }
            Column(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(horizontal = edgeInset, vertical = 8.dp),
            ) {
                HudStat("LEVEL", hud.level.toString())
                HudStat("CUBES", hud.cubesDropped.toString(), modifier = Modifier.padding(top = 8.dp))
            }
            if (settings.leftHandedMode) {
                RotateCluster(onRotate = onRotateAction, modifier = leftModifier, scale = settings.buttonScale)
                MoveDPad(
                    diagonalEnabled = settings.diagonalButtonsEnabled,
                    onMove = onMove,
                    onDiagonalMove = onDiagonalMove,
                    onHardDrop = onHardDropAction,
                    modifier = rightModifier,
                    scale = settings.buttonScale,
                )
            } else {
                MoveDPad(
                    diagonalEnabled = settings.diagonalButtonsEnabled,
                    onMove = onMove,
                    onDiagonalMove = onDiagonalMove,
                    onHardDrop = onHardDropAction,
                    modifier = leftModifier,
                    scale = settings.buttonScale,
                )
                RotateCluster(onRotate = onRotateAction, modifier = rightModifier, scale = settings.buttonScale)
            }
        } else {
            AndroidView(modifier = Modifier.fillMaxSize(), factory = { surfaceView })

            GameHud(snapshot = hud, modifier = Modifier.fillMaxWidth())

            // BlokoutRenderer's camera solves the vertical FOV so the well's near opening exactly
            // fills the viewport width; since the well is square (width == depth), that makes the
            // opening's projected height a fixed `aspect` fraction of the screen height, centered —
            // i.e. the rendered grid occupies the vertical band [0.5 - aspect/2, 0.5 + aspect/2].
            val containerHeight = maxHeight
            val aspect = maxWidth.value / maxHeight.value
            val gridBottomFraction = 0.5f + aspect / 2f
            val gridTopHeight = containerHeight * (0.5f - aspect / 2f)

            // Tapping anywhere above the grid (the HUD's band included) pauses/unpauses — placed
            // before the controls/overlays below so it never steals taps meant for them, and it's
            // a no-op once the game has ended (pausing a finished game doesn't mean anything).
            if (!hud.isGameOver) {
                Spacer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(gridTopHeight)
                        .pointerInput(engine) {
                            detectTapGestures {
                                surfaceView.enqueue { setPaused(!isPaused) }
                            }
                        },
                )
            }

            // Default position is right after the grid's bottom edge (plus a small gap), which
            // keeps controls just clear of it regardless of screen size; the "Button Position"
            // setting slides that down towards the bottom edge instead of a fixed guessed position.
            val minSpacerHeight = containerHeight * gridBottomFraction + 12.dp
            val maxSpacerHeight = (containerHeight - 200.dp).coerceAtLeast(minSpacerHeight)
            val spacerHeight = lerp(minSpacerHeight, maxSpacerHeight, settings.buttonVerticalPosition)
            Column(modifier = Modifier.fillMaxSize()) {
                Spacer(Modifier.height(spacerHeight))
                GameControls(
                    onMove = onMove,
                    onDiagonalMove = onDiagonalMove,
                    onRotate = onRotateAction,
                    onHardDrop = onHardDropAction,
                    diagonalEnabled = settings.diagonalButtonsEnabled,
                    leftHanded = settings.leftHandedMode,
                    opacity = settings.buttonOpacity,
                    scale = settings.buttonScale,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                )
            }
        }

        AnimatedVisibility(
            visible = hud.isPaused && !hud.isGameOver,
            enter = fadeIn(animationSpec = tween(150)),
            exit = fadeOut(animationSpec = tween(150)),
        ) {
            PausedOverlay(onMenuClick = { showExitConfirm = true })
        }

        if (showExitConfirm) {
            AlertDialog(
                onDismissRequest = { showExitConfirm = false },
                title = { Text("Quit to Menu?") },
                text = { Text("Your current game will be lost.") },
                confirmButton = {
                    val quit = {
                        showExitConfirm = false
                        sfx.playMenu()
                        onExitToMenu()
                    }
                    TextButton(onClick = quit, modifier = Modifier.gamepadFocusable(onActivate = quit)) {
                        Text("Quit")
                    }
                },
                dismissButton = {
                    val cancel = { showExitConfirm = false }
                    TextButton(onClick = cancel, modifier = Modifier.gamepadFocusable(onActivate = cancel)) {
                        Text("Cancel")
                    }
                },
            )
        }

        if (hud.isGameOver) {
            if (highScoreQualified == true) {
                NameEntryOverlay(
                    finalScore = hud.score,
                    onSubmit = { name ->
                        coroutineScope.launch {
                            highScoreStore.submit(name, hud.score)
                            highScoreQualified = false // falls through to GameOverOverlay below
                        }
                    },
                )
            } else if (highScoreQualified == false) {
                GameOverOverlay(
                    finalScore = hud.score,
                    onPlayAgain = {
                        highScoreQualified = null
                        surfaceView.enqueue { restart() }
                    },
                    onMainMenu = onExitToMenu,
                )
            }
        }
    }
}

private fun lerp(start: Dp, stop: Dp, fraction: Float): Dp = start + (stop - start) * fraction
