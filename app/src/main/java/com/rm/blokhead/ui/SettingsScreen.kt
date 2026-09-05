package com.rm.blokhead.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rm.blokhead.data.Settings
import com.rm.blokhead.game.BlockSet
import kotlin.math.roundToInt

/** Settings screen — new, standing in for nothing in the original beyond a couple of hardcoded
 *  compile-time globals it never exposed as an in-game menu. */
@Composable
fun SettingsScreen(
    settings: Settings,
    onSettingsChange: (Settings) -> Unit,
    onBack: () -> Unit,
    onShowGamepadBindings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
    ) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 8.dp),
        )

        GroupHeader("Gameplay & Sound", topPadding = 8.dp)
        SwitchRow(
            title = "Sound",
            subtitle = "Move/rotate/lock/clear/game-over effects",
            checked = settings.soundEnabled,
            onCheckedChange = { onSettingsChange(settings.copy(soundEnabled = it)) },
        )
        IntSliderRow(
            title = "Starting Difficulty",
            value = settings.startingDifficulty,
            range = 1..9,
            onValueChange = { onSettingsChange(settings.copy(startingDifficulty = it)) },
        )
        IntSliderRow(
            title = "Well Size",
            value = settings.wellSize,
            range = 4..7,
            valueLabel = { "${it}×$it" },
            onValueChange = { onSettingsChange(settings.copy(wellSize = it)) },
        )
        IntSliderRow(
            title = "Well Height",
            value = settings.wellHeight,
            range = 10..24,
            onValueChange = { onSettingsChange(settings.copy(wellHeight = it)) },
        )
        SectionLabel("Block Set")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BlockSetChip("Flat", BlockSet.FLAT, settings, onSettingsChange)
            BlockSetChip("Extended", BlockSet.EXTENDED, settings, onSettingsChange)
            BlockSetChip("All", BlockSet.ALL, settings, onSettingsChange)
        }

        HorizontalDivider(modifier = Modifier.padding(top = 24.dp), color = MaterialTheme.colorScheme.outlineVariant)
        GroupHeader("Controls")
        SwitchRow(
            title = "Diagonal D-Pad Corners",
            subtitle = "Adds diagonal move buttons to the move d-pad's empty corners",
            checked = settings.diagonalButtonsEnabled,
            onCheckedChange = { onSettingsChange(settings.copy(diagonalButtonsEnabled = it)) },
        )
        SwitchRow(
            title = "Left-Handed Mode",
            subtitle = "Swaps the move and rotate control clusters to opposite sides",
            checked = settings.leftHandedMode,
            onCheckedChange = { onSettingsChange(settings.copy(leftHandedMode = it)) },
        )
        SectionLabel("Button Position")
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Below Grid", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Bottom Edge", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Slider(
            value = settings.buttonVerticalPosition,
            onValueChange = { onSettingsChange(settings.copy(buttonVerticalPosition = it)) },
        )
        PercentSliderRow(
            title = "Button Scale",
            // Each control cluster is a fixed ~156.dp-wide 3-column grid of true circles (see
            // GameControls.kt's requiredSize use — buttons never squish to fit, so on a ~360.dp-
            // wide phone both clusters combined only have room up to about 112% before the
            // rightmost column would clip off the edge). Capped at 110% for a safety margin.
            value = settings.buttonScale,
            range = 0.7f..1.1f,
            onValueChange = { onSettingsChange(settings.copy(buttonScale = it)) },
        )
        PercentSliderRow(
            title = "Button Opacity",
            value = settings.buttonOpacity,
            // Floored above 0 so the buttons never fade all the way to invisible/untappable-by-
            // sight — they'd still technically receive taps, but there'd be nothing to tap on.
            range = 0.2f..1f,
            onValueChange = { onSettingsChange(settings.copy(buttonOpacity = it)) },
        )
        OutlinedButton(
            onClick = onShowGamepadBindings,
            modifier = Modifier
                .padding(top = 20.dp)
                .gamepadFocusable(onActivate = onShowGamepadBindings),
        ) {
            Text("Map Controls")
        }

        OutlinedButton(
            onClick = onBack,
            modifier = Modifier
                .padding(top = 24.dp)
                .gamepadFocusable(onActivate = onBack),
        ) {
            Text("Back")
        }
    }
}

@Composable
private fun GroupHeader(text: String, topPadding: Dp = 0.dp) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = topPadding, bottom = 4.dp),
    )
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.padding(top = 20.dp, bottom = 8.dp),
    )
}

@Composable
private fun SwitchRow(title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onBackground)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun IntSliderRow(
    title: String,
    value: Int,
    range: IntRange,
    valueLabel: (Int) -> String = { it.toString() },
    onValueChange: (Int) -> Unit,
) {
    SectionLabel("$title: ${valueLabel(value)}")
    Slider(
        value = value.toFloat(),
        onValueChange = { onValueChange(it.roundToInt()) },
        valueRange = range.first.toFloat()..range.last.toFloat(),
        steps = (range.last - range.first - 1).coerceAtLeast(0),
    )
}

@Composable
private fun PercentSliderRow(title: String, value: Float, range: ClosedFloatingPointRange<Float>, onValueChange: (Float) -> Unit) {
    SectionLabel("$title: ${(value * 100).roundToInt()}%")
    Slider(value = value, onValueChange = onValueChange, valueRange = range)
}

@Composable
private fun BlockSetChip(label: String, value: BlockSet, settings: Settings, onSettingsChange: (Settings) -> Unit) {
    FilterChip(
        selected = settings.blockSet == value,
        onClick = { onSettingsChange(settings.copy(blockSet = value)) },
        label = { Text(label) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
    )
}
