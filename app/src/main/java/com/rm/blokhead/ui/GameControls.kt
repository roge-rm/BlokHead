package com.rm.blokhead.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
 *  arrow keys -> move (X/Y), Q/A W/S D/E -> rotate (X/Y/Z, +/-), space -> hard drop. */
@Composable
fun GameControls(
    onMove: (axis: Int, sign: Int) -> Unit,
    onRotate: (axis: Int, sign: Int) -> Unit,
    onHardDrop: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom,
    ) {
        MoveDPad(onMove)
        DropButton(onHardDrop)
        RotateCluster(onRotate)
    }
}

@Composable
private fun MoveDPad(onMove: (axis: Int, sign: Int) -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        RoundButton("▲") { onMove(Axis.Y, 1) }
        Row(horizontalArrangement = Arrangement.spacedBy(GAP)) {
            RoundButton("◀") { onMove(Axis.X, -1) }
            BlankCell()
            RoundButton("▶") { onMove(Axis.X, 1) }
        }
        RoundButton("▼") { onMove(Axis.Y, -1) }
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
private fun DropButton(onHardDrop: () -> Unit) {
    FilledTonalButton(
        onClick = onHardDrop,
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        ),
    ) {
        Text("DROP")
    }
}

@Composable
private fun RoundButton(label: String, size: Dp = CELL, onClick: () -> Unit) {
    FilledTonalButton(
        onClick = onClick,
        shape = CircleShape,
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
