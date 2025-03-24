package com.hnpage.ghinomobile.screen


import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.hnpage.ghinomobile.viewmodel.DebtViewModel
import com.hnpage.ghinomobile.work.scheduleReminder
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OverviewScreen(viewModel: DebtViewModel) {
    val context = LocalContext.current
    val balances by viewModel.balanceByContact.collectAsState(initial = emptyMap())
    var showDialog by remember { mutableStateOf(false) }
    var permissionDenied by remember { mutableStateOf(false) }
    val totalDebt = balances.values.sumOf { if (it > 0) it else 0.0 }
    val totalCredit = balances.values.sumOf { if (it < 0) -it else 0.0 }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) showDialog = true else permissionDenied = true
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Tổng quan") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
                    showDialog = true
                } else {
                    permissionLauncher.launch(Manifest.permission.READ_CONTACTS)
                }
            }) { Text("+") }
        }
    ) { padding ->
        Column(Modifier.padding(padding).padding(16.dp)) {
            Text("Tổng nợ: $totalDebt", style = MaterialTheme.typography.headlineSmall)
            Text("Tổng có: $totalCredit", style = MaterialTheme.typography.headlineSmall)
            if (showDialog) {
                TransactionDialog(
                    onDismiss = { showDialog = false },
                    onAdd = { transaction ->
                        viewModel.insert(transaction)
                        if (transaction.isReminderSet) scheduleReminder(context, transaction)
                        showDialog = false
                    }
                )
            }
            if (permissionDenied) {
                AlertDialog(
                    onDismissRequest = { permissionDenied = false },
                    title = { Text("Quyền bị từ chối") },
                    text = { Text("Cần quyền truy cập danh bạ.") },
                    confirmButton = { TextButton(onClick = { permissionDenied = false }) { Text("OK") } }
                )
            }
        }
    }
}