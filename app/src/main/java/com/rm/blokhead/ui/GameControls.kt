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

/** Rotate cluster laid out as a 3x3 grid: X/Y sit where a D-pad would put left/right/up/down,
 *  and the two Z rotations fill the opposite diagonal corners.
 *
 *  ```
 *      Y+ Z+
 *  X-      X+
 *  Z- Y-
 *  ```
 */
@Composable
private fun RotateCluster(onRotate: (axis: Int, sign: Int) -> Unit) {
    val cell = 40.dp
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            RotateSpacer(cell)
            RoundButton("Y+", size = cell) { onRotate(Axis.Y, 1) }
            RoundButton("Z+", size = cell) { onRotate(Axis.Z, 1) }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            RoundButton("X−", size = cell) { onRotate(Axis.X, -1) }
            RotateSpacer(cell)
            RoundButton("X+", size = cell) { onRotate(Axis.X, 1) }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            RoundButton("Z−", size = cell) { onRotate(Axis.Z, -1) }
            RoundButton("Y−", size = cell) { onRotate(Axis.Y, -1) }
            RotateSpacer(cell)
        }
    }
}

@Composable
private fun RotateSpacer(size: androidx.compose.ui.unit.Dp) {
    androidx.compose.foundation.layout.Spacer(Modifier.size(size))
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
