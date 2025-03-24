package com.hnpage.ghinomobile.screen

import android.content.Context
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hnpage.ghinomobile.data.Transaction
import com.hnpage.ghinomobile.utils.createTransactionImage
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
    var showAddDialog by remember { mutableStateOf(false) }
    var editTransaction by remember { mutableStateOf<Transaction?>(null) }
    var deleteTransaction by remember { mutableStateOf<Transaction?>(null) }
    var confirmUpdateTransaction by remember { mutableStateOf<Transaction?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var fabExpanded by remember { mutableStateOf(false) }

    val filteredTransactions = transactions.filter {
        it.contactName.contains(searchQuery, ignoreCase = true) ||
                it.phoneNumber.contains(searchQuery, ignoreCase = true)
    }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .background(color = MaterialTheme.colorScheme.primary),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Lịch sử giao dịch",
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            }
        },
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End) {
                if (fabExpanded) {
                    FloatingActionButton(
                        onClick = {
                            showAddDialog = true
                            fabExpanded = false
                        },
                        containerColor = MaterialTheme.colorScheme.tertiary,
                        contentColor = MaterialTheme.colorScheme.onTertiary,
                        shape = CircleShape,
                        modifier = Modifier
                            .padding(bottom = 8.dp)
                            .size(48.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Thêm giao dịch")
                    }
                    FloatingActionButton(
                        onClick = {
                            exportTransactions(context, filteredTransactions)
                            fabExpanded = false
                        },
                        containerColor = MaterialTheme.colorScheme.secondary,
                        contentColor = MaterialTheme.colorScheme.onSecondary,
                        shape = CircleShape,
                        modifier = Modifier
                            .padding(bottom = 8.dp)
                            .size(48.dp)
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
        containerColor = MaterialTheme.colorScheme.surface
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 24.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Tìm kiếm giao dịch") },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.onSurface
                )
            )
            Spacer(modifier = Modifier.height(16.dp))
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(filteredTransactions) { transaction ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        ListItem(
                            headlineContent = {
                                Text(
                                    color = if (transaction.type == "debit") MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary,
                                    text = "${transaction.contactName}: ${formatAmount(transaction.amount)} (${if (transaction.type == "debit") "Nợ tôi" else "Tôi nợ"})",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            },
                            supportingContent = {
                                Text(
                                    "SĐT: ${transaction.phoneNumber}\n" +
                                            "Ngày: ${SimpleDateFormat("dd/MM/yyyy").format(Date(transaction.date))}\n" +
                                            "Ghi chú: ${transaction.note}",
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            },
                            trailingContent = {
                                Row {
                                    IconButton(onClick = { editTransaction = transaction }) {
                                        Icon(Icons.Default.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
                                    }
                                    IconButton(onClick = { deleteTransaction = transaction }) {
                                        Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
                                    }
                                    IconButton(onClick = {
                                        val uri = createTransactionImage(context, transaction)
                                        uri?.let { shareTransactionImage(context, it) }
                                    }) {
                                        Icon(Icons.Default.Share, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }

        if (showAddDialog) {
            TransactionDialog(
                onDismiss = { showAddDialog = false },
                onAdd = { transaction ->
                    viewModel.insert(transaction)
                    if (transaction.isReminderSet) scheduleReminder(context, transaction)
                    showAddDialog = false
                }
            )
        }
        if (editTransaction != null) {
            TransactionDialog(
                transaction = editTransaction,
                onDismiss = { editTransaction = null },
                onAdd = { updated ->
                    confirmUpdateTransaction = updated
                    editTransaction = null
                }
            )
        }
        if (deleteTransaction != null) {
            AlertDialog(
                onDismissRequest = { deleteTransaction = null },
                title = { Text("Xác nhận xóa") },
                text = { Text("Bạn có chắc muốn xóa giao dịch này?") },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.delete(deleteTransaction!!)
                        deleteTransaction = null
                    }) { Text("Xác nhận") }
                },
                dismissButton = {
                    TextButton(onClick = { deleteTransaction = null }) { Text("Hủy") }
                }
            )
        }
        if (confirmUpdateTransaction != null) {
            AlertDialog(
                onDismissRequest = { confirmUpdateTransaction = null },
                title = { Text("Xác nhận cập nhật") },
                text = { Text("Bạn có chắc muốn cập nhật giao dịch này?") },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.update(confirmUpdateTransaction!!)
                        if (confirmUpdateTransaction!!.isReminderSet) scheduleReminder(context, confirmUpdateTransaction!!)
                        confirmUpdateTransaction = null
                    }) { Text("Xác nhận") }
                },
                dismissButton = {
                    TextButton(onClick = { confirmUpdateTransaction = null }) { Text("Hủy") }
                }
            )
        }
    }
}

private fun exportTransactions(context: Context, transactions: List<Transaction>) {
    // Xuất ra Excel
    val workbook = XSSFWorkbook()
    val sheet = workbook.createSheet("TransactionHistory")
    val header = sheet.createRow(0)
    header.createCell(0).setCellValue("Tên")
    header.createCell(1).setCellValue("SĐT")
    header.createCell(2).setCellValue("Số tiền")
    header.createCell(3).setCellValue("Loại")
    header.createCell(4).setCellValue("Ngày")
    header.createCell(5).setCellValue("Ghi chú")

    transactions.forEachIndexed { index, transaction ->
        val row = sheet.createRow(index + 1)
        row.createCell(0).setCellValue(transaction.contactName)
        row.createCell(1).setCellValue(transaction.phoneNumber)
        row.createCell(2).setCellValue(transaction.amount)
        row.createCell(3).setCellValue(if (transaction.type == "debit") "Nợ tôi" else "Tôi nợ")
        row.createCell(4).setCellValue(SimpleDateFormat("dd/MM/yyyy").format(Date(transaction.date)))
        row.createCell(5).setCellValue(transaction.note)
    }

    val excelFile = File(
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
        "TransactionHistory_${System.currentTimeMillis()}.xlsx"
    )
    FileOutputStream(excelFile).use { outputStream ->
        workbook.write(outputStream) // Ghi trực tiếp vào OutputStream
    }
    workbook.close()

    // Xuất ra PDF
    val pdfDocument = PdfDocument()
    val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 size
    val page = pdfDocument.startPage(pageInfo)
    val canvas = page.canvas
    var yPos = 50f
    canvas.drawText("Lịch sử giao dịch", 50f, yPos, android.graphics.Paint().apply { textSize = 20f })
    yPos += 30f
    transactions.forEach { transaction ->
        canvas.drawText(
            "${transaction.contactName}: ${formatAmount(transaction.amount)} (${if (transaction.type == "debit") "Nợ tôi" else "Tôi nợ"})",
            50f, yPos, android.graphics.Paint().apply { textSize = 14f }
        )
        yPos += 20f
    }
    pdfDocument.finishPage(page)

    val pdfFile = File(
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
        "TransactionHistory_${System.currentTimeMillis()}.pdf"
    )
    pdfDocument.writeTo(FileOutputStream(pdfFile))
    pdfDocument.close()

    // Thông báo người dùng
    android.widget.Toast.makeText(context, "Đã xuất file vào Downloads", android.widget.Toast.LENGTH_SHORT).show()
}