package com.hnpage.ghinomobile.screen

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.ContactsContract
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.hnpage.ghinomobile.data.Transaction
import java.text.DecimalFormat
import com.hnpage.ghinomobile.utils.formatAmount
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID

@Composable
fun TransactionDialog(
    transaction: Transaction? = null,
    onDismiss: () -> Unit,
    onAdd: (Transaction) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val contacts = remember { mutableStateListOf<Pair<String, String>>() }
    var searchQuery by remember { mutableStateOf("") }
    var selectedContact by remember { mutableStateOf(transaction?.let { Pair(it.contactName, it.phoneNumber) }) }
    var customName by remember { mutableStateOf(transaction?.contactName ?: "") }
    var customPhone by remember { mutableStateOf(transaction?.phoneNumber ?: "") }
    var amount by remember { mutableStateOf(transaction?.amount?.let { DecimalFormat("#").format(it) } ?: "") }
    var type by remember { mutableStateOf(transaction?.type ?: "debit") }
    var note by remember { mutableStateOf(transaction?.note ?: "") }
    var isReminderSet by remember { mutableStateOf(transaction?.isReminderSet ?: false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isManualEntry by remember { mutableStateOf(false) }

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
    val textColor = Color(0xFF1B5E20) // Xanh lá đậm để tương phản với nền

    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
            coroutineScope.launch(Dispatchers.IO) {
                val loadedContacts = loadContacts(context)
                contacts.clear()
                contacts.addAll(loadedContacts)
                if (transaction == null && selectedContact == null && contacts.isNotEmpty()) {
                    selectedContact = contacts[0]
                    customName = contacts[0].first
                    customPhone = contacts[0].second
                }
                isLoading = false
            }
        } else {
            isLoading = false
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (transaction == null || transaction.id.isEmpty()) "Thêm giao dịch" else "Sửa giao dịch",
                color = textColor, // Đổi màu chữ tiêu đề để tương phản
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            if (isLoading) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary
                )
            } else if (contacts.isEmpty() && !isManualEntry && transaction == null) {
                Text(
                    text = "Không tìm thấy danh bạ.",
                    color = textColor // Đổi màu chữ để tương phản
                )
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (transaction == null || transaction.id.isEmpty()) { // Thêm mới
                        var expanded by remember { mutableStateOf(false) }
                        OutlinedTextField(
                            value = if (isManualEntry) "Nhập số thủ công" else searchQuery,
                            onValueChange = { if (!isManualEntry) searchQuery = it },
                            label = { Text("Tìm kiếm liên hệ", color = textColor) },
                            modifier = Modifier.fillMaxWidth(),
                            trailingIcon = {
                                IconButton(onClick = { expanded = true }) {
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = textColor)
                                }
                            },
                            enabled = !isManualEntry,
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
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false },
                            modifier = Modifier.heightIn(max = 200.dp)
                        ) {
                            DropdownMenuItem(
                                text = { Text("Nhập số thủ công", color = textColor) },
                                onClick = {
                                    isManualEntry = true
                                    selectedContact = null
                                    customPhone = ""
                                    customName = ""
                                    expanded = false
                                    searchQuery = ""
                                }
                            )
                            contacts.filter {
                                it.first.contains(searchQuery, ignoreCase = true) ||
                                        it.second.contains(searchQuery, ignoreCase = true)
                            }.take(50).forEach { contact ->
                                DropdownMenuItem(
                                    text = { Text("${contact.first} (${contact.second})", color = textColor) },
                                    onClick = {
                                        selectedContact = contact
                                        customName = contact.first
                                        customPhone = contact.second
                                        isManualEntry = false
                                        expanded = false
                                        searchQuery = ""
                                    }
                                )
                            }
                        }
                        if (isManualEntry) {
                            OutlinedTextField(
                                value = customPhone,
                                onValueChange = { customPhone = it },
                                label = { Text("Số điện thoại", color = textColor) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = textColor,
                                    unfocusedTextColor = textColor,
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = textColor,
                                    focusedLabelColor = textColor,
                                    unfocusedLabelColor = textColor
                                )
                            )
                        }
                        OutlinedTextField(
                            value = customName,
                            onValueChange = { customName = it },
                            label = { Text("Tên liên hệ (có thể chỉnh sửa)", color = textColor) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = textColor,
                                unfocusedTextColor = textColor,
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = textColor,
                                focusedLabelColor = textColor,
                                unfocusedLabelColor = textColor
                            )
                        )
                    } else { // Chỉnh sửa hoặc thêm từ ContactDetailScreen
                        OutlinedTextField(
                            value = customPhone,
                            onValueChange = {},
                            label = { Text("Số điện thoại", color = textColor) },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = false,
                            colors = OutlinedTextFieldDefaults.colors(
                                disabledTextColor = textColor.copy(alpha = 0.7f),
                                disabledBorderColor = textColor.copy(alpha = 0.7f),
                                disabledLabelColor = textColor.copy(alpha = 0.7f)
                            )
                        )
                        OutlinedTextField(
                            value = customName,
                            onValueChange = {},
                            label = { Text("Tên liên hệ", color = textColor) },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = false,
                            colors = OutlinedTextFieldDefaults.colors(
                                disabledTextColor = textColor.copy(alpha = 0.7f),
                                disabledBorderColor = textColor.copy(alpha = 0.7f),
                                disabledLabelColor = textColor.copy(alpha = 0.7f)
                            )
                        )
                    }
                    OutlinedTextField(
                        value = amount,
                        onValueChange = { amount = it },
                        label = { Text("Số tiền", color = textColor) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        isError = amount.isNotEmpty() && amount.toDoubleOrNull() == null,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = textColor,
                            unfocusedTextColor = textColor,
                            errorTextColor = MaterialTheme.colorScheme.error,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = textColor,
                            errorBorderColor = MaterialTheme.colorScheme.error,
                            focusedLabelColor = textColor,
                            unfocusedLabelColor = textColor,
                            errorLabelColor = MaterialTheme.colorScheme.error
                        )
                    )
                    Spinner(
                        options = listOf("Ghi nợ" to "debit", "Ghi có" to "credit"),
                        selected = type,
                        onSelected = { type = it }
                    )
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = { Text("Ghi chú", color = textColor) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = textColor,
                            unfocusedTextColor = textColor,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = textColor,
                            focusedLabelColor = textColor,
                            unfocusedLabelColor = textColor
                        )
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isReminderSet,
                            onCheckedChange = { isReminderSet = it },
                            colors = CheckboxDefaults.colors(
                                checkedColor = MaterialTheme.colorScheme.primary,
                                uncheckedColor = textColor
                            )
                        )
                        Text(
                            text = "Đặt nhắc nhở (7 ngày)",
                            color = textColor // Đổi màu chữ để tương phản
                        )
                    }
                    errorMessage?.let {
                        Text(
                            text = it,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        },
        confirmButton = {
            if (!isLoading) {
                Button(
                    onClick = {
                        val amountDouble = amount.toDoubleOrNull()
                        if (amountDouble == null || amountDouble <= 0) {
                            errorMessage = "Vui lòng nhập số tiền hợp lệ (lớn hơn 0)"
                        } else if (customName.isBlank()) {
                            errorMessage = "Vui lòng nhập tên liên hệ"
                        } else if (transaction == null && isManualEntry && customPhone.isBlank()) {
                            errorMessage = "Vui lòng nhập số điện thoại"
                        } else {
                            val phoneNumber = if (transaction != null) customPhone else if (isManualEntry) customPhone else (selectedContact?.second ?: contacts[0].second)
                            onAdd(
                                Transaction(
                                    id = transaction?.id?.takeIf { it.isNotEmpty() } ?: UUID.randomUUID().toString(),
                                    contactName = customName,
                                    phoneNumber = phoneNumber,
                                    amount = amountDouble,
                                    type = type,
                                    date = transaction?.date ?: System.currentTimeMillis(),
                                    note = note,
                                    isReminderSet = isReminderSet
                                )
                            )
                            errorMessage = null
                        }
                    },
                    modifier = Modifier.background(
                        brush = if (type == "debit") debitGradient else creditGradient,
                        shape = MaterialTheme.shapes.small
                    ),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        contentColor = Color.White
                    )
                ) {
                    Text(if (transaction == null || transaction.id.isEmpty()) "Thêm" else "Cập nhật")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "Hủy",
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        containerColor = backgroundColor, // Màu nền đồng bộ
        shape = RoundedCornerShape(12.dp)
    )
}

fun loadContacts(context: Context): List<Pair<String, String>> {
    val contacts = mutableListOf<Pair<String, String>>()
    val cursor = context.contentResolver.query(
        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
        arrayOf(
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER
        ),
        null,
        null,
        null
    )
    cursor?.use {
        val nameIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
        val phoneIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
        while (it.moveToNext()) {
            val name = it.getString(nameIdx) ?: "Không có tên"
            val phone = it.getString(phoneIdx) ?: "Không có số"
            contacts.add(name to phone)
        }
    }
    return contacts
}

@Composable
fun Spinner(options: List<Pair<String, String>>, selected: String, onSelected: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Button(
            onClick = { expanded = true },
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White // Đổi màu chữ để tương phản với nền nút
            )
        ) {
            Text(options.find { it.second == selected }?.first ?: "")
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { (label, value) ->
                DropdownMenuItem(
                    text = { Text(label, color = Color(0xFF1B5E20)) }, // Đổi màu chữ để tương phản
                    onClick = {
                        onSelected(value)
                        expanded = false
                    }
                )
            }
        }
    }
}