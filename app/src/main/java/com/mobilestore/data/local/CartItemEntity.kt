package com.mobilestore.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

/**
 * Linea del carrito. Guarda solo el id del producto y la cantidad: el
 * nombre y el precio se leen del producto con un JOIN, asi no quedan
 * desactualizados si el producto se edita.
 *
 * La clave foranea con onDelete = CASCADE hace que, al borrar un producto,
 * SQLite saque sola su linea del carrito.
 */
@Entity(
    tableName = "cart_items",
    foreignKeys = [
        ForeignKey(
            entity = ProductEntity::class,
            parentColumns = ["id"],
            childColumns = ["productId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class CartItemEntity(
    @PrimaryKey val productId: Long,
    val quantity: Int
)
