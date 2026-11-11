package com.mobilestore.domain.validation

/**
 * Error de validacion expresado en terminos del dominio, sin depender de
 * Android. La traduccion a un texto de strings.xml ocurre en la capa de UI
 * (ver ui/form/ValidationMessages.kt), de modo que:
 *  - el validador se puede testear con JUnit puro, sin emulador;
 *  - no queda ningun texto hardcodeado en el codigo.
 */
sealed interface ValidationError {

    // Nombre
    data object TitleRequired : ValidationError
    data class TitleTooShort(val min: Int) : ValidationError
    data class TitleTooLong(val max: Int) : ValidationError
    data object TitleDuplicated : ValidationError

    // Precio
    data object PriceRequired : ValidationError
    data object PriceNotANumber : ValidationError
    data object PriceNotPositive : ValidationError
    data class PriceTooManyDecimals(val max: Int) : ValidationError
    data class PriceTooHigh(val max: Double) : ValidationError

    // Categoria
    data object CategoryRequired : ValidationError

    // Descripcion
    data object DescriptionRequired : ValidationError
    data class DescriptionTooShort(val min: Int) : ValidationError
    data class DescriptionTooLong(val max: Int) : ValidationError

    // Stock
    data object StockRequired : ValidationError
    data object StockNotAnInteger : ValidationError
    data object StockNegative : ValidationError
    data class StockTooHigh(val max: Int) : ValidationError

    // Imagen
    data object ImageInvalidUrl : ValidationError
}
