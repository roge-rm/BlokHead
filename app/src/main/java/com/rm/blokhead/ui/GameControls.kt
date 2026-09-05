package com.rm.blokhead.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rm.blokhead.game.Axis

/** Every round control button is this size at the default 100% [Settings.buttonScale]; [GAP] is
 *  the breathing room between adjacent cells in a row, real or blank, so a row of e.g. two
 *  buttons never has them touching. Both [MoveDPad] and [RotateCluster] build their rows from
 *  these same two constants (scaled by their own `scale` parameter) so the two clusters stay
 *  true mirrors of each other at any size. */
private val CELL = 48.dp
private val GAP = 6.dp

/** Move/rotate/drop controls, standing in for the original's keyboard scheme in control.c:
 *  arrow keys -> move (X/Y), Q/A W/S D/E -> rotate (X/Y/Z, +/-), space -> hard drop. Two
 *  corner-anchored clusters (move d-pad left, rotate cluster right by default — swapped by
 *  [leftHanded]) rather than three separate groups with a dedicated drop button in the dead zone
 *  between them — hard drop instead lives in the d-pad's own center cell, since it's the same
 *  thumb doing the moving and dropping. */
@Composable
fun GameControls(
    onMove: (axis: Int, sign: Int) -> Unit,
    onDiagonalMove: (xSign: Int, ySign: Int) -> Unit,
    onRotate: (axis: Int, sign: Int) -> Unit,
    onHardDrop: () -> Unit,
    diagonalEnabled: Boolean = false,
    leftHanded: Boolean = false,
    opacity: Float = 1f,
    scale: Float = 1f,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().alpha(opacity),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val dpad = @Composable { MoveDPad(diagonalEnabled, onMove, onDiagonalMove, onHardDrop, scale = scale) }
        val rotate = @Composable { RotateCluster(onRotate, scale = scale) }
        if (leftHanded) {
            rotate()
            dpad()
        } else {
            dpad()
            rotate()
        }
    }
}

/** Move d-pad, optionally with diagonal buttons filling the corners (blank when disabled, so
 *  the up/down buttons stay in the exact same spot either way). Public (not `private`, unlike
 *  the rest of this file's helpers) so the landscape layout in MainActivity.kt can place it
 *  standalone at a screen edge instead of only via [GameControls]' single full-width row. */
@Composable
fun MoveDPad(
    diagonalEnabled: Boolean,
    onMove: (axis: Int, sign: Int) -> Unit,
    onDiagonalMove: (xSign: Int, ySign: Int) -> Unit,
    onHardDrop: () -> Unit,
    modifier: Modifier = Modifier,
    scale: Float = 1f,
) {
    val cell = CELL * scale
    val gap = GAP * scale
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(gap),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
            if (diagonalEnabled) RoundButton("↖", size = cell) { onDiagonalMove(-1, 1) } else BlankCell(cell)
            RoundButton("▲", size = cell) { onMove(Axis.Y, 1) }
            if (diagonalEnabled) RoundButton("↗", size = cell) { onDiagonalMove(1, 1) } else BlankCell(cell)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
            RoundButton("◀", size = cell) { onMove(Axis.X, -1) }
            RoundButton(
                "⏬",
                size = cell,
                // Faded relative to the other buttons — it's still a different color so it
                // reads as a distinct kind of action, just not shouting over move/rotate.
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.55f),
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.85f),
                ),
                onClick = onHardDrop,
            )
            RoundButton("▶", size = cell) { onMove(Axis.X, 1) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
            if (diagonalEnabled) RoundButton("↙", size = cell) { onDiagonalMove(-1, -1) } else BlankCell(cell)
            RoundButton("▼", size = cell) { onMove(Axis.Y, -1) }
            if (diagonalEnabled) RoundButton("↘", size = cell) { onDiagonalMove(1, -1) } else BlankCell(cell)
        }
    }
}

/** Rotate cluster laid out as a 3x3 grid, sized and spaced to mirror [MoveDPad] exactly (same
 *  button size, same gaps, same overall footprint) — X sits where the D-pad puts up/down, Y
 *  where it puts left/right, and the two Z rotations fill the opposite diagonal corners, the
 *  same size as X/Y, with the same gap separating every cell.
 *
 *  ```
 *      X+ Z+
 *  Y-      Y+
 *  Z- X-
 *  ```
 *
 *  Public for the same reason as [MoveDPad] — reused standalone in the landscape layout. */
@Composable
fun RotateCluster(
    onRotate: (axis: Int, sign: Int) -> Unit,
    modifier: Modifier = Modifier,
    scale: Float = 1f,
) {
    val cell = CELL * scale
    val gap = GAP * scale
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(gap),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
            BlankCell(cell)
            RoundButton("X+", size = cell) { onRotate(Axis.X, 1) }
            RoundButton("Z+", size = cell) { onRotate(Axis.Z, 1) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
            RoundButton("Y−", size = cell) { onRotate(Axis.Y, -1) }
            BlankCell(cell)
            RoundButton("Y+", size = cell) { onRotate(Axis.Y, 1) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
            RoundButton("Z−", size = cell) { onRotate(Axis.Z, -1) }
            RoundButton("X−", size = cell) { onRotate(Axis.X, -1) }
            BlankCell(cell)
        }
    }
}

@Composable
private fun RoundButton(
    label: String,
    size: Dp = CELL,
    colors: ButtonColors = ButtonDefaults.filledTonalButtonColors(),
    onClick: () -> Unit,
) {
    FilledTonalButton(
        onClick = onClick,
        shape = CircleShape,
        colors = colors,
        contentPadding = PaddingValues(0.dp),
        // requiredSize, not size: if a row runs short on width (e.g. Button Scale pushed a
        // cluster wider than its share of the screen), `size()` lets the incoming width
        // constraint override the requested one while height stays unconstrained — squishing
        // the button into a horizontal oval instead of clipping it. requiredSize keeps every
        // button a true circle no matter what the parent offers, even if that means part of a
        // cluster runs off the edge.
        modifier = Modifier.requiredSize(size),
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge)
    }
}

/** An empty grid cell, the same size as a real button, so blank corners/centers keep their row
 *  aligned with the others. */
@Composable
private fun BlankCell(size: Dp = CELL) {
    androidx.compose.foundation.layout.Spacer(Modifier.requiredSize(size))
}
