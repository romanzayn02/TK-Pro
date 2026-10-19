package com.example.ui.components

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContactPhone
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.example.data.CustomerWithBalance
import com.example.ui.AppStrings
import com.example.ui.LedgerViewModel
import com.example.ui.LocalizationProvider
import com.example.ui.theme.LiquidSurfaceDark
import com.example.ui.theme.TallyCyanRefraction
import com.example.ui.theme.TallyEmeraldGreen
import com.example.ui.theme.TallyGoldAccent
import com.example.ui.theme.TallyRubyRed
import java.io.File
import java.util.Calendar
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Helper to resolve contact name and phone number from Android's native Contact Picker URI.
 */
private fun extractContactDetails(context: Context, contactUri: Uri): Pair<String, String> {
    var name = ""
    var phone = ""
    var contactId = ""

    context.contentResolver.query(
        contactUri,
        arrayOf(ContactsContract.Contacts._ID, ContactsContract.Contacts.DISPLAY_NAME),
        null,
        null,
        null
    )?.use { cursor ->
        if (cursor.moveToFirst()) {
            val idIdx = cursor.getColumnIndex(ContactsContract.Contacts._ID)
            val nameIdx = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
            if (idIdx >= 0) contactId = cursor.getString(idIdx) ?: ""
            if (nameIdx >= 0) name = cursor.getString(nameIdx) ?: ""
        }
    }

    if (contactId.isNotEmpty()) {
        context.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
            "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
            arrayOf(contactId),
            null
        )?.use { phoneCursor ->
            if (phoneCursor.moveToFirst()) {
                val numIdx = phoneCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                if (numIdx >= 0) {
                    phone = phoneCursor.getString(numIdx) ?: ""
                }
            }
        }
    }
    return name.trim() to phone.trim()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCustomerBottomSheet(
    strings: AppStrings,
    useDarkGlass: Boolean,
    currencySymbol: String,
    onDismiss: () -> Unit,
    onSaveCustomer: (
        name: String,
        phone: String,
        partyType: String,
        address: String,
        openingDebit: Double,
        openingCredit: Double
    ) -> Unit,
    onPermissionMessage: (String) -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var partyType by remember { mutableStateOf("CUSTOMER") }
    var address by remember { mutableStateOf("") }
    var openingDebitText by remember { mutableStateOf("") }
    var openingCreditText by remember { mutableStateOf("") }
    var showMoreCustomerOptions by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val contactPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickContact()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val (pickedName, pickedPhone) = extractContactDetails(context, uri)
                if (pickedName.isNotBlank()) name = pickedName
                if (pickedPhone.isNotBlank()) phone = pickedPhone
                errorMessage = null
            } catch (e: Exception) {
                onPermissionMessage(
                    if (strings.isBn) "কন্টাক্ট তথ্য পড়তে সমস্যা হয়েছে" else "Could not read contact details"
                )
            }
        }
    }

    val contactPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            contactPickerLauncher.launch(null)
        } else {
            onPermissionMessage(
                if (strings.isBn) {
                    "কন্টাক্ট ইমপোর্ট করতে READ_CONTACTS অনুমতি প্রয়োজন"
                } else {
                    "Contacts permission is required to auto-fill phone numbers"
                }
            )
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(TallyCyanRefraction.copy(alpha = 0.2f))
                            .border(1.dp, TallyCyanRefraction, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PersonAdd,
                            contentDescription = strings.addCustomer,
                            tint = TallyCyanRefraction,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Text(
                        text = strings.addCustomer,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = {
                            val hasPerm = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.READ_CONTACTS
                            ) == PackageManager.PERMISSION_GRANTED
                            if (hasPerm) {
                                contactPickerLauncher.launch(null)
                            } else {
                                contactPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
                            }
                        },
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(TallyCyanRefraction.copy(alpha = 0.18f))
                            .border(1.dp, TallyCyanRefraction.copy(alpha = 0.6f), CircleShape)
                            .testTag("contact_picker_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContactPhone,
                            contentDescription = strings.importContact,
                            tint = TallyCyanRefraction,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = strings.closeButton,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            OutlinedTextField(
                value = name,
                onValueChange = {
                    name = it
                    errorMessage = null
                },
                label = { Text(strings.customerNameLabel) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("customer_name_input"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = TallyCyanRefraction,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                )
            )

            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text(strings.phoneNumberLabel) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("customer_phone_input"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = TallyCyanRefraction,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                )
            )

            // Icon-only toggle for Extra Customer Options (Party Type, Address, Opening Balance)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { showMoreCustomerOptions = !showMoreCustomerOptions },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(
                            if (showMoreCustomerOptions) {
                                TallyCyanRefraction.copy(alpha = 0.24f)
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                            }
                        )
                        .border(
                            width = 1.dp,
                            color = if (showMoreCustomerOptions) TallyCyanRefraction else Color.Transparent,
                            shape = CircleShape
                        )
                        .testTag("toggle_customer_more_options")
                ) {
                    Icon(
                        imageVector = if (showMoreCustomerOptions) Icons.Default.ExpandLess else Icons.Default.Tune,
                        contentDescription = if (strings.isBn) "আরও অপশন" else "More Options",
                        tint = if (showMoreCustomerOptions) TallyCyanRefraction else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            AnimatedVisibility(
                visible = showMoreCustomerOptions,
                enter = fadeIn() + expandVertically(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)),
                exit = fadeOut() + shrinkVertically(animationSpec = spring(stiffness = Spring.StiffnessMedium))
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Party Type selector (Customer vs Supplier)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        LiquidFilterChip(
                            label = strings.partyTypeCustomer,
                            selected = partyType == "CUSTOMER",
                            onClick = { partyType = "CUSTOMER" },
                            useDarkGlass = useDarkGlass,
                            accentColor = TallyCyanRefraction,
                            modifier = Modifier.weight(1f),
                            testTag = "party_type_customer_chip"
                        )
                        LiquidFilterChip(
                            label = strings.partyTypeSupplier,
                            selected = partyType == "SUPPLIER",
                            onClick = { partyType = "SUPPLIER" },
                            useDarkGlass = useDarkGlass,
                            accentColor = TallyGoldAccent,
                            modifier = Modifier.weight(1f),
                            testTag = "party_type_supplier_chip"
                        )
                    }

                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text(strings.addressLabel) },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("customer_address_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TallyCyanRefraction,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )

                    // Optional opening balances
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = openingDebitText,
                            onValueChange = { openingDebitText = it.filter { ch -> ch.isDigit() || ch == '.' } },
                            label = {
                                Text(
                                    if (strings.isBn) "পূর্বের পাব ($currencySymbol)" else "Opening Debit ($currencySymbol)"
                                )
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("opening_debit_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = TallyRubyRed,
                                unfocusedBorderColor = TallyRubyRed.copy(alpha = 0.45f)
                            )
                        )
                        OutlinedTextField(
                            value = openingCreditText,
                            onValueChange = { openingCreditText = it.filter { ch -> ch.isDigit() || ch == '.' } },
                            label = {
                                Text(
                                    if (strings.isBn) "পূর্বের দেব ($currencySymbol)" else "Opening Credit ($currencySymbol)"
                                )
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("opening_credit_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = TallyEmeraldGreen,
                                unfocusedBorderColor = TallyEmeraldGreen.copy(alpha = 0.45f)
                            )
                        )
                    }
                }
            }

            if (errorMessage != null) {
                Text(
                    text = errorMessage!!,
                    color = TallyRubyRed,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("cancel_add_customer_button")
                ) {
                    Text(strings.cancelButton)
                }
                SpringyLiquidButton(
                    onClick = {
                        if (name.trim().isEmpty()) {
                            errorMessage = if (strings.isBn) {
                                "অনুগ্রহ করে কাস্টমারের নাম লিখুন"
                            } else {
                                "Please enter customer name"
                            }
                        } else {
                            val debit = openingDebitText.toDoubleOrNull() ?: 0.0
                            val credit = openingCreditText.toDoubleOrNull() ?: 0.0
                            onSaveCustomer(name, phone, partyType, address, debit, credit)
                        }
                    },
                    accentColor = TallyEmeraldGreen,
                    secondaryColor = Color(0xFF00965E),
                    modifier = Modifier.weight(1.6f),
                    testTag = "save_customer_button"
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = strings.saveButton,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddTransactionBottomSheet(
    strings: AppStrings,
    useDarkGlass: Boolean,
    currencySymbol: String,
    customers: List<CustomerWithBalance>,
    preselectedCustomerId: Long?,
    initialType: String, // "DEBIT" (টাকা পাব / দিয়েছি) or "CREDIT" (টাকা দেব / পেয়েছি)
    categories: List<String>,
    viewModel: LedgerViewModel,
    editingTransaction: com.example.data.TransactionEntity? = null,
    onDismiss: () -> Unit,
    onSaved: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var selectedCustomerId by remember(editingTransaction, preselectedCustomerId) {
        mutableStateOf(
            editingTransaction?.customerId
                ?: preselectedCustomerId
                ?: customers.firstOrNull()?.customer?.id
        )
    }
    var txType by remember(editingTransaction, initialType) {
        mutableStateOf(editingTransaction?.type ?: initialType)
    }
    var amountText by remember(editingTransaction) {
        mutableStateOf(
            editingTransaction?.amount?.let { amt ->
                if (amt % 1.0 == 0.0) amt.toLong().toString() else amt.toString()
            } ?: ""
        )
    }
    var selectedCategory by remember(editingTransaction, initialType) {
        mutableStateOf(
            editingTransaction?.category ?: if (initialType == "DEBIT") {
                categories.firstOrNull() ?: "বাকিতে বিক্রি"
            } else {
                categories.getOrNull(1) ?: categories.firstOrNull() ?: "নগদ জমা"
            }
        )
    }
    var paymentMethod by remember(editingTransaction) {
        mutableStateOf(editingTransaction?.paymentMethod ?: "CASH")
    }
    var note by remember(editingTransaction) {
        mutableStateOf(editingTransaction?.note ?: "")
    }
    var timestamp by remember(editingTransaction) {
        mutableLongStateOf(editingTransaction?.timestamp ?: System.currentTimeMillis())
    }
    var receiptPath by remember(editingTransaction) {
        mutableStateOf<String?>(editingTransaction?.receiptUri)
    }
    var pendingCameraPair by remember { mutableStateOf<Pair<Uri, String>?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showMoreTxOptions by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val amountFocusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        delay(150)
        try {
            amountFocusRequester.requestFocus()
            keyboardController?.show()
        } catch (_: Exception) {
        }
    }

    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && pendingCameraPair != null) {
            receiptPath = pendingCameraPair!!.second
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val pair = viewModel.createCameraReceiptUri(context)
            if (pair != null) {
                pendingCameraPair = pair
                takePictureLauncher.launch(pair.first)
            }
        } else {
            viewModel.showStatusMessage(
                if (strings.isBn) "রশিদের ছবি তুলতে ক্যামেরা অনুমতি প্রয়োজন" else "Camera permission is required to capture receipts"
            )
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val savedPath = viewModel.copyGalleryReceiptToInternalStorage(context, uri)
                receiptPath = savedPath
            }
        }
    }

    val activeAccent = if (txType == "DEBIT") TallyRubyRed else TallyEmeraldGreen

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Compact Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (editingTransaction != null) {
                        if (strings.isBn) "লেনদেন এডিট করুন" else "Edit Ledger Entry"
                    } else {
                        strings.addEntryButton
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = strings.closeButton,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Compact Dual Toggle: টাকা পাব / দিয়েছি (Debit) vs টাকা দেব / পেয়েছি (Credit)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SpringyLiquidButton(
                    onClick = {
                        txType = "DEBIT"
                        try {
                            amountFocusRequester.requestFocus()
                            keyboardController?.show()
                        } catch (_: Exception) {
                        }
                    },
                    accentColor = if (txType == "DEBIT") TallyRubyRed else TallyRubyRed.copy(alpha = 0.25f),
                    secondaryColor = if (txType == "DEBIT") Color(0xFFB71C1C) else TallyRubyRed.copy(alpha = 0.15f),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                    testTag = "toggle_debit_button"
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowUpward,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = strings.debitTitle,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                SpringyLiquidButton(
                    onClick = {
                        txType = "CREDIT"
                        try {
                            amountFocusRequester.requestFocus()
                            keyboardController?.show()
                        } catch (_: Exception) {
                        }
                    },
                    accentColor = if (txType == "CREDIT") TallyEmeraldGreen else TallyEmeraldGreen.copy(alpha = 0.25f),
                    secondaryColor = if (txType == "CREDIT") Color(0xFF007E33) else TallyEmeraldGreen.copy(alpha = 0.15f),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                    testTag = "toggle_credit_button"
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowDownward,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = strings.creditTitle,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Customer selector if not pre-locked
            if (preselectedCustomerId == null && customers.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    customers.forEach { item ->
                        LiquidFilterChip(
                            label = item.customer.name,
                            selected = selectedCustomerId == item.customer.id,
                            onClick = {
                                selectedCustomerId = item.customer.id
                                try {
                                    amountFocusRequester.requestFocus()
                                    keyboardController?.show()
                                } catch (_: Exception) {
                                }
                            },
                            useDarkGlass = useDarkGlass,
                            accentColor = activeAccent,
                            testTag = "select_customer_chip_${item.customer.id}"
                        )
                    }
                }
            }

            // Primary Amount Input
            OutlinedTextField(
                value = amountText,
                onValueChange = {
                    amountText = it.filter { ch -> ch.isDigit() || ch == '.' }
                    errorMessage = null
                },
                label = { Text("${strings.amountLabel} ($currencySymbol)") },
                textStyle = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = activeAccent
                ),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(amountFocusRequester)
                    .testTag("transaction_amount_input"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = activeAccent,
                    unfocusedBorderColor = activeAccent.copy(alpha = 0.5f)
                )
            )

            // Compact Note Input
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text(strings.noteLabel) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("transaction_note_input"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = activeAccent,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                )
            )

            // Clean Icon-Only Action Bar (Date/Time, Camera, Gallery, More Options Toggle, & Attached Receipt Thumbnail)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Date & Time Picker Icon Button
                    IconButton(
                        onClick = { showDatePicker = true },
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(TallyCyanRefraction.copy(alpha = 0.16f))
                            .border(1.dp, TallyCyanRefraction.copy(alpha = 0.5f), CircleShape)
                            .testTag("timestamp_picker_card")
                    ) {
                        Icon(
                            imageVector = Icons.Default.EditCalendar,
                            contentDescription = strings.dateTimeLabel,
                            tint = TallyCyanRefraction,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Camera Receipt Icon Button
                    IconButton(
                        onClick = {
                            val hasCam = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.CAMERA
                            ) == PackageManager.PERMISSION_GRANTED
                            if (hasCam) {
                                val pair = viewModel.createCameraReceiptUri(context)
                                if (pair != null) {
                                    pendingCameraPair = pair
                                    takePictureLauncher.launch(pair.first)
                                }
                            } else {
                                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                        },
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1E88E5).copy(alpha = 0.16f))
                            .border(1.dp, Color(0xFF1E88E5).copy(alpha = 0.5f), CircleShape)
                            .testTag("camera_receipt_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddAPhoto,
                            contentDescription = strings.cameraReceiptButton,
                            tint = Color(0xFF1E88E5),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Gallery Receipt Icon Button
                    IconButton(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF8E24AA).copy(alpha = 0.16f))
                            .border(1.dp, Color(0xFF8E24AA).copy(alpha = 0.5f), CircleShape)
                            .testTag("gallery_receipt_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Image,
                            contentDescription = strings.galleryReceiptButton,
                            tint = Color(0xFF8E24AA),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // More Options (Payment Method, Category, Quick Amounts) Icon Toggle
                    IconButton(
                        onClick = { showMoreTxOptions = !showMoreTxOptions },
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(
                                if (showMoreTxOptions) {
                                    activeAccent.copy(alpha = 0.24f)
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                                }
                            )
                            .border(
                                width = 1.dp,
                                color = if (showMoreTxOptions) activeAccent else Color.Transparent,
                                shape = CircleShape
                            )
                            .testTag("toggle_tx_more_options")
                    ) {
                        Icon(
                            imageVector = if (showMoreTxOptions) Icons.Default.ExpandLess else Icons.Default.Tune,
                            contentDescription = if (strings.isBn) "আরও অপশন" else "More Options",
                            tint = if (showMoreTxOptions) activeAccent else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Compact Icon/Thumbnail Preview if Receipt Attached
                AnimatedVisibility(
                    visible = receiptPath != null,
                    enter = fadeIn() + scaleIn(animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy))
                ) {
                    receiptPath?.let { path ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            AsyncImage(
                                model = if (path.startsWith("/")) File(path) else Uri.parse(path),
                                contentDescription = strings.receiptAttachedBadge,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .border(1.5.dp, TallyEmeraldGreen, CircleShape)
                            )
                            IconButton(
                                onClick = { receiptPath = null },
                                modifier = Modifier
                                    .size(26.dp)
                                    .clip(CircleShape)
                                    .background(TallyRubyRed.copy(alpha = 0.2f))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = strings.deleteButton,
                                    tint = TallyRubyRed,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Expandable Extra Options with Smooth Spring Animation
            AnimatedVisibility(
                visible = showMoreTxOptions,
                enter = fadeIn() + expandVertically(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)),
                exit = fadeOut() + shrinkVertically(animationSpec = spring(stiffness = Spring.StiffnessMedium))
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Quick Amount Pills (+100, +500, +1000, +5000)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(100, 500, 1000, 5000).forEach { inc ->
                            val labelText = if (strings.isBn) {
                                "+${LocalizationProvider.toBengaliDigits(inc.toString())}"
                            } else {
                                "+$inc"
                            }
                            LiquidFilterChip(
                                label = labelText,
                                selected = false,
                                onClick = {
                                    val current = amountText.toDoubleOrNull() ?: 0.0
                                    val updated = current + inc
                                    amountText = if (updated % 1.0 == 0.0) {
                                        updated.toLong().toString()
                                    } else {
                                        updated.toString()
                                    }
                                },
                                useDarkGlass = useDarkGlass,
                                accentColor = activeAccent,
                                modifier = Modifier.weight(1f),
                                testTag = "quick_amount_$inc"
                            )
                        }
                    }

                    // Payment Method Selector (Cash, bKash, Nagad, Bank)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val methods = listOf(
                            Triple("CASH", if (strings.isBn) "নগদ" else "Cash", TallyEmeraldGreen),
                            Triple("BKASH", if (strings.isBn) "বিকাশ" else "bKash", Color(0xFFE2136E)),
                            Triple("NAGAD", if (strings.isBn) "নগদ অ্যাপ" else "Nagad", Color(0xFFF7941D)),
                            Triple("BANK", if (strings.isBn) "ব্যাংক" else "Bank", TallyCyanRefraction)
                        )
                        methods.forEach { (code, label, tint) ->
                            LiquidFilterChip(
                                label = label,
                                selected = paymentMethod == code,
                                onClick = { paymentMethod = code },
                                useDarkGlass = useDarkGlass,
                                accentColor = tint,
                                leadingIcon = {
                                    Icon(
                                        imageVector = when (code) {
                                            "CASH" -> Icons.Default.Payments
                                            "BANK" -> Icons.Default.AccountBalance
                                            else -> Icons.Default.PhoneAndroid
                                        },
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = if (paymentMethod == code) Color.White else tint
                                    )
                                },
                                testTag = "payment_method_$code"
                            )
                        }
                    }

                    // Customizable Category Selector
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        categories.forEach { cat ->
                            LiquidFilterChip(
                                label = cat,
                                selected = selectedCategory == cat,
                                onClick = { selectedCategory = cat },
                                useDarkGlass = useDarkGlass,
                                accentColor = activeAccent,
                                testTag = "category_chip_$cat"
                            )
                        }
                    }
                }
            }

            if (errorMessage != null) {
                Text(
                    text = errorMessage!!,
                    color = TallyRubyRed,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            // Save button
            SpringyLiquidButton(
                onClick = {
                    val parsedAmount = amountText.toDoubleOrNull() ?: 0.0
                    val targetCustomerId = selectedCustomerId
                    if (targetCustomerId == null) {
                        errorMessage = if (strings.isBn) "প্রথমে একজন কাস্টমার নির্বাচন করুন" else "Please select a customer first"
                    } else if (parsedAmount <= 0.0) {
                        errorMessage = if (strings.isBn) "সঠিক টাকার পরিমাণ লিখুন" else "Please enter a valid amount"
                    } else {
                        if (editingTransaction != null) {
                            viewModel.updateTransaction(
                                transactionId = editingTransaction.id,
                                customerId = targetCustomerId,
                                type = txType,
                                amount = parsedAmount,
                                category = selectedCategory,
                                paymentMethod = paymentMethod,
                                note = note,
                                receiptUri = receiptPath,
                                timestamp = timestamp
                            )
                        } else {
                            viewModel.addTransaction(
                                customerId = targetCustomerId,
                                type = txType,
                                amount = parsedAmount,
                                category = selectedCategory,
                                paymentMethod = paymentMethod,
                                note = note,
                                receiptUri = receiptPath,
                                timestamp = timestamp
                            )
                        }
                        onSaved()
                    }
                },
                accentColor = activeAccent,
                secondaryColor = if (txType == "DEBIT") Color(0xFFB71C1C) else Color(0xFF007E33),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 14.dp),
                testTag = "save_transaction_button"
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${strings.saveButton} (${if (txType == "DEBIT") strings.debitShort else strings.creditShort})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = timestamp)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { selectedDate ->
                            val calOld = Calendar.getInstance().apply { timeInMillis = timestamp }
                            val calNew = Calendar.getInstance().apply { timeInMillis = selectedDate }
                            calNew.set(Calendar.HOUR_OF_DAY, calOld.get(Calendar.HOUR_OF_DAY))
                            calNew.set(Calendar.MINUTE, calOld.get(Calendar.MINUTE))
                            timestamp = calNew.timeInMillis
                        }
                        showDatePicker = false
                        showTimePicker = true
                    }
                ) {
                    Text(if (strings.isBn) "সময় নির্বাচন" else "Pick Time")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(strings.cancelButton)
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showTimePicker) {
        val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
        val timePickerState = rememberTimePickerState(
            initialHour = cal.get(Calendar.HOUR_OF_DAY),
            initialMinute = cal.get(Calendar.MINUTE),
            is24Hour = false
        )
        DatePickerDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val updatedCal = Calendar.getInstance().apply {
                            timeInMillis = timestamp
                            set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                            set(Calendar.MINUTE, timePickerState.minute)
                        }
                        timestamp = updatedCal.timeInMillis
                        showTimePicker = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text(strings.cancelButton)
                }
            }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                TimePicker(state = timePickerState)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmsReminderBottomSheet(
    strings: AppStrings,
    useDarkGlass: Boolean,
    customerName: String,
    customerPhone: String,
    balanceAmount: Double,
    currencySymbol: String,
    businessName: String,
    ownerPhone: String,
    onDismiss: () -> Unit,
    onCopiedNotice: (String) -> Unit
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val templates = remember(customerName, balanceAmount, currencySymbol, businessName, ownerPhone) {
        LocalizationProvider.generateBengaliSmsTemplates(
            customerName = customerName,
            balanceAmount = balanceAmount,
            currencySymbol = currencySymbol,
            businessName = businessName,
            ownerPhone = ownerPhone
        )
    }
    var selectedIdx by remember { mutableStateOf(0) }
    var customSmsText by remember(selectedIdx) { mutableStateOf(templates[selectedIdx].second) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = strings.smsTemplateSheetTitle,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                templates.forEachIndexed { idx, (title, _) ->
                    LiquidFilterChip(
                        label = title,
                        selected = selectedIdx == idx,
                        onClick = {
                            selectedIdx = idx
                            customSmsText = templates[idx].second
                        },
                        useDarkGlass = useDarkGlass,
                        accentColor = TallyGoldAccent,
                        testTag = "sms_template_chip_$idx"
                    )
                }
            }

            OutlinedTextField(
                value = customSmsText,
                onValueChange = { customSmsText = it },
                maxLines = 6,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("sms_template_body_input")
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SpringyLiquidButton(
                    onClick = {
                        clipboard.setText(AnnotatedString(customSmsText))
                        onCopiedNotice(
                            if (strings.isBn) "✅ তাগাদা মেসেজ কপি করা হয়েছে" else "✅ SMS Reminder copied to clipboard"
                        )
                        onDismiss()
                    },
                    accentColor = Color(0xFF3949AB),
                    secondaryColor = Color(0xFF1A237E),
                    modifier = Modifier.weight(0.85f),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 12.dp),
                    testTag = "copy_sms_button"
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (strings.isBn) "কপি" else "Copy",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                SpringyLiquidButton(
                    onClick = {
                        try {
                            val smsIntent = Intent(Intent.ACTION_SENDTO).apply {
                                data = Uri.parse("smsto:${Uri.encode(customerPhone)}")
                                putExtra("sms_body", customSmsText)
                            }
                            context.startActivity(smsIntent)
                        } catch (e: Exception) {
                            onCopiedNotice(
                                if (strings.isBn) "মেসেজ অ্যাপ খোলা যায়নি, টেক্সট কপি করা হয়েছে" else "Copied SMS to clipboard"
                            )
                        }
                        onDismiss()
                    },
                    accentColor = TallyCyanRefraction,
                    secondaryColor = Color(0xFF0077B6),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 12.dp),
                    testTag = "send_sms_intent_button"
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (strings.isBn) "SMS" else "SMS",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                SpringyLiquidButton(
                    onClick = {
                        clipboard.setText(AnnotatedString(customSmsText))
                        val rawDigits = customerPhone.filter { it.isDigit() }
                        val formattedWaPhone = when {
                            rawDigits.length == 11 && rawDigits.startsWith("01") -> "88$rawDigits"
                            rawDigits.startsWith("00") -> rawDigits.removePrefix("00")
                            else -> rawDigits
                        }
                        val encodedMsg = Uri.encode(customSmsText)
                        val waUri = if (formattedWaPhone.isNotBlank()) {
                            Uri.parse("https://wa.me/$formattedWaPhone?text=$encodedMsg")
                        } else {
                            Uri.parse("https://wa.me/?text=$encodedMsg")
                        }

                        var launched = false
                        for (pkg in listOf("com.whatsapp", "com.whatsapp.w4b")) {
                            if (!launched) {
                                try {
                                    val pkgIntent = Intent(Intent.ACTION_VIEW, waUri).apply {
                                        setPackage(pkg)
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    context.startActivity(pkgIntent)
                                    launched = true
                                } catch (_: Exception) {
                                }
                            }
                        }

                        if (!launched) {
                            try {
                                val fallbackIntent = Intent(Intent.ACTION_VIEW, waUri).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(fallbackIntent)
                            } catch (_: Exception) {
                                onCopiedNotice(
                                    if (strings.isBn) "✅ WhatsApp মেসেজ কপি হয়েছে" else "✅ Message copied for WhatsApp"
                                )
                            }
                        }
                        onDismiss()
                    },
                    accentColor = Color(0xFF25D366),
                    secondaryColor = Color(0xFF128C7E),
                    modifier = Modifier.weight(1.25f),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 12.dp),
                    testTag = "send_whatsapp_message_button"
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Chat,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "WhatsApp",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun ReceiptViewerDialog(
    receiptPath: String,
    strings: AppStrings,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        LiquidGlassCard(
            useDarkGlass = true,
            accentTint = TallyCyanRefraction,
            cornerRadius = 24.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ReceiptLong,
                            contentDescription = null,
                            tint = TallyCyanRefraction
                        )
                        Text(
                            text = strings.viewReceiptTitle,
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = strings.closeButton,
                            tint = Color.White
                        )
                    }
                }

                AsyncImage(
                    model = if (receiptPath.startsWith("/")) File(receiptPath) else Uri.parse(receiptPath),
                    contentDescription = strings.viewReceiptTitle,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(340.dp)
                        .clip(RoundedCornerShape(16.dp))
                )

                SpringyLiquidButton(
                    onClick = onDismiss,
                    accentColor = TallyCyanRefraction,
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "close_receipt_dialog_button"
                ) {
                    Text(strings.closeButton, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/**
 * First-time onboarding setup dialog asking for Business info and optional first customer.
 */
@Composable
fun FirstLaunchSetupDialog(
    strings: AppStrings,
    useDarkGlass: Boolean,
    initialLanguage: String,
    initialCurrency: String,
    onLanguageChange: (String) -> Unit,
    onCompleteSetup: (
        businessName: String,
        ownerPhone: String,
        currencySymbol: String,
        language: String,
        firstCustomerName: String,
        firstCustomerPhone: String,
        firstCustomerOpeningDebit: Double,
        firstCustomerOpeningCredit: Double
    ) -> Unit
) {
    var businessName by remember { mutableStateOf("") }
    var ownerPhone by remember { mutableStateOf("") }
    var currencySymbol by remember { mutableStateOf(initialCurrency.ifBlank { "৳" }) }
    var firstCustomerName by remember { mutableStateOf("") }
    var firstCustomerPhone by remember { mutableStateOf("") }
    var openingDebitText by remember { mutableStateOf("") }
    var openingCreditText by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = {}) {
        LiquidGlassCard(
            useDarkGlass = useDarkGlass,
            accentTint = TallyCyanRefraction,
            cornerRadius = 24.dp,
            testTag = "first_launch_setup_dialog",
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = if (strings.isBn) "আপনার ব্যবসার তথ্য দিন" else "Set Up Your Ledger",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = if (strings.isBn) {
                        "শুরু করার জন্য আপনার ব্যবসা প্রতিষ্ঠানের নাম এবং চাইলে প্রথম কাস্টমারের হিসাব যুক্ত করুন।"
                    } else {
                        "Enter your business details and optionally add your first customer to start your ledger."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = businessName,
                    onValueChange = {
                        businessName = it
                        errorMessage = null
                    },
                    label = { Text(strings.businessNameLabel) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("setup_business_name_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TallyCyanRefraction,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )

                OutlinedTextField(
                    value = ownerPhone,
                    onValueChange = { ownerPhone = it },
                    label = { Text(strings.ownerPhoneLabel) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("setup_owner_phone_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TallyCyanRefraction,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (strings.isBn) "মুদ্রা:" else "Currency:",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    listOf("৳", "Tk", "₹", "$").forEach { sym ->
                        LiquidFilterChip(
                            label = sym,
                            selected = currencySymbol == sym,
                            onClick = { currencySymbol = sym },
                            useDarkGlass = useDarkGlass,
                            accentColor = TallyEmeraldGreen
                        )
                    }
                }

                Text(
                    text = if (strings.isBn) "প্রথম কাস্টমার যুক্ত করুন (ঐচ্ছিক)" else "Add First Customer (Optional)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TallyCyanRefraction
                )

                OutlinedTextField(
                    value = firstCustomerName,
                    onValueChange = { firstCustomerName = it },
                    label = { Text(strings.customerNameLabel) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("setup_first_customer_name_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TallyCyanRefraction,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )

                OutlinedTextField(
                    value = firstCustomerPhone,
                    onValueChange = { firstCustomerPhone = it },
                    label = { Text(strings.phoneNumberLabel) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("setup_first_customer_phone_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TallyCyanRefraction,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = openingDebitText,
                        onValueChange = { openingDebitText = it.filter { ch -> ch.isDigit() || ch == '.' } },
                        label = {
                            Text(
                                if (strings.isBn) "পূর্বের পাব ($currencySymbol)" else "Opening Receivable"
                            )
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TallyRubyRed,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )
                    OutlinedTextField(
                        value = openingCreditText,
                        onValueChange = { openingCreditText = it.filter { ch -> ch.isDigit() || ch == '.' } },
                        label = {
                            Text(
                                if (strings.isBn) "পূর্বের দেব ($currencySymbol)" else "Opening Payable"
                            )
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TallyEmeraldGreen,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )
                }

                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = TallyRubyRed,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                SpringyLiquidButton(
                    onClick = {
                        if (businessName.trim().isEmpty()) {
                            errorMessage = if (strings.isBn) {
                                "অনুগ্রহ করে আপনার ব্যবসা প্রতিষ্ঠানের নাম লিখুন"
                            } else {
                                "Please enter your business name"
                            }
                        } else {
                            onCompleteSetup(
                                businessName.trim(),
                                ownerPhone.trim(),
                                currencySymbol,
                                initialLanguage,
                                firstCustomerName.trim(),
                                firstCustomerPhone.trim(),
                                openingDebitText.toDoubleOrNull() ?: 0.0,
                                openingCreditText.toDoubleOrNull() ?: 0.0
                            )
                        }
                    },
                    accentColor = TallyEmeraldGreen,
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "complete_setup_button"
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (strings.isBn) "হিসাব খাতা শুরু করুন" else "Start Ledger",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

