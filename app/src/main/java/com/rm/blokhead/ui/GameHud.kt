package com.rm.blokhead.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** A snapshot of the fields [GameEngine] exposes for display, read on a polling timer from
 *  MainActivity (see its doc for why polling rather than a proper state stream). */
data class HudSnapshot(
    val score: Int,
    val level: Int,
    val cubesDropped: Int,
    val isGameOver: Boolean,
    val isPaused: Boolean,
)

/** Score/level/cubes readout pinned to the top of the screen, standing in for the original's
 *  scoreDisplay() sidebar (counter.c's odometer-style digit widgets are simplified to plain
 *  text here). The colored surface itself runs all the way to the top edge, under the status
 *  bar, so the bar doesn't cut a hard seam into it — only the text content is inset below the
 *  status bar so it doesn't overlap the clock/battery icons. */
@Composable
fun GameHud(snapshot: HudSnapshot, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f),
        shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            HudStat("SCORE", snapshot.score.toString())
            HudStat("LEVEL", snapshot.level.toString())
            HudStat("CUBES", snapshot.cubesDropped.toString())
        }
    }
}

/** Shown while [GameEngine.isPaused] — dims the (still-visible-through-it) grid behind it and
 *  labels the frozen state. Tapping the same top-of-screen zone that triggered the pause
 *  unpauses; this overlay itself doesn't intercept taps, so that gesture keeps working while
 *  it's shown. */
@Composable
fun PausedOverlay(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.75f)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "PAUSED",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}

@Composable
private fun HudStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
    }
}

/** Full-screen scrim shown once [GameEngine.isGameOver] and the score has been resolved (either
 *  it didn't qualify for the high-score table, or [NameEntryOverlay] already saved it). Standing
 *  in for the original's endGame() menu re-entry. */
@Composable
fun GameOverOverlay(
    finalScore: Int,
    onPlayAgain: () -> Unit,
    onMainMenu: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.9f)),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "GAME OVER",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = "Score: $finalScore",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp, bottom = 24.dp),
            )
            Button(onClick = onPlayAgain) {
                Text("Play Again")
            }
            OutlinedButton(
                onClick = onMainMenu,
                modifier = Modifier.padding(top = 12.dp),
            ) {
                Text("Main Menu")
            }
        }
    }
}

/** Shown instead of [GameOverOverlay] when the just-finished score qualifies for the high-score
 *  table, standing in for the original's beginHighScore()/highScoreDisplay() name-entry flow
 *  (highscoreui.c). */
@Composable
fun NameEntryOverlay(finalScore: Int, onSubmit: (name: String) -> Unit, modifier: Modifier = Modifier) {
    var name by remember { mutableStateOf("") }
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.9f)),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "NEW HIGH SCORE!",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = "Score: $finalScore",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp, bottom = 24.dp),
            )
            OutlinedTextField(
                value = name,
                onValueChange = { if (it.length <= 24) name = it },
                label = { Text("Your name") },
                singleLine = true,
            )
            Button(
                onClick = { onSubmit(name.trim().ifBlank { "Player" }) },
                modifier = Modifier.padding(top = 16.dp),
            ) {
                Text("Save")
            }
        }
    }
}
