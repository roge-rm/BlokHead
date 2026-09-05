package com.rm.blokhead.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rm.blokhead.game.Axis

/** Every round control button is this size; [GAP] is the breathing room between adjacent cells
 *  in a row, real or blank, so a row of e.g. two buttons never has them touching. Both
 *  [MoveDPad] and [RotateCluster] build their rows from these same two constants so the two
 *  clusters are true mirrors of each other. */
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
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val dpad = @Composable { MoveDPad(diagonalEnabled, onMove, onDiagonalMove, onHardDrop) }
        val rotate = @Composable { RotateCluster(onRotate) }
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
 *  the up/down buttons stay in the exact same spot either way). */
@Composable
private fun MoveDPad(
    diagonalEnabled: Boolean,
    onMove: (axis: Int, sign: Int) -> Unit,
    onDiagonalMove: (xSign: Int, ySign: Int) -> Unit,
    onHardDrop: () -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(horizontalArrangement = Arrangement.spacedBy(GAP)) {
            if (diagonalEnabled) RoundButton("↖") { onDiagonalMove(-1, 1) } else BlankCell()
            RoundButton("▲") { onMove(Axis.Y, 1) }
            if (diagonalEnabled) RoundButton("↗") { onDiagonalMove(1, 1) } else BlankCell()
        }
        Row(horizontalArrangement = Arrangement.spacedBy(GAP)) {
            RoundButton("◀") { onMove(Axis.X, -1) }
            RoundButton(
                "⏬",
                // Faded relative to the other buttons — it's still a different color so it
                // reads as a distinct kind of action, just not shouting over move/rotate.
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.55f),
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.85f),
                ),
                onClick = onHardDrop,
            )
            RoundButton("▶") { onMove(Axis.X, 1) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(GAP)) {
            if (diagonalEnabled) RoundButton("↙") { onDiagonalMove(-1, -1) } else BlankCell()
            RoundButton("▼") { onMove(Axis.Y, -1) }
            if (diagonalEnabled) RoundButton("↘") { onDiagonalMove(1, -1) } else BlankCell()
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
 */
@Composable
private fun RotateCluster(onRotate: (axis: Int, sign: Int) -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(horizontalArrangement = Arrangement.spacedBy(GAP)) {
            BlankCell()
            RoundButton("X+") { onRotate(Axis.X, 1) }
            RoundButton("Z+") { onRotate(Axis.Z, 1) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(GAP)) {
            RoundButton("Y−") { onRotate(Axis.Y, -1) }
            BlankCell()
            RoundButton("Y+") { onRotate(Axis.Y, 1) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(GAP)) {
            RoundButton("Z−") { onRotate(Axis.Z, -1) }
            RoundButton("X−") { onRotate(Axis.X, -1) }
            BlankCell()
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
        modifier = Modifier.size(size),
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge)
    }
}

/** An empty grid cell, the same size as a real button, so blank corners/centers keep their row
 *  aligned with the others. */
@Composable
private fun BlankCell() {
    androidx.compose.foundation.layout.Spacer(Modifier.size(CELL))
}
