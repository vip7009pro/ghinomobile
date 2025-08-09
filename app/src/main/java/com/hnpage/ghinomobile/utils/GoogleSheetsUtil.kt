package com.hnpage.ghinomobile.utils

import android.content.Context
import android.util.Log
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.sheets.v4.Sheets
import com.google.api.services.sheets.v4.model.ValueRange
import com.google.auth.http.HttpCredentialsAdapter
import com.google.auth.oauth2.GoogleCredentials
import com.hnpage.ghinomobile.data.Payment
import com.hnpage.ghinomobile.data.Transaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*

object GoogleSheetsUtil {
    private const val APPLICATION_NAME = "GhinoMobile"
    private const val SPREADSHEET_ID = "1oukSQ6n27dWIlxv8F195_OV9hnnpqv_8bCKMcnJvUZE"
    //private const val SPREADSHEET_ID = "1qVa7h0KsZARexejrOMETQTCrN6lF0-vjcytvwIZxE7k"
    private const val TRANSACTIONS_RANGE = "Transactions!A1:H" // Thêm cột H cho "Trạng thái"
    private const val PAYMENTS_RANGE = "Payments!A1:H"       // Thêm cột G cho "Trạng thái"

    // Định dạng số tiền không có phần thập phân
    private val amountFormat = DecimalFormat("#") // Chỉ lấy nguyên, ví dụ: 50000

    private suspend fun getSheetsService(context: Context): Sheets = withContext(Dispatchers.IO) {
        val credentialStream: InputStream = context.assets.open("credentials.json")
        val credentials = GoogleCredentials.fromStream(credentialStream)
            .createScoped(listOf("https://www.googleapis.com/auth/spreadsheets"))
        val httpTransport = GoogleNetHttpTransport.newTrustedTransport()
        Sheets.Builder(httpTransport, GsonFactory.getDefaultInstance(), HttpCredentialsAdapter(credentials))
            .setApplicationName(APPLICATION_NAME)
            .build()
    }

    suspend fun syncTransactionsToSheets(context: Context, transactions: List<Transaction>, payments: List<Payment>) {
        try {
            val sheetsService = getSheetsService(context)

            withContext(Dispatchers.IO) {
                // Đọc dữ liệu hiện có từ Google Sheets
                val currentTransactionsResponse = sheetsService.spreadsheets().values()
                    .get(SPREADSHEET_ID, TRANSACTIONS_RANGE)
                    .execute()
                val currentPaymentsResponse = sheetsService.spreadsheets().values()
                    .get(SPREADSHEET_ID, PAYMENTS_RANGE)
                    .execute()

                // Lấy danh sách ID hiện tại từ ứng dụng
                val transactionIds = transactions.map { it.id }.toSet()
                val paymentKeys = payments.map {
                    it.id
                }.toSet() // Định dạng số tiền thống nhất

                // Chuẩn bị dữ liệu giao dịch
                val transactionData = mutableListOf<List<Any>>()
                transactionData.add(listOf("Tên", "SĐT", "Số tiền", "Loại", "Ngày", "Ghi chú", "Transaction ID", "Trạng thái"))

                // Xử lý dữ liệu hiện có từ Sheets cho giao dịch
                val currentTransactions = currentTransactionsResponse.getValues() ?: emptyList()
                currentTransactions.drop(1).forEach { row -> // Bỏ qua tiêu đề
                    if (row.size >= 7) { // Đảm bảo có ít nhất 7 cột (bao gồm Transaction ID)
                        val transactionId = row[6].toString()
                        if (!transactionIds.contains(transactionId)) {
                            val amount = row[2].toString().toDoubleOrNull() ?: 0.0
                            transactionData.add(
                                listOf(
                                    row[0], // Tên
                                    row[1], // SĐT
                                    amount, // Số tiền (giữ kiểu số)
                                    row[3], // Loại
                                    row[4], // Ngày
                                    row[5], // Ghi chú
                                    row[6], // Transaction ID
                                    "deleted" // Trạng thái
                                )
                            )
                        }
                    }
                }

                // Thêm các giao dịch hiện tại từ ứng dụng
                transactions.forEach { transaction ->
                    transactionData.add(
                        listOf(
                            transaction.contactName,
                            transaction.phoneNumber,
                            transaction.amount, // Giữ nguyên kiểu Double
                            if (transaction.type == "debit") "Nợ tôi" else "Tôi nợ",
                            SimpleDateFormat("dd/MM/yyyy").format(Date(transaction.date)),
                            transaction.note,
                            transaction.id,
                            "active" // Trạng thái mặc định cho bản ghi còn tồn tại
                        )
                    )
                }

                // Chuẩn bị dữ liệu thanh toán
                val paymentData = mutableListOf<List<Any>>()
                paymentData.add(listOf("Tên", "SĐT", "Số tiền thanh toán", "Ngày thanh toán", "Ghi chú", "ID Giao dịch","PaymentID", "Trạng thái" ))

                // Xử lý dữ liệu thanh toán hiện có từ Sheets
                val currentPayments = currentPaymentsResponse.getValues() ?: emptyList()
                val processedPaymentKeys = mutableSetOf<String>() // Theo dõi các dòng đã xử lý
                currentPayments.drop(1).forEach { row -> // Bỏ qua tiêu đề
                    if (row.size >= 6) { // Đảm bảo có ít nhất 6 cột (bao gồm Transaction ID)
                        val paymentTransactionId = row[5].toString()
                        val amount = row[2].toString().toDoubleOrNull() ?: 0.0
                        val date = row[3].toString()
                        val note = if (row.size > 4) row[4].toString() else ""
                        val key = row[6]
                        if (!paymentKeys.contains(key) && !processedPaymentKeys.contains(key)) {
                            paymentData.add(
                                listOf(
                                    row[0], // Tên
                                    row[1], // SĐT
                                    amount, // Số tiền thanh toán (giữ kiểu số)
                                    row[3], // Ngày thanh toán
                                    row[4], // Ghi chú
                                    row[5], // ID Giao dịch
                                    row[6], // PaymentID
                                    "deleted" // Trạng thái
                                )
                            )
                            processedPaymentKeys.add(key.toString()) // Đánh dấu đã xử lý
                        }
                    }
                }

                // Thêm các thanh toán hiện tại từ ứng dụng
                payments.forEach { payment ->
                    val transaction = transactions.find { it.id == payment.transactionId }
                    paymentData.add(
                        listOf(
                            transaction?.contactName ?: "Không xác định",
                            transaction?.phoneNumber ?: "Không xác định",
                            payment.amount, // Giữ nguyên kiểu Double
                            SimpleDateFormat("dd/MM/yyyy").format(Date(payment.date)),
                            payment.note,
                            payment.transactionId,
                            payment.id,
                            "active" // Trạng thái mặc định cho bản ghi còn tồn tại
                        )
                    )
                }

                // Ghi dữ liệu lên Google Sheets
                val transactionBody = ValueRange().setValues(transactionData)
                sheetsService.spreadsheets().values()
                    .update(SPREADSHEET_ID, TRANSACTIONS_RANGE, transactionBody)
                    .setValueInputOption("RAW")
                    .execute()

                val paymentBody = ValueRange().setValues(paymentData)
                sheetsService.spreadsheets().values()
                    .update(SPREADSHEET_ID, PAYMENTS_RANGE, paymentBody)
                    .setValueInputOption("RAW")
                    .execute()
            }

            withContext(Dispatchers.Main) {
                android.widget.Toast.makeText(context, "Đã đồng bộ với Google Sheets", android.widget.Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                android.widget.Toast.makeText(context, "Lỗi đồng bộ: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
                Log.d("SyncError", "Lỗi đồng bộ: ${e.message}")
            }
        }
    }
}