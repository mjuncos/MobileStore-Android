package com.mobilestore.ui.form

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.mobilestore.R
import com.mobilestore.core.Formatters
import com.mobilestore.domain.validation.ValidationError

/**
 * Traduce el error de dominio al texto de strings.xml. Es el unico lugar
 * donde se cruzan la validacion y los recursos de Android.
 */
@Composable
fun ValidationError.message(): String = when (this) {
    ValidationError.TitleRequired -> stringResource(R.string.error_title_required)
    is ValidationError.TitleTooShort -> stringResource(R.string.error_title_too_short, min)
    is ValidationError.TitleTooLong -> stringResource(R.string.error_title_too_long, max)
    ValidationError.TitleDuplicated -> stringResource(R.string.error_title_duplicated)

    ValidationError.PriceRequired -> stringResource(R.string.error_price_required)
    ValidationError.PriceNotANumber -> stringResource(R.string.error_price_not_a_number)
    ValidationError.PriceNotPositive -> stringResource(R.string.error_price_not_positive)
    is ValidationError.PriceTooManyDecimals ->
        stringResource(R.string.error_price_too_many_decimals, max)
    is ValidationError.PriceTooHigh ->
        stringResource(R.string.error_price_too_high, Formatters.money(max))

    ValidationError.CategoryRequired -> stringResource(R.string.error_category_required)

    ValidationError.DescriptionRequired -> stringResource(R.string.error_description_required)
    is ValidationError.DescriptionTooShort ->
        stringResource(R.string.error_description_too_short, min)
    is ValidationError.DescriptionTooLong ->
        stringResource(R.string.error_description_too_long, max)

    ValidationError.StockRequired -> stringResource(R.string.error_stock_required)
    ValidationError.StockNotAnInteger -> stringResource(R.string.error_stock_not_an_integer)
    ValidationError.StockNegative -> stringResource(R.string.error_stock_negative)
    is ValidationError.StockTooHigh -> stringResource(R.string.error_stock_too_high, max)

    ValidationError.ImageInvalidUrl -> stringResource(R.string.error_image_invalid_url)
}
