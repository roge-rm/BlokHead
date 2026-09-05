package com.rm.blokhead.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.rm.blokhead.GamepadInputRouter
import com.rm.blokhead.data.GamepadAction
import com.rm.blokhead.data.GamepadBindings
import com.rm.blokhead.game.keycodeDisplayName
import com.rm.blokhead.game.reassignBinding
import kotlinx.coroutines.delay

/** Settings sub-screen letting the player see and rebind every gameplay gamepad action — A and B
 *  included, like any other button (see [com.rm.blokhead.game.reassignBinding]). Menu Confirm/
 *  Back stay reachable through whichever physical A/B buttons regardless, since gameplay actions
 *  stop resolving at all while their own affordances are on screen. */
@Composable
fun GamepadBindingsScreen(
    gamepadRouter: GamepadInputRouter,
    bindings: GamepadBindings,
    onBindingsChange: (GamepadBindings) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var listeningAction by remember { mutableStateOf<GamepadAction?>(null) }
    var message by remember { mutableStateOf<String?>(null) }

    // Installs/clears the router's captureHandler for exactly as long as a row is listening —
    // the "thin shim" over the pure resolveGamepadAction/reassignBinding logic in game/GamepadInput.kt.
    DisposableEffect(listeningAction, bindings) {
        val action = listeningAction
        if (action != null) {
            gamepadRouter.captureHandler = { keyCode ->
                val (updated, bumped) = reassignBinding(bindings, action, keyCode)
                onBindingsChange(updated)
                message = bumped?.let { "Reassigned from ${actionLabel(it)}" }
                listeningAction = null
            }
        }
        onDispose { gamepadRouter.captureHandler = null }
    }

    LaunchedEffect(message) {
        if (message != null) {
            delay(2500)
            message = null
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(12.dp),
    ) {
        Text(
            text = "Map Controls",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 4.dp),
        )
        Text(
            text = "Tap an action, then press the button you want to use for it.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp),
        )

        AnimatedVisibility(visible = message != null) {
            Text(
                text = message.orEmpty(),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 6.dp),
            )
        }

        for (action in GamepadAction.entries) {
            BindingRow(
                label = actionLabel(action),
                keyCode = bindings.keyCodes[action],
                isListening = listeningAction == action,
                onClick = { listeningAction = if (listeningAction == action) null else action },
                onUnbind = { onBindingsChange(bindings.copy(keyCodes = bindings.keyCodes + (action to null))) },
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(top = 4.dp),
        ) {
            OutlinedButton(
                onClick = { onBindingsChange(GamepadBindings()) },
                modifier = Modifier.gamepadFocusable(onActivate = { onBindingsChange(GamepadBindings()) }),
            ) {
                Text("Reset to Defaults")
            }
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.gamepadFocusable(onActivate = onBack),
            ) {
                Text("Back")
            }
        }
    }
}

@Composable
private fun BindingRow(
    label: String,
    keyCode: Int?,
    isListening: Boolean,
    onClick: () -> Unit,
    onUnbind: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (isListening) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
            )
            .clickable(onClick = onClick)
            .gamepadFocusable(shape = RoundedCornerShape(8.dp), onActivate = onClick)
            .padding(vertical = 2.dp, horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = if (isListening) "Press a button…" else keycodeDisplayName(keyCode),
                style = MaterialTheme.typography.bodyMedium,
                color = if (isListening) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
            if (!isListening && keyCode != null) {
                TextButton(onClick = onUnbind, modifier = Modifier.gamepadFocusable(onActivate = onUnbind)) {
                    Text("Unbind")
                }
            }
        }
    }
}

private fun actionLabel(action: GamepadAction): String = when (action) {
    GamepadAction.MoveLeft -> "Move Left"
    GamepadAction.MoveRight -> "Move Right"
    GamepadAction.MoveForward -> "Move Forward"
    GamepadAction.MoveBackward -> "Move Backward"
    GamepadAction.RotateXPositive -> "Rotate X+"
    GamepadAction.RotateXNegative -> "Rotate X−"
    GamepadAction.RotateYPositive -> "Rotate Y+"
    GamepadAction.RotateYNegative -> "Rotate Y−"
    GamepadAction.RotateZPositive -> "Rotate Z+"
    GamepadAction.RotateZNegative -> "Rotate Z−"
    GamepadAction.HardDrop -> "Hard Drop"
    GamepadAction.Pause -> "Pause"
}
