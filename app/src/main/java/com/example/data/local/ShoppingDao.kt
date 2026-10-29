package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ShoppingDao {

    @Query("SELECT * FROM shopping_items ORDER BY createdAt ASC")
    fun getAllItemsFlow(): Flow<List<ShoppingEntity>>

    @Query("SELECT * FROM shopping_items ORDER BY createdAt ASC")
    suspend fun getAllItems(): List<ShoppingEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: ShoppingEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<ShoppingEntity>)

    @Update
    suspend fun updateItem(item: ShoppingEntity)

    @Delete
    suspend fun deleteItem(item: ShoppingEntity)

    @Query("DELETE FROM shopping_items WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE shopping_items SET isPurchased = :isPurchased WHERE id = :id")
    suspend fun setPurchased(id: Long, isPurchased: Boolean)

    @Query("DELETE FROM shopping_items WHERE LOWER(name) LIKE '%' || LOWER(:query) || '%'")
    suspend fun deleteByQuery(query: String): Int

    @Query("UPDATE shopping_items SET isPurchased = 1 WHERE LOWER(name) LIKE '%' || LOWER(:query) || '%'")
    suspend fun markPurchasedByQuery(query: String): Int

    @Query("DELETE FROM shopping_items")
    suspend fun clearAll()
}
