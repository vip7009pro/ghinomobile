package com.hnpage.ghinomobile.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
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
        containerColor = MaterialTheme.colorScheme.surface
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(horizontal = 10.dp, vertical = 10.dp)
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
                items(
                    transactions.filter {
                        it.contactName.contains(searchQuery, ignoreCase = true) ||
                                it.phoneNumber.contains(searchQuery, ignoreCase = true)
                    }
                ) { transaction ->
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
                                            "Ngày: ${java.text.SimpleDateFormat("dd/MM/yyyy").format(java.util.Date(transaction.date))}\n" +
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

        // Dialog thêm giao dịch
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
                containerColor = MaterialTheme.colorScheme.surface,
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
                containerColor = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(12.dp)
            )
        }
    }
}