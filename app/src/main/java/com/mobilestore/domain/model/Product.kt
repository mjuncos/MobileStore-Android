package com.mobilestore.domain.model

/**
 * Modelo de dominio. Es el unico tipo que viaja entre la capa de datos,
 * los ViewModel y la interfaz: ni las entidades de Room ni los DTO de la
 * API salen de la capa de datos.
 */
data class Product(
    val id: Long = 0L,
    val remoteId: Int? = null,
    val title: String,
    val price: Double,
    val category: String,
    val description: String,
    val stock: Int,
    val imageUrl: String? = null,
    val updatedAt: Long = System.currentTimeMillis()
) {
    val isAvailable: Boolean get() = stock > 0

    /** El listado y el detalle lo marcan en color para avisar que hay que reponer. */
    val isLowStock: Boolean get() = stock in 1..LOW_STOCK_THRESHOLD

    companion object {
        const val LOW_STOCK_THRESHOLD = 3
    }
}

/** Categorias soportadas por el alta/edicion de productos. */
object ProductCategories {
    val ALL = listOf(
        "electronics",
        "jewelery",
        "men's clothing",
        "women's clothing",
        "hogar",
        "otros"
    )
}
