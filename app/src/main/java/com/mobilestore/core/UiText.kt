package com.mobilestore.core

import android.content.Context
import androidx.annotation.StringRes

/**
 * Texto que el ViewModel puede producir sin conocer Context ni hardcodear
 * cadenas: guarda el id del recurso y sus argumentos, y la pantalla lo
 * resuelve. Cumple con "textos definidos en recursos" del punto 3.6.
 */
data class UiText(
    @StringRes val id: Int,
    val args: List<Any> = emptyList()
) {
    fun asString(context: Context): String =
        context.getString(id, *args.toTypedArray())

    companion object {
        fun of(@StringRes id: Int, vararg args: Any): UiText = UiText(id, args.toList())
    }
}
