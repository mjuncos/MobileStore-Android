package com.mobilestore.ui.form

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mobilestore.R
import com.mobilestore.core.Resource
import com.mobilestore.core.UiText
import com.mobilestore.core.toUiText
import com.mobilestore.data.repository.ProductRepository
import com.mobilestore.domain.model.Product
import com.mobilestore.domain.model.ProductCategories
import com.mobilestore.domain.validation.FormField
import com.mobilestore.domain.validation.ProductFormErrors
import com.mobilestore.domain.validation.ProductFormInput
import com.mobilestore.domain.validation.ProductFormValidator
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Estado del formulario de alta/edicion.
 *
 * [touched] guarda que campos ya visito el usuario: los errores solo se
 * muestran una vez que el campo perdio el foco o despues de un intento de
 * guardado, para no marcar en rojo un formulario recien abierto.
 */
data class ProductFormUiState(
    val isEditing: Boolean = false,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val title: String = "",
    val price: String = "",
    val category: String = "",
    val description: String = "",
    val stock: String = "",
    val imageUrl: String = "",
    val categories: List<String> = ProductCategories.ALL,
    val errors: ProductFormErrors = ProductFormErrors(),
    val touched: Set<FormField> = emptySet(),
    val submitAttempted: Boolean = false,
    val isDirty: Boolean = false,
    val showDiscardDialog: Boolean = false
) {
    /** Un error se muestra solo si el campo fue tocado o ya se intento guardar. */
    fun visibleError(field: FormField) = when {
        !submitAttempted && field !in touched -> null
        else -> when (field) {
            FormField.TITLE -> errors.title
            FormField.PRICE -> errors.price
            FormField.CATEGORY -> errors.category
            FormField.DESCRIPTION -> errors.description
            FormField.STOCK -> errors.stock
            FormField.IMAGE -> errors.image
        }
    }

    val canSave: Boolean get() = !isSaving && !isLoading
}

class ProductFormViewModel(
    private val productId: Long?,
    private val repository: ProductRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        ProductFormUiState(isEditing = productId != null, isLoading = productId != null)
    )
    val uiState: StateFlow<ProductFormUiState> = _uiState.asStateFlow()

    private val _events = Channel<FormEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private var original: Product? = null

    init {
        if (productId != null) {
            viewModelScope.launch {
                val product = repository.findById(productId)
                if (product == null) {
                    _events.send(FormEvent.Message(UiText.of(R.string.detail_not_found)))
                    _events.send(FormEvent.Close)
                } else {
                    original = product
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            title = product.title,
                            price = trimTrailingZeros(product.price),
                            category = product.category,
                            description = product.description,
                            stock = product.stock.toString(),
                            imageUrl = product.imageUrl.orEmpty()
                        )
                    }
                }
            }
        }
    }

    // ------------------------------------------------------------------
    // Entrada de datos: cada cambio revalida ese campo en vivo.
    // ------------------------------------------------------------------

    fun onTitleChange(value: String) {
        _uiState.update {
            it.copy(
                title = value,
                isDirty = true,
                errors = it.errors.copy(title = ProductFormValidator.validateTitle(value))
            )
        }
    }

    fun onPriceChange(value: String) {
        // Filtramos la entrada para que el teclado numerico no meta simbolos
        // raros, pero igual validamos el contenido.
        val sanitized = value.filter { it.isDigit() || it == '.' || it == ',' }
        _uiState.update {
            it.copy(
                price = sanitized,
                isDirty = true,
                errors = it.errors.copy(price = ProductFormValidator.validatePrice(sanitized))
            )
        }
    }

    fun onCategoryChange(value: String) {
        _uiState.update {
            it.copy(
                category = value,
                isDirty = true,
                touched = it.touched + FormField.CATEGORY,
                errors = it.errors.copy(
                    category = ProductFormValidator.validateCategory(value, it.categories)
                )
            )
        }
    }

    fun onDescriptionChange(value: String) {
        _uiState.update {
            it.copy(
                description = value,
                isDirty = true,
                errors = it.errors.copy(
                    description = ProductFormValidator.validateDescription(value)
                )
            )
        }
    }

    fun onStockChange(value: String) {
        val sanitized = value.filter { it.isDigit() || it == '-' }
        _uiState.update {
            it.copy(
                stock = sanitized,
                isDirty = true,
                errors = it.errors.copy(stock = ProductFormValidator.validateStock(sanitized))
            )
        }
    }

    fun onImageChange(value: String) {
        _uiState.update {
            it.copy(
                imageUrl = value,
                isDirty = true,
                touched = it.touched + FormField.IMAGE,
                errors = it.errors.copy(image = ProductFormValidator.validateImage(value))
            )
        }
    }

    fun clearImage() = onImageChange("")

    /** Se llama cuando un campo pierde el foco: recien ahi mostramos su error. */
    fun onFieldTouched(field: FormField) {
        _uiState.update { it.copy(touched = it.touched + field) }
    }

    // ------------------------------------------------------------------
    // Guardado
    // ------------------------------------------------------------------

    fun save() {
        val state = _uiState.value
        if (!state.canSave) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, submitAttempted = true) }

            // La unicidad del nombre se consulta contra SQLite, no en memoria.
            val titleTaken = repository.isTitleTaken(state.title, productId ?: 0L)
            val errors = ProductFormValidator.validateAll(
                ProductFormInput(
                    title = state.title,
                    price = state.price,
                    category = state.category,
                    description = state.description,
                    stock = state.stock,
                    imageUrl = state.imageUrl,
                    allowedCategories = state.categories,
                    titleTaken = titleTaken
                )
            )

            if (!errors.isValid) {
                _uiState.update { it.copy(isSaving = false, errors = errors) }
                _events.send(FormEvent.Message(UiText.of(R.string.error_form_has_errors)))
                errors.firstErrorField?.let { _events.send(FormEvent.FocusField(it)) }
                return@launch
            }

            val product = buildProduct(state)

            // create devuelve el id generado por Room; update no devuelve nada,
            // asi que reutilizamos el id que ya teniamos.
            val outcome: Resource<Long> = if (productId == null) {
                repository.create(product)
            } else {
                when (val updated = repository.update(product)) {
                    is Resource.Success -> Resource.Success(productId)
                    is Resource.Failure -> updated
                }
            }

            when (outcome) {
                is Resource.Success -> {
                    _uiState.update { it.copy(isSaving = false, isDirty = false, errors = errors) }
                    _events.send(
                        FormEvent.Saved(
                            productId = outcome.data,
                            message = UiText.of(
                                if (productId == null) R.string.success_product_created
                                else R.string.success_product_updated
                            )
                        )
                    )
                }
                is Resource.Failure -> {
                    _uiState.update { it.copy(isSaving = false) }
                    _events.send(FormEvent.Message(outcome.error.toUiText()))
                }
            }
        }
    }

    // ------------------------------------------------------------------
    // Salida con cambios sin guardar
    // ------------------------------------------------------------------

    fun onBackRequested() {
        if (_uiState.value.isDirty) {
            _uiState.update { it.copy(showDiscardDialog = true) }
        } else {
            viewModelScope.launch { _events.send(FormEvent.Close) }
        }
    }

    fun cancelDiscard() {
        _uiState.update { it.copy(showDiscardDialog = false) }
    }

    fun confirmDiscard() {
        _uiState.update { it.copy(showDiscardDialog = false) }
        viewModelScope.launch { _events.send(FormEvent.Close) }
    }

    // ------------------------------------------------------------------

    private fun buildProduct(state: ProductFormUiState): Product {
        val base = original
        return Product(
            id = base?.id ?: 0L,
            remoteId = base?.remoteId,
            title = state.title.trim(),
            price = state.price.trim().replace(',', '.').toDouble(),
            category = state.category.trim(),
            description = state.description.trim(),
            stock = state.stock.trim().toInt(),
            imageUrl = state.imageUrl.trim().ifBlank { null }
        )
    }

    private fun trimTrailingZeros(value: Double): String =
        if (value % 1.0 == 0.0) value.toLong().toString()
        else value.toString()

    class Factory(
        private val productId: Long?,
        private val repository: ProductRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            ProductFormViewModel(productId, repository) as T
    }
}

sealed interface FormEvent {
    data class Saved(val productId: Long, val message: UiText) : FormEvent

    data class Message(val text: UiText) : FormEvent
    data class FocusField(val field: FormField) : FormEvent
    data object Close : FormEvent
}
