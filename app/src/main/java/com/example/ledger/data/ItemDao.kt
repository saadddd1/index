package com.example.ledger.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ItemDao {
    @Query("SELECT * FROM items ORDER BY isSold ASC, purchaseDateMillis DESC")
    fun getAllItems(): Flow<List<Item>>

    @Insert
    suspend fun insertItem(item: Item)

    @Update
    suspend fun updateItem(item: Item)

    @Query("DELETE FROM items WHERE id = :itemId")
    suspend fun deleteItem(itemId: Int)

    @Query("SELECT COUNT(*) FROM items")
    suspend fun getCount(): Int
}
