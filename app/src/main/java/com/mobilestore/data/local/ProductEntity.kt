package com.mobilestore.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.mobilestore.domain.model.Product

@Entity(
    tableName = "products",
    indices = [Index(value = ["remoteId"], unique = true)]
)
data class ProductEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    /** Id del producto en la API. Es null si el producto se creo en el dispositivo. */
    val remoteId: Int? = null,
    val title: String,
    val price: Double,
    val category: String,
    val description: String,
    val stock: Int,
    val imageUrl: String?,
    val updatedAt: Long = System.currentTimeMillis()
)

fun ProductEntity.toDomain(): Product = Product(
    id = id,
    remoteId = remoteId,
    title = title,
    price = price,
    category = category,
    description = description,
    stock = stock,
    imageUrl = imageUrl,
    updatedAt = updatedAt
)

fun Product.toEntity(): ProductEntity = ProductEntity(
    id = id,
    remoteId = remoteId,
    title = title,
    price = price,
    category = category,
    description = description,
    stock = stock,
    imageUrl = imageUrl,
    updatedAt = updatedAt
)
