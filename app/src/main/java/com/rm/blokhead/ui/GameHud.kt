package com.rm.blokhead.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
)

/** Score/level/cubes readout pinned to the top of the screen, standing in for the original's
 *  scoreDisplay() sidebar (counter.c's odometer-style digit widgets are simplified to plain
 *  text here). */
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
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            HudStat("SCORE", snapshot.score.toString())
            HudStat("LEVEL", snapshot.level.toString())
            HudStat("CUBES", snapshot.cubesDropped.toString())
        }
    }
}

@Composable
private fun HudStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
    }
}

/** Full-screen scrim shown once [GameEngine.isGameOver], standing in for the original's
 *  endGame()/high-score-table flow — high scores themselves land in a later pass. */
@Composable
fun GameOverOverlay(finalScore: Int, onPlayAgain: () -> Unit, modifier: Modifier = Modifier) {
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
        }
    }
}
