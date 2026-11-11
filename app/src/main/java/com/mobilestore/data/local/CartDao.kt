package com.mobilestore.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/** Proyeccion del JOIN entre el carrito y los productos. */
data class CartLineRow(
    val productId: Long,
    val quantity: Int,
    val remoteId: Int?,
    val title: String,
    val price: Double,
    val category: String,
    val description: String,
    val stock: Int,
    val imageUrl: String?,
    val updatedAt: Long
)

private const val CART_LINES_QUERY = """
    SELECT c.productId AS productId, c.quantity AS quantity,
           p.remoteId AS remoteId, p.title AS title, p.price AS price,
           p.category AS category, p.description AS description,
           p.stock AS stock, p.imageUrl AS imageUrl, p.updatedAt AS updatedAt
    FROM cart_items c
    INNER JOIN products p ON p.id = c.productId
    ORDER BY p.title ASC
"""

@Dao
interface CartDao {

    /** Version reactiva: la pantalla del carrito se actualiza sola. */
    @Query(CART_LINES_QUERY)
    fun observeLines(): Flow<List<CartLineRow>>

    /** Version puntual: la usa el checkout dentro de la transaccion. */
    @Query(CART_LINES_QUERY)
    suspend fun currentLines(): List<CartLineRow>

    @Query("SELECT quantity FROM cart_items WHERE productId = :productId")
    suspend fun quantityOf(productId: Long): Int?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: CartItemEntity)

    @Query("DELETE FROM cart_items WHERE productId = :productId")
    suspend fun remove(productId: Long)

    @Query("DELETE FROM cart_items")
    suspend fun clear()
}
