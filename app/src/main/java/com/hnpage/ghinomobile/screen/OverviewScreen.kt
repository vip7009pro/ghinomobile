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
import java.util.Calendar
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding

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
            Color(0xFFA5D6A7), // Xanh lá nhạt (màu lá non)
            Color(0xFF4CAF50), // Xanh lá trung (màu rừng)
            Color(0xFF2E7D32)  // Xanh lá đậm (màu lá trưởng thành)
        )
    )

    // Màu nền cố định phong cách thiên nhiên
    val backgroundColor = Color(0xFFF1F8E9) // Xanh lá rất nhạt

    Scaffold(
     /*   topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.statusBars) // Kéo dài lên thanh trạng thái
                    .background(color = Color.Transparent),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Tổng quan",
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            }
        },*/
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
        containerColor = backgroundColor // Thay đổi màu nền của Scaffold
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(0.dp)
                .padding(horizontal = 5.dp, vertical = 5.dp)
                .background(backgroundColor), // Đảm bảo LazyColumn cũng có cùng màu nền
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(debitGradient)
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxSize(),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Tổng nợ tôi", fontSize = 18.sp, fontWeight = FontWeight.Medium, color = Color.White)
                            Text(text = formatAmount(totalDebt), fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(creditGradient)
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxSize(),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Tổng tôi nợ", fontSize = 18.sp, fontWeight = FontWeight.Medium, color = Color.White)
                            Text(formatAmount(totalCredit), fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .height(80.dp),
                        shape = RoundedCornerShape(8.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(debitGradient)
                        ) {
                            Column(
                                modifier = Modifier
                                    .padding(8.dp)
                                    .fillMaxSize(),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Ngày (Nợ)", fontSize = 12.sp, color = Color.White)
                                Text(formatAmount(dailyDebit), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .height(80.dp),
                        shape = RoundedCornerShape(8.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(creditGradient)
                        ) {
                            Column(
                                modifier = Modifier
                                    .padding(8.dp)
                                    .fillMaxSize(),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Ngày (Có)", fontSize = 12.sp, color = Color.White)
                                Text(formatAmount(dailyCredit), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                }
            }
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .height(80.dp),
                        shape = RoundedCornerShape(8.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(debitGradient)
                        ) {
                            Column(
                                modifier = Modifier
                                    .padding(8.dp)
                                    .fillMaxSize(),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Tuần (Nợ)", fontSize = 12.sp, color = Color.White)
                                Text(formatAmount(weeklyDebit), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .height(80.dp),
                        shape = RoundedCornerShape(8.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(creditGradient)
                        ) {
                            Column(
                                modifier = Modifier
                                    .padding(8.dp)
                                    .fillMaxSize(),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Tuần (Có)", fontSize = 12.sp, color = Color.White)
                                Text(formatAmount(weeklyCredit), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                }
            }
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .height(80.dp),
                        shape = RoundedCornerShape(8.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(debitGradient)
                        ) {
                            Column(
                                modifier = Modifier
                                    .padding(8.dp)
                                    .fillMaxSize(),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Tháng (Nợ)", fontSize = 12.sp, color = Color.White)
                                Text(formatAmount(monthlyDebit), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .height(80.dp),
                        shape = RoundedCornerShape(8.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(creditGradient)
                        ) {
                            Column(
                                modifier = Modifier
                                    .padding(8.dp)
                                    .fillMaxSize(),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Tháng (Có)", fontSize = 12.sp, color = Color.White)
                                Text(formatAmount(monthlyCredit), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                }
            }
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .height(80.dp),
                        shape = RoundedCornerShape(8.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(debitGradient)
                        ) {
                            Column(
                                modifier = Modifier
                                    .padding(8.dp)
                                    .fillMaxSize(),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Năm (Nợ)", fontSize = 12.sp, color = Color.White)
                                Text(formatAmount(yearlyDebit), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .height(80.dp),
                        shape = RoundedCornerShape(8.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(creditGradient)
                        ) {
                            Column(
                                modifier = Modifier
                                    .padding(8.dp)
                                    .fillMaxSize(),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Năm (Có)", fontSize = 12.sp, color = Color.White)
                                Text(formatAmount(yearlyCredit), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
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
                containerColor = backgroundColor, // Đồng bộ màu nền với Scaffold
                shape = RoundedCornerShape(12.dp)
            )
        }
    }
}