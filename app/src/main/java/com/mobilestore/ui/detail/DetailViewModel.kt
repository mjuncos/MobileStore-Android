package com.mobilestore.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mobilestore.R
import com.mobilestore.core.Resource
import com.mobilestore.core.UiText
import com.mobilestore.core.toUiText
import com.mobilestore.data.repository.CartRepository
import com.mobilestore.data.repository.ProductRepository
import com.mobilestore.domain.model.Product
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DetailUiState(
    val isLoading: Boolean = true,
    val product: Product? = null,
    val showDeleteDialog: Boolean = false
)

class DetailViewModel(
    private val productId: Long,
    private val repository: ProductRepository,
    private val cartRepository: CartRepository
) : ViewModel() {

    private val showDeleteDialog = MutableStateFlow(false)
    private val loaded = MutableStateFlow(false)

    private val _events = Channel<DetailEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    /** Ultimo producto eliminado, para poder deshacer desde el Snackbar. */
    private var lastDeleted: Product? = null

    val uiState: StateFlow<DetailUiState> = combine(
        repository.observeProduct(productId),
        showDeleteDialog,
        loaded
    ) { product, showDialog, hasLoaded ->
        DetailUiState(
            isLoading = !hasLoaded && product == null,
            product = product,
            showDeleteDialog = showDialog
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DetailUiState()
    )

    init {
        viewModelScope.launch {
            repository.findById(productId)
            loaded.value = true
        }
    }

    fun addToCart() {
        val product = uiState.value.product ?: return
        viewModelScope.launch {
            when (val result = cartRepository.add(product)) {
                is Resource.Success -> _events.send(
                    DetailEvent.Message(UiText.of(R.string.success_added_to_cart, product.title))
                )
                is Resource.Failure ->
                    _events.send(DetailEvent.Message(result.error.toUiText()))
            }
        }
    }

    fun requestDelete() {
        showDeleteDialog.value = true
    }

    fun cancelDelete() {
        showDeleteDialog.value = false
    }

    /** Solo se ejecuta despues de la confirmacion explicita del usuario. */
    fun confirmDelete() {
        val product = uiState.value.product ?: return
        viewModelScope.launch {
            showDeleteDialog.value = false
            when (val result = repository.delete(product)) {
                is Resource.Success -> {
                    lastDeleted = product
                    _events.send(
                        DetailEvent.Deleted(UiText.of(R.string.success_product_deleted, product.title))
                    )
                }
                is Resource.Failure ->
                    _events.send(DetailEvent.Message(result.error.toUiText()))
            }
        }
    }

    fun undoDelete() {
        val product = lastDeleted ?: return
        viewModelScope.launch {
            repository.restore(product)
            lastDeleted = null
        }
    }

    class Factory(
        private val productId: Long,
        private val repository: ProductRepository,
        private val cartRepository: CartRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            DetailViewModel(productId, repository, cartRepository) as T
    }
}

sealed interface DetailEvent {
    data class Message(val text: UiText) : DetailEvent
    data class Deleted(val text: UiText) : DetailEvent
}
