package com.mobilestore.data

import android.content.Context
import com.mobilestore.data.local.AppDatabase
import com.mobilestore.data.remote.RetrofitClient
import com.mobilestore.data.repository.CartRepository
import com.mobilestore.data.repository.ProductRepository

/**
 * Inyeccion de dependencias manual. Con dos repositorios, Hilt agregaria
 * mas configuracion que valor; la decision esta documentada en el README.
 */
object ServiceLocator {

    @Volatile
    private var productRepository: ProductRepository? = null

    @Volatile
    private var cartRepository: CartRepository? = null

    fun productRepository(context: Context): ProductRepository =
        productRepository ?: synchronized(this) {
            productRepository ?: ProductRepository(
                dao = AppDatabase.getInstance(context).productDao(),
                api = RetrofitClient.api
            ).also { productRepository = it }
        }

    fun cartRepository(context: Context): CartRepository =
        cartRepository ?: synchronized(this) {
            cartRepository ?: CartRepository(
                db = AppDatabase.getInstance(context)
            ).also { cartRepository = it }
        }
}
