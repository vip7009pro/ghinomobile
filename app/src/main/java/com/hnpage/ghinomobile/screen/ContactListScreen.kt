package com.hnpage.ghinomobile.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Money
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hnpage.ghinomobile.data.Payment
import com.hnpage.ghinomobile.data.Transaction
import com.hnpage.ghinomobile.utils.createTransactionImage
import com.hnpage.ghinomobile.utils.formatAmount
import com.hnpage.ghinomobile.utils.shareTransactionImage
import com.hnpage.ghinomobile.viewmodel.DebtViewModel
import com.hnpage.ghinomobile.work.scheduleReminder
import java.text.SimpleDateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactListScreen(viewModel: DebtViewModel, onContactClick: (String) -> Unit) {
    val contactStats by viewModel.getContactStats().collectAsState(initial = emptyMap())
    var searchQuery by remember { mutableStateOf("") }

    val debitGradient = Brush.horizontalGradient(
        colors = listOf(Color(0xFFD32F2F),Color(0xFFFF5722), Color(0xFFFFCC80))
    )
    val creditGradient = Brush.horizontalGradient(
        colors = listOf(Color(0xFF2E7D32),Color(0xFF4CAF50), Color(0xFFA5D6A7))
    )
    val neutralGradient = Brush.horizontalGradient(
        colors = listOf(Color(0xFF2E7D32),Color(0xFF4CAF50), Color(0xFFA5D6A7))
    )
    val backgroundColor = Color(0xFFF1F8E9)

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { /* Không có hành động trực tiếp */ },
                containerColor = MaterialTheme.colorScheme.tertiary,
                contentColor = MaterialTheme.colorScheme.onTertiary,
                shape = CircleShape,
                modifier = Modifier.size(56.dp),
                elevation = FloatingActionButtonDefaults.elevation(8.dp)
            ) {
                Text("+", fontSize = 24.sp)
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
                label = { Text("Tìm kiếm danh bạ") },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color(0xFF1B5E20),
                    unfocusedTextColor = Color(0xFF1B5E20),
                    disabledTextColor = Color(0xFF1B5E20).copy(alpha = 0.7f),
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = Color(0xFF1B5E20),
                    disabledBorderColor = Color(0xFF1B5E20).copy(alpha = 0.7f),
                    focusedLabelColor = Color(0xFF1B5E20),
                    unfocusedLabelColor = Color(0xFF1B5E20)
                )
            )
            Spacer(modifier = Modifier.height(16.dp))
            LazyColumn(
                modifier = Modifier.background(backgroundColor),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(
                    contactStats.toList().filter { (contactPair, _) ->
                        val (phone, name) = contactPair
                        name.contains(searchQuery, ignoreCase = true) || phone.contains(searchQuery, ignoreCase = true)
                    }
                ) { (contactPair, stats) ->
                    val (phone, name) = contactPair
                    val (totalDebt, totalPaid, remainingBalance) = stats

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onContactClick(phone) },
                        shape = RoundedCornerShape(8.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    when {
                                        (totalDebt-totalPaid) > 0 -> debitGradient // Nợ tôi (chưa trả hết)
                                        (totalDebt-totalPaid) < 0 -> creditGradient // Tôi nợ (chưa trả hết)
                                        else -> neutralGradient // Không nợ
                                    }
                                )
                                .padding(16.dp)
                        ) {
                            Column {
                                Text(
                                    text = name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "SĐT: $phone",
                                    fontSize = 14.sp,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "Tổng nợ: ${formatAmount(totalDebt)}",
                                    fontSize = 14.sp,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "Tổng đã trả: ${formatAmount(totalPaid)}",
                                    fontSize = 14.sp,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "Dư nợ: ${formatAmount(totalDebt-totalPaid)}",
                                    fontSize = 14.sp,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactDetailScreen(viewModel: DebtViewModel, phoneNumber: String, onBack: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val transactions by viewModel.getTransactionsByPhone(phoneNumber).collectAsState(initial = emptyList())
    val balances by viewModel.balanceByContact.collectAsState(initial = emptyMap())
    var showAddDialog by remember { mutableStateOf(false) }
    var showPaymentDialog by remember { mutableStateOf<Transaction?>(null) }
    var editTransaction by remember { mutableStateOf<Transaction?>(null) }
    var deleteTransaction by remember { mutableStateOf<Transaction?>(null) }
    var confirmUpdateTransaction by remember { mutableStateOf<Transaction?>(null) }
    var editPayment by remember { mutableStateOf<Payment?>(null) }
    var deletePayment by remember { mutableStateOf<Payment?>(null) }
    val contactName = transactions.maxByOrNull { it.date }?.contactName ?: "Không xác định"
    val balance = balances.entries.find { it.key.first == phoneNumber }?.value ?: 0.0

    val debitGradient = Brush.horizontalGradient(
        colors = listOf(Color(0xFFD32F2F),Color(0xFFFF5722), Color(0xFFFFCC80))
    )
    val creditGradient = Brush.horizontalGradient(
        colors = listOf(Color(0xFF2E7D32),Color(0xFF4CAF50), Color(0xFFA5D6A7))
    )
    val neutralGradient = Brush.horizontalGradient(
        colors = listOf(Color(0xFFE0E0E0), Color(0xFFB0B0B0)) // Không nợ (xám)
    )
    val backgroundColor = Color(0xFFF1F8E9)

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .background(color = Color.Transparent),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
                }
                Text(
                    text = "Chi tiết - $contactName",
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
                Spacer(modifier = Modifier.width(48.dp))
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.tertiary,
                contentColor = MaterialTheme.colorScheme.onTertiary,
                shape = CircleShape,
                modifier = Modifier.size(56.dp),
                elevation = FloatingActionButtonDefaults.elevation(8.dp)
            ) {
                Text("+", fontSize = 24.sp)
            }
        },
        containerColor = backgroundColor
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 24.dp)
                .background(backgroundColor),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(transactions) { transaction ->
                val payments by viewModel.getPaymentsByTransaction(transaction.id).collectAsState(initial = emptyList())
                val paidAmount by viewModel.getPaidAmountForTransaction(transaction.id).collectAsState(initial = 0.0)
                val remainingAmount = transaction.amount - paidAmount

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                when {
                                    remainingAmount == 0.0 -> creditGradient // Đã trả hết
                                    transaction.type == "debit" -> debitGradient // Nợ tôi, chưa trả hết
                                    else -> debitGradient // Tôi nợ, chưa trả hết
                                }
                            )
                            .padding(16.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        color = Color.White,
                                        text = transaction.contactName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        color = Color.White,
                                        text = "${formatAmount(transaction.amount)} (${if (transaction.type == "debit") "Nợ tôi" else "Tôi nợ"})",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                    )

                                    Text(
                                        text = "Còn lại: ${formatAmount(remainingAmount)}",
                                        fontSize = 14.sp,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "Ngày: ${SimpleDateFormat("dd/MM/yyyy").format(Date(transaction.date))}",
                                        fontSize = 14.sp,
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "Ghi chú: ${transaction.note}",
                                        fontSize = 14.sp,
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                    )
                                }
                                Column {
                                    IconButton(onClick = { editTransaction = transaction }) {
                                        Icon(Icons.Default.Edit, contentDescription = null, tint = Color.White)
                                    }
                                    IconButton(onClick = { deleteTransaction = transaction }) {
                                        Icon(Icons.Default.Delete, contentDescription = null, tint = Color.White)
                                    }
                                    IconButton(onClick = {
                                        val uri = createTransactionImage(context, transaction, transaction.amount- paidAmount, paidAmount, payments)
                                        uri?.let { shareTransactionImage(context, it) }
                                    }) {
                                        Icon(Icons.Default.Share, contentDescription = null, tint = Color.White)
                                    }
                                    IconButton(onClick = {
                                        //make showPaymentDialog = transaction with amount = remainingAmount
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

        if (showAddDialog) {
            TransactionDialog(
                onDismiss = { showAddDialog = false },
                onAddTransaction = { transaction ->
                    viewModel.insertTransaction(transaction)
                    if (transaction.isReminderSet) scheduleReminder(context, transaction)
                    showAddDialog = false
                },
                onAddPayment = {},
                prefilledContactName = contactName,
                prefilledPhoneNumber = phoneNumber
            )
        }

        if (showPaymentDialog != null) {
            TransactionDialog(
                transaction = showPaymentDialog,
                isPaymentMode = true,
                isEditPaymentMode = false,
                onDismiss = { showPaymentDialog = null },
                onAddTransaction = {},
                onAddPayment = { payment ->
                    viewModel.insertPayment(payment)
                    showPaymentDialog = null
                }
            )
        }

        if (editTransaction != null) {
            TransactionDialog(
                transaction = editTransaction,
                isPaymentMode = false,
                isEditPaymentMode = false,
                onDismiss = { editTransaction = null },
                onAddTransaction = { updated ->
                    confirmUpdateTransaction = updated
                    editTransaction = null
                },
                onAddPayment = {}
            )
        }

        if (editPayment != null) {
            TransactionDialog(
                transaction = Transaction(
                    id = editPayment!!.transactionId,
                    contactName = contactName,
                    phoneNumber = phoneNumber,
                    amount = editPayment!!.amount,
                    type = "",
                    date = 0L,
                    note = editPayment!!.note,
                    isReminderSet = false
                ),
                isPaymentMode = true,
                isEditPaymentMode = true,
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
                title = { Text("Xác nhận xóa", color = MaterialTheme.colorScheme.onSurface) },
                text = { Text("Bạn có chắc muốn xóa giao dịch này?", color = MaterialTheme.colorScheme.onSurface) },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.deleteTransaction(deleteTransaction!!)
                        deleteTransaction = null
                    }) {
                        Text("Xác nhận", color = MaterialTheme.colorScheme.primary)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { deleteTransaction = null }) {
                        Text("Hủy", color = MaterialTheme.colorScheme.onSurface)
                    }
                },
                containerColor = backgroundColor,
                shape = RoundedCornerShape(12.dp)
            )
        }

        if (deletePayment != null) {
            AlertDialog(
                onDismissRequest = { deletePayment = null },
                title = { Text("Xác nhận xóa", color = MaterialTheme.colorScheme.onSurface) },
                text = { Text("Bạn có chắc muốn xóa lần thanh toán này?", color = MaterialTheme.colorScheme.onSurface) },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.deletePayment(deletePayment!!)
                        deletePayment = null
                    }) {
                        Text("Xác nhận", color = MaterialTheme.colorScheme.primary)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { deletePayment = null }) {
                        Text("Hủy", color = MaterialTheme.colorScheme.onSurface)
                    }
                },
                containerColor = backgroundColor,
                shape = RoundedCornerShape(12.dp)
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
                    }) {
                        Text("Xác nhận", color = MaterialTheme.colorScheme.primary)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { confirmUpdateTransaction = null }) {
                        Text("Hủy", color = MaterialTheme.colorScheme.onSurface)
                    }
                },
                containerColor = backgroundColor,
                shape = RoundedCornerShape(12.dp)
            )
        }
    }
}