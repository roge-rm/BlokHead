package com.rm.blokhead.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.rm.blokhead.ui.theme.AxisStroke
import com.rm.blokhead.ui.theme.BrandCyan80
import com.rm.blokhead.ui.theme.FilledTeal
import com.rm.blokhead.ui.theme.OnFilledTeal

internal val GhostBorder = BorderStroke(1.dp, AxisStroke.copy(alpha = 0.55f))

@Composable
internal fun ghostButtonColors() = ButtonDefaults.outlinedButtonColors(contentColor = BrandCyan80)

@Composable
internal fun tealButtonColors() = ButtonDefaults.buttonColors(
    containerColor = FilledTeal,
    contentColor = OnFilledTeal,
)
