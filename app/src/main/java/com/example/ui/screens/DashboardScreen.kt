package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.CustomerWithBalance
import com.example.ui.AppStrings
import com.example.ui.CustomerFilter
import com.example.ui.LedgerUiState
import com.example.ui.LocalizationProvider
import com.example.ui.components.LiquidFilterChip
import com.example.ui.components.LiquidGlassCard
import com.example.ui.components.SpringyLiquidButton
import com.example.ui.theme.TallyCyanRefraction
import com.example.ui.theme.TallyEmeraldGreen
import com.example.ui.theme.TallyGoldAccent
import com.example.ui.theme.TallyRubyRed
import java.io.File
import kotlin.math.abs

@Composable
fun DashboardScreen(
    uiState: LedgerUiState,
    strings: AppStrings,
    onSearchQueryChange: (String) -> Unit,
    onFilterChange: (CustomerFilter) -> Unit,
    onOpenCustomerDetail: (Long) -> Unit,
    onOpenAddCustomer: () -> Unit,
    onOpenQuickEntry: (String, Long?) -> Unit,
    onOpenSmsReminder: (CustomerWithBalance) -> Unit,
    onRetryOfflineSync: () -> Unit = {},
    onOpenWelcomeSetup: () -> Unit = {},
    onSearchingChanged: (Boolean) -> Unit = {},
    onScanContactPhotos: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val useDarkGlass = uiState.settings.useDarkGlassTheme
    val currency = uiState.settings.currencySymbol

    val contactsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            onScanContactPhotos()
        }
    }

    var isSearchFocused by remember { mutableStateOf(false) }
    val isSearching = isSearchFocused || uiState.searchQuery.isNotEmpty()

    LaunchedEffect(isSearching) {
        onSearchingChanged(isSearching)
    }

    BackHandler(enabled = isSearching) {
        onSearchQueryChange("")
        isSearchFocused = false
        focusManager.clearFocus()
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("dashboard_lazy_column"),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 12.dp,
            bottom = if (isSearching) 48.dp else 140.dp
        ),
        verticalArrangement = Arrangement.spacedBy(if (isSearching) 10.dp else 14.dp)
    ) {
        // 1. Hero Ledger Dashboard Card with Net Balance (Smoothly hides when searching)
        item(key = "hero_ledger_summary") {
            AnimatedVisibility(
                visible = !isSearching,
                enter = fadeIn(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) +
                    expandVertically(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)),
                exit = fadeOut(animationSpec = spring(stiffness = Spring.StiffnessMedium)) +
                    shrinkVertically(animationSpec = spring(stiffness = Spring.StiffnessMedium))
            ) {
                LiquidGlassCard(
                    useDarkGlass = useDarkGlass,
                    accentTint = TallyCyanRefraction,
                    cornerRadius = 24.dp,
                    testTag = "ledger_summary_hero_card",
                    modifier = Modifier.fillMaxWidth()
                ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Business Brand Banner Row
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
                            val effectiveOwnerPhoto = uiState.settings.effectiveProfilePhoto
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(if (effectiveOwnerPhoto.isNotBlank()) CircleShape else RoundedCornerShape(14.dp))
                                    .background(TallyCyanRefraction.copy(alpha = 0.18f))
                                    .border(
                                        1.2.dp,
                                        TallyCyanRefraction.copy(alpha = 0.6f),
                                        if (effectiveOwnerPhoto.isNotBlank()) CircleShape else RoundedCornerShape(14.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (effectiveOwnerPhoto.isNotBlank()) {
                                    AsyncImage(
                                        model = if (effectiveOwnerPhoto.startsWith("/")) File(effectiveOwnerPhoto) else effectiveOwnerPhoto,
                                        contentDescription = uiState.settings.businessName.ifBlank { strings.appTitle },
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(CircleShape)
                                    )
                                } else {
                                    Icon(
                                        imageVector = if (strings.isIndividual) Icons.Default.Person else Icons.Default.Storefront,
                                        contentDescription = null,
                                        tint = TallyCyanRefraction
                                    )
                                }
                            }
                            Column {
                                val isGoogleConnected = uiState.settings.syncedGoogleDocId.isNotBlank() ||
                                    uiState.settings.syncedGoogleSheetId.isNotBlank()
                                val isOfflineMode = uiState.isOfflineMode
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = uiState.settings.businessName.ifBlank { strings.appTitle },
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                    if (isOfflineMode) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(50))
                                                .background(TallyGoldAccent.copy(alpha = 0.18f))
                                                .border(
                                                    1.dp,
                                                    TallyGoldAccent.copy(alpha = 0.55f),
                                                    RoundedCornerShape(50)
                                                )
                                                .clickable { onRetryOfflineSync() }
                                                .padding(horizontal = 7.dp, vertical = 3.dp)
                                                .testTag("home_offline_mode_badge")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.CloudOff,
                                                contentDescription = null,
                                                tint = TallyGoldAccent,
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Text(
                                                text = if (strings.isBn) "অফলাইন মোড" else "Offline Mode",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = TallyGoldAccent,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    } else if (isGoogleConnected) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(50))
                                                .background(TallyEmeraldGreen.copy(alpha = 0.14f))
                                                .border(
                                                    1.dp,
                                                    TallyEmeraldGreen.copy(alpha = 0.45f),
                                                    RoundedCornerShape(50)
                                                )
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                                .testTag("home_google_connected_badge")
                                        ) {
                                            Text(
                                                text = "✅",
                                                style = MaterialTheme.typography.labelSmall
                                            )
                                            GoogleGLogoIcon(modifier = Modifier.size(14.dp))
                                        }
                                    }
                                }
                                Text(
                                    text = strings.appSubtitle,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Customer count badge
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(TallyCyanRefraction.copy(alpha = 0.15f))
                                .border(1.dp, TallyCyanRefraction.copy(alpha = 0.45f), RoundedCornerShape(50))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            val cText = if (strings.isBn) {
                                val countStr = LocalizationProvider.toBengaliDigits(uiState.summary.customerCount.toString())
                                if (strings.isIndividual) "$countStr হিসাব" else "$countStr পার্টি"
                            } else {
                                if (strings.isIndividual) "${uiState.summary.customerCount} Accounts" else "${uiState.summary.customerCount} Parties"
                            }
                            Text(
                                text = cText,
                                style = MaterialTheme.typography.labelLarge,
                                color = TallyCyanRefraction
                            )
                        }
                    }

                    // Compact Offline Mode / Pending Cloud Sync Banner when offline or sync failed
                    AnimatedVisibility(visible = uiState.isOfflineMode) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(TallyGoldAccent.copy(alpha = 0.14f))
                                .border(1.dp, TallyGoldAccent.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
                                .clickable { onRetryOfflineSync() }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                                .testTag("home_offline_sync_banner"),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloudOff,
                                    contentDescription = null,
                                    tint = TallyGoldAccent,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = if (strings.isBn) {
                                        if (uiState.settings.hasPendingCloudSync) {
                                            "অফলাইন মোড: লেনদেন ফোনে সেভ আছে, অনলাইনে এলে অটো-সিঙ্ক হবে"
                                        } else {
                                            "অফলাইন মোড চালু আছে (ক্লাউড সিঙ্ক বন্ধ)"
                                        }
                                    } else {
                                        if (uiState.settings.hasPendingCloudSync) {
                                            "Offline Mode: Entries saved locally & will auto-sync when online"
                                        } else {
                                            "Offline Mode active (Cloud sync unavailable)"
                                        }
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            if (uiState.settings.syncedGoogleDocId.isNotBlank() ||
                                uiState.settings.syncedGoogleSheetId.isNotBlank()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Sync,
                                    contentDescription = if (strings.isBn) "পুনরায় সিঙ্ক" else "Retry Sync",
                                    tint = TallyGoldAccent,
                                    modifier = Modifier
                                        .padding(start = 8.dp)
                                        .size(18.dp)
                                )
                            }
                        }
                    }

                    // Two High-Contrast Overview Cards: মোট পাবেন (Receivable) & মোট দেবেন (Payable)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // মোট পাবেন (Total Receivable)
                        LiquidGlassCard(
                            useDarkGlass = useDarkGlass,
                            accentTint = TallyRubyRed,
                            cornerRadius = 20.dp,
                            onClick = { onFilterChange(CustomerFilter.RECEIVABLE) },
                            testTag = "card_total_receivable",
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(TallyRubyRed.copy(alpha = 0.25f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.TrendingUp,
                                            contentDescription = null,
                                            tint = TallyRubyRed,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    Text(
                                        text = strings.totalReceivable,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    text = LocalizationProvider.formatAmount(
                                        uiState.summary.totalReceivable,
                                        currency,
                                        strings.isBn
                                    ),
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = TallyRubyRed
                                )
                            }
                        }

                        // মোট দেবেন (Total Payable)
                        LiquidGlassCard(
                            useDarkGlass = useDarkGlass,
                            accentTint = TallyEmeraldGreen,
                            cornerRadius = 20.dp,
                            onClick = { onFilterChange(CustomerFilter.PAYABLE) },
                            testTag = "card_total_payable",
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(TallyEmeraldGreen.copy(alpha = 0.25f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.TrendingDown,
                                            contentDescription = null,
                                            tint = TallyEmeraldGreen,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    Text(
                                        text = strings.totalPayable,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    text = LocalizationProvider.formatAmount(
                                        uiState.summary.totalPayable,
                                        currency,
                                        strings.isBn
                                    ),
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = TallyEmeraldGreen
                                )
                            }
                        }
                    }

                    // মোট ব্যালেন্স (Net Balance) Bar
                    LiquidGlassCard(
                        useDarkGlass = useDarkGlass,
                        accentTint = TallyGoldAccent,
                        cornerRadius = 18.dp,
                        testTag = "card_net_balance",
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AccountBalanceWallet,
                                    contentDescription = null,
                                    tint = TallyGoldAccent
                                )
                                Column {
                                    Text(
                                        text = strings.netBalance,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    val netStatusText = when {
                                        uiState.summary.netBalance > 0.001 ->
                                            if (strings.isBn) "নিট পাওনা (Receivable)" else "Net Receivable"
                                        uiState.summary.netBalance < -0.001 ->
                                            if (strings.isBn) "নিট দেনা (Payable)" else "Net Payable"
                                        else -> strings.settledBadge
                                    }
                                    Text(
                                        text = netStatusText,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            val netSign = when {
                                uiState.summary.netBalance > 0.001 -> "+"
                                uiState.summary.netBalance < -0.001 -> "-"
                                else -> ""
                            }
                            Text(
                                text = "$netSign${
                                    LocalizationProvider.formatAmount(
                                        uiState.summary.netBalance,
                                        currency,
                                        strings.isBn
                                    )
                                }",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (uiState.summary.netBalance >= 0) TallyGoldAccent else TallyRubyRed
                            )
                        }
                    }

                    // Quick Action Row: টাকা পাব / দিয়েছি (Debit) & টাকা দেব / পেয়েছি (Credit)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        SpringyLiquidButton(
                            onClick = { onOpenQuickEntry("DEBIT", null) },
                            accentColor = TallyRubyRed,
                            secondaryColor = Color(0xFFB71C1C),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 12.dp),
                            testTag = "quick_debit_entry_button"
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
                            onClick = { onOpenQuickEntry("CREDIT", null) },
                            accentColor = TallyEmeraldGreen,
                            secondaryColor = Color(0xFF007E33),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 12.dp),
                            testTag = "quick_credit_entry_button"
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
        }

        // 2. Search Bar & Filter Chips (Filter chips smoothly hide when searching)
        item(key = "search_and_filters") {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = { Text(strings.searchPlaceholder) },
                    leadingIcon = {
                        if (isSearching) {
                            IconButton(
                                onClick = {
                                    onSearchQueryChange("")
                                    isSearchFocused = false
                                    focusManager.clearFocus()
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = strings.cancelButton,
                                    tint = TallyCyanRefraction
                                )
                            }
                        } else {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = TallyCyanRefraction
                            )
                        }
                    },
                    trailingIcon = {
                        if (uiState.searchQuery.isNotEmpty() || isSearchFocused) {
                            IconButton(
                                onClick = {
                                    onSearchQueryChange("")
                                    isSearchFocused = false
                                    focusManager.clearFocus()
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = strings.cancelButton
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { focusState ->
                            isSearchFocused = focusState.isFocused
                        }
                        .testTag("customer_search_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TallyCyanRefraction,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )

                AnimatedVisibility(
                    visible = !isSearching,
                    enter = fadeIn(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) +
                        expandVertically(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)),
                    exit = fadeOut(animationSpec = spring(stiffness = Spring.StiffnessMedium)) +
                        shrinkVertically(animationSpec = spring(stiffness = Spring.StiffnessMedium))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LiquidFilterChip(
                            label = strings.filterAll,
                            selected = uiState.activeFilter == CustomerFilter.ALL,
                            onClick = { onFilterChange(CustomerFilter.ALL) },
                            useDarkGlass = useDarkGlass,
                            accentColor = TallyCyanRefraction,
                            testTag = "filter_chip_all"
                        )
                        LiquidFilterChip(
                            label = strings.filterReceivable,
                            selected = uiState.activeFilter == CustomerFilter.RECEIVABLE,
                            onClick = { onFilterChange(CustomerFilter.RECEIVABLE) },
                            useDarkGlass = useDarkGlass,
                            accentColor = TallyRubyRed,
                            testTag = "filter_chip_receivable"
                        )
                        LiquidFilterChip(
                            label = strings.filterPayable,
                            selected = uiState.activeFilter == CustomerFilter.PAYABLE,
                            onClick = { onFilterChange(CustomerFilter.PAYABLE) },
                            useDarkGlass = useDarkGlass,
                            accentColor = TallyEmeraldGreen,
                            testTag = "filter_chip_payable"
                        )
                        LiquidFilterChip(
                            label = strings.filterSupplier,
                            selected = uiState.activeFilter == CustomerFilter.SUPPLIER,
                            onClick = { onFilterChange(CustomerFilter.SUPPLIER) },
                            useDarkGlass = useDarkGlass,
                            accentColor = TallyGoldAccent,
                            testTag = "filter_chip_supplier"
                        )
                        if (uiState.customersWithBalance.any { it.customer.phone.isNotBlank() && it.customer.photoUri.isNullOrBlank() }) {
                            LiquidFilterChip(
                                label = if (strings.isBn) "📇 কন্টাক্ট ছবি স্ক্যান" else "📇 Scan Contact Photos",
                                selected = false,
                                onClick = {
                                    val hasPerm = ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.READ_CONTACTS
                                    ) == PackageManager.PERMISSION_GRANTED
                                    if (hasPerm) {
                                        onScanContactPhotos()
                                    } else {
                                        contactsPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
                                    }
                                },
                                useDarkGlass = useDarkGlass,
                                accentColor = TallyCyanRefraction,
                                testTag = "dashboard_scan_contact_photos_chip"
                            )
                        }
                    }
                }
            }
        }

        // 3. Empty State or Customer Ledger Cards
        if (uiState.filteredCustomers.isEmpty()) {
            item(key = "empty_customers") {
                LiquidGlassCard(
                    useDarkGlass = useDarkGlass,
                    accentTint = TallyCyanRefraction,
                    cornerRadius = 24.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PersonAdd,
                            contentDescription = null,
                            tint = TallyCyanRefraction,
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = strings.noCustomersEmptyTitle,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = strings.noCustomersEmptyDesc,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SpringyLiquidButton(
                                onClick = onOpenAddCustomer,
                                accentColor = TallyCyanRefraction,
                                secondaryColor = Color(0xFF0077B6),
                                testTag = "empty_state_add_customer_button"
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PersonAdd,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(strings.addCustomer, fontWeight = FontWeight.Bold)
                            }

                            if (uiState.settings.businessName.isBlank()) {
                                SpringyLiquidButton(
                                    onClick = onOpenWelcomeSetup,
                                    accentColor = TallyEmeraldGreen,
                                    secondaryColor = Color(0xFF007E33),
                                    testTag = "empty_state_open_welcome_setup_button"
                                ) {
                                    Icon(
                                        imageVector = if (strings.isIndividual) Icons.Default.Person else Icons.Default.Storefront,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (strings.isIndividual) {
                                            if (strings.isBn) "প্রোফাইল সেটআপ" else "Setup Profile"
                                        } else {
                                            if (strings.isBn) "ব্যবসা সেটআপ" else "Setup Shop"
                                        },
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else {
            items(
                items = uiState.filteredCustomers,
                key = { it.customer.id }
            ) { item ->
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn() + scaleIn(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        )
                    )
                ) {
                    CustomerLedgerRowCard(
                        item = item,
                        strings = strings,
                        currency = currency,
                        useDarkGlass = useDarkGlass,
                        isSearching = isSearching,
                        onCardClick = { onOpenCustomerDetail(item.customer.id) },
                        onDirectCall = {
                            if (item.customer.phone.isNotBlank()) {
                                val dialIntent = Intent(Intent.ACTION_DIAL).apply {
                                    data = Uri.parse("tel:${Uri.encode(item.customer.phone)}")
                                }
                                context.startActivity(dialIntent)
                            }
                        },
                        onSmsReminder = { onOpenSmsReminder(item) },
                        onQuickDebit = { onOpenQuickEntry("DEBIT", item.customer.id) },
                        onQuickCredit = { onOpenQuickEntry("CREDIT", item.customer.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun CustomerLedgerRowCard(
    item: CustomerWithBalance,
    strings: AppStrings,
    currency: String,
    useDarkGlass: Boolean,
    isSearching: Boolean = false,
    onCardClick: () -> Unit,
    onDirectCall: () -> Unit,
    onSmsReminder: () -> Unit,
    onQuickDebit: () -> Unit,
    onQuickCredit: () -> Unit
) {
    val net = item.netBalance
    val badgeColor = when {
        net > 0.001 -> TallyRubyRed
        net < -0.001 -> TallyEmeraldGreen
        else -> TallyCyanRefraction
    }
    val badgeLabel = when {
        net > 0.001 -> strings.receivableBadge
        net < -0.001 -> strings.payableBadge
        else -> strings.settledBadge
    }

    LiquidGlassCard(
        useDarkGlass = useDarkGlass,
        accentTint = badgeColor,
        cornerRadius = 22.dp,
        onClick = onCardClick,
        testTag = "customer_card_${item.customer.id}",
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    // Customer Photo or Initials Frosted Avatar
                    val avatarColor = Color(item.customer.avatarColorHex)
                    val customerPhoto = item.customer.photoUri?.trim().orEmpty()
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(avatarColor.copy(alpha = 0.24f))
                            .border(1.5.dp, avatarColor, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (customerPhoto.isNotBlank()) {
                            AsyncImage(
                                model = if (customerPhoto.startsWith("/")) File(customerPhoto) else customerPhoto,
                                contentDescription = item.customer.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                            )
                        } else {
                            Text(
                                text = item.customer.name.trim().take(1).uppercase(),
                                style = MaterialTheme.typography.titleLarge,
                                color = avatarColor,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = item.customer.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (item.customer.partyType == "SUPPLIER") {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(TallyGoldAccent.copy(alpha = 0.22f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = strings.partyTypeSupplier,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TallyGoldAccent
                                    )
                                }
                            }
                        }
                        Text(
                            text = item.customer.phone.ifBlank {
                                if (strings.isBn) "নম্বর যুক্ত নেই" else "No phone"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Balance Pill
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = LocalizationProvider.formatAmount(abs(net), currency, strings.isBn),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = badgeColor
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(badgeColor.copy(alpha = 0.2f))
                            .border(1.dp, badgeColor.copy(alpha = 0.6f), RoundedCornerShape(50))
                            .padding(horizontal = 10.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = badgeLabel,
                            style = MaterialTheme.typography.bodyMedium,
                            color = badgeColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Quick Actions Row inside Customer Card (Smoothly hides during search for a compact result list)
            AnimatedVisibility(
                visible = !isSearching,
                enter = fadeIn(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) +
                    expandVertically(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)),
                exit = fadeOut(animationSpec = spring(stiffness = Spring.StiffnessMedium)) +
                    shrinkVertically(animationSpec = spring(stiffness = Spring.StiffnessMedium))
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        IconButton(
                            onClick = onDirectCall,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(TallyEmeraldGreen.copy(alpha = 0.18f))
                                .testTag("call_customer_button_${item.customer.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Call,
                                contentDescription = strings.callDirect,
                                tint = TallyEmeraldGreen,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        IconButton(
                            onClick = onSmsReminder,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(TallyGoldAccent.copy(alpha = 0.18f))
                                .testTag("sms_customer_button_${item.customer.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Sms,
                                contentDescription = strings.smsReminder,
                                tint = TallyGoldAccent,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(
                            onClick = onQuickDebit,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(TallyRubyRed.copy(alpha = 0.18f))
                                .border(1.dp, TallyRubyRed.copy(alpha = 0.5f), CircleShape)
                                .testTag("customer_quick_debit_${item.customer.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowUpward,
                                contentDescription = strings.debitShort,
                                tint = TallyRubyRed,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        IconButton(
                            onClick = onQuickCredit,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(TallyEmeraldGreen.copy(alpha = 0.18f))
                                .border(1.dp, TallyEmeraldGreen.copy(alpha = 0.5f), CircleShape)
                                .testTag("customer_quick_credit_${item.customer.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowDownward,
                                contentDescription = strings.creditShort,
                                tint = TallyEmeraldGreen,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GoogleGLogoIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val stroke = size.minDimension * 0.22f
        val diameter = size.minDimension - stroke
        val topLeft = Offset(stroke / 2f, stroke / 2f)
        val arcSize = Size(diameter, diameter)
        val arcStroke = Stroke(width = stroke, cap = StrokeCap.Butt)

        // Red top-left arc
        drawArc(
            color = Color(0xFFEA4335),
            startAngle = -145f,
            sweepAngle = 100f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = arcStroke
        )
        // Yellow left-bottom arc
        drawArc(
            color = Color(0xFFFBBC05),
            startAngle = 145f,
            sweepAngle = 70f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = arcStroke
        )
        // Green bottom-right arc
        drawArc(
            color = Color(0xFF34A853),
            startAngle = 45f,
            sweepAngle = 100f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = arcStroke
        )
        // Blue right arc
        drawArc(
            color = Color(0xFF4285F4),
            startAngle = -12f,
            sweepAngle = 57f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = arcStroke
        )
        // Blue horizontal crossbar of the 'G'
        drawLine(
            color = Color(0xFF4285F4),
            start = Offset(size.width * 0.52f, size.height * 0.5f),
            end = Offset(size.width - stroke * 0.2f, size.height * 0.5f),
            strokeWidth = stroke,
            cap = StrokeCap.Butt
        )
    }
}

