package com.hnpage.ghinomobile.utils
import java.text.DecimalFormat

fun formatAmount(amount: Double): String {
    val formatter = DecimalFormat("#.##") // Giới hạn 1 chữ số thập phân
    return when {
        amount >= 1_000_000 -> "${formatter.format(amount / 1_000_000)}M VND"
        amount >= 1_000 -> "${formatter.format(amount / 1_000)}K VND"
        amount <= -1_000_000 -> "${formatter.format(amount / 1_000_000)}M VND"
        amount <= -1_000 -> "${formatter.format(amount / 1_000)}K VND"
        else -> amount.toString()
    }
}