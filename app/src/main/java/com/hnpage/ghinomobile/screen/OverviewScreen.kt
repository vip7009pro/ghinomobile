package com.hnpage.ghinomobile.screen

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.hnpage.ghinomobile.utils.formatAmount
import com.hnpage.ghinomobile.viewmodel.DebtViewModel
import com.hnpage.ghinomobile.work.scheduleReminder
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OverviewScreen(viewModel: DebtViewModel) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val transactions by viewModel.transactions.collectAsState(initial = emptyList())
    var showDialog by remember { mutableStateOf(false) }
    var permissionDenied by remember { mutableStateOf(false) }

    val totalDebt = transactions.filter { it.type == "debit" }.sumOf { it.amount }
    val totalCredit = transactions.filter { it.type == "credit" }.sumOf { it.amount }

    val calendar = Calendar.getInstance()
    calendar.set(Calendar.HOUR_OF_DAY, 0); calendar.set(Calendar.MINUTE, 0); calendar.set(Calendar.SECOND, 0); calendar.set(Calendar.MILLISECOND, 0)
    val startOfDay = calendar.timeInMillis
    val dailyDebit = transactions.filter { it.date >= startOfDay && it.type == "debit" }.sumOf { it.amount }
    val dailyCredit = transactions.filter { it.date >= startOfDay && it.type == "credit" }.sumOf { it.amount }
    calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
    val startOfWeek = calendar.timeInMillis
    val weeklyDebit = transactions.filter { it.date >= startOfWeek && it.type == "debit" }.sumOf { it.amount }
    val weeklyCredit = transactions.filter { it.date >= startOfWeek && it.type == "credit" }.sumOf { it.amount }
    calendar.set(Calendar.DAY_OF_MONTH, 1)
    val startOfMonth = calendar.timeInMillis
    val monthlyDebit = transactions.filter { it.date >= startOfMonth && it.type == "debit" }.sumOf { it.amount }
    val monthlyCredit = transactions.filter { it.date >= startOfMonth && it.type == "credit" }.sumOf { it.amount }
    calendar.set(Calendar.DAY_OF_YEAR, 1)
    val startOfYear = calendar.timeInMillis
    val yearlyDebit = transactions.filter { it.date >= startOfYear && it.type == "debit" }.sumOf { it.amount }
    val yearlyCredit = transactions.filter { it.date >= startOfYear && it.type == "credit" }.sumOf { it.amount }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) showDialog = true else permissionDenied = true
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
                Text(text = "Tổng quan", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
                        showDialog = true
                    } else {
                        permissionLauncher.launch(Manifest.permission.READ_CONTACTS)
                    }
                },
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
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .padding(horizontal = 10.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.error),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp).fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Tổng nợ tôi", fontSize = 18.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onError)
                        Text(text = formatAmount(totalDebt), fontSize = 32.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onError)
                    }
                }
            }
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondary),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp).fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Tổng tôi nợ", fontSize = 18.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSecondary)
                        Text(formatAmount(totalCredit), fontSize = 32.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondary)
                    }
                }
            }
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Card(
                        modifier = Modifier.weight(1f).height(80.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.error),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(8.dp).fillMaxSize(),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Ngày (Nợ)", fontSize = 12.sp, color = MaterialTheme.colorScheme.onError)
                            Text(formatAmount(dailyDebit), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onError)
                        }
                    }
                    Card(
                        modifier = Modifier.weight(1f).height(80.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondary),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(8.dp).fillMaxSize(),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Ngày (Có)", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSecondary)
                            Text(formatAmount(dailyCredit), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondary)
                        }
                    }
                }
            }
            // Tương tự cho Tuần, Tháng, Năm (giữ cấu trúc, thay đổi text)
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Card(
                        modifier = Modifier.weight(1f).height(80.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.error),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(8.dp).fillMaxSize(),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Tuần (Nợ)", fontSize = 12.sp, color = MaterialTheme.colorScheme.onError)
                            Text(formatAmount(weeklyDebit), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onError)
                        }
                    }
                    Card(
                        modifier = Modifier.weight(1f).height(80.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondary),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(8.dp).fillMaxSize(),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Tuần (Có)", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSecondary)
                            Text(formatAmount(weeklyCredit), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondary)
                        }
                    }
                }
            }
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Card(
                        modifier = Modifier.weight(1f).height(80.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.error),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(8.dp).fillMaxSize(),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Tháng (Nợ)", fontSize = 12.sp, color = MaterialTheme.colorScheme.onError)
                            Text(formatAmount(monthlyDebit), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onError)
                        }
                    }
                    Card(
                        modifier = Modifier.weight(1f).height(80.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondary),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(8.dp).fillMaxSize(),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Tháng (Có)", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSecondary)
                            Text(formatAmount(monthlyCredit), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondary)
                        }
                    }
                }
            }
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Card(
                        modifier = Modifier.weight(1f).height(80.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.error),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(8.dp).fillMaxSize(),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Năm (Nợ)", fontSize = 12.sp, color = MaterialTheme.colorScheme.onError)
                            Text(formatAmount(yearlyDebit), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onError)
                        }
                    }
                    Card(
                        modifier = Modifier.weight(1f).height(80.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondary),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(8.dp).fillMaxSize(),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Năm (Có)", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSecondary)
                            Text(formatAmount(yearlyCredit), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondary)
                        }
                    }
                }
            }
        }
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
                title = { Text("Quyền bị từ chối", color = MaterialTheme.colorScheme.onSurface) },
                text = { Text("Cần quyền truy cập danh bạ.", color = MaterialTheme.colorScheme.onSurface) },
                confirmButton = {
                    TextButton(onClick = { permissionDenied = false }) {
                        Text("OK", color = MaterialTheme.colorScheme.primary)
                    }
                },
                containerColor = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(12.dp)
            )
        }
    }
}