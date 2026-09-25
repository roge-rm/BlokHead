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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/** The title screen, standing in for the original's initial menu.c state before startGame(). */
@Composable
fun MenuScreen(
    onStartGame: () -> Unit,
    onShowHighScores: () -> Unit,
    onShowSettings: () -> Unit,
    onShowAbout: () -> Unit,
    modifier: Modifier = Modifier,
    /** A short line under the buttons, e.g. the browser build's keyboard controls. */
    footnote: String? = null,
) {
    val startGameFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { startGameFocus.requestFocus() }

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
        Button(
            onClick = onStartGame,
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .focusRequester(startGameFocus)
                .gamepadFocusable(onActivate = onStartGame),
        ) {
            Text("Start Game")
        }
        OutlinedButton(
            onClick = onShowHighScores,
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .padding(top = 12.dp)
                .gamepadFocusable(onActivate = onShowHighScores),
        ) {
            Text("High Scores")
        }
        OutlinedButton(
            onClick = onShowSettings,
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .padding(top = 12.dp)
                .gamepadFocusable(onActivate = onShowSettings),
        ) {
            Text("Settings")
        }
        OutlinedButton(
            onClick = onShowAbout,
            modifier = Modifier
                .fillMaxWidth(0.7f)
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
                modifier = Modifier.fillMaxWidth(0.85f).padding(top = 32.dp),
            )
        }
    }
}
