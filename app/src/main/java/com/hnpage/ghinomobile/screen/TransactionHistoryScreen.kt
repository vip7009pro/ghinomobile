package com.hnpage.ghinomobile.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.hnpage.ghinomobile.data.Transaction
import com.hnpage.ghinomobile.viewmodel.DebtViewModel
import com.hnpage.ghinomobile.work.scheduleReminder


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionHistoryScreen(viewModel: DebtViewModel) {
    val context = LocalContext.current
    val transactions by viewModel.transactions.collectAsState(initial = emptyList())
    var showAddDialog by remember { mutableStateOf(false) }
    var editTransaction by remember { mutableStateOf<Transaction?>(null) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Lịch sử giao dịch") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) { Text("+") }
        }
    ) { padding ->
        LazyColumn(Modifier.padding(padding)) {
            items(transactions) { transaction ->
                ListItem(
                    headlineContent = { Text("${transaction.contactName}: ${transaction.amount} (${transaction.type})") },
                    supportingContent = {
                        Text("${java.text.SimpleDateFormat("dd/MM/yyyy").format(java.util.Date(transaction.date))} - ${transaction.note}")
                    },
                    trailingContent = {
                        Row {
                            IconButton(onClick = { editTransaction = transaction }) {
                                Icon(Icons.Default.Edit, contentDescription = null)
                            }
                            IconButton(onClick = { viewModel.delete(transaction) }) {
                                Icon(Icons.Default.Delete, contentDescription = null)
                            }
                        }
                    }
                )
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
                    viewModel.update(updated)
                    if (updated.isReminderSet) scheduleReminder(context, updated)
                    editTransaction = null
                }
            )
        }
    }
}