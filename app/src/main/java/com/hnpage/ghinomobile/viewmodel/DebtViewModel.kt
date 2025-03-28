package com.hnpage.ghinomobile.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hnpage.ghinomobile.data.Payment
import com.hnpage.ghinomobile.data.Transaction
import com.hnpage.ghinomobile.data.TransactionRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class DebtViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = TransactionRepository(application)
    val transactions: Flow<List<Transaction>> = repository.getAllTransactions()
    val payments: Flow<List<Payment>> = repository.getAllPayments()

    // Tính toán số dư tổng quát cho từng liên hệ (dư nợ còn lại)
    val balanceByContact: Flow<Map<Pair<String, String>, Double>> = transactions.map { list ->
        list.groupBy { Pair(it.phoneNumber, it.contactName) }.mapValues { entry ->
            entry.value.sumOf { t -> if (t.type == "debit") t.amount else -t.amount }
        }.mapKeys { entry ->
            val latestTransaction = list.filter { it.phoneNumber == entry.key.first }
                .maxByOrNull { it.date }
            Pair(entry.key.first, latestTransaction?.contactName ?: entry.key.second)
        }
    }

    // Tổng nợ ghi nhận từ debit (nợ tôi)
    val totalDebit: Flow<Double> = transactions.map { list ->
        list.filter { it.type == "debit" }.sumOf { it.amount }
    }

    // Tổng nợ ghi nhận từ credit (tôi nợ)
    val totalCredit: Flow<Double> = transactions.map { list ->
        list.filter { it.type == "credit" }.sumOf { it.amount }
    }

    // Tổng đã trả cho các giao dịch debit
    val totalDebitPaid: Flow<Double> = transactions.map { list ->
        list.filter { it.type == "debit" }.map { it.id }
    }.combine(payments) { debitIds, payments ->
        payments.filter { it.transactionId in debitIds }.sumOf { it.amount }
    }

    // Tổng đã trả cho các giao dịch credit
    val totalCreditPaid: Flow<Double> = transactions.map { list ->
        list.filter { it.type == "credit" }.map { it.id }
    }.combine(payments) { creditIds, payments ->
        payments.filter { it.transactionId in creditIds }.sumOf { it.amount }
    }

    // Tính toán chi tiết cho từng liên hệ: tổng nợ, tổng đã trả, dư nợ
    fun getContactStats(): Flow<Map<Pair<String, String>, Triple<Double, Double, Double>>> {
        return combine(transactions, payments) { txs, pays ->
            val stats = mutableMapOf<Pair<String, String>, Triple<Double, Double, Double>>()

            // Tính tổng nợ (debit + credit) cho từng liên hệ
            txs.groupBy { Pair(it.phoneNumber, it.contactName) }.forEach { (contactPair, transactions) ->
                val totalDebit = transactions.filter { it.type == "debit" }.sumOf { it.amount }
                val totalCredit = transactions.filter { it.type == "credit" }.sumOf { it.amount }
                val totalDebt = totalDebit + totalCredit
                stats[contactPair] = Triple(totalDebt, 0.0, 0.0)
            }

            // Tính tổng đã trả cho từng liên hệ
            pays.forEach { payment ->
                val transaction = txs.find { it.id == payment.transactionId }
                if (transaction != null) {
                    val contactPair = Pair(transaction.phoneNumber, transaction.contactName)
                    val (totalDebt, totalPaid, _) = stats.getOrDefault(contactPair, Triple(0.0, 0.0, 0.0))
                    stats[contactPair] = Triple(totalDebt, totalPaid + payment.amount, 0.0)
                }
            }

            // Tính dư nợ (remainingBalance = totalDebit - totalCredit - totalPaid)
            stats.mapValues { (contactPair, triple) ->
                val (totalDebt, totalPaid, _) = triple
                val transactionsForContact = txs.filter { it.phoneNumber == contactPair.first }
                val totalDebit = transactionsForContact.filter { it.type == "debit" }.sumOf { it.amount }
                val totalCredit = transactionsForContact.filter { it.type == "credit" }.sumOf { it.amount }
                val remainingBalance = totalDebit - totalCredit - totalPaid
                Triple(totalDebt, totalPaid, remainingBalance)
            }.mapKeys { entry ->
                val latestTransaction = txs.filter { it.phoneNumber == entry.key.first }
                    .maxByOrNull { it.date }
                Pair(entry.key.first, latestTransaction?.contactName ?: entry.key.second)
            }
        }.stateIn(viewModelScope, SharingStarted.Lazily, emptyMap())
    }

    fun getTransactionsByPhone(phoneNumber: String): Flow<List<Transaction>> =
        repository.getTransactionsByPhone(phoneNumber)

    fun getPaymentsByTransaction(transactionId: String): Flow<List<Payment>> =
        repository.getPaymentsByTransaction(transactionId)

    fun insertTransaction(transaction: Transaction) = viewModelScope.launch {
        repository.insertTransaction(transaction)
    }

    fun insertPayment(payment: Payment) = viewModelScope.launch {
        repository.insertPayment(payment)
    }

    fun updateTransaction(transaction: Transaction) = viewModelScope.launch {
        repository.updateTransaction(transaction)
    }

    fun deleteTransaction(transaction: Transaction) = viewModelScope.launch {
        repository.deleteTransaction(transaction)
    }

    fun updatePayment(payment: Payment) = viewModelScope.launch {
        repository.updatePayment(payment)
    }

    fun deletePayment(payment: Payment) = viewModelScope.launch {
        repository.deletePayment(payment)
    }

    fun getPaidAmountForTransaction(transactionId: String): Flow<Double> =
        getPaymentsByTransaction(transactionId).map { payments ->
            payments.sumOf { it.amount }
        }
}