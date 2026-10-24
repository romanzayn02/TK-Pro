package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.CustomerWithBalance
import com.example.data.TransactionEntity
import com.example.data.TransactionWithCustomer
import com.example.ui.AppStrings
import com.example.ui.LocalizationProvider
import com.example.ui.components.ConfirmActionDialog
import com.example.ui.components.LiquidGlassCard
import com.example.ui.components.SpringyLiquidButton
import com.example.ui.theme.TallyCyanRefraction
import com.example.ui.theme.TallyEmeraldGreen
import com.example.ui.theme.TallyGoldAccent
import com.example.ui.theme.TallyRubyRed
import androidx.core.content.FileProvider
import java.io.File
import kotlin.math.abs

private fun sendDirectWhatsAppMessage(
    context: android.content.Context,
    customerPhone: String?,
    messageText: String,
    chooserTitle: String
) {
    val rawDigits = customerPhone?.filter { it.isDigit() }.orEmpty()
    val formattedWaPhone = when {
        rawDigits.length == 11 && rawDigits.startsWith("01") -> "88$rawDigits"
        rawDigits.startsWith("00") -> rawDigits.removePrefix("00")
        else -> rawDigits
    }
    val encodedMsg = Uri.encode(messageText)
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
            try {
                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, messageText)
                }
                context.startActivity(Intent.createChooser(sendIntent, chooserTitle))
            } catch (_: Exception) {
            }
        }
    }
}

private fun shareSingleTransaction(
    context: android.content.Context,
    transaction: TransactionEntity,
    customerName: String?,
    customerPhone: String?,
    balanceAfterTransaction: Double?,
    currency: String,
    strings: AppStrings
) {
    val isDebit = transaction.type == "DEBIT"
    val amountFormatted = LocalizationProvider.formatAmount(transaction.amount, currency, strings.isBn)
    val dateFormatted = LocalizationProvider.formatTimestamp(transaction.timestamp, strings.isBn)
    val methodLabel = LocalizationProvider.paymentMethodLabel(transaction.paymentMethod, strings.isBn)

    val shareText = buildString {
        if (isDebit) {
            append(
                if (strings.isBn) {
                    "🔴 ${strings.appTitle} — বাকি / প্রদান হিসাব (Debit)\n"
                } else {
                    "🔴 ${strings.appTitle} — Debit Entry Notice\n"
                }
            )
            append("━━━━━━━━━━━━━━━\n")
            if (!customerName.isNullOrBlank()) {
                append(
                    if (strings.isBn) {
                        "প্রিয় $customerName,\nআপনার নামে $amountFormatted বাকি/প্রদান (Debit) হিসাব যোগ হয়েছে।\n"
                    } else {
                        "Dear $customerName,\nA Debit entry of $amountFormatted has been added to your account.\n"
                    }
                )
            } else {
                append(if (strings.isBn) "💰 বাকি/প্রদান পরিমাণ: $amountFormatted\n" else "💰 Debit Amount: $amountFormatted\n")
            }
        } else {
            append(
                if (strings.isBn) {
                    "🟢 ${strings.appTitle} — জমা / পরিশোধ রশিদ (Credit)\n"
                } else {
                    "🟢 ${strings.appTitle} — Payment Received Receipt (Credit)\n"
                }
            )
            append("━━━━━━━━━━━━━━━\n")
            if (!customerName.isNullOrBlank()) {
                append(
                    if (strings.isBn) {
                        "প্রিয় $customerName,\nআপনার $amountFormatted জমা/পরিশোধ (Credit) সফলভাবে গৃহীত হয়েছে। ধন্যবাদ!\n"
                    } else {
                        "Dear $customerName,\nYour payment/credit of $amountFormatted has been received. Thank you!\n"
                    }
                )
            } else {
                append(if (strings.isBn) "💰 জমা/পরিশোধ পরিমাণ: $amountFormatted\n" else "💰 Credit Amount: $amountFormatted\n")
            }
        }
        val catSuffix = if (transaction.category.isNotBlank()) " (${transaction.category})" else ""
        append(if (strings.isBn) "💳 মাধ্যম: $methodLabel$catSuffix\n" else "💳 Method: $methodLabel$catSuffix\n")
        if (transaction.note.isNotBlank()) {
            append(if (strings.isBn) "📝 বিবরণ: ${transaction.note}\n" else "📝 Note: ${transaction.note}\n")
        }
        if (balanceAfterTransaction != null) {
            val statusLabel = when {
                balanceAfterTransaction > 0.001 -> strings.receivableBadge
                balanceAfterTransaction < -0.001 -> strings.payableBadge
                else -> strings.settledBadge
            }
            val balFormatted = LocalizationProvider.formatAmount(abs(balanceAfterTransaction), currency, strings.isBn)
            append("━━━━━━━━━━━━━━━\n")
            append(if (strings.isBn) "📊 বর্তমান জের: $balFormatted ($statusLabel)" else "📊 Current Balance: $balFormatted ($statusLabel)")
        }
    }

    sendDirectWhatsAppMessage(
        context = context,
        customerPhone = customerPhone,
        messageText = shareText,
        chooserTitle = if (strings.isBn) "লেনদেন শেয়ার করুন" else "Share Transaction"
    )
}

private fun shareCustomerLedgerStatement(
    context: android.content.Context,
    customerDetail: CustomerWithBalance,
    transactions: List<TransactionEntity>,
    currency: String,
    strings: AppStrings
) {
    val customer = customerDetail.customer
    val net = customerDetail.netBalance
    val statusLabel = when {
        net > 0.001 -> strings.receivableBadge
        net < -0.001 -> strings.payableBadge
        else -> strings.settledBadge
    }

    val statementText = buildString {
        append("📊 ${strings.appTitle} — ")
        append(if (strings.isBn) "হিসাব বিবরণী\n" else "Account Statement\n")
        append("━━━━━━━━━━━━━━━\n")
        append("👤 ${customer.name}")
        if (customer.phone.isNotBlank()) append(" (${customer.phone})")
        append("\n")
        append(
            if (strings.isBn) {
                "• মোট দিয়েছি/পাব: ${LocalizationProvider.formatAmount(customerDetail.totalDebit, currency, true)}\n" +
                    "• মোট পেয়েছি/দেব: ${LocalizationProvider.formatAmount(customerDetail.totalCredit, currency, true)}\n" +
                    "• বর্তমান ব্যালেন্স: ${LocalizationProvider.formatAmount(abs(net), currency, true)} ($statusLabel)\n"
            } else {
                "• Total Debit: ${LocalizationProvider.formatAmount(customerDetail.totalDebit, currency, false)}\n" +
                    "• Total Credit: ${LocalizationProvider.formatAmount(customerDetail.totalCredit, currency, false)}\n" +
                    "• Net Balance: ${LocalizationProvider.formatAmount(abs(net), currency, false)} ($statusLabel)\n"
            }
        )
        if (transactions.isNotEmpty()) {
            append("━━━━━━━━━━━━━━━\n")
            append(if (strings.isBn) "সাম্প্রতিক লেনদেন:\n" else "Recent Transactions:\n")
            transactions.take(10).forEach { tx ->
                val tSign = if (tx.type == "DEBIT") "+" else "-"
                val tAmt = LocalizationProvider.formatAmount(tx.amount, currency, strings.isBn)
                val tDate = LocalizationProvider.formatTimestamp(tx.timestamp, strings.isBn)
                val catTag = if (tx.category.isNotBlank()) " (${tx.category})" else ""
                append("• $tDate: $tSign$tAmt$catTag")
                if (tx.note.isNotBlank()) append(" - ${tx.note}")
                append("\n")
            }
        }
    }

    sendDirectWhatsAppMessage(
        context = context,
        customerPhone = customer.phone,
        messageText = statementText,
        chooserTitle = if (strings.isBn) "হিসাব বিবরণী শেয়ার করুন" else "Share Ledger Statement"
    )
}

@Composable
fun CustomerDetailScreen(
    customerDetail: CustomerWithBalance,
    transactions: List<TransactionEntity>,
    strings: AppStrings,
    currency: String,
    useDarkGlass: Boolean,
    onBack: () -> Unit,
    onAddDebit: () -> Unit,
    onAddCredit: () -> Unit,
    onOpenSmsReminder: () -> Unit,
    onViewReceipt: (String) -> Unit,
    onEditTransaction: (TransactionEntity) -> Unit,
    onDeleteTransaction: (Long) -> Unit,
    onDeleteCustomer: () -> Unit,
    onUpdateCustomerPhoto: (Uri?) -> Unit = {},
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)

    val context = LocalContext.current
    val customer = customerDetail.customer
    val net = customerDetail.netBalance
    val statusAccent = when {
        net > 0.001 -> TallyRubyRed
        net < -0.001 -> TallyEmeraldGreen
        else -> TallyCyanRefraction
    }

    val customerPhotoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            onUpdateCustomerPhoto(uri)
        }
    }

    var showDeleteCustomerConfirm by remember { mutableStateOf(false) }
    var pendingDeleteTransaction by remember { mutableStateOf<TransactionEntity?>(null) }

    val runningBalancesMap = remember(transactions) {
        val chronologicalList = transactions.sortedWith(compareBy<TransactionEntity> { it.timestamp }.thenBy { it.id })
        var currentRunning = 0.0
        val map = mutableMapOf<Long, Double>()
        for (t in chronologicalList) {
            if (t.type == "DEBIT") {
                currentRunning += t.amount
            } else {
                currentRunning -= t.amount
            }
            map[t.id] = currentRunning
        }
        map
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("customer_detail_screen")
    ) {
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Top Customer Header & Balance Summary Card
            item(key = "customer_header_card") {
                LiquidGlassCard(
                    useDarkGlass = useDarkGlass,
                    accentTint = statusAccent,
                    cornerRadius = 26.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                IconButton(
                                    onClick = onBack,
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.14f))
                                        .testTag("back_from_customer_detail_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = strings.cancelButton
                                    )
                                }

                                // Tappable Customer Avatar with Camera Badge to upload/change local photo
                                val avatarColor = Color(customer.avatarColorHex)
                                val customerPhoto = customer.photoUri?.trim().orEmpty()
                                Box(
                                    modifier = Modifier
                                        .size(50.dp)
                                        .clickable {
                                            customerPhotoPickerLauncher.launch(
                                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                            )
                                        }
                                        .testTag("customer_detail_avatar_picker"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(46.dp)
                                            .clip(CircleShape)
                                            .background(avatarColor.copy(alpha = 0.24f))
                                            .border(1.5.dp, avatarColor, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (customerPhoto.isNotBlank()) {
                                            AsyncImage(
                                                model = if (customerPhoto.startsWith("/")) File(customerPhoto) else customerPhoto,
                                                contentDescription = customer.name,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .clip(CircleShape)
                                            )
                                        } else {
                                            Text(
                                                text = customer.name.trim().take(1).uppercase(),
                                                style = MaterialTheme.typography.titleLarge,
                                                color = avatarColor,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .size(18.dp)
                                            .clip(CircleShape)
                                            .background(TallyCyanRefraction)
                                            .border(1.dp, Color.White, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.AddAPhoto,
                                            contentDescription = if (strings.isBn) "ছবি পরিবর্তন" else "Change Photo",
                                            tint = Color.White,
                                            modifier = Modifier.size(10.dp)
                                        )
                                    }
                                }

                                Column(modifier = Modifier.weight(1f, fill = false)) {
                                    Text(
                                        text = customer.name,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = buildString {
                                            append(customer.phone.ifBlank { "---" })
                                            if (customer.address.isNotBlank()) {
                                                append(" • ")
                                                append(customer.address)
                                            }
                                        },
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                IconButton(
                                    onClick = {
                                        shareCustomerLedgerStatement(
                                            context = context,
                                            customerDetail = customerDetail,
                                            transactions = transactions,
                                            currency = currency,
                                            strings = strings
                                        )
                                    },
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(TallyCyanRefraction.copy(alpha = 0.18f))
                                        .border(1.dp, TallyCyanRefraction.copy(alpha = 0.5f), CircleShape)
                                        .testTag("detail_share_ledger_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Share,
                                        contentDescription = if (strings.isBn) "হিসাব শেয়ার" else "Share Statement",
                                        tint = TallyCyanRefraction,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                IconButton(
                                    onClick = { showDeleteCustomerConfirm = true },
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(TallyRubyRed.copy(alpha = 0.16f))
                                        .testTag("delete_customer_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteOutline,
                                        contentDescription = strings.deleteButton,
                                        tint = TallyRubyRed,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        // Three mini summary columns: মোট দিয়েছি / পাব, মোট পেয়েছি / দেব, বর্তমান ব্যালেন্স
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            MiniStatBox(
                                label = strings.debitShort,
                                value = LocalizationProvider.formatAmount(
                                    customerDetail.totalDebit,
                                    currency,
                                    strings.isBn
                                ),
                                color = TallyRubyRed,
                                modifier = Modifier.weight(1f)
                            )
                            MiniStatBox(
                                label = strings.creditShort,
                                value = LocalizationProvider.formatAmount(
                                    customerDetail.totalCredit,
                                    currency,
                                    strings.isBn
                                ),
                                color = TallyEmeraldGreen,
                                modifier = Modifier.weight(1f)
                            )
                            MiniStatBox(
                                label = when {
                                    net > 0.001 -> strings.receivableBadge
                                    net < -0.001 -> strings.payableBadge
                                    else -> strings.settledBadge
                                },
                                value = LocalizationProvider.formatAmount(
                                    abs(net),
                                    currency,
                                    strings.isBn
                                ),
                                color = statusAccent,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // Quick Communications: Direct Dial (`tel:`), SMS Reminder, and Share Statement
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            SpringyLiquidButton(
                                onClick = {
                                    if (customer.phone.isNotBlank()) {
                                        val intent = Intent(Intent.ACTION_DIAL).apply {
                                            data = Uri.parse("tel:${Uri.encode(customer.phone)}")
                                        }
                                        context.startActivity(intent)
                                    }
                                },
                                accentColor = TallyEmeraldGreen,
                                secondaryColor = Color(0xFF007E33),
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                                testTag = "detail_direct_dial_button"
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Call,
                                    contentDescription = strings.callDirect,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = strings.callDirect,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            SpringyLiquidButton(
                                onClick = onOpenSmsReminder,
                                accentColor = TallyGoldAccent,
                                secondaryColor = Color(0xFFE65100),
                                modifier = Modifier.weight(1.2f),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                                testTag = "detail_sms_reminder_button"
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Sms,
                                    contentDescription = strings.smsReminder,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = strings.smsReminder,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            if (transactions.isEmpty()) {
                item(key = "empty_customer_txs") {
                    LiquidGlassCard(
                        useDarkGlass = useDarkGlass,
                        accentTint = TallyCyanRefraction,
                        cornerRadius = 20.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ReceiptLong,
                                contentDescription = null,
                                tint = TallyCyanRefraction,
                                modifier = Modifier.size(40.dp)
                            )
                            Text(
                                text = strings.noTransactionsEmptyTitle,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = strings.noTransactionsEmptyDesc,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(
                    items = transactions,
                    key = { it.id }
                ) { tx ->
                    val balanceAfter = runningBalancesMap[tx.id]
                    TransactionItemGlassCard(
                        transaction = tx,
                        customerName = customer.name,
                        customerPhone = customer.phone,
                        showCustomerNameTitle = false,
                        balanceAfterTransaction = balanceAfter,
                        strings = strings,
                        currency = currency,
                        useDarkGlass = useDarkGlass,
                        onViewReceipt = onViewReceipt,
                        onEdit = { onEditTransaction(tx) },
                        onDelete = { pendingDeleteTransaction = tx }
                    )
                }
            }
        }

        // Bottom Sticky Dual Buttons: "টাকা পাব / দিয়েছি" (Debit) and "টাকা দেব / পেয়েছি" (Credit)
        LiquidGlassCard(
            useDarkGlass = useDarkGlass,
            accentTint = TallyCyanRefraction,
            cornerRadius = 24.dp,
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SpringyLiquidButton(
                    onClick = onAddDebit,
                    accentColor = TallyRubyRed,
                    secondaryColor = Color(0xFFB71C1C),
                    modifier = Modifier.weight(1f),
                    testTag = "detail_add_debit_button"
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowUpward,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = strings.debitTitle,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                SpringyLiquidButton(
                    onClick = onAddCredit,
                    accentColor = TallyEmeraldGreen,
                    secondaryColor = Color(0xFF007E33),
                    modifier = Modifier.weight(1f),
                    testTag = "detail_add_credit_button"
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowDownward,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = strings.creditTitle,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    if (showDeleteCustomerConfirm) {
        val deleteCustomerTitle = if (strings.isBn) {
            if (strings.isIndividual) "হিসাব মুছে ফেলবেন?" else "কাস্টমার মুছে ফেলবেন?"
        } else {
            if (strings.isIndividual) "Delete Person / Account?" else "Delete Customer?"
        }
        val deleteCustomerMsg = if (strings.isBn) {
            "\"${customer.name}\"-এর প্রোফাইল এবং এর সাথে যুক্ত সকল লেনদেন স্থায়ীভাবে মুছে যাবে। আপনি কি নিশ্চিত?"
        } else {
            "All transaction history and details for \"${customer.name}\" will be permanently deleted. Are you sure?"
        }

        ConfirmActionDialog(
            title = deleteCustomerTitle,
            message = deleteCustomerMsg,
            confirmLabel = if (strings.isBn) "হ্যাঁ, মুছে ফেলুন" else "Delete",
            cancelLabel = strings.cancelButton,
            accentColor = TallyRubyRed,
            useDarkGlass = useDarkGlass,
            onConfirm = onDeleteCustomer,
            onDismiss = { showDeleteCustomerConfirm = false },
            confirmTestTag = "confirm_delete_customer_button",
            cancelTestTag = "cancel_delete_customer_button"
        )
    }

    pendingDeleteTransaction?.let { txToDelete ->
        val txTypeName = if (txToDelete.type == "DEBIT") {
            if (strings.isBn) "বাকি / প্রদান" else "Debit"
        } else {
            if (strings.isBn) "জমা / পরিশোধ" else "Credit"
        }
        val txAmountStr = LocalizationProvider.formatAmount(txToDelete.amount, currency, strings.isBn)
        val deleteTxTitle = if (strings.isBn) "লেনদেন মুছে ফেলবেন?" else "Delete Transaction?"
        val deleteTxMsg = if (strings.isBn) {
            "$txTypeName ($txAmountStr)-এর এই এন্ট্রিটি স্থায়ীভাবে মুছে ফেলতে চান? এটি অ্যাকাউন্টের মোট ব্যালেন্সে প্রভাব ফেলবে।"
        } else {
            "Permanently delete this $txTypeName entry of $txAmountStr? This will adjust the account balance."
        }

        ConfirmActionDialog(
            title = deleteTxTitle,
            message = deleteTxMsg,
            confirmLabel = if (strings.isBn) "হ্যাঁ, মুছুন" else "Delete",
            cancelLabel = strings.cancelButton,
            accentColor = TallyRubyRed,
            useDarkGlass = useDarkGlass,
            onConfirm = {
                onDeleteTransaction(txToDelete.id)
                pendingDeleteTransaction = null
            },
            onDismiss = { pendingDeleteTransaction = null },
            confirmTestTag = "confirm_delete_transaction_button",
            cancelTestTag = "cancel_delete_transaction_button"
        )
    }
}

@Composable
private fun MiniStatBox(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(color.copy(alpha = 0.14f))
            .border(1.dp, color.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color,
            maxLines = 1
        )
    }
}

@Composable
fun TransactionHistoryScreen(
    transactionsWithCustomer: List<TransactionWithCustomer>,
    strings: AppStrings,
    currency: String,
    useDarkGlass: Boolean,
    onViewReceipt: (String) -> Unit,
    onEditTransaction: (TransactionEntity) -> Unit,
    onDeleteTransaction: (Long) -> Unit,
    onOpenCustomerDetail: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var pendingDeleteTransaction by remember { mutableStateOf<TransactionEntity?>(null) }
    val runningBalancesMap = remember(transactionsWithCustomer) {
        val byCustomer = transactionsWithCustomer.groupBy { it.transaction.customerId }
        val map = mutableMapOf<Long, Double>()
        for ((_, list) in byCustomer) {
            val chronological = list.map { it.transaction }.sortedWith(compareBy<TransactionEntity> { it.timestamp }.thenBy { it.id })
            var currentRunning = 0.0
            for (t in chronological) {
                if (t.type == "DEBIT") {
                    currentRunning += t.amount
                } else {
                    currentRunning -= t.amount
                }
                map[t.id] = currentRunning
            }
        }
        map
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("transaction_history_lazy_column"),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 12.dp,
            bottom = 140.dp
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "history_header") {
            Text(
                text = strings.tabHistory,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }

        if (transactionsWithCustomer.isEmpty()) {
            item(key = "empty_all_txs") {
                LiquidGlassCard(
                    useDarkGlass = useDarkGlass,
                    accentTint = TallyCyanRefraction,
                    cornerRadius = 22.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ReceiptLong,
                            contentDescription = null,
                            tint = TallyCyanRefraction,
                            modifier = Modifier.size(44.dp)
                        )
                        Text(
                            text = strings.noTransactionsEmptyTitle,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = strings.noTransactionsEmptyDesc,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(
                items = transactionsWithCustomer,
                key = { it.transaction.id }
            ) { item ->
                val balanceAfter = runningBalancesMap[item.transaction.id]
                TransactionItemGlassCard(
                    transaction = item.transaction,
                    customerName = item.customerName,
                    customerPhone = item.customerPhone,
                    balanceAfterTransaction = balanceAfter,
                    strings = strings,
                    currency = currency,
                    useDarkGlass = useDarkGlass,
                    onViewReceipt = onViewReceipt,
                    onEdit = { onEditTransaction(item.transaction) },
                    onDelete = { pendingDeleteTransaction = item.transaction },
                    onCardClick = { onOpenCustomerDetail(item.transaction.customerId) }
                )
            }
        }
    }

    pendingDeleteTransaction?.let { txToDelete ->
        val txTypeName = if (txToDelete.type == "DEBIT") {
            if (strings.isBn) "বাকি / প্রদান" else "Debit"
        } else {
            if (strings.isBn) "জমা / পরিশোধ" else "Credit"
        }
        val txAmountStr = LocalizationProvider.formatAmount(txToDelete.amount, currency, strings.isBn)
        val deleteTxTitle = if (strings.isBn) "লেনদেন মুছে ফেলবেন?" else "Delete Transaction?"
        val deleteTxMsg = if (strings.isBn) {
            "$txTypeName ($txAmountStr)-এর এই এন্ট্রিটি স্থায়ীভাবে মুছে ফেলতে চান? এটি অ্যাকাউন্টের মোট ব্যালেন্সে প্রভাব ফেলবে।"
        } else {
            "Permanently delete this $txTypeName entry of $txAmountStr? This will adjust the account balance."
        }

        ConfirmActionDialog(
            title = deleteTxTitle,
            message = deleteTxMsg,
            confirmLabel = if (strings.isBn) "হ্যাঁ, মুছুন" else "Delete",
            cancelLabel = strings.cancelButton,
            accentColor = TallyRubyRed,
            useDarkGlass = useDarkGlass,
            onConfirm = {
                onDeleteTransaction(txToDelete.id)
                pendingDeleteTransaction = null
            },
            onDismiss = { pendingDeleteTransaction = null },
            confirmTestTag = "confirm_delete_history_transaction_button",
            cancelTestTag = "cancel_delete_history_transaction_button"
        )
    }
}

@Composable
fun TransactionItemGlassCard(
    transaction: TransactionEntity,
    customerName: String?,
    customerPhone: String? = null,
    showCustomerNameTitle: Boolean = true,
    balanceAfterTransaction: Double? = null,
    strings: AppStrings,
    currency: String,
    useDarkGlass: Boolean,
    onViewReceipt: (String) -> Unit,
    onEdit: (() -> Unit)? = null,
    onDelete: () -> Unit,
    onCardClick: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val isDebit = transaction.type == "DEBIT"
    val accent = if (isDebit) TallyRubyRed else TallyEmeraldGreen
    val typeBadgeText = if (isDebit) strings.debitTitle else strings.creditTitle

    LiquidGlassCard(
        useDarkGlass = useDarkGlass,
        accentTint = accent,
        cornerRadius = 20.dp,
        onClick = onCardClick,
        testTag = "transaction_card_${transaction.id}",
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(accent.copy(alpha = 0.22f))
                            .border(1.dp, accent, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isDebit) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                            contentDescription = null,
                            tint = accent,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column {
                        if (customerName != null && showCustomerNameTitle) {
                            Text(
                                text = customerName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = typeBadgeText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = accent,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = LocalizationProvider.formatTimestamp(
                                transaction.timestamp,
                                strings.isBn
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = LocalizationProvider.formatAmount(
                            transaction.amount,
                            currency,
                            strings.isBn
                        ),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = accent
                    )
                    Text(
                        text = LocalizationProvider.paymentMethodLabel(
                            transaction.paymentMethod,
                            strings.isBn
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TallyCyanRefraction
                    )
                }
            }

            // Running Balance after this transaction row
            if (balanceAfterTransaction != null) {
                val balColor = when {
                    balanceAfterTransaction > 0.001 -> TallyRubyRed
                    balanceAfterTransaction < -0.001 -> TallyEmeraldGreen
                    else -> TallyCyanRefraction
                }
                val balLabel = when {
                    balanceAfterTransaction > 0.001 -> strings.receivableBadge
                    balanceAfterTransaction < -0.001 -> strings.payableBadge
                    else -> strings.settledBadge
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (useDarkGlass) Color(0xFF16253B) else Color(0xFFF1F5F9))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = strings.balanceAfterTxLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = LocalizationProvider.formatAmount(
                                abs(balanceAfterTransaction),
                                currency,
                                strings.isBn
                            ),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = balColor
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(balColor.copy(alpha = 0.18f))
                                .border(1.dp, balColor.copy(alpha = 0.5f), RoundedCornerShape(50))
                                .padding(horizontal = 6.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = balLabel,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = balColor
                            )
                        }
                    }
                }
            }

            // Category, Note & Receipt Attachment Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    if (transaction.category.isNotBlank()) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (useDarkGlass) Color(0xFF334155) else Color(0xFFE2E8F0))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = transaction.category,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    if (!transaction.receiptUri.isNullOrBlank()) {
                        IconButton(
                            onClick = { onViewReceipt(transaction.receiptUri) },
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(TallyEmeraldGreen.copy(alpha = 0.2f))
                                .border(1.dp, TallyEmeraldGreen.copy(alpha = 0.6f), CircleShape)
                                .testTag("view_receipt_badge_${transaction.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Receipt,
                                contentDescription = strings.receiptAttachedBadge,
                                tint = TallyEmeraldGreen,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = {
                            shareSingleTransaction(
                                context = context,
                                transaction = transaction,
                                customerName = customerName,
                                customerPhone = customerPhone,
                                balanceAfterTransaction = balanceAfterTransaction,
                                currency = currency,
                                strings = strings
                            )
                        },
                        modifier = Modifier
                            .size(34.dp)
                            .testTag("share_transaction_button_${transaction.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = if (strings.isBn) "হোয়াটসঅ্যাপে শেয়ার করুন" else "Share on WhatsApp",
                            tint = Color(0xFF25D366),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    if (onEdit != null) {
                        IconButton(
                            onClick = onEdit,
                            modifier = Modifier
                                .size(34.dp)
                                .testTag("edit_transaction_button_${transaction.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = if (strings.isBn) "এডিট করুন" else "Edit",
                                tint = TallyCyanRefraction,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .size(34.dp)
                            .testTag("delete_transaction_button_${transaction.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = strings.deleteButton,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            if (transaction.note.isNotBlank()) {
                Text(
                    text = transaction.note,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.88f)
                )
            }
        }
    }
}
