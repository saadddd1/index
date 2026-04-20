package com.example.ledger.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AutoBillDao {
    @Query("SELECT * FROM auto_bills WHERE isProcessed = 0 ORDER BY timestampMillis DESC")
    fun getPendingBills(): Flow<List<AutoBill>>

    @Query("SELECT * FROM auto_bills ORDER BY timestampMillis DESC")
    fun getAllAutoBills(): Flow<List<AutoBill>>

    @Insert
    suspend fun insertAutoBill(bill: AutoBill)

    @Update
    suspend fun updateAutoBill(bill: AutoBill)

    @Query("DELETE FROM auto_bills WHERE id = :billId")
    suspend fun deleteAutoBill(billId: Int)

    @Query("SELECT COUNT(*) FROM auto_bills")
    suspend fun getCount(): Int
}
