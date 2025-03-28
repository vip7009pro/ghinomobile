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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.hnpage.ghinomobile.utils.formatAmount
import com.hnpage.ghinomobile.viewmodel.DebtViewModel
import com.hnpage.ghinomobile.work.scheduleReminder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OverviewScreen(viewModel: DebtViewModel) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val totalDebit by viewModel.totalDebit.collectAsState(initial = 0.0)
    val totalCredit by viewModel.totalCredit.collectAsState(initial = 0.0)
    val totalDebitPaid by viewModel.totalDebitPaid.collectAsState(initial = 0.0)
    val totalCreditPaid by viewModel.totalCreditPaid.collectAsState(initial = 0.0)
    var showDialog by remember { mutableStateOf(false) }
    var permissionDenied by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) showDialog = true else permissionDenied = true
    }

    val debitGradient = Brush.horizontalGradient(
        colors = listOf(Color(0xFFD32F2F),Color(0xFFFF5722), Color(0xFFFFCC80))
    )
    val creditGradient = Brush.horizontalGradient(
        colors = listOf(Color(0xFF2E7D32),Color(0xFF4CAF50), Color(0xFFA5D6A7))
    )
    val backgroundColor = Color(0xFFF1F8E9)

    Scaffold(
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
        containerColor = backgroundColor
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(0.dp)
                .padding(horizontal = 5.dp, vertical = 5.dp)
                .background(backgroundColor),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize().background(debitGradient)) {
                        Column(
                            modifier = Modifier.padding(16.dp).fillMaxSize(),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Tổng nợ tôi", fontSize = 18.sp, fontWeight = FontWeight.Medium, color = Color.White)
                            Text(formatAmount(totalDebit), fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize().background(debitGradient)) {
                        Column(
                            modifier = Modifier.padding(16.dp).fillMaxSize(),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Tổng đã trả tôi", fontSize = 18.sp, fontWeight = FontWeight.Medium, color = Color.White)
                            Text(formatAmount(totalDebitPaid), fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize().background(debitGradient)) {
                        Column(
                            modifier = Modifier.padding(16.dp).fillMaxSize(),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Tổng chưa trả tôi", fontSize = 18.sp, fontWeight = FontWeight.Medium, color = Color.White)
                            Text(formatAmount(totalDebit-totalDebitPaid), fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize().background(creditGradient)) {
                        Column(
                            modifier = Modifier.padding(16.dp).fillMaxSize(),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Tổng tôi nợ", fontSize = 18.sp, fontWeight = FontWeight.Medium, color = Color.White)
                            Text(formatAmount(totalCredit), fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize().background(creditGradient)) {
                        Column(
                            modifier = Modifier.padding(16.dp).fillMaxSize(),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Tổng tôi đã trả", fontSize = 18.sp, fontWeight = FontWeight.Medium, color = Color.White)
                            Text(formatAmount(totalCreditPaid), fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize().background(creditGradient)) {
                        Column(
                            modifier = Modifier.padding(16.dp).fillMaxSize(),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Tổng tôi chưa trả", fontSize = 18.sp, fontWeight = FontWeight.Medium, color = Color.White)
                            Text(formatAmount(totalCredit-totalCreditPaid), fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }
        if (showDialog) {
            TransactionDialog(
                onDismiss = { showDialog = false },
                onAddTransaction = { transaction ->
                    viewModel.insertTransaction(transaction)
                    if (transaction.isReminderSet) scheduleReminder(context, transaction)
                    showDialog = false
                },
                onAddPayment = {}
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
                containerColor = backgroundColor,
                shape = RoundedCornerShape(12.dp)
            )
        }
    }
}