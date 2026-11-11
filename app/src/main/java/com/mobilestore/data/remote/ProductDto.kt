package com.mobilestore.data.remote

import com.mobilestore.data.local.ProductEntity
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Representacion del producto tal como la devuelve la API REST.
 * Se mantiene separada de la entidad de Room para que un cambio en el
 * contrato del backend no obligue a migrar la base local.
 */
@JsonClass(generateAdapter = true)
data class ProductDto(
    @Json(name = "id") val id: Int? = null,
    @Json(name = "title") val title: String = "",
    @Json(name = "price") val price: Double = 0.0,
    @Json(name = "category") val category: String = "",
    @Json(name = "description") val description: String = "",
    @Json(name = "image") val image: String? = null,
    @Json(name = "rating") val rating: RatingDto? = null
)

@JsonClass(generateAdapter = true)
data class RatingDto(
    @Json(name = "rate") val rate: Double = 0.0,
    @Json(name = "count") val count: Int = 0
)

/**
 * La API no expone stock, asi que lo derivamos de rating.count. Es una
 * decision explicita para poder ejercitar el campo en el formulario.
 */
fun ProductDto.toEntity(existingId: Long = 0L): ProductEntity = ProductEntity(
    id = existingId,
    remoteId = id,
    title = title.trim(),
    price = price,
    category = category.ifBlank { "otros" },
    description = description.trim(),
    stock = rating?.count ?: 0,
    imageUrl = image,
    updatedAt = System.currentTimeMillis()
)
