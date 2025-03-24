package com.hnpage.ghinomobile.data

import android.content.Context
import kotlinx.coroutines.flow.Flow

class TransactionRepository(context: Context) {
    private val dao = AppDatabase.getDatabase(context).transactionDao()

    fun getAllTransactions(): Flow<List<Transaction>> = dao.getAllTransactions()
    fun getTransactionsByPhone(phoneNumber: String): Flow<List<Transaction>> =
        dao.getTransactionsByPhone(phoneNumber)

    suspend fun insert(transaction: Transaction) = dao.insert(transaction)
    suspend fun update(transaction: Transaction) = dao.update(transaction)
    suspend fun delete(transaction: Transaction) = dao.delete(transaction)
}