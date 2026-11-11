package com.mobilestore.ui.cart

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mobilestore.core.Formatters
import com.mobilestore.core.Resource
import com.mobilestore.core.UiText
import com.mobilestore.core.toUiText
import com.mobilestore.data.repository.CartRepository
import com.mobilestore.domain.model.CartLine
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CartUiState(
    val isLoading: Boolean = true,
    val lines: List<CartLine> = emptyList(),
    val showClearDialog: Boolean = false
) {
    val total: Double get() = lines.sumOf { it.subtotal }
    val itemCount: Int get() = lines.sumOf { it.quantity }
    val isEmpty: Boolean get() = !isLoading && lines.isEmpty()
    val formattedTotal: String get() = Formatters.money(total)
}

class CartViewModel(
    private val cartRepository: CartRepository
) : ViewModel() {

    private val showClearDialog = MutableStateFlow(false)
    private val loaded = MutableStateFlow(false)

    private val _events = Channel<CartEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    val uiState: StateFlow<CartUiState> = combine(
        cartRepository.observeLines(),
        showClearDialog,
        loaded
    ) { lines, showDialog, hasLoaded ->
        CartUiState(
            isLoading = !hasLoaded && lines.isEmpty(),
            lines = lines,
            showClearDialog = showDialog
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = CartUiState()
    )

    init {
        // La primera emision de Room marca el fin del estado de carga.
        viewModelScope.launch {
            cartRepository.observeLines().first()
            loaded.value = true
        }
    }

    fun increase(line: CartLine) {
        viewModelScope.launch {
            // No se puede vender mas de lo que hay en stock.
            val target = (line.quantity + 1).coerceAtMost(line.product.stock)
            cartRepository.setQuantity(line.product.id, target)
        }
    }

    fun decrease(line: CartLine) {
        viewModelScope.launch {
            cartRepository.setQuantity(line.product.id, line.quantity - 1)
        }
    }

    fun remove(line: CartLine) {
        viewModelScope.launch {
            when (val result = cartRepository.remove(line.product.id)) {
                is Resource.Success -> Unit
                is Resource.Failure -> _events.send(CartEvent.Message(result.error.toUiText()))
            }
        }
    }

    fun requestClear() {
        showClearDialog.value = true
    }

    fun cancelClear() {
        showClearDialog.value = false
    }

    fun confirmClear() {
        viewModelScope.launch {
            showClearDialog.value = false
            cartRepository.clear()
        }
    }

    /**
     * Confirma la compra y pide a la pantalla que dispare la notificacion
     * del sistema. El ViewModel no conoce NotificationCompat: solo emite el
     * evento con los datos ya formateados.
     */
    fun checkout() {
        if (uiState.value.lines.isEmpty()) return
        viewModelScope.launch {
            when (val result = cartRepository.checkout()) {
                is Resource.Success -> _events.send(
                    CartEvent.CheckoutConfirmed(
                        itemCount = result.data.itemCount,
                        formattedTotal = Formatters.money(result.data.total)
                    )
                )
                is Resource.Failure -> _events.send(CartEvent.Message(result.error.toUiText()))
            }
        }
    }

    class Factory(
        private val cartRepository: CartRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            CartViewModel(cartRepository) as T
    }
}

sealed interface CartEvent {
    data class Message(val text: UiText) : CartEvent
    data class CheckoutConfirmed(val itemCount: Int, val formattedTotal: String) : CartEvent
}
