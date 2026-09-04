package com.rm.blokhead.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rm.blokhead.data.HighScoreEntry

/** Ranked score list, standing in for the original's drawScoreTableOverlay()/highScoreDisplay()
 *  (highscoreui.c) — those rendered the table as an OpenGL overlay; this is a plain Compose list
 *  reading from [com.rm.blokhead.data.HighScoreStore]. */
@Composable
fun HighScoreScreen(entries: List<HighScoreEntry>, onBack: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        Text(
            text = "High Scores",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(24.dp),
        )
        if (entries.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth().padding(24.dp)) {
                Text(
                    text = "No scores yet — be the first to drop a block.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 24.dp),
            ) {
                itemsIndexed(entries) { index, entry ->
                    HighScoreRow(rank = index + 1, entry = entry)
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        }
        OutlinedButton(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(24.dp),
        ) {
            Text("Back")
        }
    }
}

@Composable
private fun HighScoreRow(rank: Int, entry: HighScoreEntry) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = "$rank. ${entry.name.ifBlank { "—" }}",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = entry.score.toString(),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
