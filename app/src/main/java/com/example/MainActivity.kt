package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.CustomerWithBalance
import com.example.ui.LedgerViewModel
import com.example.ui.LocalizationProvider
import com.example.ui.MainTab
import com.example.ui.components.AddCustomerBottomSheet
import com.example.ui.components.AddTransactionBottomSheet
import com.example.ui.components.LiquidFilterChip
import com.example.ui.components.LiquidGlassBackdrop
import com.example.ui.components.LiquidGlassCard
import com.example.ui.components.ReceiptViewerDialog
import com.example.ui.components.SmsReminderBottomSheet
import com.example.ui.components.SpringyLiquidButton
import com.example.ui.screens.CustomerDetailScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.TransactionHistoryScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.TallyCyanRefraction
import com.example.ui.theme.TallyEmeraldGreen
import com.example.ui.theme.TallyGoldAccent
import com.example.ui.theme.TallyRubyRed
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val ledgerViewModel: LedgerViewModel = viewModel()
            val uiState by ledgerViewModel.uiState.collectAsStateWithLifecycle()

            MyApplicationTheme(darkTheme = uiState.settings.useDarkGlassTheme) {
                TallyKhataProApp(viewModel = ledgerViewModel)
            }
        }
    }
}

@Composable
fun TallyKhataProApp(
    viewModel: LedgerViewModel
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val strings = LocalizationProvider.getStrings(uiState.settings.language)
    val useDarkGlass = uiState.settings.useDarkGlassTheme

    var showAddCustomerSheet by remember { mutableStateOf(false) }
    var activeTxSheetConfig by remember { mutableStateOf<Pair<String, Long?>?>(null) }
    var editingTransaction by remember { mutableStateOf<com.example.data.TransactionEntity?>(null) }
    var activeSmsCustomer by remember { mutableStateOf<CustomerWithBalance?>(null) }
    var viewingReceiptPath by remember { mutableStateOf<String?>(null) }
    var isDashboardSearching by remember { mutableStateOf(false) }

    val hideBarsForSearch = isDashboardSearching &&
        uiState.currentTab == MainTab.DASHBOARD &&
        uiState.selectedCustomerDetail == null

    // Auto-dismiss status banner after 3.5 seconds
    LaunchedEffect(uiState.statusBannerMessage) {
        if (uiState.statusBannerMessage != null) {
            delay(3500)
            viewModel.clearStatusBanner()
        }
    }

    LiquidGlassBackdrop(useDarkGlass = useDarkGlass) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            contentColor = MaterialTheme.colorScheme.onBackground,
            contentWindowInsets = WindowInsets.safeDrawing,
            topBar = {
                AnimatedVisibility(
                    visible = !hideBarsForSearch,
                    enter = fadeIn(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) +
                        expandVertically(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)),
                    exit = fadeOut(animationSpec = spring(stiffness = Spring.StiffnessMedium)) +
                        shrinkVertically(animationSpec = spring(stiffness = Spring.StiffnessMedium))
                ) {
                    TopLiquidAppBar(
                        appTitle = strings.appTitle,
                        onAddCustomerClick = { showAddCustomerSheet = true },
                        addCustomerLabel = strings.addCustomer
                    )
                }
            },
            bottomBar = {
                AnimatedVisibility(
                    visible = uiState.selectedCustomerDetail == null && !hideBarsForSearch,
                    enter = fadeIn(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) +
                        expandVertically(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)),
                    exit = fadeOut(animationSpec = spring(stiffness = Spring.StiffnessMedium)) +
                        shrinkVertically(animationSpec = spring(stiffness = Spring.StiffnessMedium))
                ) {
                    BottomLiquidNavigationBar(
                        currentTab = uiState.currentTab,
                        strings = strings,
                        useDarkGlass = useDarkGlass,
                        onSelectTab = { viewModel.selectTab(it) },
                        onAddCustomerClick = { showAddCustomerSheet = true }
                    )
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                AnimatedContent(
                    targetState = Pair(uiState.selectedCustomerDetail, uiState.currentTab),
                    transitionSpec = {
                        (fadeIn(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) +
                            scaleIn(
                                initialScale = 0.96f,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessMediumLow
                                )
                            )).togetherWith(
                            fadeOut(animationSpec = spring(stiffness = Spring.StiffnessMedium)) +
                                scaleOut(targetScale = 0.96f)
                        )
                    },
                    label = "screen_transition"
                ) { (selectedDetail, currentTab) ->
                    if (selectedDetail != null) {
                        CustomerDetailScreen(
                            customerDetail = selectedDetail,
                            transactions = uiState.selectedCustomerTransactions,
                            strings = strings,
                            currency = uiState.settings.currencySymbol,
                            useDarkGlass = useDarkGlass,
                            onBack = { viewModel.closeCustomerDetail() },
                            onAddDebit = {
                                activeTxSheetConfig = "DEBIT" to selectedDetail.customer.id
                            },
                            onAddCredit = {
                                activeTxSheetConfig = "CREDIT" to selectedDetail.customer.id
                            },
                            onOpenSmsReminder = {
                                activeSmsCustomer = selectedDetail
                            },
                            onViewReceipt = { path ->
                                viewingReceiptPath = path
                            },
                            onEditTransaction = { tx ->
                                editingTransaction = tx
                            },
                            onDeleteTransaction = { txId ->
                                viewModel.deleteTransaction(txId)
                            },
                            onDeleteCustomer = {
                                viewModel.deleteCustomer(selectedDetail.customer.id)
                            }
                        )
                    } else {
                        when (currentTab) {
                            MainTab.DASHBOARD -> DashboardScreen(
                                uiState = uiState,
                                strings = strings,
                                onSearchQueryChange = { viewModel.setSearchQuery(it) },
                                onFilterChange = { viewModel.setCustomerFilter(it) },
                                onOpenCustomerDetail = { viewModel.openCustomerDetail(it) },
                                onOpenAddCustomer = { showAddCustomerSheet = true },
                                onOpenQuickEntry = { type, customerId ->
                                    if (uiState.customersWithBalance.isEmpty()) {
                                        showAddCustomerSheet = true
                                    } else {
                                        activeTxSheetConfig = type to customerId
                                    }
                                },
                                onOpenSmsReminder = { customerWithBalance ->
                                    activeSmsCustomer = customerWithBalance
                                },
                                onRetryOfflineSync = {
                                    viewModel.retryOfflineCloudSync()
                                },
                                onSearchingChanged = { searching ->
                                    isDashboardSearching = searching
                                }
                            )

                            MainTab.HISTORY -> TransactionHistoryScreen(
                                transactionsWithCustomer = uiState.allTransactionsWithCustomer,
                                strings = strings,
                                currency = uiState.settings.currencySymbol,
                                useDarkGlass = useDarkGlass,
                                onViewReceipt = { path -> viewingReceiptPath = path },
                                onEditTransaction = { tx -> editingTransaction = tx },
                                onDeleteTransaction = { txId -> viewModel.deleteTransaction(txId) },
                                onOpenCustomerDetail = { cId -> viewModel.openCustomerDetail(cId) }
                            )

                            MainTab.SETTINGS -> SettingsScreen(
                                settings = uiState.settings,
                                strings = strings,
                                viewModel = viewModel
                            )
                        }
                    }
                }

                // First-time launch setup prompt when no setup has been completed and database is empty
                if (uiState.settings.isSettingsLoaded && !uiState.settings.isSetupCompleted && uiState.customersWithBalance.isEmpty()) {
                    com.example.ui.components.FirstLaunchSetupDialog(
                        strings = strings,
                        useDarkGlass = useDarkGlass,
                        initialLanguage = uiState.settings.language,
                        initialCurrency = uiState.settings.currencySymbol,
                        onLanguageChange = { lang -> viewModel.setLanguage(lang) },
                        onCompleteSetup = { bName, oPhone, cSym, lang, firstCustName, firstCustPhone, openDebit, openCredit ->
                            viewModel.completeInitialSetup(
                                businessName = bName,
                                ownerPhone = oPhone,
                                currencySymbol = cSym,
                                language = lang,
                                firstCustomerName = firstCustName,
                                firstCustomerPhone = firstCustPhone,
                                firstCustomerOpeningDebit = openDebit,
                                firstCustomerOpeningCredit = openCredit
                            )
                        }
                    )
                }

                // Floating Status Toast Banner
                AnimatedVisibility(
                    visible = uiState.statusBannerMessage != null,
                    enter = fadeIn() + scaleIn(
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                    ),
                    exit = fadeOut() + scaleOut(),
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                ) {
                    uiState.statusBannerMessage?.let { msg ->
                        LiquidGlassCard(
                            useDarkGlass = true,
                            accentTint = TallyEmeraldGreen,
                            cornerRadius = 50.dp,
                            testTag = "status_banner_toast"
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = msg,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                                IconButton(
                                    onClick = { viewModel.clearStatusBanner() },
                                    modifier = Modifier.size(22.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = strings.closeButton,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddCustomerSheet) {
        AddCustomerBottomSheet(
            strings = strings,
            useDarkGlass = useDarkGlass,
            currencySymbol = uiState.settings.currencySymbol,
            onDismiss = { showAddCustomerSheet = false },
            onSaveCustomer = { name, phone, partyType, address, openingDebit, openingCredit ->
                viewModel.addCustomer(
                    name = name,
                    phone = phone,
                    partyType = partyType,
                    address = address,
                    openingDebit = openingDebit,
                    openingCredit = openingCredit
                ) {
                    showAddCustomerSheet = false
                }
            },
            onPermissionMessage = { msg ->
                viewModel.showStatusMessage(msg)
            }
        )
    }

    activeTxSheetConfig?.let { (txType, preselectedCustomerId) ->
        AddTransactionBottomSheet(
            strings = strings,
            useDarkGlass = useDarkGlass,
            currencySymbol = uiState.settings.currencySymbol,
            customers = uiState.customersWithBalance,
            preselectedCustomerId = preselectedCustomerId,
            initialType = txType,
            categories = uiState.settings.customCategories,
            viewModel = viewModel,
            onDismiss = { activeTxSheetConfig = null },
            onSaved = { activeTxSheetConfig = null }
        )
    }

    editingTransaction?.let { txToEdit ->
        AddTransactionBottomSheet(
            strings = strings,
            useDarkGlass = useDarkGlass,
            currencySymbol = uiState.settings.currencySymbol,
            customers = uiState.customersWithBalance,
            preselectedCustomerId = txToEdit.customerId,
            initialType = txToEdit.type,
            categories = uiState.settings.customCategories,
            viewModel = viewModel,
            editingTransaction = txToEdit,
            onDismiss = { editingTransaction = null },
            onSaved = { editingTransaction = null }
        )
    }

    activeSmsCustomer?.let { item ->
        val latestTx = uiState.allTransactionsWithCustomer
            .asSequence()
            .map { it.transaction }
            .filter { it.customerId == item.customer.id }
            .maxByOrNull { it.timestamp }
        SmsReminderBottomSheet(
            strings = strings,
            useDarkGlass = useDarkGlass,
            customerName = item.customer.name,
            customerPhone = item.customer.phone,
            balanceAmount = item.netBalance,
            currencySymbol = uiState.settings.currencySymbol,
            businessName = uiState.settings.businessName,
            ownerPhone = uiState.settings.ownerPhone,
            lastTransactionAmount = latestTx?.amount,
            lastTransactionType = latestTx?.type,
            onDismiss = { activeSmsCustomer = null },
            onCopiedNotice = { msg -> viewModel.showStatusMessage(msg) }
        )
    }

    viewingReceiptPath?.let { path ->
        ReceiptViewerDialog(
            receiptPath = path,
            strings = strings,
            onDismiss = { viewingReceiptPath = null }
        )
    }
}

@Composable
private fun TopLiquidAppBar(
    appTitle: String,
    onAddCustomerClick: () -> Unit,
    addCustomerLabel: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(TallyRubyRed.copy(alpha = 0.24f))
                    .border(1.2.dp, TallyRubyRed, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "৳",
                    style = MaterialTheme.typography.titleLarge,
                    color = TallyRubyRed,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = appTitle,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }

        IconButton(
            onClick = onAddCustomerClick,
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(TallyEmeraldGreen.copy(alpha = 0.22f))
                .border(1.dp, TallyEmeraldGreen, CircleShape)
                .testTag("top_bar_add_customer_button")
        ) {
            Icon(
                imageVector = Icons.Default.PersonAdd,
                contentDescription = addCustomerLabel,
                tint = TallyEmeraldGreen
            )
        }
    }
}

@Composable
private fun BottomLiquidNavigationBar(
    currentTab: MainTab,
    strings: com.example.ui.AppStrings,
    useDarkGlass: Boolean,
    onSelectTab: (MainTab) -> Unit,
    onAddCustomerClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LiquidGlassCard(
            useDarkGlass = useDarkGlass,
            accentTint = TallyCyanRefraction,
            cornerRadius = 28.dp,
            modifier = Modifier.weight(1f)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val tabs = listOf(
                    Triple(MainTab.DASHBOARD, strings.tabDashboard, Icons.Default.MenuBook),
                    Triple(MainTab.HISTORY, strings.tabHistory, Icons.Default.History),
                    Triple(MainTab.SETTINGS, strings.tabSettings, Icons.Default.Settings)
                )
                tabs.forEach { (tab, label, icon) ->
                    val isSelected = currentTab == tab
                    val pillColor = if (isSelected) TallyCyanRefraction else Color.Transparent
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) pillColor.copy(alpha = 0.24f) else Color.Transparent
                            )
                            .border(
                                width = if (isSelected) 1.dp else 0.dp,
                                color = if (isSelected) TallyCyanRefraction else Color.Transparent,
                                shape = CircleShape
                            )
                            .clickable { onSelectTab(tab) }
                            .testTag("nav_tab_${tab.name.lowercase()}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = label,
                            tint = if (isSelected) TallyCyanRefraction else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }

        // Bouncy Add Customer FAB
        SpringyLiquidButton(
            onClick = onAddCustomerClick,
            accentColor = TallyGoldAccent,
            secondaryColor = Color(0xFFFF6D00),
            cornerRadius = 24.dp,
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            testTag = "fab_add_customer"
        ) {
            Icon(
                imageVector = Icons.Default.PersonAdd,
                contentDescription = strings.addCustomer,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}
