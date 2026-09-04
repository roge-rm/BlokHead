package com.rm.blokhead.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** The title screen, standing in for the original's initial menu.c state before startGame(). */
@Composable
fun MenuScreen(onStartGame: () -> Unit, onShowHighScores: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "BlokHead",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = "A Falling Block Game",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, bottom = 48.dp),
        )
        Button(onClick = onStartGame, modifier = Modifier.fillMaxWidth(0.7f)) {
            Text("Start Game")
        }
        OutlinedButton(
            onClick = onShowHighScores,
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .padding(top = 12.dp),
        ) {
            Text("High Scores")
        }
    }
}
