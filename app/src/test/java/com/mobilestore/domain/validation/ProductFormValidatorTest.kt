package com.mobilestore.domain.validation

import com.mobilestore.domain.model.ProductCategories
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProductFormValidatorTest {

    // ---------- Nombre ----------

    @Test
    fun `titulo vacio es requerido`() {
        assertEquals(ValidationError.TitleRequired, ProductFormValidator.validateTitle("   "))
    }

    @Test
    fun `titulo corto informa el minimo`() {
        val error = ProductFormValidator.validateTitle("ab")
        assertEquals(ValidationError.TitleTooShort(ProductFormValidator.TITLE_MIN), error)
    }

    @Test
    fun `titulo largo informa el maximo`() {
        val error = ProductFormValidator.validateTitle("a".repeat(81))
        assertEquals(ValidationError.TitleTooLong(ProductFormValidator.TITLE_MAX), error)
    }

    @Test
    fun `titulo duplicado se rechaza`() {
        val error = ProductFormValidator.validateTitle("Mochila Fjallraven", titleTaken = true)
        assertEquals(ValidationError.TitleDuplicated, error)
    }

    @Test
    fun `titulo valido no devuelve error`() {
        assertNull(ProductFormValidator.validateTitle("  Mochila Fjallraven  "))
    }

    // ---------- Precio ----------

    @Test
    fun `precio vacio es requerido`() {
        assertEquals(ValidationError.PriceRequired, ProductFormValidator.validatePrice(""))
    }

    @Test
    fun `precio no numerico se rechaza`() {
        assertEquals(ValidationError.PriceNotANumber, ProductFormValidator.validatePrice("12a3"))
    }

    @Test
    fun `precio cero o negativo se rechaza`() {
        assertEquals(ValidationError.PriceNotPositive, ProductFormValidator.validatePrice("0"))
        assertEquals(ValidationError.PriceNotPositive, ProductFormValidator.validatePrice("-5.5"))
    }

    @Test
    fun `precio con mas de dos decimales se rechaza`() {
        val error = ProductFormValidator.validatePrice("10.999")
        assertEquals(
            ValidationError.PriceTooManyDecimals(ProductFormValidator.PRICE_MAX_DECIMALS),
            error
        )
    }

    @Test
    fun `precio por encima del maximo se rechaza`() {
        val error = ProductFormValidator.validatePrice("1000000.01")
        assertEquals(ValidationError.PriceTooHigh(ProductFormValidator.PRICE_MAX), error)
    }

    @Test
    fun `precio con coma decimal se acepta`() {
        assertNull(ProductFormValidator.validatePrice("1499,99"))
    }

    // ---------- Categoria ----------

    @Test
    fun `categoria fuera de la lista se rechaza`() {
        val error = ProductFormValidator.validateCategory("juguetes", ProductCategories.ALL)
        assertEquals(ValidationError.CategoryRequired, error)
    }

    @Test
    fun `categoria de la lista se acepta sin importar mayusculas`() {
        assertNull(ProductFormValidator.validateCategory("Electronics", ProductCategories.ALL))
    }

    // ---------- Descripcion ----------

    @Test
    fun `descripcion corta informa el minimo`() {
        val error = ProductFormValidator.validateDescription("corta")
        assertEquals(
            ValidationError.DescriptionTooShort(ProductFormValidator.DESCRIPTION_MIN),
            error
        )
    }

    @Test
    fun `descripcion larga informa el maximo`() {
        val error = ProductFormValidator.validateDescription("x".repeat(501))
        assertEquals(
            ValidationError.DescriptionTooLong(ProductFormValidator.DESCRIPTION_MAX),
            error
        )
    }

    // ---------- Stock ----------

    @Test
    fun `stock decimal se rechaza`() {
        assertEquals(ValidationError.StockNotAnInteger, ProductFormValidator.validateStock("3.5"))
    }

    @Test
    fun `stock negativo se rechaza`() {
        assertEquals(ValidationError.StockNegative, ProductFormValidator.validateStock("-1"))
    }

    @Test
    fun `stock cero es valido`() {
        assertNull(ProductFormValidator.validateStock("0"))
    }

    // ---------- Imagen ----------

    @Test
    fun `imagen vacia es valida porque el campo es opcional`() {
        assertNull(ProductFormValidator.validateImage(""))
        assertNull(ProductFormValidator.validateImage(null))
    }

    @Test
    fun `imagen con esquema invalido se rechaza`() {
        assertEquals(ValidationError.ImageInvalidUrl, ProductFormValidator.validateImage("ftp://x.com/a.png"))
    }

    @Test
    fun `imagen del selector del dispositivo se acepta`() {
        assertNull(ProductFormValidator.validateImage("content://media/picker/0/com.android.providers.media.photopicker/media/42"))
    }

    // ---------- Formulario completo ----------

    @Test
    fun `formulario completo y valido pasa la validacion`() {
        val errors = ProductFormValidator.validateAll(validInput())
        assertTrue(errors.isValid)
        assertNull(errors.firstErrorField)
    }

    @Test
    fun `formulario invalido reporta el primer campo con error`() {
        val errors = ProductFormValidator.validateAll(validInput().copy(title = ""))
        assertFalse(errors.isValid)
        assertEquals(FormField.TITLE, errors.firstErrorField)
    }

    @Test
    fun `formulario acumula errores de varios campos a la vez`() {
        val errors = ProductFormValidator.validateAll(
            validInput().copy(title = "", price = "abc", stock = "-4")
        )
        assertEquals(ValidationError.TitleRequired, errors.title)
        assertEquals(ValidationError.PriceNotANumber, errors.price)
        assertEquals(ValidationError.StockNegative, errors.stock)
        assertNull(errors.description)
    }

    private fun validInput() = ProductFormInput(
        title = "Mochila Fjallraven",
        price = "109.95",
        category = "men's clothing",
        description = "Mochila de uso diario con compartimento acolchado para notebook.",
        stock = "12",
        imageUrl = "https://fakestoreapi.com/img/81fPKd-2AYL._AC_SL1500_.jpg",
        allowedCategories = ProductCategories.ALL
    )
}
