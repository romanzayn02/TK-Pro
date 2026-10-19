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
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.CustomerWithBalance
import com.example.data.TransactionEntity
import com.example.data.TransactionWithCustomer
import com.example.ui.AppStrings
import com.example.ui.LocalizationProvider
import com.example.ui.components.LiquidGlassCard
import com.example.ui.components.SpringyLiquidButton
import com.example.ui.theme.TallyCyanRefraction
import com.example.ui.theme.TallyEmeraldGreen
import com.example.ui.theme.TallyGoldAccent
import com.example.ui.theme.TallyRubyRed
import kotlin.math.abs

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
                                Column {
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

                            IconButton(
                                onClick = onDeleteCustomer,
                                modifier = Modifier.testTag("delete_customer_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = strings.deleteButton,
                                    tint = TallyRubyRed
                                )
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

                        // Quick Communications: Direct Dial (`tel:`) and Bengali SMS Reminder
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
                                    contentDescription = null,
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
                                    contentDescription = null,
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
                        customerName = null,
                        balanceAfterTransaction = balanceAfter,
                        strings = strings,
                        currency = currency,
                        useDarkGlass = useDarkGlass,
                        onViewReceipt = onViewReceipt,
                        onEdit = { onEditTransaction(tx) },
                        onDelete = { onDeleteTransaction(tx.id) }
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
            bottom = 110.dp
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
                    balanceAfterTransaction = balanceAfter,
                    strings = strings,
                    currency = currency,
                    useDarkGlass = useDarkGlass,
                    onViewReceipt = onViewReceipt,
                    onEdit = { onEditTransaction(item.transaction) },
                    onDelete = { onDeleteTransaction(item.transaction.id) },
                    onCardClick = { onOpenCustomerDetail(item.transaction.customerId) }
                )
            }
        }
    }
}

@Composable
fun TransactionItemGlassCard(
    transaction: TransactionEntity,
    customerName: String?,
    balanceAfterTransaction: Double? = null,
    strings: AppStrings,
    currency: String,
    useDarkGlass: Boolean,
    onViewReceipt: (String) -> Unit,
    onEdit: (() -> Unit)? = null,
    onDelete: () -> Unit,
    onCardClick: (() -> Unit)? = null
) {
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
                        if (customerName != null) {
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

                    if (!transaction.receiptUri.isNullOrBlank()) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(TallyEmeraldGreen.copy(alpha = 0.22f))
                                .border(1.dp, TallyEmeraldGreen, RoundedCornerShape(10.dp))
                                .clickable { onViewReceipt(transaction.receiptUri) }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                .testTag("view_receipt_badge_${transaction.id}"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Receipt,
                                contentDescription = strings.receiptAttachedBadge,
                                tint = TallyEmeraldGreen,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = strings.receiptAttachedBadge,
                                style = MaterialTheme.typography.bodyMedium,
                                color = TallyEmeraldGreen,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
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
