package com.hnpage.ghinomobile.utils

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.FileProvider
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.net.Uri
import com.hnpage.ghinomobile.data.Payment
import com.hnpage.ghinomobile.data.Transaction
import java.io.File
import java.io.FileOutputStream
import androidx.core.graphics.createBitmap
import android.graphics.LinearGradient
import android.graphics.Shader

fun createTransactionImage(
    context: Context,
    transaction: Transaction,
    balance: Double,         // Dư nợ của liên hệ
    paidAmount: Double,      // Số tiền đã trả
    payments: List<Payment>  // Thêm danh sách lịch sử thanh toán
): Uri? {
    val width = 800
    // Tính chiều cao động dựa trên số lượng thanh toán (mỗi dòng cao 40f, cộng thêm phần tiêu đề)
    val paymentLinesHeight = if (payments.isNotEmpty()) (payments.size + 1) * 40f else 0f
    val height = (500 + paymentLinesHeight).toInt() // Chiều cao cơ bản + chiều cao lịch sử thanh toán
    val bitmap = createBitmap(width, height)
    val canvas = Canvas(bitmap)
    val paint = Paint().apply {
        isAntiAlias = true
        typeface = Typeface.DEFAULT_BOLD
    }

    // Màu nền cố định phong cách thiên nhiên
    val backgroundColor = Color(0xFFF1F8E9).toArgb() // Xanh lá rất nhạt
    canvas.drawColor(backgroundColor)

    // Gradient cho Debit (Nợ) - Sắc thái đỏ
    val debitGradient = LinearGradient(
        0f, 0f, width.toFloat(), 0f, // Gradient ngang
        intArrayOf(
            Color(0xFFD32F2F).toArgb(),
            Color(0xFFFF5722).toArgb(), // Cam đậm
            Color(0xFFFFCC80).toArgb(), // Cam nhạt
             // Đỏ đậm
        ),
        null,
        Shader.TileMode.CLAMP
    )

    // Gradient cho Credit (Có) - Sắc thái xanh lá thiên nhiên
    val creditGradient = LinearGradient(
        0f, 0f, width.toFloat(), 0f, // Gradient ngang
        intArrayOf(
            Color(0xFF2E7D32).toArgb(),// Xanh lá nhạt
            Color(0xFF4CAF50).toArgb(), // Xanh lá trung
            Color(0xFFA5D6A7).toArgb(),
              // Xanh lá đậm
        ),
        null,
        Shader.TileMode.CLAMP
    )

    // Gradient trung tính cho trạng thái đã trả hết
    val neutralGradient = LinearGradient(
        0f, 0f, width.toFloat(), 0f, // Gradient ngang
        intArrayOf(
            Color(0xFFE0E0E0).toArgb(), // Xám nhạt
            Color(0xFFB0B0B0).toArgb()  // Xám đậm
        ),
        null,
        Shader.TileMode.CLAMP
    )

    // Áp dụng gradient làm nền dựa trên trạng thái thanh toán
    val remainingAmount = transaction.amount - paidAmount
    paint.shader = when {
        remainingAmount == 0.0 -> creditGradient // Đã trả hết
        transaction.type == "debit" -> debitGradient // Nợ tôi, chưa trả hết
        else -> debitGradient // Tôi nợ, chưa trả hết
    }
    canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
    paint.shader = null // Reset shader sau khi vẽ nền

    // Màu chữ tương phản
    paint.color = Color.White.toArgb() // Chữ trắng để nổi bật trên gradient
    paint.textSize = 40f
    paint.typeface = Typeface.DEFAULT_BOLD
    canvas.drawText("Rượu-Gạo Tươi Hòa - Thông tin giao dịch", 50f, 80f, paint)

    // Nội dung giao dịch
    paint.textSize = 30f
    paint.typeface = Typeface.DEFAULT
    val transactionLines = listOf(
        "Tên: ${transaction.contactName}",
        "SĐT: ${transaction.phoneNumber}",
        "Số tiền: ${formatAmount(transaction.amount)} (${if (transaction.type == "debit") "Nợ tôi" else "Tôi nợ"})",
        "Đã trả: ${formatAmount(paidAmount)} - Còn lại: ${formatAmount(remainingAmount)} ${if(remainingAmount <= 0) " (Đã trả hết)" else ""}",
        "Ngày: ${java.text.SimpleDateFormat("dd/MM/yyyy").format(java.util.Date(transaction.date))}",
        "Ghi chú: ${transaction.note}",
        "Dư nợ: ${formatAmount(balance)}"
    )
    transactionLines.forEachIndexed { index, line ->
        canvas.drawText(line, 50f, 140f + index * 50f, paint)
    }

    // Hiển thị lịch sử thanh toán (nếu có)
    if (payments.isNotEmpty()) {
        val paymentStartY = 140f + transactionLines.size * 50f + 30f // Vị trí bắt đầu của lịch sử thanh toán
        paint.textSize = 30f
        paint.typeface = Typeface.DEFAULT_BOLD
        canvas.drawText("Lịch sử thanh toán:", 50f, paymentStartY, paint)

        paint.typeface = Typeface.DEFAULT
        payments.forEachIndexed { index, payment ->
            val paymentLine = "- ${formatAmount(payment.amount)} (Ngày: ${java.text.SimpleDateFormat("dd/MM/yyyy").format(java.util.Date(payment.date))}) - ${payment.note}"
            canvas.drawText(paymentLine, 50f, paymentStartY + 40f + index * 40f, paint)
        }
    }

    // Lưu ảnh vào cache
    val file = File(context.cacheDir, "transaction_${transaction.id}.png")
    FileOutputStream(file).use { out ->
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
    }

    // Trả về Uri từ FileProvider
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}

fun shareTransactionImage(context: Context, uri: Uri) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Chia sẻ giao dịch qua"))
}