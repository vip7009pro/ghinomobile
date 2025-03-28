package com.hnpage.ghinomobile.screen

import android.content.Context
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Environment
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hnpage.ghinomobile.data.Payment
import com.hnpage.ghinomobile.data.Transaction
import com.hnpage.ghinomobile.utils.createTransactionImage
import androidx.compose.ui.text.style.TextOverflow
import com.hnpage.ghinomobile.utils.formatAmount
import com.hnpage.ghinomobile.utils.shareTransactionImage
import com.hnpage.ghinomobile.viewmodel.DebtViewModel
import com.hnpage.ghinomobile.work.scheduleReminder
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionHistoryScreen(viewModel: DebtViewModel) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val transactions by viewModel.transactions.collectAsState(initial = emptyList())
    val balances by viewModel.balanceByContact.collectAsState(initial = emptyMap())
    val allPayments = viewModel.payments.collectAsState(initial = emptyList()).value
    var showAddDialog by remember { mutableStateOf(false) }
    var editTransaction by remember { mutableStateOf<Transaction?>(null) }
    var deleteTransaction by remember { mutableStateOf<Transaction?>(null) }
    var confirmUpdateTransaction by remember { mutableStateOf<Transaction?>(null) }
    var showPaymentDialog by remember { mutableStateOf<Transaction?>(null) }
    var editPayment by remember { mutableStateOf<Payment?>(null) }
    var deletePayment by remember { mutableStateOf<Payment?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var fabExpanded by remember { mutableStateOf(false) }

    val filteredTransactions = transactions.filter {
        it.contactName.contains(searchQuery, ignoreCase = true) ||
                it.phoneNumber.contains(searchQuery, ignoreCase = true)
    }

    val debitGradient = Brush.horizontalGradient(
        colors = listOf(Color(0xFFD32F2F),Color(0xFFFF5722), Color(0xFFFFCC80))
    )
    val creditGradient = Brush.horizontalGradient(
        colors = listOf(Color(0xFF2E7D32),Color(0xFF4CAF50), Color(0xFFA5D6A7))
    )
    val backgroundColor = Color(0xFFF1F8E9)
    val textColor = Color(0xFF1B5E20)

    Scaffold(
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End) {
                if (fabExpanded) {
                    FloatingActionButton(
                        onClick = { showAddDialog = true; fabExpanded = false },
                        containerColor = MaterialTheme.colorScheme.tertiary,
                        contentColor = MaterialTheme.colorScheme.onTertiary,
                        shape = CircleShape,
                        modifier = Modifier.padding(bottom = 8.dp).size(48.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Thêm giao dịch")
                    }
                    FloatingActionButton(
                        onClick = { exportTransactions(context, filteredTransactions,allPayments); fabExpanded = false },
                        containerColor = MaterialTheme.colorScheme.secondary,
                        contentColor = MaterialTheme.colorScheme.onSecondary,
                        shape = CircleShape,
                        modifier = Modifier.padding(bottom = 8.dp).size(48.dp)
                    ) {
                        Icon(Icons.Default.FileDownload, contentDescription = "Xuất giao dịch")
                    }
                }
                FloatingActionButton(
                    onClick = { fabExpanded = !fabExpanded },
                    containerColor = MaterialTheme.colorScheme.tertiary,
                    contentColor = MaterialTheme.colorScheme.onTertiary,
                    shape = CircleShape,
                    modifier = Modifier.size(56.dp),
                    elevation = FloatingActionButtonDefaults.elevation(8.dp)
                ) {
                    Icon(
                        imageVector = if (fabExpanded) Icons.Default.Close else Icons.Default.Add,
                        contentDescription = "Mở/Đóng FAB"
                    )
                }
            }
        },
        containerColor = backgroundColor
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(0.dp)
                .padding(horizontal = 5.dp, vertical = 5.dp)
                .background(backgroundColor)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Tìm kiếm giao dịch") },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = textColor,
                    unfocusedTextColor = textColor,
                    disabledTextColor = textColor.copy(alpha = 0.7f),
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = textColor,
                    disabledBorderColor = textColor.copy(alpha = 0.7f),
                    focusedLabelColor = textColor,
                    unfocusedLabelColor = textColor
                )
            )
            Spacer(modifier = Modifier.height(16.dp))
            LazyColumn(
                modifier = Modifier.background(backgroundColor),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(filteredTransactions) { transaction ->
                    val balance = balances.entries.find { it.key.first == transaction.phoneNumber }?.value ?: 0.0
                    val paidAmount by viewModel.getPaidAmountForTransaction(transaction.id).collectAsState(initial = 0.0)
                    val payments by viewModel.getPaymentsByTransaction(transaction.id).collectAsState(initial = emptyList())
                    val remainingAmount = transaction.amount - paidAmount
                    val isPaid = remainingAmount <= 0

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(if (isPaid) creditGradient else debitGradient)
                                .padding(10.dp)
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            color = Color.White,
                                            text = transaction.contactName,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            color = Color.White,
                                            text = "${formatAmount(transaction.amount)} (${if (transaction.type == "debit") "Nợ tôi" else "Tôi nợ"})",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            color = Color.White,
                                            text = "Đã trả: ${formatAmount(paidAmount)} - Còn lại: ${formatAmount(remainingAmount)}",
                                            fontSize = 14.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "SĐT: ${transaction.phoneNumber}",
                                            fontSize = 14.sp,
                                            color = Color.White,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "Ngày: ${SimpleDateFormat("dd/MM/yyyy").format(Date(transaction.date))}",
                                            fontSize = 14.sp,
                                            color = Color.White,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "Ghi chú: ${transaction.note}",
                                            fontSize = 14.sp,
                                            color = Color.White,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Column {
                                        IconButton(onClick = { editTransaction = transaction }) {
                                            Icon(Icons.Default.Edit, contentDescription = "Sửa giao dịch", tint = Color.White)
                                        }
                                        IconButton(onClick = { deleteTransaction = transaction }) {
                                            Icon(Icons.Default.Delete, contentDescription = "Xóa giao dịch", tint = Color.White)
                                        }
                                        IconButton(onClick = {
                                            val uri = createTransactionImage(context, transaction, transaction.amount- paidAmount, paidAmount, payments)
                                            uri?.let { shareTransactionImage(context, it) }
                                        }) {
                                            Icon(Icons.Default.Share, contentDescription = "Chia sẻ", tint = Color.White)
                                        }
                                        IconButton(onClick = {
                                            showPaymentDialog = transaction.copy(amount  = remainingAmount)
                                        }) {
                                            Icon(Icons.Default.Money, contentDescription = "Thêm thanh toán", tint = Color.White)
                                        }
                                    }
                                }
                                if (payments.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Lịch sử thanh toán:",
                                        fontSize = 14.sp,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                    payments.forEach { payment ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "- ${formatAmount(payment.amount)} (Ngày: ${SimpleDateFormat("dd/MM/yyyy").format(Date(payment.date))}) - ${payment.note}",
                                                fontSize = 12.sp,
                                                color = Color.White,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Row {
                                                IconButton(onClick = { editPayment = payment }) {
                                                    Icon(Icons.Default.Edit, contentDescription = "Sửa thanh toán", tint = Color.White, modifier = Modifier.size(16.dp))
                                                }
                                                IconButton(onClick = { deletePayment = payment }) {
                                                    Icon(Icons.Default.Delete, contentDescription = "Xóa thanh toán", tint = Color.White, modifier = Modifier.size(16.dp))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showAddDialog) {
            TransactionDialog(
                onDismiss = { showAddDialog = false },
                onAddTransaction = { transaction ->
                    viewModel.insertTransaction(transaction)
                    if (transaction.isReminderSet) scheduleReminder(context, transaction)
                    showAddDialog = false
                },
                onAddPayment = {}
            )
        }

        if (editTransaction != null) {
            TransactionDialog(
                transaction = editTransaction,
                isPaymentMode = false,
                isEditPaymentMode = false, // Không phải sửa thanh toán
                onDismiss = { editTransaction = null },
                onAddTransaction = { updated ->
                    confirmUpdateTransaction = updated
                    editTransaction = null
                },
                onAddPayment = {}
            )
        }

        if (showPaymentDialog != null) {
            TransactionDialog(
                transaction = showPaymentDialog,
                isPaymentMode = true,
                isEditPaymentMode = false, // Thêm thanh toán
                onDismiss = { showPaymentDialog = null },
                onAddTransaction = {},
                onAddPayment = { payment ->
                    viewModel.insertPayment(payment)
                    showPaymentDialog = null
                }
            )
        }

        if (editPayment != null) {
            TransactionDialog(
                transaction = Transaction(
                    id = editPayment!!.transactionId,
                    contactName = filteredTransactions.find { it.id == editPayment!!.transactionId }?.contactName ?: "",
                    phoneNumber = filteredTransactions.find { it.id == editPayment!!.transactionId }?.phoneNumber ?: "",
                    amount = editPayment!!.amount,
                    type = "",
                    date = 0L,
                    note = editPayment!!.note,
                    isReminderSet = false
                ),
                isPaymentMode = true,
                isEditPaymentMode = true, // Sửa thanh toán
                onDismiss = { editPayment = null },
                onAddTransaction = {},
                onAddPayment = { updatedPayment ->
                    viewModel.updatePayment(updatedPayment.copy(id = editPayment!!.id, transactionId = editPayment!!.transactionId))
                    editPayment = null
                }
            )
        }

        if (deleteTransaction != null) {
            AlertDialog(
                onDismissRequest = { deleteTransaction = null },
                title = { Text("Xác nhận xóa", color = textColor) },
                text = { Text("Bạn có chắc muốn xóa giao dịch này?", color = textColor) },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.deleteTransaction(deleteTransaction!!)
                        deleteTransaction = null
                    }) { Text("Xác nhận", color = Color.Red) }
                },
                dismissButton = {
                    TextButton(onClick = { deleteTransaction = null }) { Text("Hủy", color = textColor) }
                },
                containerColor = backgroundColor
            )
        }

        if (deletePayment != null) {
            AlertDialog(
                onDismissRequest = { deletePayment = null },
                title = { Text("Xác nhận xóa", color = textColor) },
                text = { Text("Bạn có chắc muốn xóa lần thanh toán này?", color = textColor) },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.deletePayment(deletePayment!!)
                        deletePayment = null
                    }) { Text("Xác nhận", color = Color.Red) }
                },
                dismissButton = {
                    TextButton(onClick = { deletePayment = null }) { Text("Hủy", color = textColor) }
                },
                containerColor = backgroundColor
            )
        }

        if (confirmUpdateTransaction != null) {
            AlertDialog(
                onDismissRequest = { confirmUpdateTransaction = null },
                title = { Text("Xác nhận cập nhật", color = MaterialTheme.colorScheme.onSurface) },
                text = { Text("Bạn có chắc muốn cập nhật giao dịch này?", color = MaterialTheme.colorScheme.onSurface) },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.updateTransaction(confirmUpdateTransaction!!)
                        if (confirmUpdateTransaction!!.isReminderSet) scheduleReminder(context, confirmUpdateTransaction!!)
                        confirmUpdateTransaction = null
                    }) { Text("Xác nhận", color = MaterialTheme.colorScheme.primary) }
                },
                dismissButton = {
                    TextButton(onClick = { confirmUpdateTransaction = null }) { Text("Hủy", color = textColor) }
                },
                containerColor = backgroundColor
            )
        }
    }
}

private fun exportTransactions(context: Context, transactions: List<Transaction>, payments: List<Payment>) {
    // Lọc các thanh toán liên quan đến transactions được lọc
    val filteredTransactionIds = transactions.map { it.id }.toSet()
    val filteredPayments = payments.filter { it.transactionId in filteredTransactionIds }

    // Xuất ra Excel
    val workbook = XSSFWorkbook()

    // Sheet 1: Lịch sử giao dịch
    val transactionSheet = workbook.createSheet("TransactionHistory")
    val transactionHeader = transactionSheet.createRow(0)
    transactionHeader.createCell(0).setCellValue("Tên")
    transactionHeader.createCell(1).setCellValue("SĐT")
    transactionHeader.createCell(2).setCellValue("Số tiền")
    transactionHeader.createCell(3).setCellValue("Loại")
    transactionHeader.createCell(4).setCellValue("Ngày")
    transactionHeader.createCell(5).setCellValue("Ghi chú")
    transactionHeader.createCell(6).setCellValue("Transaction ID") // Thêm cột Transaction ID

    transactions.forEachIndexed { index, transaction ->
        val row = transactionSheet.createRow(index + 1)
        row.createCell(0).setCellValue(transaction.contactName)
        row.createCell(1).setCellValue(transaction.phoneNumber)
        row.createCell(2).setCellValue(transaction.amount)
        row.createCell(3).setCellValue(if (transaction.type == "debit") "Nợ tôi" else "Tôi nợ")
        row.createCell(4).setCellValue(SimpleDateFormat("dd/MM/yyyy").format(Date(transaction.date)))
        row.createCell(5).setCellValue(transaction.note)
        row.createCell(6).setCellValue(transaction.id) // Gán giá trị Transaction ID
    }

    // Sheet 2: Lịch sử thanh toán (chỉ cho các giao dịch được lọc)
    val paymentSheet = workbook.createSheet("PaymentHistory")
    val paymentHeader = paymentSheet.createRow(0)
    paymentHeader.createCell(0).setCellValue("Tên")
    paymentHeader.createCell(1).setCellValue("SĐT")
    paymentHeader.createCell(2).setCellValue("Số tiền thanh toán")
    paymentHeader.createCell(3).setCellValue("Ngày thanh toán")
    paymentHeader.createCell(4).setCellValue("Ghi chú")
    paymentHeader.createCell(5).setCellValue("ID Giao dịch")

    filteredPayments.forEachIndexed { index, payment ->
        val transaction = transactions.find { it.id == payment.transactionId }
        val row = paymentSheet.createRow(index + 1)
        row.createCell(0).setCellValue(transaction?.contactName ?: "Không xác định")
        row.createCell(1).setCellValue(transaction?.phoneNumber ?: "Không xác định")
        row.createCell(2).setCellValue(payment.amount)
        row.createCell(3).setCellValue(SimpleDateFormat("dd/MM/yyyy").format(Date(payment.date)))
        row.createCell(4).setCellValue(payment.note)
        row.createCell(5).setCellValue(payment.transactionId)
    }

    val excelFile = File(
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
        "TransactionHistory_${System.currentTimeMillis()}.xlsx"
    )
    FileOutputStream(excelFile).use { outputStream ->
        workbook.write(outputStream)
    }
    workbook.close()

    // Xuất ra PDF
    val pdfDocument = PdfDocument()

    // Trang 1: Lịch sử giao dịch
    val transactionPageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 size
    val transactionPage = pdfDocument.startPage(transactionPageInfo)
    val transactionCanvas = transactionPage.canvas
    var transactionYPos = 50f
    val titlePaint = android.graphics.Paint().apply { textSize = 20f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) }
    val textPaint = android.graphics.Paint().apply { textSize = 14f }
    transactionCanvas.drawText("Lịch sử giao dịch", 50f, transactionYPos, titlePaint)
    transactionYPos += 30f
    for (transaction in transactions) {
        if (transactionYPos > 800f) { // Nếu hết trang, dừng lại
            break
        }
        val line = "${transaction.contactName} (${transaction.phoneNumber}): ${formatAmount(transaction.amount)} (${if (transaction.type == "debit") "Nợ tôi" else "Tôi nợ"}) - Ngày: ${SimpleDateFormat("dd/MM/yyyy").format(Date(transaction.date))} - ${transaction.note} - ID: ${transaction.id}"
        transactionCanvas.drawText(line, 50f, transactionYPos, textPaint)
        transactionYPos += 20f
    }
    pdfDocument.finishPage(transactionPage)

    // Trang 2: Lịch sử thanh toán (chỉ cho các giao dịch được lọc)
    val paymentPageInfo = PdfDocument.PageInfo.Builder(595, 842, 2).create() // Trang thứ 2
    val paymentPage = pdfDocument.startPage(paymentPageInfo)
    val paymentCanvas = paymentPage.canvas
    var paymentYPos = 50f
    paymentCanvas.drawText("Lịch sử thanh toán", 50f, paymentYPos, titlePaint)
    paymentYPos += 30f
    for (payment in filteredPayments) {
        if (paymentYPos > 800f) { // Nếu hết trang, dừng lại
            break
        }
        val transaction = transactions.find { it.id == payment.transactionId }
        val line = "${transaction?.contactName ?: "Không xác định"} (${transaction?.phoneNumber ?: "N/A"}): ${formatAmount(payment.amount)} - Ngày: ${SimpleDateFormat("dd/MM/yyyy").format(Date(payment.date))} - ${payment.note} - ID: ${payment.transactionId}"
        paymentCanvas.drawText(line, 50f, paymentYPos, textPaint)
        paymentYPos += 20f
    }
    pdfDocument.finishPage(paymentPage)

    val pdfFile = File(
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
        "TransactionHistory_${System.currentTimeMillis()}.pdf"
    )
    pdfDocument.writeTo(FileOutputStream(pdfFile))
    pdfDocument.close()

    // Thông báo người dùng
    android.widget.Toast.makeText(context, "Đã xuất file vào Downloads", android.widget.Toast.LENGTH_SHORT).show()
}