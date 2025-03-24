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

fun createTransactionImage(context: Context, transaction: Transaction): Uri? {
    val width = 800
    val height = 400
    val bitmap = createBitmap(width, height)
    val canvas = Canvas(bitmap)
    val paint = Paint().apply {
        isAntiAlias = true
        typeface = Typeface.DEFAULT_BOLD
    }

    // Nền trắng
    canvas.drawColor(Color.White.toArgb())

    // Tiêu đề
    paint.color = Color.Black.toArgb()
    paint.textSize = 40f
    canvas.drawText("Rượu-Gạo Tươi Hòa - Thông tin giao dịch", 50f, 80f, paint)

    // Nội dung
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