package com.mobilestore.domain.model

/** Una linea del carrito: el producto elegido y la cantidad. */
data class CartLine(
    val product: Product,
    val quantity: Int
) {
    val subtotal: Double get() = product.price * quantity
}

/** Resultado de confirmar una compra; alimenta la notificacion del sistema. */
data class CheckoutSummary(
    val itemCount: Int,
    val total: Double
)
