package com.hnpage.ghinomobile.data

import android.content.Context
import kotlinx.coroutines.flow.Flow

class TransactionRepository(context: Context) {
    private val transactionDao = AppDatabase.getDatabase(context).transactionDao()
    private val paymentDao = AppDatabase.getDatabase(context).paymentDao()

    fun getAllTransactions(): Flow<List<Transaction>> = transactionDao.getAllTransactions()
    fun getTransactionsByPhone(phoneNumber: String): Flow<List<Transaction>> =
        transactionDao.getTransactionsByPhone(phoneNumber)
    fun getPaymentsByTransaction(transactionId: String): Flow<List<Payment>> =
        paymentDao.getPaymentsByTransaction(transactionId)
    fun getAllPayments(): Flow<List<Payment>> = paymentDao.getAllPayments()

    suspend fun insertTransaction(transaction: Transaction) = transactionDao.insert(transaction)
    suspend fun insertPayment(payment: Payment) = paymentDao.insert(payment)
    suspend fun updateTransaction(transaction: Transaction) = transactionDao.update(transaction)
    suspend fun deleteTransaction(transaction: Transaction) = transactionDao.delete(transaction)
    suspend fun updatePayment(payment: Payment) = paymentDao.update(payment) // Thêm update Payment
    suspend fun deletePayment(payment: Payment) = paymentDao.delete(payment) // Thêm delete Payment
}