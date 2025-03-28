package com.hnpage.ghinomobile.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "payments",
    foreignKeys = [ForeignKey(
        entity = Transaction::class,
        parentColumns = ["id"],
        childColumns = ["transactionId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class Payment(
    @PrimaryKey val id: String,
    val transactionId: String, // Liên kết với Transaction
    val amount: Double,        // Số tiền thanh toán
    val date: Long,            // Thời gian thanh toán
    val note: String           // Ghi chú (tùy chọn)
)