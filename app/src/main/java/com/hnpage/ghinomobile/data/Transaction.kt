package com.hnpage.ghinomobile.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey val id: String,
    val contactName: String,
    val phoneNumber: String,
    val amount: Double, // Số tiền gốc của giao dịch
    val type: String,   // "debit" (nợ tôi) hoặc "credit" (tôi nợ)
    val date: Long,     // Thời gian tạo giao dịch
    val note: String,
    val isReminderSet: Boolean
)