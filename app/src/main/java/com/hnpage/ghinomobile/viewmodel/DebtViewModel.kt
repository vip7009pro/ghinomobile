package com.hnpage.ghinomobile.viewmodel



import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hnpage.ghinomobile.data.Transaction
import com.hnpage.ghinomobile.data.TransactionRepository


import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class DebtViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = TransactionRepository(application)
    val transactions: Flow<List<Transaction>> = repository.getAllTransactions()

    val balanceByContact: Flow<Map<Pair<String, String>, Double>> = transactions.map { list ->
        list.groupBy { Pair(it.phoneNumber, it.contactName) }.mapValues { entry ->
            entry.value.sumOf { t ->
                if (t.type == "debit") t.amount else -t.amount
            }
        }.mapKeys { entry ->
            // Lấy tên mới nhất cho số điện thoại
            val latestTransaction = list.filter { it.phoneNumber == entry.key.first }
                .maxByOrNull { it.date }
            Pair(entry.key.first, latestTransaction?.contactName ?: entry.key.second)
        }
    }

    fun getTransactionsByPhone(phoneNumber: String): Flow<List<Transaction>> =
        repository.getTransactionsByPhone(phoneNumber)

    fun insert(transaction: Transaction) = viewModelScope.launch {
        repository.insert(transaction)
    }

    fun update(transaction: Transaction) = viewModelScope.launch {
        repository.update(transaction)
    }

    fun delete(transaction: Transaction) = viewModelScope.launch {
        repository.delete(transaction)
    }
}