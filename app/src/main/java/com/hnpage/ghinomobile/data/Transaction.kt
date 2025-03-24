package com.hnpage.ghinomobile.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey val id: String,
    val contactName: String,
    val phoneNumber: String,
    val amount: Double,
    val type: String, // "debit" or "credit"
    val date: Long,   // Timestamp
    val note: String,
    val isReminderSet: Boolean
)