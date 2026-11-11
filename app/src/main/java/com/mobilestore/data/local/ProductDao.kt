package com.mobilestore.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {

    /**
     * Flow: Room vuelve a emitir automaticamente ante cualquier cambio en la
     * tabla, asi la lista se mantiene al dia sin refrescos manuales.
     */
    @Query("SELECT * FROM products ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE id = :id")
    fun observeById(id: Long): Flow<ProductEntity?>

    @Query("SELECT * FROM products WHERE id = :id")
    suspend fun findById(id: Long): ProductEntity?

    @Query("SELECT * FROM products WHERE remoteId = :remoteId LIMIT 1")
    suspend fun findByRemoteId(remoteId: Int): ProductEntity?

    /** Se usa para la validacion de nombre duplicado del formulario. */
    @Query("SELECT COUNT(*) FROM products WHERE LOWER(TRIM(title)) = LOWER(TRIM(:title)) AND id != :excludeId")
    suspend fun countByTitle(title: String, excludeId: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(product: ProductEntity): Long

    @Update
    suspend fun update(product: ProductEntity)

    /** La usa el checkout para descontar lo vendido. */
    @Query("UPDATE products SET stock = :stock WHERE id = :id")
    suspend fun updateStock(id: Long, stock: Int)

    @Query("DELETE FROM products WHERE id = :id")
    suspend fun deleteById(id: Long)
}
