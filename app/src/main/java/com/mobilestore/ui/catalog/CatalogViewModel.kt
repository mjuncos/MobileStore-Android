package com.mobilestore.ui.catalog

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

/**
 * Estado completo de la pantalla de listado. Un unico objeto inmutable
 * cubre los estados pedidos en el punto 3.3: carga, contenido, lista vacia
 * y error.
 */
data class CatalogUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val query: String = "",
    val products: List<Product> = emptyList(),
    val errorMessage: UiText? = null
) {
    val isEmpty: Boolean get() = !isLoading && products.isEmpty()
    val isEmptyBecauseOfSearch: Boolean get() = isEmpty && query.isNotBlank()
}

class CatalogViewModel(
    private val repository: ProductRepository,
    private val cartRepository: CartRepository
) : ViewModel() {

    private val query = MutableStateFlow("")
    private val refreshing = MutableStateFlow(false)
    private val error = MutableStateFlow<UiText?>(null)

    private val _events = Channel<UiText>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    /**
     * stateIn con SharingStarted.WhileSubscribed conserva el estado ante
     * cambios de configuracion (rotacion) y corta la suscripcion cuando la
     * pantalla deja de estar visible.
     */
    val uiState: StateFlow<CatalogUiState> = combine(
        repository.observeProducts(),
        query,
        refreshing,
        error
    ) { products, currentQuery, isRefreshing, currentError ->
        CatalogUiState(
            isLoading = false,
            isRefreshing = isRefreshing,
            query = currentQuery,
            products = products.filter { it.matches(currentQuery) },
            errorMessage = currentError
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = CatalogUiState()
    )

    init {
        // Primera carga: si hay red trae el catalogo remoto; si no, Room ya
        // esta emitiendo lo que haya guardado en el dispositivo.
        refresh(silent = true)
    }

    fun onQueryChange(value: String) {
        query.value = value
    }

    fun clearQuery() {
        query.value = ""
    }

    fun refresh(silent: Boolean = false) {
        if (refreshing.value) return
        viewModelScope.launch {
            refreshing.value = true
            error.value = null
            when (val result = repository.refreshFromRemote()) {
                is Resource.Success ->
                    if (!silent) _events.send(UiText.of(R.string.success_catalog_refreshed))
                is Resource.Failure -> {
                    // Con datos locales disponibles el error va al Snackbar y no
                    // bloquea la pantalla; sin datos se muestra a pantalla completa.
                    val message = result.error.toUiText()
                    if (uiState.value.products.isEmpty()) error.value = message
                    else if (!silent) _events.send(message)
                }
            }
            refreshing.value = false
        }
    }

    fun addToCart(product: Product) {
        viewModelScope.launch {
            when (val result = cartRepository.add(product)) {
                is Resource.Success ->
                    _events.send(UiText.of(R.string.success_added_to_cart, product.title))
                is Resource.Failure -> _events.send(result.error.toUiText())
            }
        }
    }

    fun dismissError() {
        error.value = null
    }

    private fun Product.matches(q: String): Boolean {
        if (q.isBlank()) return true
        val needle = q.trim().lowercase()
        return title.lowercase().contains(needle) || category.lowercase().contains(needle)
    }

    class Factory(
        private val repository: ProductRepository,
        private val cartRepository: CartRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            CatalogViewModel(repository, cartRepository) as T
    }
}
