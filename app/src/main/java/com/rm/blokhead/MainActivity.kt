package com.rm.blokhead

import android.content.res.Configuration
import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
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
import com.rm.blokhead.ui.gestureControls
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

/** Each control cluster's fixed footprint in [GameControls]'s [MoveDPad]/[RotateCluster] — a
 *  square 3x3 grid of 48.dp cells with 6.dp gaps, so this one constant is both its width (used to
 *  keep the landscape layout's centered grid from ever overlapping the clusters beside it) and
 *  its height (used by [clusterTopFor] for every layout's Button Height range); not imported
 *  directly since those are private constants of a different file's internal layout. */
private val CLUSTER_SIZE = 156.dp

/** Matches [GameControls]'s private `CELL` — a single button's width at 100% Button Scale. Both
 *  orientations' control clusters sit right against the screen's physical edges by default, which
 *  on a real device can land right under a camera cutout or the gesture-nav area; insetting by a
 *  full button width (on top of a small fixed gap) keeps them clear of either — scaled further by
 *  [Settings.portraitButtonInset]/[Settings.landscapeButtonInset] for devices that need more or
 *  less. */
private val EDGE_INSET_UNIT = 48.dp

/** How far from 1:1 a window can be and still get the square layout, as the ratio of its longer
 *  edge to its shorter one. Deliberately not exactly 1.0: a true 1:1 panel is rare, but windows
 *  that are square *enough* that neither the portrait nor the landscape layout has a spare band
 *  to work with are not — split-screen halves, freeform windows and unfolded foldables all land
 *  in here, and all of them want the same treatment. */
private const val SQUARE_MAX_EDGE_RATIO = 1.18f

/** The square layout's HUD bar opacity, overriding [GameHud]'s portrait-tuned default — the bar
 *  sits over live play area here, so it has to be see-through enough to read the well's top rows
 *  through it. */
private const val SQUARE_HUD_ALPHA = 0.45f

/** Multiplied into [Settings.buttonOpacity] for the square layout's corner clusters, for the same
 *  reason as [SQUARE_HUD_ALPHA] — they overlap the well rather than sitting below it, so the
 *  shipped default of fully opaque buttons would hide the corners of the bottom rows. The user's
 *  own opacity setting still scales on top of this rather than being replaced by it. */
private const val SQUARE_BUTTON_ALPHA = 0.7f

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

    // The system/hardware Back button and gesture reuse that exact same navigation — disabled at
    // MENU (so Back there falls through to the platform default and actually exits the app) and
    // at GAME (which registers its own BackHandler below instead, since Back means something
    // different mid-playthrough than simple screen-to-screen navigation).
    BackHandler(enabled = screen != AppScreen.MENU && screen != AppScreen.GAME) {
        gamepadRouter.backHandler?.invoke()
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

    // Shared by the system Back button/gesture and the gamepad's fixed "B" button alike, so both
    // agree on what Back means mid-playthrough: pause first (giving the player a chance to see
    // the board settle before committing to anything), then on a second press open the exit-
    // confirm dialog rather than dropping them straight to the menu. Once the dialog itself is
    // showing, further Back presses never reach here — Compose's AlertDialog owns its own back
    // handling and dismisses itself (matching Cancel) before this callback would fire again. A
    // finished game has nothing left to pause, so Back there just returns to the menu instead,
    // same as tapping GameOverOverlay's own "Main Menu" button.
    val handleBack: () -> Unit = {
        when {
            hud.isGameOver -> onExitToMenu()
            hud.isPaused -> showExitConfirm = true
            else -> surfaceView.enqueue { setPaused(true) }
        }
    }

    // Installs the gamepad gameplay handler for exactly as long as this session is playable —
    // reads showExitConfirm/hud/gamepadBindings live on every call (Compose state read by
    // reference, not captured by value), so it doesn't need to be reinstalled when any of those
    // change. While a dialog/overlay is covering the game, it defers entirely to Back/Compose's
    // own key handling instead of acting on the piece underneath — including while merely paused
    // (not just mid-dialog/game-over): every rotate/move button is a no-op against a frozen piece
    // anyway, and since the default bindings now put two rotations on the same A/B buttons the
    // paused overlay's own "Menu" button listens for as Confirm, resolving anything but Pause
    // itself here would silently eat that press before it ever reached Compose's focus handling.
    DisposableEffect(sessionId) {
        gamepadRouter.gameplayHandler = { event ->
            val action = if (showExitConfirm || hud.isGameOver) {
                null
            } else {
                resolveGamepadAction(event.keyCode, gamepadBindings.keyCodes)
                    ?.takeIf { !hud.isPaused || it == GamepadAction.Pause }
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
        gamepadRouter.backHandler = { if (showExitConfirm) showExitConfirm = false else handleBack() }
        onDispose {
            gamepadRouter.gameplayHandler = null
            gamepadRouter.backHandler = null
        }
    }

    // The exit-confirm AlertDialog below dismisses itself on Back on its own (Compose's Dialog
    // owns a back dispatcher of its own while shown, matching its Cancel button), so this only
    // ever fires while that dialog isn't up — matching the gamepad backHandler just above.
    BackHandler { if (showExitConfirm) showExitConfirm = false else handleBack() }

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
    val onTogglePause: () -> Unit = { if (!hud.isGameOver) surfaceView.enqueue { setPaused(!isPaused) } }

    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(wellBackgroundColor),
    ) {
        // Measured from the container rather than from Configuration, which cannot answer this:
        // ORIENTATION_SQUARE has been deprecated and unreturned since API 16, so a 1:1 window
        // reports ORIENTATION_PORTRAIT and would fall into the portrait branch below, where
        // `aspect` of ~1.0 collapses gridTopHeight to zero (HUD drawn straight over the well, no
        // pause band) and inflates minSpacerHeight past the container's own height (control
        // clusters pushed entirely off the bottom edge, with the Button Height knob unable to
        // pull them back). BoxWithConstraints is also the only one of the two that sees the real
        // window: screenWidthDp/screenHeightDp describe the display, not a split-screen or
        // freeform slice of it.
        val longEdge = maxOf(maxWidth, maxHeight)
        val shortEdge = minOf(maxWidth, maxHeight)
        val isSquare = shortEdge > 0.dp && longEdge / shortEdge <= SQUARE_MAX_EDGE_RATIO

        // The bottom of the Button Height range means "resting on the bottom edge", which has to
        // mean the bottom edge the player can actually reach — the gesture pill or the
        // back/home/recents bar sits below it. Read as a value rather than applied as a modifier
        // so it can go into clusterTopFor's arithmetic; a navigationBarsPadding() on the clusters
        // themselves would be counted twice.
        val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 8.dp

        if (isSquare) {
            // Neither of the other two layouts has anything to offer a square window. The
            // renderer projects the well's near opening at exactly the viewport width and the
            // opening is square, so its rendered size is min(width, height) whatever we do —
            // which means every dp the HUD or a control cluster claims for itself here is a dp
            // taken off the well. Portrait's stacked Column and landscape's pillarbox both spend
            // slack on one axis that a square window simply doesn't have. So the grid takes the
            // largest square the window allows and the HUD and clusters float over it as
            // translucent overlays instead of sitting beside it.
            val gridSize = shortEdge
            val edgeInset = 8.dp + EDGE_INSET_UNIT * settings.buttonScale * settings.portraitButtonInset
            // Bottom-anchored, so Button Height becomes a lift off the bottom edge: 0f leaves the
            // clusters in the corners this layout is built around, and raising the knob walks
            // them up off the well's bottom rows, as far as the halfway line.
            val clusterSize = CLUSTER_SIZE * settings.buttonScale
            val clusterLift = maxHeight - clusterSize -
                clusterTopFor(maxHeight, clusterSize, bottomInset, settings.portraitButtonHeight)

            AndroidView(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(gridSize)
                    .let { base ->
                        if (settings.gestureControlsEnabled) {
                            base.gestureControls(
                                onMove = onMove,
                                onRotate = onRotateAction,
                                onHardDrop = onHardDropAction,
                                // Same reasoning as the other two layouts: one physical cell's
                                // on-screen size is the natural "how far is one move" distance,
                                // and the well is wellSize cells across this square.
                                cellSize = gridSize / settings.wellSize,
                            )
                        } else {
                            base
                        }
                    },
                factory = { surfaceView },
            )

            // The HUD bar doubles as the pause target in both control schemes, since a grid this
            // close to full-bleed leaves no border band to tap. It gets a real tap-catcher laid
            // over it rather than a pause zone handed to gestureControls (portrait's approach)
            // because Material3's Surface consumes pointer input: touches landing on the bar
            // never reach the grid's gesture modifier underneath it in the first place. The
            // drag-swallowing that portrait's comment warns about isn't a concern at this size —
            // the bar is a visible piece of UI at the very top of the window, not an invisible
            // margin a move-drag would plausibly start inside.
            Box(modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth()) {
                GameHud(snapshot = hud, alpha = SQUARE_HUD_ALPHA, modifier = Modifier.fillMaxWidth())
                if (!hud.isGameOver) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .pointerInput(engine) { detectTapGestures { onTogglePause() } },
                    )
                }
            }

            if (settings.onScreenButtonsEnabled) {
                // No navigationBarsPadding here: clusterLift already has the nav bar in it via
                // bottomInset, since these rest in the bottom corners by default — exactly where
                // the gesture pill and the back/home/recents bar live.
                val clusterAlpha = settings.buttonOpacity * SQUARE_BUTTON_ALPHA
                val bottomStartModifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(horizontal = edgeInset)
                    .offset(y = -clusterLift)
                    .alpha(clusterAlpha)
                val bottomEndModifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(horizontal = edgeInset)
                    .offset(y = -clusterLift)
                    .alpha(clusterAlpha)
                val dpadModifier = if (settings.leftHandedMode) bottomEndModifier else bottomStartModifier
                val rotateModifier = if (settings.leftHandedMode) bottomStartModifier else bottomEndModifier
                MoveDPad(
                    diagonalEnabled = settings.diagonalButtonsEnabled,
                    onMove = onMove,
                    onDiagonalMove = onDiagonalMove,
                    onHardDrop = onHardDropAction,
                    modifier = dpadModifier,
                    scale = settings.buttonScale,
                )
                RotateCluster(onRotate = onRotateAction, modifier = rotateModifier, scale = settings.buttonScale)
            }
        } else if (isLandscape) {
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
            val edgeInset = 8.dp + EDGE_INSET_UNIT * settings.buttonScale * settings.landscapeButtonInset
            val clusterSize = CLUSTER_SIZE * settings.buttonScale
            // No clusters to clear once buttons are hidden — the grid can claim the full square
            // instead of leaving the side margins dead.
            val clusterClearance = if (!settings.onScreenButtonsEnabled) 0.dp else (clusterSize + edgeInset + 8.dp) * 2
            val gridWidth = minOf(maxHeight, maxWidth - clusterClearance).coerceAtLeast(0.dp)

            // Same Button Height range as the other two layouts (see [clusterTopFor]) — expressed
            // as an offset from center, since the clusters are aligned CenterStart/CenterEnd.
            val clusterTop = clusterTopFor(maxHeight, clusterSize, bottomInset, settings.landscapeButtonHeight)
            val verticalOffset = clusterTop - (maxHeight - clusterSize) / 2f

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
                    .let { base ->
                        if (settings.gestureControlsEnabled) {
                            base.gestureControls(
                                onMove = onMove,
                                onRotate = onRotateAction,
                                onHardDrop = onHardDropAction,
                                // The rendered well is `settings.wellSize` cells across this same
                                // gridWidth, so one physical cell's on-screen width is the natural
                                // "how far is one move" distance — matching what the player
                                // actually sees, not an arbitrary fixed distance.
                                cellSize = gridWidth / settings.wellSize,
                            )
                        } else {
                            base.pointerInput(engine) { detectTapGestures { onTogglePause() } }
                        }
                    },
                factory = { surfaceView },
            )

            // Gesture mode: a tap on the grid itself does nothing (it's move/rotate/drop
            // territory) — pause instead lives on the pillarbox side margins, the only "black
            // border" area landscape has, one tap-catcher per side since the grid sits centered.
            if (settings.gestureControlsEnabled) {
                val marginWidth = ((maxWidth - gridWidth) / 2f).coerceAtLeast(0.dp)
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .width(marginWidth)
                        .fillMaxHeight()
                        .pointerInput(engine) { detectTapGestures { onTogglePause() } },
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .width(marginWidth)
                        .fillMaxHeight()
                        .pointerInput(engine) { detectTapGestures { onTogglePause() } },
                )
            }

            val leftModifier = Modifier.align(Alignment.CenterStart).padding(horizontal = edgeInset).offset(y = verticalOffset)
            val rightModifier = Modifier.align(Alignment.CenterEnd).padding(horizontal = edgeInset).offset(y = verticalOffset)

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
            if (settings.onScreenButtonsEnabled) {
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
            }
        } else {
            // BlokoutRenderer's camera solves the vertical FOV so the well's near opening exactly
            // fills the viewport width; since the well is square (width == depth), that makes the
            // opening's projected height a fixed `aspect` fraction of the screen height, centered —
            // i.e. the rendered grid occupies the vertical band [0.5 - aspect/2, 0.5 + aspect/2].
            val containerHeight = maxHeight
            val aspect = maxWidth.value / maxHeight.value
            val gridTopHeight = containerHeight * (0.5f - aspect / 2f)

            // In gesture mode the pause-tap zone (the border above/below the rendered grid) is
            // handled by the same full-screen modifier as move/rotate/drop below, rather than a
            // separate element layered on top of it — Android/Compose hands an entire gesture to
            // whichever element first hit-tested its touch-down, so a drag starting a few pixels
            // into the border (easy to do near the grid's edge) would otherwise be swallowed by a
            // separate border-only tap catcher even once the finger moved well into the grid,
            // making ordinary moves feel like they need a much bigger drag than they actually do.
            val density = LocalDensity.current
            val gridTopHeightPx = with(density) { gridTopHeight.toPx() }
            val gridBottomYPx = with(density) { (containerHeight - gridTopHeight).toPx() }

            AndroidView(
                modifier = Modifier
                    .fillMaxSize()
                    .let { base ->
                        if (settings.gestureControlsEnabled) {
                            base.gestureControls(
                                onMove = onMove,
                                onRotate = onRotateAction,
                                onHardDrop = onHardDropAction,
                                // The rendered grid fills the full screen width here (see the
                                // camera comment above), so that's the pixel width to divide by
                                // wellSize for one cell's actual on-screen size.
                                cellSize = maxWidth / settings.wellSize,
                                isInPauseZone = { pos -> pos.y < gridTopHeightPx || pos.y > gridBottomYPx },
                                onTogglePause = onTogglePause,
                            )
                        } else {
                            base
                        }
                    },
                factory = { surfaceView },
            )

            GameHud(snapshot = hud, modifier = Modifier.fillMaxWidth())

            // Tapping anywhere above the grid (the HUD's band included) pauses/unpauses — placed
            // before the controls/overlays below so it never steals taps meant for them, and it's
            // a no-op once the game has ended (pausing a finished game doesn't mean anything).
            // Button-mode only: gesture mode's pause-tap is handled above, inside gestureControls.
            if (!hud.isGameOver && !settings.gestureControlsEnabled) {
                Spacer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(gridTopHeight)
                        .pointerInput(engine) {
                            detectTapGestures { onTogglePause() }
                        },
                )
            }

            if (settings.onScreenButtonsEnabled) {
                // The spacer's height is the cluster's top edge, so Button Height maps straight
                // onto it (see [clusterTopFor]). On a typical phone 0f lands the controls just
                // below the grid, which is where this used to start its range from directly — the
                // difference is that the top of the range is now the halfway line rather than the
                // grid's bottom edge, so the knob still travels somewhere useful on a screen whose
                // grid reaches much further down.
                val clusterSize = CLUSTER_SIZE * settings.buttonScale
                val spacerHeight = clusterTopFor(containerHeight, clusterSize, bottomInset, settings.portraitButtonHeight)
                val portraitEdgeInset = 8.dp + EDGE_INSET_UNIT * settings.buttonScale * settings.portraitButtonInset
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
                            .padding(horizontal = portraitEdgeInset),
                    )
                }
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

/** Where a control cluster's top edge sits for a given Button Height [fraction], shared by all
 *  three layouts so the knob means the same thing on any screen size or orientation: 0f rests the
 *  cluster on the bottom edge (clear of the navigation bar) and 1f raises it until it straddles
 *  the container's vertical midpoint — halfway up the screen, measured by where the cluster sits
 *  rather than by its top edge, which on a short landscape window would otherwise leave the knob
 *  almost no room to travel at all.
 *
 *  Anchoring the top of the travel to the container rather than to the layout's own geometry is
 *  the point: the range used to stop at the bottom of the rendered grid, which is a different
 *  place on every device (portrait solved it from the aspect ratio) and nowhere at all on a square
 *  screen, where the grid runs clear to the bottom edge.
 *
 *  Note that 1f works out to exactly vertically centered, since a cluster centered on the midpoint
 *  is a cluster centered in the container — which is what landscape's Button Height default has
 *  always meant, and why that default is 1f rather than 0.5f.
 *
 *  [coerceAtLeast] keeps the range from inverting on a container too short to hold a cluster below
 *  its midpoint — there the knob has nowhere to travel and every value lands centered. */
private fun clusterTopFor(containerHeight: Dp, clusterHeight: Dp, bottomInset: Dp, fraction: Float): Dp {
    val highest = (containerHeight - clusterHeight) / 2f
    val lowest = (containerHeight - bottomInset - clusterHeight).coerceAtLeast(highest)
    return lerp(lowest, highest, fraction)
}
