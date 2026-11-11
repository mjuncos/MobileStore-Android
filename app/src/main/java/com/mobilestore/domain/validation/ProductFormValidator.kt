package com.mobilestore.domain.validation

import java.util.Locale

/**
 * Validador del formulario de alta/edicion de productos.
 *
 * Es un objeto puro de Kotlin: no conoce Android, no accede a la base y no
 * guarda estado. Cada campo se valida por separado para poder mostrar el
 * error apenas el usuario sale del campo, y [validateAll] arma el resultado
 * completo al intentar guardar.
 */
object ProductFormValidator {

    const val TITLE_MIN = 3
    const val TITLE_MAX = 80
    const val DESCRIPTION_MIN = 10
    const val DESCRIPTION_MAX = 500
    const val PRICE_MAX_DECIMALS = 2
    const val PRICE_MAX = 1_000_000.0
    const val STOCK_MAX = 100_000

    /**
     * @param titleTaken lo resuelve el repositorio consultando SQLite; el
     * validador solo decide que hacer con esa respuesta.
     */
    fun validateTitle(raw: String, titleTaken: Boolean = false): ValidationError? {
        val value = raw.trim()
        return when {
            value.isEmpty() -> ValidationError.TitleRequired
            value.length < TITLE_MIN -> ValidationError.TitleTooShort(TITLE_MIN)
            value.length > TITLE_MAX -> ValidationError.TitleTooLong(TITLE_MAX)
            titleTaken -> ValidationError.TitleDuplicated
            else -> null
        }
    }

    fun validatePrice(raw: String): ValidationError? {
        val value = raw.trim().replace(',', '.')
        if (value.isEmpty()) return ValidationError.PriceRequired

        val parsed = value.toDoubleOrNull()
            ?: return ValidationError.PriceNotANumber
        if (parsed.isNaN() || parsed.isInfinite()) return ValidationError.PriceNotANumber
        if (parsed <= 0.0) return ValidationError.PriceNotPositive

        val decimals = value.substringAfter('.', missingDelimiterValue = "").length
        if (decimals > PRICE_MAX_DECIMALS) {
            return ValidationError.PriceTooManyDecimals(PRICE_MAX_DECIMALS)
        }
        if (parsed > PRICE_MAX) return ValidationError.PriceTooHigh(PRICE_MAX)
        return null
    }

    fun validateCategory(raw: String, allowed: List<String>): ValidationError? {
        val value = raw.trim()
        return if (value.isEmpty() || allowed.none { it.equals(value, ignoreCase = true) }) {
            ValidationError.CategoryRequired
        } else {
            null
        }
    }

    fun validateDescription(raw: String): ValidationError? {
        val value = raw.trim()
        return when {
            value.isEmpty() -> ValidationError.DescriptionRequired
            value.length < DESCRIPTION_MIN -> ValidationError.DescriptionTooShort(DESCRIPTION_MIN)
            value.length > DESCRIPTION_MAX -> ValidationError.DescriptionTooLong(DESCRIPTION_MAX)
            else -> null
        }
    }

    fun validateStock(raw: String): ValidationError? {
        val value = raw.trim()
        if (value.isEmpty()) return ValidationError.StockRequired
        val parsed = value.toIntOrNull() ?: return ValidationError.StockNotAnInteger
        return when {
            parsed < 0 -> ValidationError.StockNegative
            parsed > STOCK_MAX -> ValidationError.StockTooHigh(STOCK_MAX)
            else -> null
        }
    }

    /** La imagen es opcional; si se completa debe ser una URL http(s) o un content:// del selector. */
    fun validateImage(raw: String?): ValidationError? {
        val value = raw?.trim().orEmpty()
        if (value.isEmpty()) return null
        val lower = value.lowercase(Locale.ROOT)
        val looksValid = (lower.startsWith("http://") || lower.startsWith("https://")) &&
            value.length > 10 &&
            !value.contains(' ')
        val isLocalContent = lower.startsWith("content://") || lower.startsWith("file://")
        return if (looksValid || isLocalContent) null else ValidationError.ImageInvalidUrl
    }

    fun validateAll(input: ProductFormInput): ProductFormErrors = ProductFormErrors(
        title = validateTitle(input.title, input.titleTaken),
        price = validatePrice(input.price),
        category = validateCategory(input.category, input.allowedCategories),
        description = validateDescription(input.description),
        stock = validateStock(input.stock),
        image = validateImage(input.imageUrl)
    )
}

/** Datos crudos del formulario, tal como los tipeo el usuario. */
data class ProductFormInput(
    val title: String,
    val price: String,
    val category: String,
    val description: String,
    val stock: String,
    val imageUrl: String?,
    val allowedCategories: List<String>,
    val titleTaken: Boolean = false
)

/** Resultado de la validacion campo por campo. */
data class ProductFormErrors(
    val title: ValidationError? = null,
    val price: ValidationError? = null,
    val category: ValidationError? = null,
    val description: ValidationError? = null,
    val stock: ValidationError? = null,
    val image: ValidationError? = null
) {
    val isValid: Boolean
        get() = title == null && price == null && category == null &&
            description == null && stock == null && image == null

    val firstErrorField: FormField?
        get() = when {
            title != null -> FormField.TITLE
            price != null -> FormField.PRICE
            category != null -> FormField.CATEGORY
            description != null -> FormField.DESCRIPTION
            stock != null -> FormField.STOCK
            image != null -> FormField.IMAGE
            else -> null
        }
}

enum class FormField { TITLE, PRICE, CATEGORY, DESCRIPTION, STOCK, IMAGE }
