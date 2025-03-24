package com.hnpage.ghinomobile.work

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.hnpage.ghinomobile.data.Transaction
import java.util.concurrent.TimeUnit

class ReminderWorker(appContext: Context, workerParams: WorkerParameters) :
    Worker(appContext, workerParams) {
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun doWork(): Result {
        val title = inputData.getString("title") ?: "Nhắc nợ"
        val body = inputData.getString("body") ?: "Đến hạn thanh toán"

        // Kiểm tra quyền POST_NOTIFICATIONS
        val hasPermission = ContextCompat.checkSelfPermission(
            applicationContext,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            val notification = NotificationCompat.Builder(applicationContext, "debt_channel")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(body)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build()

            NotificationManagerCompat.from(applicationContext).notify(
                inputData.getInt("id", 0),
                notification
            )
            return Result.success()
        } else {
            // Quyền bị từ chối, có thể ghi log hoặc xử lý khác
            println("Permission POST_NOTIFICATIONS not granted, skipping notification")
            return Result.failure() // Hoặc Result.success() nếu không coi đây là lỗi nghiêm trọng
        }
    }
}

fun scheduleReminder(context: Context, transaction: Transaction) {
    val request = OneTimeWorkRequestBuilder<ReminderWorker>()
        .setInitialDelay(10, TimeUnit.SECONDS)
        .setInputData(
            workDataOf(
                "id" to transaction.id.hashCode(),
                "title" to "Nhắc nợ",
                "body" to "Đến hạn thanh toán ${transaction.amount} với ${transaction.contactName}"
            )
        )
        .build()
    WorkManager.getInstance(context).enqueue(request)
}