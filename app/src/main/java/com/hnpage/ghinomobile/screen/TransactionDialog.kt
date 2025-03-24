package com.hnpage.ghinomobile.screen

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.ContactsContract
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
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
        title = { Text(if (transaction == null || transaction.id.isEmpty()) "Thêm giao dịch" else "Sửa giao dịch") },
        text = {
            if (isLoading) {
                CircularProgressIndicator()
            } else if (contacts.isEmpty() && !isManualEntry && transaction == null) {
                Text("Không tìm thấy danh bạ.")
            } else {
                Column {
                    if (transaction == null || transaction.id.isEmpty()) { // Thêm mới
                        var expanded by remember { mutableStateOf(false) }
                        OutlinedTextField(
                            value = if (isManualEntry) "Nhập số thủ công" else searchQuery,
                            onValueChange = { if (!isManualEntry) searchQuery = it },
                            label = { Text("Tìm kiếm liên hệ") },
                            modifier = Modifier.fillMaxWidth(),
                            trailingIcon = {
                                IconButton(onClick = { expanded = true }) {
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                }
                            },
                            enabled = !isManualEntry
                        )
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false },
                            modifier = Modifier.heightIn(max = 200.dp)
                        ) {
                            DropdownMenuItem(
                                text = { Text("Nhập số thủ công") },
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
                                    text = { Text("${contact.first} (${contact.second})") },
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
                                label = { Text("Số điện thoại") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        OutlinedTextField(
                            value = customName,
                            onValueChange = { customName = it },
                            label = { Text("Tên liên hệ (có thể chỉnh sửa)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else { // Chỉnh sửa hoặc thêm từ ContactDetailScreen
                        OutlinedTextField(
                            value = customPhone,
                            onValueChange = {},
                            label = { Text("Số điện thoại") },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = false
                        )
                        OutlinedTextField(
                            value = customName,
                            onValueChange = {},
                            label = { Text("Tên liên hệ") },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = false
                        )
                    }
                    OutlinedTextField(
                        value = amount,
                        onValueChange = { amount = it },
                        label = { Text("Số tiền") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        isError = amount.isNotEmpty() && amount.toDoubleOrNull() == null,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spinner(
                        options = listOf("Ghi nợ" to "debit", "Ghi có" to "credit"),
                        selected = type,
                        onSelected = { type = it }
                    )
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = { Text("Ghi chú") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row {
                        Checkbox(checked = isReminderSet, onCheckedChange = { isReminderSet = it })
                        Text("Đặt nhắc nhở (7 ngày)")
                    }
                    errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            }
        },
        confirmButton = {
            if (!isLoading) {
                TextButton(onClick = {
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
                }) {
                    Text(if (transaction == null || transaction.id.isEmpty()) "Thêm" else "Cập nhật")
                }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Hủy") } }
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
        Button(onClick = { expanded = true }) {
            Text(options.find { it.second == selected }?.first ?: "")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { (label, value) ->
                DropdownMenuItem(
                    text = { Text(label) },
                    onClick = {
                        onSelected(value)
                        expanded = false
                    }
                )
            }
        }
    }
}