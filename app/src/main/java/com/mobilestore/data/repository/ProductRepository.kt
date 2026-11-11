package com.mobilestore.data.repository

import com.mobilestore.core.AppError
import com.mobilestore.core.Resource
import com.mobilestore.data.local.ProductDao
import com.mobilestore.data.local.toDomain
import com.mobilestore.data.local.toEntity
import com.mobilestore.data.remote.ProductApi
import com.mobilestore.data.remote.toEntity
import com.mobilestore.domain.model.Product
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException

/**
 * Fuente unica de verdad: la interfaz siempre lee de Room, y la red solo
 * alimenta a Room. Esto evita que la pantalla quede en blanco cuando la API
 * falla y mantiene el listado y el detalle siempre consistentes entre si,
 * porque ambos observan la misma tabla.
 */
class ProductRepository(
    private val dao: ProductDao,
    private val api: ProductApi,
    private val io: CoroutineDispatcher = Dispatchers.IO
) {

    fun observeProducts(): Flow<List<Product>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    fun observeProduct(id: Long): Flow<Product?> =
        dao.observeById(id).map { it?.toDomain() }

    suspend fun findById(id: Long): Product? = withContext(io) {
        dao.findById(id)?.toDomain()
    }

    /** Usado por la validacion de nombre duplicado del formulario. */
    suspend fun isTitleTaken(title: String, excludeId: Long): Boolean = withContext(io) {
        dao.countByTitle(title, excludeId) > 0
    }

    /**
     * Trae el catalogo remoto y lo vuelca en Room. Los productos creados en
     * el dispositivo (remoteId == null) no se tocan.
     */
    suspend fun refreshFromRemote(): Resource<Int> = withContext(io) {
        try {
            val remote = api.getProducts()
            var count = 0
            remote.forEach { dto ->
                val remoteId = dto.id ?: return@forEach
                val existing = dao.findByRemoteId(remoteId)
                if (existing == null) {
                    dao.insert(dto.toEntity())
                } else {
                    dao.update(dto.toEntity(existingId = existing.id))
                }
                count++
            }
            // Si no se guardo ni un producto, es un fallo: antes devolvia
            // Success(0) y la pantalla lo mostraba como "catalogo vacio".
            if (count == 0) Resource.Failure(AppError.EmptyRemote)
            else Resource.Success(count)
        } catch (e: IOException) {
            Resource.Failure(AppError.Network)
        } catch (e: HttpException) {
            Resource.Failure(AppError.Server(e.code()))
        } catch (e: Exception) {
            Resource.Failure(AppError.Unknown(e.message))
        }
    }
    suspend fun create(product: Product): Resource<Long> = withContext(io) {
        try {
            Resource.Success(dao.insert(product.copy(id = 0L).toEntity()))
        } catch (e: Exception) {
            Resource.Failure(AppError.Database)
        }
    }

    suspend fun update(product: Product): Resource<Unit> = withContext(io) {
        try {
            dao.update(product.copy(updatedAt = System.currentTimeMillis()).toEntity())
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Failure(AppError.Database)
        }
    }

    suspend fun delete(product: Product): Resource<Unit> = withContext(io) {
        try {
            dao.deleteById(product.id)
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Failure(AppError.Database)
        }
    }

    /** Reinserta un producto eliminado (accion "Deshacer" del Snackbar). */
    suspend fun restore(product: Product): Resource<Long> = withContext(io) {
        try {
            Resource.Success(dao.insert(product.toEntity()))
        } catch (e: Exception) {
            Resource.Failure(AppError.Database)
        }
    }
}
