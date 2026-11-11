package com.mobilestore.core

import java.text.NumberFormat
import java.util.Locale

object Formatters {

    private val currency: NumberFormat = NumberFormat.getCurrencyInstance(Locale("es", "AR"))

    fun money(value: Double): String = runCatching { currency.format(value) }
        .getOrElse { String.format(Locale.getDefault(), "$%.2f", value) }
}
