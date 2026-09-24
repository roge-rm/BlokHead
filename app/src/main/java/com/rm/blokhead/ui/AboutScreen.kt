package com.rm.blokhead.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.unit.dp

/** What the game is, where it came from, and the GPL notice. Back sits on the title row rather
 *  than at the bottom so it's reachable without scrolling, and the column is width-capped since
 *  a full-width line of text on a landscape phone is hard to read. */
@Composable
fun AboutScreen(versionName: String, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val backFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { backFocus.requestFocus() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(modifier = Modifier.widthIn(max = 560.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "BlokHead $versionName",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                OutlinedButton(
                    onClick = onBack,
                    modifier = Modifier
                        .focusRequester(backFocus)
                        .gamepadFocusable(onActivate = onBack),
                ) {
                    Text("Back")
                }
            }
            AboutParagraph(
                "An Android port of Blokout, the open source 3D falling block game based on the " +
                    "classic Blockout. The game code was ported over from Blokout so it plays the " +
                    "way the original does, then tweaked to be more customizable and quicker to " +
                    "play. The graphics and the UI have been redone for mobile."
            )
            AboutParagraph(
                "It hasn't been tested much on different shaped devices, so if something looks " +
                    "wrong on yours, please let me know."
            )
            AboutHeading("Copyright")
            AboutParagraph(
                "Blokout is Copyright (C) 1998-1999 Johannes Lehtinen and Petri Salmi.\n" +
                    "Android port Copyright (C) 2026 Dan Hunke."
            )
            AboutHeading("Licence")
            AboutParagraph(
                "This program is free software: you can redistribute it and/or modify it under " +
                    "the terms of the GNU General Public License as published by the Free " +
                    "Software Foundation, either version 3 of the License, or (at your option) " +
                    "any later version.\n\n" +
                    "This program is distributed in the hope that it will be useful, but WITHOUT " +
                    "ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or " +
                    "FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for " +
                    "more details: gnu.org/licenses/gpl-3.0.html"
            )
            AboutHeading("Source code")
            AboutParagraph(
                "The full source for this build:\n\n" +
                    "github.com/roge-rm/BlokHead\n\n" +
                    "Blokout's original source is at github.com/jlehtine/blokout."
            )
            AboutHeading("Support")
            AboutParagraph(
                "Questions, bug reports, suggestions? Check out the #blokhead channel on " +
                    "my discord:\n\n" +
                    "discord.gg/9Wun47jGC6"
            )
        }
    }
}

@Composable
private fun AboutHeading(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 20.dp),
    )
}

@Composable
private fun AboutParagraph(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 8.dp),
    )
}
