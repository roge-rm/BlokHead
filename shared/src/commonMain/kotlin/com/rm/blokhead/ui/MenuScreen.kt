package com.rm.blokhead.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.rm.blokhead.render.wellBackgroundColor

/** The title screen, standing in for the original's initial menu.c state before startGame(). */
@Composable
fun MenuScreen(
    onStartGame: () -> Unit,
    onShowHighScores: () -> Unit,
    onShowSettings: () -> Unit,
    onShowAbout: () -> Unit,
    bestScore: Int?,
    wellSize: Int,
    wellHeight: Int,
    modifier: Modifier = Modifier,
    /** A short line under the buttons, e.g. the browser build's keyboard controls. */
    footnote: String? = null,
) {
    val startGameFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { startGameFocus.requestFocus() }
    val goldCubes = remember(wellSize, wellHeight) { goldLPiece(wellSize, wellHeight) }
    val meta = if (bestScore != null) {
        "Best ${formatScore(bestScore)} · ${wellSize}×${wellSize}"
    } else {
        "${wellSize}×${wellSize}"
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(wellBackgroundColor)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        val landscape = maxWidth > maxHeight
        if (landscape) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                WellPreview(
                    wellSize = wellSize,
                    wellHeight = wellHeight,
                    cubes = goldCubes,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                )
                MenuActions(
                    onStartGame = onStartGame,
                    onShowHighScores = onShowHighScores,
                    onShowSettings = onShowSettings,
                    onShowAbout = onShowAbout,
                    startGameFocus = startGameFocus,
                    meta = meta,
                    footnote = footnote,
                    modifier = Modifier.weight(1f),
                    buttonWidthFraction = 0.9f,
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 32.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                MenuActions(
                    onStartGame = onStartGame,
                    onShowHighScores = onShowHighScores,
                    onShowSettings = onShowSettings,
                    onShowAbout = onShowAbout,
                    startGameFocus = startGameFocus,
                    meta = meta,
                    footnote = footnote,
                    modifier = Modifier.fillMaxSize(),
                    well = {
                        WellPreview(
                            wellSize = wellSize,
                            wellHeight = wellHeight,
                            cubes = goldCubes,
                            modifier = Modifier.fillMaxSize(),
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun MenuActions(
    onStartGame: () -> Unit,
    onShowHighScores: () -> Unit,
    onShowSettings: () -> Unit,
    onShowAbout: () -> Unit,
    startGameFocus: FocusRequester,
    meta: String,
    footnote: String?,
    modifier: Modifier = Modifier,
    buttonWidthFraction: Float = 0.7f,
    well: (@Composable () -> Unit)? = null,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = if (well != null) Arrangement.Top else Arrangement.Center,
    ) {
        Text(
            text = "BlokHead",
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = meta,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp),
        )
        if (well != null) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                well()
            }
        }
        val buttonMod = Modifier.fillMaxWidth(buttonWidthFraction).widthIn(max = 360.dp)
        Button(
            onClick = onStartGame,
            colors = tealButtonColors(),
            modifier = buttonMod
                .focusRequester(startGameFocus)
                .gamepadFocusable(onActivate = onStartGame),
        ) {
            Text("Start Game")
        }
        OutlinedButton(
            onClick = onShowHighScores,
            colors = ghostButtonColors(),
            border = GhostBorder,
            modifier = buttonMod
                .padding(top = 12.dp)
                .gamepadFocusable(onActivate = onShowHighScores),
        ) {
            Text("High Scores")
        }
        OutlinedButton(
            onClick = onShowSettings,
            colors = ghostButtonColors(),
            border = GhostBorder,
            modifier = buttonMod
                .padding(top = 12.dp)
                .gamepadFocusable(onActivate = onShowSettings),
        ) {
            Text("Settings")
        }
        OutlinedButton(
            onClick = onShowAbout,
            colors = ghostButtonColors(),
            border = GhostBorder,
            modifier = buttonMod
                .padding(top = 12.dp)
                .gamepadFocusable(onActivate = onShowAbout),
        ) {
            Text("About")
        }
        if (footnote != null) {
            Text(
                text = footnote,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(0.85f).padding(top = 16.dp),
            )
        }
    }
}

private val GoldTop = Color(0xFFFFE08A)
private val GoldLeft = Color(0xFFC9871A)
private val GoldRight = Color(0xFFFFC93C)

private fun goldLPiece(wellSize: Int, wellHeight: Int): List<PreviewCube> {
    val cx = (wellSize / 2 - 1).coerceAtLeast(0)
    val cy = (wellSize / 2).coerceAtMost(wellSize - 2)
    val cz = (previewDrawHeight(wellHeight) - 2).coerceAtLeast(1)
    return listOf(
        PreviewCube(cx, cy, cz, GoldTop, GoldLeft, GoldRight),
        PreviewCube(cx, cy + 1, cz, GoldTop, GoldLeft, GoldRight),
        PreviewCube(cx + 1, cy + 1, cz, GoldTop, GoldLeft, GoldRight),
    )
}

/** Groups of three digits from the right, so it works on every target (no JVM NumberFormat). */
internal fun formatScore(score: Int): String {
    val digits = score.toString()
    val out = StringBuilder(digits.length + digits.length / 3)
    digits.forEachIndexed { index, c ->
        if (index > 0 && (digits.length - index) % 3 == 0) out.append(',')
        out.append(c)
    }
    return out.toString()
}
