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
import com.hnpage.ghinomobile.data.Transaction
import java.io.File
import java.io.FileOutputStream
import androidx.core.graphics.createBitmap
import android.graphics.LinearGradient
import android.graphics.Shader

fun createTransactionImage(context: Context, transaction: Transaction): Uri? {
    val width = 800
    val height = 400
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
            Color(0xFFFFCC80).toArgb(), // Cam nhạt
            Color(0xFFFF5722).toArgb(), // Cam đậm
            Color(0xFFD32F2F).toArgb()  // Đỏ đậm
        ),
        null,
        Shader.TileMode.CLAMP
    )

    // Gradient cho Credit (Có) - Sắc thái xanh lá thiên nhiên
    val creditGradient = LinearGradient(
        0f, 0f, width.toFloat(), 0f, // Gradient ngang
        intArrayOf(
            Color(0xFFA5D6A7).toArgb(), // Xanh lá nhạt
            Color(0xFF4CAF50).toArgb(), // Xanh lá trung
            Color(0xFF2E7D32).toArgb()  // Xanh lá đậm
        ),
        null,
        Shader.TileMode.CLAMP
    )

    // Áp dụng gradient làm nền dựa trên type
    paint.shader = if (transaction.type == "debit") debitGradient else creditGradient
    canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
    paint.shader = null // Reset shader sau khi vẽ nền

    // Màu chữ tương phản
    val textColor = Color(0xFF1B5E20).toArgb() // Xanh lá đậm

    // Tiêu đề
    paint.color = Color.White.toArgb() // Chữ trắng để nổi bật trên gradient
    paint.textSize = 40f
    paint.typeface = Typeface.DEFAULT_BOLD
    canvas.drawText("Rượu-Gạo Tươi Hòa - Thông tin giao dịch", 50f, 80f, paint)

    // Nội dung
    paint.color = Color.White.toArgb() // Chữ trắng để tương phản với gradient
    paint.textSize = 30f
    paint.typeface = Typeface.DEFAULT
    val lines = listOf(
        "Tên: ${transaction.contactName}",
        "SĐT: ${transaction.phoneNumber}",
        "Số tiền: ${formatAmount(transaction.amount)} (${if (transaction.type == "debit") "Nợ tôi" else "Tôi nợ"})",
        "Ngày: ${java.text.SimpleDateFormat("dd/MM/yyyy").format(java.util.Date(transaction.date))}",
        "Ghi chú: ${transaction.note}"
    )
    lines.forEachIndexed { index, line ->
        canvas.drawText(line, 50f, 140f + index * 50f, paint)
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