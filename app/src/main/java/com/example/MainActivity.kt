package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.CustomerWithBalance
import com.example.ui.LedgerViewModel
import com.example.ui.LocalizationProvider
import com.example.ui.MainTab
import com.example.ui.components.AddCustomerBottomSheet
import com.example.ui.components.AddTransactionBottomSheet
import com.example.ui.components.LiquidGlassBackdrop
import com.example.ui.components.LiquidGlassCard
import com.example.ui.components.ReceiptViewerDialog
import com.example.ui.components.SmsReminderBottomSheet
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
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            ),
            navigationBarStyle = SystemBarStyle.light(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            )
        )
        setContent {
            val ledgerViewModel: LedgerViewModel = viewModel()
            val uiState by ledgerViewModel.uiState.collectAsStateWithLifecycle()
            val isDark = uiState.settings.useDarkGlassTheme

            MyApplicationTheme(darkTheme = isDark) {
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
    val strings = LocalizationProvider.getStrings(
        lang = uiState.settings.language,
        accountType = uiState.settings.accountType
    )
    val useDarkGlass = uiState.settings.useDarkGlassTheme

    var showAddCustomerSheet by remember { mutableStateOf(false) }
    var activeTxSheetConfig by remember { mutableStateOf<Pair<String, Long?>?>(null) }
    var editingTransaction by remember { mutableStateOf<com.example.data.TransactionEntity?>(null) }
    var activeSmsCustomer by remember { mutableStateOf<CustomerWithBalance?>(null) }
    var viewingReceiptPath by remember { mutableStateOf<String?>(null) }
    var isDashboardSearching by remember { mutableStateOf(false) }
    var manualWelcomeStartStep by remember { mutableStateOf<Int?>(null) }
    var isBiometricUnlocked by remember { mutableStateOf(false) }
    var showExitConfirmDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current

    val showWelcomeDialog = manualWelcomeStartStep != null ||
        (uiState.settings.isSettingsLoaded && !uiState.settings.isSetupCompleted && uiState.customersWithBalance.isEmpty())

    val showBiometricLock = uiState.settings.isSettingsLoaded &&
        uiState.settings.biometricLockEnabled &&
        !isBiometricUnlocked &&
        !showWelcomeDialog

    val hideBarsForSearch = isDashboardSearching &&
        uiState.currentTab == MainTab.DASHBOARD &&
        uiState.selectedCustomerDetail == null

    androidx.activity.compose.BackHandler(
        enabled = !showWelcomeDialog && !showBiometricLock && uiState.selectedCustomerDetail == null && uiState.currentTab != MainTab.DASHBOARD
    ) {
        viewModel.selectTab(MainTab.DASHBOARD)
    }

    // Intercept Back on Dashboard home screen to confirm exit
    androidx.activity.compose.BackHandler(
        enabled = !showWelcomeDialog && !showBiometricLock &&
            uiState.selectedCustomerDetail == null &&
            uiState.currentTab == MainTab.DASHBOARD &&
            !showAddCustomerSheet &&
            activeTxSheetConfig == null &&
            editingTransaction == null &&
            activeSmsCustomer == null &&
            viewingReceiptPath == null &&
            !isDashboardSearching
    ) {
        showExitConfirmDialog = true
    }

    // Auto-dismiss status banner after 3.5 seconds
    LaunchedEffect(uiState.statusBannerMessage) {
        if (uiState.statusBannerMessage != null) {
            delay(3500)
            viewModel.clearStatusBanner()
        }
    }

    LiquidGlassBackdrop(useDarkGlass = useDarkGlass) {
        if (showExitConfirmDialog) {
            val exitTitle = if (strings.isBn) "অ্যাপ থেকে বের হতে চান?" else "Exit TK Pro?"
            val exitMsg = if (strings.isBn) {
                "আপনি কি নিশ্চিতভাবে অ্যাপ থেকে বের হতে চান? আপনার সকল হিসাব ও লেনদেন নিরাপদে সেভ রয়েছে।"
            } else {
                "Are you sure you want to exit the app? All your ledger transactions are securely saved."
            }

            com.example.ui.components.ConfirmActionDialog(
                title = exitTitle,
                message = exitMsg,
                confirmLabel = if (strings.isBn) "বের হন" else "Exit",
                cancelLabel = if (strings.isBn) "থাক" else "Cancel",
                icon = Icons.AutoMirrored.Filled.ExitToApp,
                accentColor = TallyRubyRed,
                useDarkGlass = useDarkGlass,
                onConfirm = {
                    (context as? android.app.Activity)?.finish()
                },
                onDismiss = { showExitConfirmDialog = false },
                confirmTestTag = "confirm_exit_app_button",
                cancelTestTag = "cancel_exit_app_button"
            )
        }

        Box(modifier = Modifier.fillMaxSize()) {
            when {
                showBiometricLock -> {
                    com.example.ui.BiometricLockScreen(
                        isBn = strings.isBn,
                        isIndividual = strings.isIndividual,
                        useDarkGlass = useDarkGlass,
                        profileName = uiState.settings.businessName.ifBlank { strings.appTitle },
                        currencySymbol = uiState.settings.currencySymbol,
                        onUnlocked = { isBiometricUnlocked = true }
                    )
                }

                showWelcomeDialog -> {
                    com.example.ui.components.FirstLaunchSetupDialog(
                        strings = strings,
                        useDarkGlass = useDarkGlass,
                        initialLanguage = uiState.settings.language,
                        initialCurrency = uiState.settings.currencySymbol,
                        initialBusinessName = uiState.settings.businessName,
                        initialOwnerPhone = uiState.settings.ownerPhone,
                        initialAccountType = uiState.settings.accountType,
                        initialStep = manualWelcomeStartStep ?: 0,
                        onLanguageChange = { lang -> viewModel.setLanguage(lang) },
                        onThemeChange = { dark -> viewModel.toggleGlassThemeMode(dark) },
                        onAccountTypeChange = { accType -> viewModel.setAccountType(accType) },
                        onSkipSetup = {
                            manualWelcomeStartStep = null
                            if (!uiState.settings.isSetupCompleted) {
                                viewModel.completeInitialSetup(
                                    businessName = uiState.settings.businessName,
                                    ownerPhone = uiState.settings.ownerPhone,
                                    currencySymbol = uiState.settings.currencySymbol,
                                    language = uiState.settings.language,
                                    accountType = uiState.settings.accountType
                                )
                            }
                        },
                        onRestoreFromCloudShortcut = {
                            manualWelcomeStartStep = null
                            viewModel.requestCloudRestoreFromWelcome()
                            viewModel.selectTab(MainTab.SETTINGS)
                        },
                        onCompleteSetup = { bName, oPhone, cSym, lang, accType, firstCustName, firstCustPhone, firstCustPartyType, openDebit, openCredit ->
                            manualWelcomeStartStep = null
                            viewModel.completeInitialSetup(
                                businessName = bName,
                                ownerPhone = oPhone,
                                currencySymbol = cSym,
                                language = lang,
                                accountType = accType,
                                firstCustomerName = firstCustName,
                                firstCustomerPhone = firstCustPhone,
                                firstCustomerPartyType = firstCustPartyType,
                                firstCustomerOpeningDebit = openDebit,
                                firstCustomerOpeningCredit = openCredit
                            )
                        }
                    )
                }

                else -> {
                    Scaffold(
                        containerColor = Color.Transparent,
                        contentColor = MaterialTheme.colorScheme.onBackground,
                        contentWindowInsets = WindowInsets.statusBars,
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
                                    useDarkGlass = useDarkGlass,
                                    isSettingsSelected = uiState.selectedCustomerDetail == null && uiState.currentTab == MainTab.SETTINGS,
                                    onSettingsClick = {
                                        if (uiState.selectedCustomerDetail == null && uiState.currentTab == MainTab.SETTINGS) {
                                            viewModel.selectTab(MainTab.DASHBOARD)
                                        } else {
                                            viewModel.selectTab(MainTab.SETTINGS)
                                        }
                                    },
                                    settingsLabel = strings.tabSettings
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
                                            onOpenWelcomeSetup = {
                                                manualWelcomeStartStep = 1
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
                                            viewModel = viewModel,
                                            onLockAppNow = { isBiometricUnlocked = false }
                                        )
                                    }
                                }
                            }

                            // Floating Material 3 Expressive Bottom Navigation Bar (no background box behind it)
                            AnimatedVisibility(
                                visible = uiState.selectedCustomerDetail == null && !hideBarsForSearch,
                                enter = fadeIn(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) +
                                    expandVertically(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)),
                                exit = fadeOut(animationSpec = spring(stiffness = Spring.StiffnessMedium)) +
                                    shrinkVertically(animationSpec = spring(stiffness = Spring.StiffnessMedium)),
                                modifier = Modifier.align(Alignment.BottomCenter)
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
                    }
                }
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
                    .statusBarsPadding()
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
    useDarkGlass: Boolean,
    isSettingsSelected: Boolean = false,
    onSettingsClick: () -> Unit,
    settingsLabel: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.96f))
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
                    .background(TallyRubyRed.copy(alpha = 0.18f))
                    .border(1.2.dp, TallyRubyRed.copy(alpha = 0.7f), RoundedCornerShape(12.dp)),
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
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold
            )
        }

        val settingsBg = when {
            isSettingsSelected && useDarkGlass -> Color(0xFF004A77)
            isSettingsSelected && !useDarkGlass -> Color(0xFFD3E3FD)
            useDarkGlass -> Color(0xFF1E293B)
            else -> Color.White
        }
        val settingsIconTint = when {
            isSettingsSelected && useDarkGlass -> Color(0xFFD3E3FD)
            isSettingsSelected && !useDarkGlass -> Color(0xFF041E49)
            useDarkGlass -> Color(0xFFCBD5E1)
            else -> Color(0xFF475569)
        }

        IconButton(
            onClick = onSettingsClick,
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(settingsBg)
                .border(1.dp, if (useDarkGlass) Color(0xFF334155) else Color(0xFFE2E8F0), CircleShape)
                .testTag("top_bar_settings_button")
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = settingsLabel,
                tint = settingsIconTint
            )
        }
    }
}

private data class M3ExpressiveTabItem(
    val tab: MainTab,
    val label: String,
    val icon: ImageVector
)

@Composable
private fun BottomLiquidNavigationBar(
    currentTab: MainTab,
    strings: com.example.ui.AppStrings,
    useDarkGlass: Boolean,
    onSelectTab: (MainTab) -> Unit,
    onAddCustomerClick: () -> Unit
) {
    val tabs = remember(strings) {
        listOf(
            M3ExpressiveTabItem(
                tab = MainTab.DASHBOARD,
                label = strings.tabDashboard,
                icon = Icons.Filled.MenuBook
            ),
            M3ExpressiveTabItem(
                tab = MainTab.HISTORY,
                label = strings.tabHistory,
                icon = Icons.Filled.History
            )
        )
    }

    val dockContainerColor = if (useDarkGlass) Color(0xFF282A2F) else Color(0xFFEAF0F8)
    val dockBorderColor = if (useDarkGlass) {
        Color(0xFF3E424B)
    } else {
        Color(0xFFD5DFEC)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Material 3 Expressive Floating Navigation Capsule (Google Photos style)
        Surface(
            modifier = Modifier
                .weight(1f)
                .height(60.dp),
            shape = CircleShape,
            color = dockContainerColor,
            shadowElevation = if (useDarkGlass) 12.dp else 8.dp,
            tonalElevation = 0.dp,
            border = BorderStroke(0.8.dp, dockBorderColor)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(5.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                tabs.forEach { item ->
                    val isSelected = currentTab == item.tab

                    val activePillColor by animateColorAsState(
                        targetValue = when {
                            isSelected && useDarkGlass -> Color(0xFF004A77)
                            isSelected && !useDarkGlass -> Color(0xFFC2E7FF)
                            else -> Color.Transparent
                        },
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                        label = "m3_pill_bg"
                    )

                    val activeContentColor by animateColorAsState(
                        targetValue = when {
                            isSelected && useDarkGlass -> Color(0xFFC2E7FF)
                            isSelected && !useDarkGlass -> Color(0xFF001D35)
                            useDarkGlass -> Color(0xFFE2E2E6)
                            else -> Color(0xFF1F1F1F)
                        },
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                        label = "m3_pill_text"
                    )

                    Box(
                        modifier = Modifier
                            .weight(if (isSelected) 1.15f else 1f)
                            .fillMaxHeight()
                            .clip(CircleShape)
                            .background(activePillColor)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = ripple(bounded = true),
                                onClick = { onSelectTab(item.tab) }
                            )
                            .padding(horizontal = 6.dp)
                            .testTag("nav_tab_${item.tab.name.lowercase()}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = null,
                                    tint = activeContentColor,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                            }

                            Text(
                                text = item.label,
                                fontSize = 14.sp,
                                lineHeight = 18.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = activeContentColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        // Detached 60dp Circular Action Button (Google Photos right circular button style)
        val fabInteractionSource = remember { MutableInteractionSource() }
        val isFabPressed by fabInteractionSource.collectIsPressedAsState()
        val fabScale by animateFloatAsState(
            targetValue = if (isFabPressed) 0.92f else 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            ),
            label = "m3_fab_scale"
        )

        Surface(
            modifier = Modifier
                .size(60.dp)
                .scale(fabScale),
            shape = CircleShape,
            color = dockContainerColor,
            shadowElevation = if (useDarkGlass) 12.dp else 8.dp,
            tonalElevation = 0.dp,
            border = BorderStroke(0.8.dp, dockBorderColor)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .clickable(
                        interactionSource = fabInteractionSource,
                        indication = ripple(bounded = true),
                        onClick = onAddCustomerClick
                    )
                    .testTag("fab_add_customer"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.PersonAdd,
                    contentDescription = strings.addCustomer,
                    tint = if (useDarkGlass) Color(0xFFE2E2E6) else Color(0xFF1F1F1F),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}


