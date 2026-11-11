package com.mobilestore.ui.common

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Snackbar compartido por todas las pantallas.
 *
 * Motivo: los mensajes de confirmacion se disparan justo antes de navegar
 * (por ejemplo "producto creado" seguido de la vuelta al detalle). Si el
 * SnackbarHostState viviera dentro de cada pantalla, la corrutina que lo
 * muestra se cancelaria al destruirse el composable y el mensaje nunca se
 * veria. Al colgarlo del NavHost sobrevive al cambio de pantalla.
 */
class SnackbarController(
    val hostState: SnackbarHostState,
    private val scope: CoroutineScope
) {

    fun show(message: String) {
        scope.launch {
            hostState.currentSnackbarData?.dismiss()
            hostState.showSnackbar(message = message, duration = SnackbarDuration.Short)
        }
    }

    fun showWithAction(message: String, actionLabel: String, onAction: () -> Unit) {
        scope.launch {
            hostState.currentSnackbarData?.dismiss()
            val result = hostState.showSnackbar(
                message = message,
                actionLabel = actionLabel,
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) onAction()
        }
    }
}

@Composable
fun rememberSnackbarController(): SnackbarController {
    val hostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    return remember(hostState, scope) { SnackbarController(hostState, scope) }
}
