package com.example.ui

import android.app.Application
import android.content.Context
import android.content.IntentSender
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.AppSettings
import com.example.data.CustomerEntity
import com.example.data.CustomerWithBalance
import com.example.data.GoogleDocsAuthOutcome
import com.example.data.GoogleDocsSyncService
import com.example.data.LedgerRepository
import com.example.data.LedgerSummary
import com.example.data.TransactionEntity
import com.example.data.TransactionWithCustomer
import java.io.File
import java.io.FileOutputStream
import kotlin.math.abs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class MainTab {
    DASHBOARD,
    HISTORY,
    SETTINGS
}

enum class CustomerFilter {
    ALL,
    RECEIVABLE,
    PAYABLE,
    SUPPLIER
}

enum class GoogleDocsSyncAction {
    PUSH_TO_DOCS,
    PULL_FROM_DOCS,
    PUSH_TO_SHEETS,
    PULL_FROM_SHEETS
}

data class LedgerUiState(
    val settings: AppSettings = AppSettings(),
    val summary: LedgerSummary = LedgerSummary(),
    val customersWithBalance: List<CustomerWithBalance> = emptyList(),
    val filteredCustomers: List<CustomerWithBalance> = emptyList(),
    val allTransactionsWithCustomer: List<TransactionWithCustomer> = emptyList(),
    val selectedCustomerId: Long? = null,
    val selectedCustomerDetail: CustomerWithBalance? = null,
    val selectedCustomerTransactions: List<TransactionEntity> = emptyList(),
    val searchQuery: String = "",
    val activeFilter: CustomerFilter = CustomerFilter.ALL,
    val currentTab: MainTab = MainTab.DASHBOARD,
    val statusBannerMessage: String? = null,
    val isSyncingGoogleDocs: Boolean = false
)

class LedgerViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getInstance(application)
    private val repository = LedgerRepository(database.ledgerDao(), application)

    private val _searchQuery = MutableStateFlow("")
    private val _activeFilter = MutableStateFlow(CustomerFilter.ALL)
    private val _currentTab = MutableStateFlow(MainTab.DASHBOARD)
    private val _selectedCustomerId = MutableStateFlow<Long?>(null)
    private val _statusBannerMessage = MutableStateFlow<String?>(null)
    private val _isSyncingGoogleDocs = MutableStateFlow(false)
    val statusBannerMessage: StateFlow<String?> = _statusBannerMessage.asStateFlow()

    val uiState: StateFlow<LedgerUiState> = combine(
        repository.settingsFlow,
        repository.allCustomersFlow,
        repository.allTransactionsFlow,
        _searchQuery,
        combine(
            _activeFilter,
            _currentTab,
            _selectedCustomerId,
            _statusBannerMessage,
            _isSyncingGoogleDocs
        ) { f, t, s, msg, syncing ->
            Quintuple(f, t, s, msg, syncing)
        }
    ) { settings, customers, transactions, query, quint ->
        val (filter, tab, selectedId, bannerMsg, syncing) = quint

        val txByCustomer = transactions.groupBy { it.customerId }
        val customerMap = customers.associateBy { it.id }

        val customersWithBalance = customers.map { customer ->
            val cTxs = txByCustomer[customer.id].orEmpty()
            var debit = 0.0
            var credit = 0.0
            var lastTime: Long? = null
            for (tx in cTxs) {
                if (tx.type == "DEBIT") debit += tx.amount else credit += tx.amount
                if (lastTime == null || tx.timestamp > lastTime) {
                    lastTime = tx.timestamp
                }
            }
            CustomerWithBalance(
                customer = customer,
                totalDebit = debit,
                totalCredit = credit,
                lastTransactionTime = lastTime ?: customer.updatedAt,
                transactionCount = cTxs.size
            )
        }.sortedByDescending { it.lastTransactionTime ?: it.customer.updatedAt }

        var totalReceivable = 0.0
        var totalPayable = 0.0
        var totalDebitVolume = 0.0
        var totalCreditVolume = 0.0

        for (item in customersWithBalance) {
            totalDebitVolume += item.totalDebit
            totalCreditVolume += item.totalCredit
            val net = item.netBalance
            if (net > 0.001) {
                totalReceivable += net
            } else if (net < -0.001) {
                totalPayable += abs(net)
            }
        }

        val summary = LedgerSummary(
            totalReceivable = totalReceivable,
            totalPayable = totalPayable,
            netBalance = totalReceivable - totalPayable,
            totalDebitVolume = totalDebitVolume,
            totalCreditVolume = totalCreditVolume,
            customerCount = customers.size,
            transactionCount = transactions.size
        )

        val trimmedQuery = query.trim().lowercase()
        val filtered = customersWithBalance.filter { item ->
            val matchesQuery = if (trimmedQuery.isEmpty()) {
                true
            } else {
                item.customer.name.lowercase().contains(trimmedQuery) ||
                    item.customer.phone.lowercase().contains(trimmedQuery) ||
                    item.customer.address.lowercase().contains(trimmedQuery)
            }
            val matchesFilter = when (filter) {
                CustomerFilter.ALL -> true
                CustomerFilter.RECEIVABLE -> item.netBalance > 0.001
                CustomerFilter.PAYABLE -> item.netBalance < -0.001
                CustomerFilter.SUPPLIER -> item.customer.partyType == "SUPPLIER"
            }
            matchesQuery && matchesFilter
        }

        val txWithCustomer = transactions.map { tx ->
            val c = customerMap[tx.customerId]
            TransactionWithCustomer(
                transaction = tx,
                customerName = c?.name ?: "অজানা কাস্টমার",
                customerPhone = c?.phone ?: ""
            )
        }

        val selectedDetail = selectedId?.let { id ->
            customersWithBalance.find { it.customer.id == id }
        }
        val selectedTxs = selectedId?.let { id ->
            txByCustomer[id].orEmpty().sortedByDescending { it.timestamp }
        }.orEmpty()

        LedgerUiState(
            settings = settings,
            summary = summary,
            customersWithBalance = customersWithBalance,
            filteredCustomers = filtered,
            allTransactionsWithCustomer = txWithCustomer,
            selectedCustomerId = selectedId,
            selectedCustomerDetail = selectedDetail,
            selectedCustomerTransactions = selectedTxs,
            searchQuery = query,
            activeFilter = filter,
            currentTab = tab,
            statusBannerMessage = bannerMsg,
            isSyncingGoogleDocs = syncing
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = LedgerUiState()
    )

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setCustomerFilter(filter: CustomerFilter) {
        _activeFilter.value = filter
    }

    fun selectTab(tab: MainTab) {
        _currentTab.value = tab
    }

    fun openCustomerDetail(customerId: Long) {
        _selectedCustomerId.value = customerId
    }

    fun closeCustomerDetail() {
        _selectedCustomerId.value = null
    }

    fun clearStatusBanner() {
        _statusBannerMessage.value = null
    }

    fun showStatusMessage(message: String) {
        _statusBannerMessage.value = message
    }

    fun addCustomer(
        name: String,
        phone: String,
        partyType: String,
        address: String,
        openingDebit: Double = 0.0,
        openingCredit: Double = 0.0,
        onCreated: (Long) -> Unit = {}
    ) {
        if (name.isBlank()) return
        val palette = listOf(
            0xFF0284C7,
            0xFFE11D48,
            0xFF10B981,
            0xFFD97706,
            0xFF8B5CF6,
            0xFF2563EB
        )
        val color = palette[abs(name.hashCode()) % palette.size]
        viewModelScope.launch {
            val newId = repository.addCustomer(
                name = name,
                phone = phone,
                partyType = partyType,
                address = address,
                avatarColorHex = color
            )
            if (openingDebit > 0.0) {
                repository.addTransaction(
                    customerId = newId,
                    type = "DEBIT",
                    amount = openingDebit,
                    category = "বাকিতে বিক্রি",
                    paymentMethod = "CASH",
                    note = "প্রারম্ভিক জের (টাকা পাব / দিয়েছি)",
                    receiptUri = null
                )
            }
            if (openingCredit > 0.0) {
                repository.addTransaction(
                    customerId = newId,
                    type = "CREDIT",
                    amount = openingCredit,
                    category = "নগদ জমা",
                    paymentMethod = "CASH",
                    note = "প্রারম্ভিক জের (টাকা দেব / পেয়েছি)",
                    receiptUri = null
                )
            }
            _statusBannerMessage.value = "✅ ${name.trim()} খাতায় যুক্ত হয়েছে"
            onCreated(newId)
            triggerAutoSyncGoogleDocsIfConfigured()
        }
    }

    fun deleteCustomer(customerId: Long) {
        viewModelScope.launch {
            repository.deleteCustomer(customerId)
            if (_selectedCustomerId.value == customerId) {
                _selectedCustomerId.value = null
            }
            _statusBannerMessage.value = "কাস্টমার হিসাব মুছে ফেলা হয়েছে"
            triggerAutoSyncGoogleDocsIfConfigured()
        }
    }

    fun addTransaction(
        customerId: Long,
        type: String,
        amount: Double,
        category: String,
        paymentMethod: String,
        note: String,
        receiptUri: String?,
        timestamp: Long = System.currentTimeMillis()
    ) {
        if (amount <= 0.0) return
        viewModelScope.launch {
            repository.addTransaction(
                customerId = customerId,
                type = type,
                amount = amount,
                category = category,
                paymentMethod = paymentMethod,
                note = note,
                receiptUri = receiptUri,
                timestamp = timestamp
            )
            val label = if (type == "DEBIT") "টাকা পাব / দিয়েছি" else "টাকা দেব / পেয়েছি"
            _statusBannerMessage.value = "✅ $label এন্ট্রি সফলভাবে সেভ হয়েছে"
            triggerAutoSyncGoogleDocsIfConfigured()
        }
    }

    fun updateTransaction(
        transactionId: Long,
        customerId: Long,
        type: String,
        amount: Double,
        category: String,
        paymentMethod: String,
        note: String,
        receiptUri: String?,
        timestamp: Long
    ) {
        if (amount <= 0.0) return
        viewModelScope.launch {
            repository.updateTransaction(
                transactionId = transactionId,
                customerId = customerId,
                type = type,
                amount = amount,
                category = category,
                paymentMethod = paymentMethod,
                note = note,
                receiptUri = receiptUri,
                timestamp = timestamp
            )
            val isBn = uiState.value.settings.language != "EN"
            _statusBannerMessage.value = if (isBn) {
                "✅ লেনদেন সফলভাবে আপডেট হয়েছে"
            } else {
                "✅ Transaction updated successfully"
            }
            triggerAutoSyncGoogleDocsIfConfigured()
        }
    }

    fun switchGoogleAccountForDocsSync(
        context: Context,
        action: GoogleDocsSyncAction,
        customDocIdInput: String?,
        onLaunchSignInIntent: (android.content.Intent, GoogleDocsSyncAction, String?) -> Unit
    ) {
        viewModelScope.launch {
            val intent = GoogleDocsSyncService.signOutAndBuildSignInIntent(context)
            onLaunchSignInIntent(intent, action, customDocIdInput)
        }
    }

    fun deleteTransaction(transactionId: Long) {
        viewModelScope.launch {
            repository.deleteTransaction(transactionId)
            _statusBannerMessage.value = "লেনদেনটি মুছে ফেলা হয়েছে"
            triggerAutoSyncGoogleDocsIfConfigured()
        }
    }

    fun toggleLanguage() {
        val current = uiState.value.settings.language
        val next = if (current == "BN") "EN" else "BN"
        viewModelScope.launch {
            repository.updateLanguage(next)
        }
    }

    fun setLanguage(lang: String) {
        viewModelScope.launch {
            repository.updateLanguage(lang)
        }
    }

    fun toggleGlassThemeMode(useDarkGlass: Boolean) {
        viewModelScope.launch {
            repository.toggleThemeMode(useDarkGlass)
        }
    }

    fun updateBusinessSettings(businessName: String, ownerPhone: String, currencySymbol: String) {
        viewModelScope.launch {
            repository.updateBusinessSettings(businessName, ownerPhone, currencySymbol)
            _statusBannerMessage.value = "✅ ব্যবসার তথ্য ও কারেন্সি আপডেট করা হয়েছে"
            triggerAutoSyncGoogleDocsIfConfigured()
        }
    }

    fun completeInitialSetup(
        businessName: String,
        ownerPhone: String,
        currencySymbol: String,
        language: String,
        firstCustomerName: String = "",
        firstCustomerPhone: String = "",
        firstCustomerOpeningDebit: Double = 0.0,
        firstCustomerOpeningCredit: Double = 0.0
    ) {
        viewModelScope.launch {
            repository.completeInitialSetup(
                businessName = businessName,
                ownerPhone = ownerPhone,
                currencySymbol = currencySymbol,
                language = language
            )
            if (firstCustomerName.isNotBlank()) {
                addCustomer(
                    name = firstCustomerName.trim(),
                    phone = firstCustomerPhone.trim(),
                    partyType = "CUSTOMER",
                    address = "",
                    openingDebit = firstCustomerOpeningDebit,
                    openingCredit = firstCustomerOpeningCredit
                )
            }
        }
    }

    fun addCustomCategory(category: String) {
        viewModelScope.launch {
            repository.addCustomCategory(category, uiState.value.settings.customCategories)
        }
    }

    fun removeCustomCategory(category: String) {
        viewModelScope.launch {
            repository.removeCustomCategory(category, uiState.value.settings.customCategories)
        }
    }

    fun setAutoSyncGoogleDocs(enabled: Boolean) {
        viewModelScope.launch {
            repository.setAutoSyncGoogleDocs(enabled)
        }
    }

    fun updateCustomGoogleDocId(docIdOrUrl: String) {
        val cleanId = GoogleDocsSyncService.extractDocIdFromInput(docIdOrUrl)
        viewModelScope.launch {
            repository.updateGoogleDocsSyncInfo(cleanId, syncedAtMillis = 0L)
            val isBn = uiState.value.settings.language != "EN"
            _statusBannerMessage.value = if (isBn) {
                "✅ Google Doc লিংক যুক্ত হয়েছে"
            } else {
                "✅ Linked Google Doc ID saved"
            }
        }
    }

    /**
     * Initiates live Google Docs sync (Push or Pull). If the user has not yet authorized Google Docs
     * scopes on this device, calls [onNeedsConsent] or [onNeedsSignInIntent].
     */
    fun performGoogleDocsSync(
        context: Context,
        action: GoogleDocsSyncAction,
        customDocIdInput: String? = null,
        onNeedsConsent: (IntentSender, GoogleDocsSyncAction, String?) -> Unit,
        onNeedsSignInIntent: (android.content.Intent, GoogleDocsSyncAction, String?) -> Unit = { _, _, _ -> }
    ) {
        viewModelScope.launch {
            _isSyncingGoogleDocs.value = true
            handleAuthOutcome(
                authOutcome = GoogleDocsSyncService.authorizeGoogleDocs(context),
                action = action,
                customDocIdInput = customDocIdInput,
                onNeedsConsent = onNeedsConsent,
                onNeedsSignInIntent = onNeedsSignInIntent
            )
        }
    }

    fun onGoogleDocsConsentResult(
        context: Context,
        intent: android.content.Intent?,
        action: GoogleDocsSyncAction,
        customDocIdInput: String?,
        onNeedsSignInIntent: (android.content.Intent, GoogleDocsSyncAction, String?) -> Unit
    ) {
        viewModelScope.launch {
            _isSyncingGoogleDocs.value = true
            val outcome = GoogleDocsSyncService.extractTokenFromResolution(context, intent)
            handleAuthOutcome(
                authOutcome = outcome,
                action = action,
                customDocIdInput = customDocIdInput,
                onNeedsConsent = { _, _, _ -> },
                onNeedsSignInIntent = onNeedsSignInIntent
            )
        }
    }

    fun onGoogleSignInActivityResult(
        context: Context,
        intent: android.content.Intent?,
        action: GoogleDocsSyncAction,
        customDocIdInput: String?,
        onNeedsSignInIntent: (android.content.Intent, GoogleDocsSyncAction, String?) -> Unit
    ) {
        viewModelScope.launch {
            _isSyncingGoogleDocs.value = true
            val outcome = GoogleDocsSyncService.extractTokenFromGoogleSignInIntent(context, intent)
            handleAuthOutcome(
                authOutcome = outcome,
                action = action,
                customDocIdInput = customDocIdInput,
                onNeedsConsent = { _, _, _ -> },
                onNeedsSignInIntent = onNeedsSignInIntent
            )
        }
    }

    private suspend fun handleAuthOutcome(
        authOutcome: GoogleDocsAuthOutcome,
        action: GoogleDocsSyncAction,
        customDocIdInput: String?,
        onNeedsConsent: (IntentSender, GoogleDocsSyncAction, String?) -> Unit,
        onNeedsSignInIntent: (android.content.Intent, GoogleDocsSyncAction, String?) -> Unit
    ) {
        when (authOutcome) {
            is GoogleDocsAuthOutcome.Authorized -> {
                executeAuthorizedGoogleDocsSync(
                    accessToken = authOutcome.accessToken,
                    action = action,
                    customDocIdInput = customDocIdInput
                )
            }
            is GoogleDocsAuthOutcome.NeedsResolution -> {
                _isSyncingGoogleDocs.value = false
                onNeedsConsent(authOutcome.intentSender, action, customDocIdInput)
            }
            is GoogleDocsAuthOutcome.NeedsSignInIntent -> {
                _isSyncingGoogleDocs.value = false
                onNeedsSignInIntent(authOutcome.signInIntent, action, customDocIdInput)
            }
            is GoogleDocsAuthOutcome.Error -> {
                _isSyncingGoogleDocs.value = false
                val isBn = uiState.value.settings.language != "EN"
                _statusBannerMessage.value = if (isBn) {
                    "❌ Google সাইন-ইন ত্রুটি: ${authOutcome.message}"
                } else {
                    "❌ Google Sign-In error: ${authOutcome.message}"
                }
            }
        }
    }

    private suspend fun executeAuthorizedGoogleDocsSync(
        accessToken: String,
        action: GoogleDocsSyncAction,
        customDocIdInput: String?
    ) {
        val currentSettings = uiState.value.settings
        val isBn = currentSettings.language != "EN"

        when (action) {
            GoogleDocsSyncAction.PUSH_TO_DOCS -> {
                val resolvedDocId = customDocIdInput?.takeIf { it.isNotBlank() }
                    ?.let { GoogleDocsSyncService.extractDocIdFromInput(it) }
                    ?: currentSettings.syncedGoogleDocId
                val customers = repository.getAllCustomersSnapshot()
                val transactions = repository.getAllTransactionsSnapshot()
                val rawJson = repository.exportBackupJson(currentSettings)
                val result = GoogleDocsSyncService.syncLedgerToGoogleDoc(
                    accessToken = accessToken,
                    existingDocId = resolvedDocId,
                    settings = currentSettings,
                    customers = customers,
                    transactions = transactions,
                    rawBackupJson = rawJson
                )
                _isSyncingGoogleDocs.value = false
                result.onSuccess { syncRes ->
                    repository.updateGoogleDocsSyncInfo(
                        docId = syncRes.documentId,
                        syncedAtMillis = syncRes.syncedAtMillis
                    )
                    _statusBannerMessage.value = if (isBn) {
                        "✅ Google Docs-এ লাইভ সিঙ্ক সম্পন্ন হয়েছে!"
                    } else {
                        "✅ Live synced to Google Docs!"
                    }
                }.onFailure { err ->
                    _statusBannerMessage.value = if (isBn) {
                        "❌ Google Docs সিঙ্ক ব্যর্থ: ${err.localizedMessage}"
                    } else {
                        "❌ Google Docs sync failed: ${err.localizedMessage}"
                    }
                }
            }

            GoogleDocsSyncAction.PULL_FROM_DOCS -> {
                val resolvedDocId = customDocIdInput?.takeIf { it.isNotBlank() }
                    ?.let { GoogleDocsSyncService.extractDocIdFromInput(it) }
                    ?: currentSettings.syncedGoogleDocId
                if (resolvedDocId.isBlank()) {
                    _isSyncingGoogleDocs.value = false
                    _statusBannerMessage.value = if (isBn) {
                        "❌ অনুগ্রহ করে প্রথমে Google Doc ID বা লিংক দিন"
                    } else {
                        "❌ Please enter a Google Doc ID or link first"
                    }
                    return
                }
                val pullRes = GoogleDocsSyncService.pullLedgerFromGoogleDoc(
                    accessToken = accessToken,
                    documentId = resolvedDocId
                )
                pullRes.onSuccess { jsonPayload ->
                    val importRes = repository.importBackupJson(jsonPayload)
                    _isSyncingGoogleDocs.value = false
                    importRes.onSuccess { (cCount, tCount) ->
                        repository.updateGoogleDocsSyncInfo(
                            docId = resolvedDocId,
                            syncedAtMillis = System.currentTimeMillis()
                        )
                        _statusBannerMessage.value = if (isBn) {
                            "✅ Google Docs থেকে $cCount জন কাস্টমার ও $tCount টি লেনদেন সিঙ্ক হয়েছে!"
                        } else {
                            "✅ Restored $cCount customers & $tCount transactions from Google Docs!"
                        }
                    }.onFailure { err ->
                        _statusBannerMessage.value = if (isBn) {
                            "❌ ডাটা পার্সিং ত্রুটি: ${err.localizedMessage}"
                        } else {
                            "❌ Data parse error: ${err.localizedMessage}"
                        }
                    }
                }.onFailure { err ->
                    _isSyncingGoogleDocs.value = false
                    _statusBannerMessage.value = if (isBn) {
                        "❌ Google Docs থেকে পড়া যায়নি: ${err.localizedMessage}"
                    } else {
                        "❌ Failed to pull from Google Docs: ${err.localizedMessage}"
                    }
                }
            }

            GoogleDocsSyncAction.PUSH_TO_SHEETS -> {
                val resolvedSheetId = customDocIdInput?.takeIf { it.isNotBlank() }
                    ?.let { GoogleDocsSyncService.extractSheetIdFromInput(it) }
                    ?: currentSettings.syncedGoogleSheetId
                val customers = repository.getAllCustomersSnapshot()
                val transactions = repository.getAllTransactionsSnapshot()
                val rawJson = repository.exportBackupJson(currentSettings)
                val result = GoogleDocsSyncService.syncLedgerToGoogleSheet(
                    accessToken = accessToken,
                    existingSheetId = resolvedSheetId,
                    settings = currentSettings,
                    customers = customers,
                    transactions = transactions,
                    rawBackupJson = rawJson
                )
                _isSyncingGoogleDocs.value = false
                result.onSuccess { syncRes ->
                    repository.updateGoogleSheetsSyncInfo(
                        sheetId = syncRes.documentId,
                        syncedAtMillis = syncRes.syncedAtMillis
                    )
                    _statusBannerMessage.value = if (isBn) {
                        "✅ Google Sheets-এ লাইভ সিঙ্ক সম্পন্ন হয়েছে!"
                    } else {
                        "✅ Live synced to Google Sheets!"
                    }
                }.onFailure { err ->
                    _statusBannerMessage.value = if (isBn) {
                        "❌ Google Sheets সিঙ্ক ব্যর্থ: ${err.localizedMessage}"
                    } else {
                        "❌ Google Sheets sync failed: ${err.localizedMessage}"
                    }
                }
            }

            GoogleDocsSyncAction.PULL_FROM_SHEETS -> {
                val resolvedSheetId = customDocIdInput?.takeIf { it.isNotBlank() }
                    ?.let { GoogleDocsSyncService.extractSheetIdFromInput(it) }
                    ?: currentSettings.syncedGoogleSheetId
                if (resolvedSheetId.isBlank()) {
                    _isSyncingGoogleDocs.value = false
                    _statusBannerMessage.value = if (isBn) {
                        "❌ অনুগ্রহ করে প্রথমে Google Sheet ID বা লিংক দিন"
                    } else {
                        "❌ Please enter a Google Sheet ID or link first"
                    }
                    return
                }
                val pullRes = GoogleDocsSyncService.pullLedgerFromGoogleSheet(
                    accessToken = accessToken,
                    spreadsheetId = resolvedSheetId
                )
                pullRes.onSuccess { jsonPayload ->
                    val importRes = repository.importBackupJson(jsonPayload)
                    _isSyncingGoogleDocs.value = false
                    importRes.onSuccess { (cCount, tCount) ->
                        repository.updateGoogleSheetsSyncInfo(
                            sheetId = resolvedSheetId,
                            syncedAtMillis = System.currentTimeMillis()
                        )
                        _statusBannerMessage.value = if (isBn) {
                            "✅ Google Sheets থেকে $cCount জন কাস্টমার ও $tCount টি লেনদেন সিঙ্ক হয়েছে!"
                        } else {
                            "✅ Restored $cCount customers & $tCount transactions from Google Sheets!"
                        }
                    }.onFailure { err ->
                        _statusBannerMessage.value = if (isBn) {
                            "❌ ডাটা পার্সিং ত্রুটি: ${err.localizedMessage}"
                        } else {
                            "❌ Data parse error: ${err.localizedMessage}"
                        }
                    }
                }.onFailure { err ->
                    _isSyncingGoogleDocs.value = false
                    _statusBannerMessage.value = if (isBn) {
                        "❌ Google Sheets থেকে পড়া যায়নি: ${err.localizedMessage}"
                    } else {
                        "❌ Failed to pull from Google Sheets: ${err.localizedMessage}"
                    }
                }
            }
        }
    }

    private fun triggerAutoSyncGoogleDocsIfConfigured() {
        val currentSettings = uiState.value.settings
        val hasDoc = currentSettings.syncedGoogleDocId.isNotBlank()
        val hasSheet = currentSettings.syncedGoogleSheetId.isNotBlank()
        if (!currentSettings.autoSyncGoogleDocs || (!hasDoc && !hasSheet)) {
            return
        }
        val appContext = getApplication<Application>()
        viewModelScope.launch {
            val authOutcome = GoogleDocsSyncService.authorizeGoogleDocs(appContext)
            if (authOutcome is GoogleDocsAuthOutcome.Authorized) {
                val customers = repository.getAllCustomersSnapshot()
                val transactions = repository.getAllTransactionsSnapshot()
                val rawJson = repository.exportBackupJson(uiState.value.settings)
                if (hasDoc) {
                    val res = GoogleDocsSyncService.syncLedgerToGoogleDoc(
                        accessToken = authOutcome.accessToken,
                        existingDocId = currentSettings.syncedGoogleDocId,
                        settings = uiState.value.settings,
                        customers = customers,
                        transactions = transactions,
                        rawBackupJson = rawJson
                    )
                    res.onSuccess { syncRes ->
                        repository.updateGoogleDocsSyncInfo(
                            docId = syncRes.documentId,
                            syncedAtMillis = syncRes.syncedAtMillis
                        )
                    }
                }
                if (hasSheet) {
                    val sheetRes = GoogleDocsSyncService.syncLedgerToGoogleSheet(
                        accessToken = authOutcome.accessToken,
                        existingSheetId = currentSettings.syncedGoogleSheetId,
                        settings = uiState.value.settings,
                        customers = customers,
                        transactions = transactions,
                        rawBackupJson = rawJson
                    )
                    sheetRes.onSuccess { syncRes ->
                        repository.updateGoogleSheetsSyncInfo(
                            sheetId = syncRes.documentId,
                            syncedAtMillis = syncRes.syncedAtMillis
                        )
                    }
                }
            }
        }
    }

    fun exportDatabaseToUri(context: Context, targetUri: Uri) {
        viewModelScope.launch {
            try {
                val jsonString = withContext(Dispatchers.IO) {
                    val json = repository.exportBackupJson(uiState.value.settings)
                    context.contentResolver.openOutputStream(targetUri)?.use { out ->
                        out.write(json.toByteArray(Charsets.UTF_8))
                    }
                    json
                }
                if (jsonString.isNotEmpty()) {
                    _statusBannerMessage.value = "✅ JSON ডাটাবেস ব্যাকআপ সফলভাবে সেভ হয়েছে!"
                }
            } catch (e: Exception) {
                _statusBannerMessage.value = "❌ ব্যাকআপ এক্সপোর্ট ব্যর্থ: ${e.localizedMessage}"
            }
        }
    }

    fun importDatabaseFromUri(context: Context, sourceUri: Uri) {
        viewModelScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    val text = context.contentResolver.openInputStream(sourceUri)
                        ?.bufferedReader(Charsets.UTF_8)
                        ?.use { it.readText() }
                        ?: throw IllegalArgumentException("Empty file")
                    repository.importBackupJson(text)
                }
                result.onSuccess { (cCount, tCount) ->
                    _statusBannerMessage.value = "✅ $cCount জন কাস্টমার ও $tCount টি লেনদেন রিস্টোর হয়েছে!"
                }.onFailure { err ->
                    _statusBannerMessage.value = "❌ JSON ইমপোর্ট ত্রুটি: ${err.localizedMessage}"
                }
            } catch (e: Exception) {
                _statusBannerMessage.value = "❌ ফাইল পড়তে সমস্যা হয়েছে: ${e.localizedMessage}"
            }
        }
    }

    fun createCameraReceiptUri(context: Context): Pair<Uri, String>? {
        return try {
            val dir = File(context.filesDir, "receipts").apply { mkdirs() }
            val file = File(dir, "receipt_${System.currentTimeMillis()}.jpg")
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            uri to file.absolutePath
        } catch (e: Exception) {
            null
        }
    }

    suspend fun copyGalleryReceiptToInternalStorage(context: Context, sourceUri: Uri): String? {
        return withContext(Dispatchers.IO) {
            try {
                val dir = File(context.filesDir, "receipts").apply { mkdirs() }
                val destFile = File(dir, "receipt_${System.currentTimeMillis()}.jpg")
                context.contentResolver.openInputStream(sourceUri)?.use { input ->
                    FileOutputStream(destFile).use { output ->
                        input.copyTo(output)
                    }
                }
                destFile.absolutePath
            } catch (e: Exception) {
                sourceUri.toString()
            }
        }
    }
}

private data class Quintuple<A, B, C, D, E>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D,
    val fifth: E
)
