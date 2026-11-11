package com.mobilestore.data.remote

import retrofit2.http.GET

/**
 * Consumo remoto (punto 3.2 del enunciado).
 *
 * Solo se lee del servidor: el catalogo remoto sirve para sembrar la base
 * local la primera vez y para refrescarla despues. El alta, la edicion y la
 * eliminacion son locales, porque fakestoreapi es una API de prueba que no
 * persiste lo que se le escribe.
 */
interface ProductApi {

    @GET("products")
    suspend fun getProducts(): List<ProductDto>
}
