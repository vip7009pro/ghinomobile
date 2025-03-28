package com.hnpage.ghinomobile.screen

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.ContactsContract
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import com.hnpage.ghinomobile.data.Payment
import com.hnpage.ghinomobile.data.Transaction
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.*
import com.hnpage.ghinomobile.utils.formatAmount
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

// VisualTransformation để định dạng số tiền với dấu phảy
class NumberFormatTransformation : VisualTransformation {
    private val decimalFormat = DecimalFormat("#,###", DecimalFormatSymbols(Locale("vi", "VN")).apply {
        groupingSeparator = ','
    })

    override fun filter(text: AnnotatedString): TransformedText {
        val originalText = text.text.filter { it.isDigit() } // Chỉ giữ số
        val formatted = if (originalText.isNotEmpty()) decimalFormat.format(originalText.toLong()) else ""
        return TransformedText(
            text = AnnotatedString(formatted),
            offsetMapping = object : OffsetMapping {
                override fun originalToTransformed(offset: Int): Int {
                    return formatted.length // Đơn giản hóa: ánh xạ đến cuối chuỗi định dạng
                }

                override fun transformedToOriginal(offset: Int): Int {
                    return originalText.length // Đơn giản hóa: ánh xạ đến cuối chuỗi gốc
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionDialog(
    transaction: Transaction? = null,
    isPaymentMode: Boolean = false,
    isEditPaymentMode: Boolean = false,
    onDismiss: () -> Unit,
    onAddTransaction: (Transaction) -> Unit,
    onAddPayment: (Payment) -> Unit,
    prefilledContactName: String? = null, // Tham số mới
    prefilledPhoneNumber: String? = null // Tham số mới
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val contacts = remember { mutableStateListOf<Pair<String, String>>() }
    var searchQuery by remember { mutableStateOf("") }
    var selectedContact by remember { mutableStateOf(transaction?.let { Pair(it.contactName, it.phoneNumber) }) }
    var customName by remember { mutableStateOf(transaction?.contactName ?: prefilledContactName ?: "") }
    var customPhone by remember { mutableStateOf(transaction?.phoneNumber ?: prefilledPhoneNumber ?: "") }
    var amount by remember { mutableStateOf(TextFieldValue(transaction?.amount?.let { DecimalFormat("#").format(it) } ?: "")) }
    var type by remember { mutableStateOf(transaction?.type ?: "debit") }
    var note by remember { mutableStateOf(transaction?.note ?: "") }
    var isReminderSet by remember { mutableStateOf(transaction?.isReminderSet ?: false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isManualEntry by remember { mutableStateOf(false) }

    val debitGradient = Brush.horizontalGradient(
        colors = listOf(Color(0xFFFFCC80), Color(0xFFFF5722), Color(0xFFD32F2F))
    )
    val creditGradient = Brush.horizontalGradient(
        colors = listOf(Color(0xFFA5D6A7), Color(0xFF4CAF50), Color(0xFF2E7D32))
    )
    val backgroundColor = Color(0xFFF1F8E9)
    val textColor = Color(0xFF1B5E20)

    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
            coroutineScope.launch(Dispatchers.IO) {
                val loadedContacts = loadContacts(context)
                contacts.clear()
                contacts.addAll(loadedContacts)
                if (transaction == null && selectedContact == null && prefilledContactName == null && contacts.isNotEmpty()) {
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

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = backgroundColor,
            modifier = Modifier
                .fillMaxWidth()
                .height(600.dp)
                .padding(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(bottom = 8.dp)
                ) {
                    Text(
                        text = when {
                            transaction == null || transaction.id.isEmpty() -> "Thêm giao dịch"
                            isPaymentMode && !isEditPaymentMode -> "Thêm thanh toán"
                            isEditPaymentMode -> "Sửa thanh toán"
                            else -> "Sửa giao dịch"
                        },
                        color = textColor,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    if (isLoading) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                    } else if (contacts.isEmpty() && !isManualEntry && transaction == null && prefilledContactName == null) {
                        Text(text = "Không tìm thấy danh bạ.", color = textColor)
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Nếu có prefilled hoặc đang chỉnh sửa, khóa các trường tên và số điện thoại
                            if (prefilledContactName != null || transaction != null) {
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
                            } else {
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
                                OutlinedTextField(
                                    value = customPhone,
                                    onValueChange = { customPhone = it },
                                    label = { Text("Số điện thoại", color = textColor) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = isManualEntry,
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
                                OutlinedTextField(
                                    value = customName,
                                    onValueChange = { customName = it },
                                    label = { Text("Tên liên hệ", color = textColor) },
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
                            // ... (Giữ nguyên các trường khác: amount, type, note, isReminderSet, errorMessage)
                            OutlinedTextField(
                                value = amount,
                                onValueChange = { newValue ->
                                    val rawText = newValue.text.filter { it.isDigit() }
                                    amount = TextFieldValue(rawText, newValue.selection, newValue.composition)
                                },
                                label = { Text(if (isPaymentMode || isEditPaymentMode) "Số tiền thanh toán" else "Số tiền", color = textColor) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                visualTransformation = NumberFormatTransformation(),
                                isError = amount.text.isNotEmpty() && amount.text.toDoubleOrNull() == null,
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
                            if (transaction == null || transaction.id.isEmpty() || (!isPaymentMode && !isEditPaymentMode)) {
                                Spinner(
                                    options = listOf("Ghi nợ" to "debit", "Ghi có" to "credit"),
                                    selected = type,
                                    onSelected = { type = it }
                                )
                            }
                            OutlinedTextField(
                                value = note,
                                onValueChange = { note = it },
                                label = { Text("Ghi chú", color = textColor) },
                                modifier = Modifier
                                    .fillMaxWidth(),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = textColor,
                                    unfocusedTextColor = textColor,
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = textColor,
                                    focusedLabelColor = textColor,
                                    unfocusedLabelColor = textColor
                                )
                            )
                            if (transaction == null || transaction.id.isEmpty() || (!isPaymentMode && !isEditPaymentMode)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(
                                        checked = isReminderSet,
                                        onCheckedChange = { isReminderSet = it },
                                        colors = CheckboxDefaults.colors(
                                            checkedColor = MaterialTheme.colorScheme.primary,
                                            uncheckedColor = textColor
                                        )
                                    )
                                    Text(text = "Đặt nhắc nhở (7 ngày)", color = textColor)

                                }
                                errorMessage?.let {
                                    Text(text = it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text(text = "Hủy", color = MaterialTheme.colorScheme.primary)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        if (!isLoading) {
                            Button(
                                onClick = {
                                    val rawAmount = amount.text.filter { it.isDigit() }
                                    val amountDouble = rawAmount.toDoubleOrNull()
                                    if (rawAmount.isEmpty() || amountDouble == null || amountDouble <= 0) {
                                        errorMessage = "Vui lòng nhập số tiền hợp lệ (lớn hơn 0)"
                                    } else if (customName.isBlank()) {
                                        errorMessage = "Vui lòng nhập tên liên hệ"
                                    } else if ((transaction == null || transaction.id.isEmpty()) && isManualEntry && customPhone.isBlank()) {
                                        errorMessage = "Vui lòng nhập số điện thoại"
                                    } else {
                                        val phoneNumber = customPhone // Dùng customPhone vì đã được điền sẵn
                                        when {
                                            transaction == null || transaction.id.isEmpty() -> {
                                                onAddTransaction(
                                                    Transaction(
                                                        id = UUID.randomUUID().toString(),
                                                        contactName = customName,
                                                        phoneNumber = phoneNumber,
                                                        amount = amountDouble,
                                                        type = type,
                                                        date = System.currentTimeMillis(),
                                                        note = note,
                                                        isReminderSet = isReminderSet
                                                    )
                                                )
                                            }
                                            isPaymentMode && !isEditPaymentMode -> {
                                                onAddPayment(
                                                    Payment(
                                                        id = UUID.randomUUID().toString(),
                                                        transactionId = transaction!!.id,
                                                        amount = amountDouble,
                                                        date = System.currentTimeMillis(),
                                                        note = note
                                                    )
                                                )
                                            }
                                            isEditPaymentMode -> {
                                                onAddPayment(
                                                    Payment(
                                                        id = UUID.randomUUID().toString(),
                                                        transactionId = transaction!!.id,
                                                        amount = amountDouble,
                                                        date = System.currentTimeMillis(),
                                                        note = note
                                                    )
                                                )
                                            }
                                            else -> {
                                                onAddTransaction(
                                                    transaction!!.copy(
                                                        contactName = customName,
                                                        phoneNumber = phoneNumber,
                                                        amount = amountDouble,
                                                        type = type,
                                                        note = note,
                                                        isReminderSet = isReminderSet
                                                    )
                                                )
                                            }
                                        }
                                        errorMessage = null
                                    }
                                },
                                modifier = Modifier.background(
                                    brush = if (type == "debit") debitGradient else creditGradient,
                                    shape = MaterialTheme.shapes.small
                                ),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = Color.White)
                            ) {
                                Text(
                                    when {
                                        transaction == null || transaction.id.isEmpty() -> "Thêm"
                                        isPaymentMode && !isEditPaymentMode -> "Thanh toán"
                                        isEditPaymentMode -> "Cập nhật"
                                        else -> "Cập nhật"
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
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

val backgroundColor = Color(0xFFF1F8E9)
val textColor = Color(0xFF1B5E20)

@Composable
fun Spinner(options: List<Pair<String, String>>, selected: String, onSelected: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Button(
            onClick = { expanded = true },
            colors = ButtonDefaults.buttonColors(
                containerColor =backgroundColor,
                contentColor = textColor
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
                    text = { Text(label, color = textColor) },
                    onClick = {
                        onSelected(value)
                        expanded = false
                    }
                )
            }
        }
    }
}