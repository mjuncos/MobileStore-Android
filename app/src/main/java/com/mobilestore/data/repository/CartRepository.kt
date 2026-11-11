package com.mobilestore.data.repository

import androidx.room.withTransaction
import com.mobilestore.core.AppError
import com.mobilestore.core.Resource
import com.mobilestore.data.local.AppDatabase
import com.mobilestore.data.local.CartItemEntity
import com.mobilestore.domain.model.CartLine
import com.mobilestore.domain.model.CheckoutSummary
import com.mobilestore.domain.model.Product
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class CartRepository(
    private val db: AppDatabase,
    private val io: CoroutineDispatcher = Dispatchers.IO
) {

    private val cartDao = db.cartDao()
    private val productDao = db.productDao()

    fun observeLines(): Flow<List<CartLine>> = cartDao.observeLines().map { rows ->
        rows.map { row ->
            CartLine(
                product = Product(
                    id = row.productId,
                    remoteId = row.remoteId,
                    title = row.title,
                    price = row.price,
                    category = row.category,
                    description = row.description,
                    stock = row.stock,
                    imageUrl = row.imageUrl,
                    updatedAt = row.updatedAt
                ),
                quantity = row.quantity
            )
        }
    }

    /** No deja agregar mas unidades de las que hay en stock. */
    suspend fun add(product: Product, quantity: Int = 1): Resource<Unit> = withContext(io) {
        try {
            val current = cartDao.quantityOf(product.id) ?: 0
            val target = (current + quantity).coerceIn(1, product.stock.coerceAtLeast(1))
            cartDao.upsert(CartItemEntity(productId = product.id, quantity = target))
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Failure(AppError.Database)
        }
    }

    suspend fun setQuantity(productId: Long, quantity: Int): Resource<Unit> = withContext(io) {
        try {
            if (quantity <= 0) cartDao.remove(productId)
            else cartDao.upsert(CartItemEntity(productId, quantity))
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Failure(AppError.Database)
        }
    }

    suspend fun remove(productId: Long): Resource<Unit> = withContext(io) {
        try {
            cartDao.remove(productId)
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Failure(AppError.Database)
        }
    }

    suspend fun clear(): Resource<Unit> = withContext(io) {
        try {
            cartDao.clear()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Failure(AppError.Database)
        }
    }

    /**
     * Confirma la venta: descuenta el stock de cada producto vendido y vacia
     * el carrito.
     *
     * Va dentro de una transaccion de Room porque toca varias filas de dos
     * tablas: o se aplica toda la venta o no se aplica nada. Si fallara a la
     * mitad quedarian productos descontados y el carrito sin vaciar.
     */
    suspend fun checkout(): Resource<CheckoutSummary> = withContext(io) {
        try {
            val summary = db.withTransaction {
                val lines = cartDao.currentLines()
                lines.forEach { row ->
                    val restante = (row.stock - row.quantity).coerceAtLeast(0)
                    productDao.updateStock(row.productId, restante)
                }
                if (lines.isNotEmpty()) cartDao.clear()
                CheckoutSummary(
                    itemCount = lines.sumOf { it.quantity },
                    total = lines.sumOf { it.price * it.quantity }
                )
            }
            Resource.Success(summary)
        } catch (e: Exception) {
            Resource.Failure(AppError.Database)
        }
    }
}
