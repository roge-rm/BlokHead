package com.rm.blokhead.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.unit.dp
import com.rm.blokhead.game.Axis

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
        Row {
            RoundButton("◀") { onMove(Axis.X, -1) }
            Spacer()
            RoundButton("▶") { onMove(Axis.X, 1) }
        }
        RoundButton("▼") { onMove(Axis.Y, -1) }
    }
}

/** Rotate cluster laid out as a 3x3 grid, sized and spaced to mirror [MoveDPad] exactly (same
 *  button size, same middle-row height) — X sits where the D-pad puts up/down, Y where it puts
 *  left/right, and the two Z rotations fill the opposite diagonal corners.
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
        Row(verticalAlignment = Alignment.CenterVertically) {
            Spacer()
            RoundButton("X+") { onRotate(Axis.X, 1) }
            RoundButton("Z+") { onRotate(Axis.Z, 1) }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            RoundButton("Y−") { onRotate(Axis.Y, -1) }
            Spacer()
            RoundButton("Y+") { onRotate(Axis.Y, 1) }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            RoundButton("Z−") { onRotate(Axis.Z, -1) }
            RoundButton("X−") { onRotate(Axis.X, -1) }
            Spacer()
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
private fun RoundButton(label: String, size: androidx.compose.ui.unit.Dp = 48.dp, onClick: () -> Unit) {
    FilledTonalButton(
        onClick = onClick,
        shape = CircleShape,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
        modifier = Modifier.size(size),
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun Spacer() {
    androidx.compose.foundation.layout.Spacer(Modifier.size(48.dp))
}
