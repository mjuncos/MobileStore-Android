package com.mobilestore.core

import android.content.Context
import android.content.Intent
import com.mobilestore.R
import com.mobilestore.domain.model.Product

/**
 * Extra: compartir contenido mediante otras aplicaciones.
 * Usa un Intent implicito ACTION_SEND, sin permisos adicionales.
 */
object ShareHelper {

    fun shareProduct(context: Context, product: Product) {
        val body = context.getString(
            R.string.detail_share_text,
            product.title,
            Formatters.money(product.price),
            "mobilestore://producto/${product.id}"
        )

        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, product.title)
            putExtra(Intent.EXTRA_TEXT, body)
        }

        val chooser = Intent.createChooser(
            sendIntent,
            context.getString(R.string.detail_share_chooser)
        ).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        runCatching { context.startActivity(chooser) }
    }
}
