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
import com.hnpage.ghinomobile.data.Transaction
import com.hnpage.ghinomobile.utils.createTransactionImage
import com.hnpage.ghinomobile.utils.formatAmount
import com.hnpage.ghinomobile.utils.shareTransactionImage
import com.hnpage.ghinomobile.viewmodel.DebtViewModel
import com.hnpage.ghinomobile.work.scheduleReminder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactListScreen(viewModel: DebtViewModel, onContactClick: (String) -> Unit) {
    val balances by viewModel.balanceByContact.collectAsState(initial = emptyMap())
    var searchQuery by remember { mutableStateOf("") }

    // Gradient cho Debit (Nợ) - Sắc thái đỏ
    val debitGradient = Brush.horizontalGradient(
        colors = listOf(
            Color(0xFFFFCC80), // Cam nhạt
            Color(0xFFFF5722), // Cam đậm
            Color(0xFFD32F2F)  // Đỏ đậm
        )
    )

    // Gradient cho Credit (Có) - Sắc thái xanh lá thiên nhiên
    val creditGradient = Brush.horizontalGradient(
        colors = listOf(
            Color(0xFFA5D6A7), // Xanh lá nhạt
            Color(0xFF4CAF50), // Xanh lá trung
            Color(0xFF2E7D32)  // Xanh lá đậm
        )
    )

    // Gradient trung tính cho balance = 0
    val neutralGradient = Brush.horizontalGradient(
        colors = listOf(
            Color(0xFFE0E0E0), // Xám nhạt
            Color(0xFFB0B0B0)  // Xám đậm
        )
    )

    // Màu nền cố định phong cách thiên nhiên
    val backgroundColor = Color(0xFFF1F8E9) // Xanh lá rất nhạt
    val textColor = Color(0xFF1B5E20) // Xanh lá đậm để tương phản với nền

    Scaffold(
        /*topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .background(color = Color.Transparent),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Danh bạ",
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            }
        },*/
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
                items(
                    balances.toList().filter { (contactPair, _) ->
                        val (phone, name) = contactPair
                        name.contains(searchQuery, ignoreCase = true) || phone.contains(searchQuery, ignoreCase = true)
                    }
                ) { (contactPair, balance) ->
                    val (phone, name) = contactPair
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
                                        balance > 0 -> debitGradient // Nợ tôi
                                        balance < 0 -> creditGradient // Tôi nợ
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
                                    text = "SĐT: $phone\nDư nợ: ${formatAmount(balance)}",
                                    fontSize = 14.sp,
                                    color = Color.White,
                                    maxLines = 2,
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
    var showAddDialog by remember { mutableStateOf(false) }
    var editTransaction by remember { mutableStateOf<Transaction?>(null) }
    var deleteTransaction by remember { mutableStateOf<Transaction?>(null) }
    var confirmUpdateTransaction by remember { mutableStateOf<Transaction?>(null) }
    val contactName = transactions.maxByOrNull { it.date }?.contactName ?: "Không xác định"

    // Gradient cho Debit (Nợ) - Sắc thái đỏ
    val debitGradient = Brush.horizontalGradient(
        colors = listOf(
            Color(0xFFFFCC80), // Cam nhạt
            Color(0xFFFF5722), // Cam đậm
            Color(0xFFD32F2F)  // Đỏ đậm
        )
    )

    // Gradient cho Credit (Có) - Sắc thái xanh lá thiên nhiên
    val creditGradient = Brush.horizontalGradient(
        colors = listOf(
            Color(0xFFA5D6A7), // Xanh lá nhạt
            Color(0xFF4CAF50), // Xanh lá trung
            Color(0xFF2E7D32)  // Xanh lá đậm
        )
    )

    // Màu nền cố định phong cách thiên nhiên
    val backgroundColor = Color(0xFFF1F8E9) // Xanh lá rất nhạt

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
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(if (transaction.type == "debit") debitGradient else creditGradient)
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    color = Color.White,
                                    text = "${transaction.contactName}: ${formatAmount(transaction.amount)} (${if (transaction.type == "debit") "Nợ tôi" else "Tôi nợ"})",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Ngày: ${java.text.SimpleDateFormat("dd/MM/yyyy").format(java.util.Date(transaction.date))}",
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
                            Row {
                                IconButton(onClick = { editTransaction = transaction }) {
                                    Icon(Icons.Default.Edit, contentDescription = null, tint = Color.White)
                                }
                                IconButton(onClick = { deleteTransaction = transaction }) {
                                    Icon(Icons.Default.Delete, contentDescription = null, tint = Color.White)
                                }
                                IconButton(onClick = {
                                    val uri = createTransactionImage(context, transaction)
                                    uri?.let { shareTransactionImage(context, it) }
                                }) {
                                    Icon(Icons.Default.Share, contentDescription = null, tint = Color.White)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Dialog thêm giao dịch
        if (showAddDialog) {
            TransactionDialog(
                transaction = Transaction(
                    id = "",
                    contactName = contactName,
                    phoneNumber = phoneNumber,
                    amount = 0.0,
                    type = "debit",
                    date = System.currentTimeMillis(),
                    note = "",
                    isReminderSet = false
                ),
                onDismiss = { showAddDialog = false },
                onAdd = { transaction ->
                    viewModel.insert(transaction)
                    if (transaction.isReminderSet) scheduleReminder(context, transaction)
                    showAddDialog = false
                }
            )
        }

        // Dialog chỉnh sửa giao dịch
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

        // Dialog xác nhận xóa
        if (deleteTransaction != null) {
            AlertDialog(
                onDismissRequest = { deleteTransaction = null },
                title = { Text("Xác nhận xóa", color = MaterialTheme.colorScheme.onSurface) },
                text = { Text("Bạn có chắc muốn xóa giao dịch này?", color = MaterialTheme.colorScheme.onSurface) },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.delete(deleteTransaction!!)
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

        // Dialog xác nhận cập nhật
        if (confirmUpdateTransaction != null) {
            AlertDialog(
                onDismissRequest = { confirmUpdateTransaction = null },
                title = { Text("Xác nhận cập nhật", color = MaterialTheme.colorScheme.onSurface) },
                text = { Text("Bạn có chắc muốn cập nhật giao dịch này?", color = MaterialTheme.colorScheme.onSurface) },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.update(confirmUpdateTransaction!!)
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