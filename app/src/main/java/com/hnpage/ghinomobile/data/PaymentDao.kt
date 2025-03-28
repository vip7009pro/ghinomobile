package com.hnpage.ghinomobile.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Update
import androidx.room.Delete
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PaymentDao {
    @Insert
    suspend fun insert(payment: Payment)

    @Update
    suspend fun update(payment: Payment) // Thêm phương thức update

    @Delete
    suspend fun delete(payment: Payment) // Thêm phương thức delete

    @Query("SELECT * FROM payments WHERE transactionId = :transactionId")
    fun getPaymentsByTransaction(transactionId: String): Flow<List<Payment>>

    @Query("SELECT * FROM payments ORDER BY date DESC")
    fun getAllPayments(): Flow<List<Payment>>
}