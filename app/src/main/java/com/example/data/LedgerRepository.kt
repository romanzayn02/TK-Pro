package com.example.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.dataStore by preferencesDataStore(name = "tallykhata_settings")

data class AppSettings(
    val accountType: String = "BUSINESS", // "BUSINESS" (ব্যবসা) or "INDIVIDUAL" (ব্যক্তিগত)
    val businessName: String = "",
    val ownerPhone: String = "",
    val currencySymbol: String = "৳",
    val language: String = "BN", // "BN" (বাংলা) default, or "EN" (English)
    val profilePhotoUri: String = "", // Local override image path on device (no cloud sync)
    val googlePhotoUrl: String = "",  // Default Google Account profile picture URL
    val customCategories: List<String> = emptyList(), // No hardcoded default transaction categories
    val useDarkGlassTheme: Boolean = false,
    val biometricLockEnabled: Boolean = false,
    val isSetupCompleted: Boolean = false,
    val isSettingsLoaded: Boolean = false,
    val syncedGoogleDocId: String = "",
    val syncedGoogleSheetId: String = "",
    val autoSyncGoogleDocs: Boolean = true,
    val lastSyncedAtMillis: Long = 0L,
    val lastSheetSyncedAtMillis: Long = 0L,
    val hasPendingCloudSync: Boolean = false,
    val lastSyncError: String = ""
) {
    /**
     * Effective profile photo to display: local custom upload if set, otherwise Google account photo URL.
     */
    val effectiveProfilePhoto: String
        get() = profilePhotoUri.ifBlank { googlePhotoUrl }
}

class LedgerRepository(
    private val dao: LedgerDao,
    private val context: Context
) {
    companion object {
        val LEGACY_DEFAULT_CATEGORIES = setOf(
            "বাকিতে বিক্রি",
            "নগদ জমা",
            "বাকি আদায়",
            "বাকি আদায়",
            "মাল ক্রয়",
            "মাল ক্রয়",
            "পরিশোধ",
            "পরিবহন ও ভাড়া",
            "দোকান খরচ",
            "অন্যান্য"
        )
    }

    private object Keys {
        val ACCOUNT_TYPE = stringPreferencesKey("account_type")
        val BUSINESS_NAME = stringPreferencesKey("business_name")
        val OWNER_PHONE = stringPreferencesKey("owner_phone")
        val CURRENCY_SYMBOL = stringPreferencesKey("currency_symbol")
        val LANGUAGE = stringPreferencesKey("language")
        val PROFILE_PHOTO_URI = stringPreferencesKey("profile_photo_uri")
        val GOOGLE_PHOTO_URL = stringPreferencesKey("google_photo_url")
        val CUSTOM_CATEGORIES = stringPreferencesKey("custom_categories")
        val USE_DARK_GLASS = booleanPreferencesKey("use_dark_glass")
        val BIOMETRIC_LOCK_ENABLED = booleanPreferencesKey("biometric_lock_enabled")
        val IS_SETUP_COMPLETED = booleanPreferencesKey("is_setup_completed")
        val SYNCED_GOOGLE_DOC_ID = stringPreferencesKey("synced_google_doc_id")
        val SYNCED_GOOGLE_SHEET_ID = stringPreferencesKey("synced_google_sheet_id")
        val AUTO_SYNC_GOOGLE_DOCS = booleanPreferencesKey("auto_sync_google_docs")
        val LAST_SYNCED_AT_MILLIS = androidx.datastore.preferences.core.longPreferencesKey("last_synced_at_millis")
        val LAST_SHEET_SYNCED_AT_MILLIS = androidx.datastore.preferences.core.longPreferencesKey("last_sheet_synced_at_millis")
        val HAS_PENDING_CLOUD_SYNC = booleanPreferencesKey("has_pending_cloud_sync")
        val LAST_SYNC_ERROR = stringPreferencesKey("last_sync_error")
    }

    val settingsFlow: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        val rawCats = prefs[Keys.CUSTOM_CATEGORIES]
        val categories = if (rawCats == null || rawCats.isBlank() || rawCats == "__EMPTY__") {
            emptyList()
        } else {
            rawCats.split("|")
                .map { it.trim() }
                .filter { it.isNotBlank() && it !in LEGACY_DEFAULT_CATEGORIES }
        }
        val fallbackGooglePhoto = GoogleDocsSyncService.getSignedInPhotoUrl(context).orEmpty()
        AppSettings(
            accountType = prefs[Keys.ACCOUNT_TYPE] ?: "BUSINESS",
            businessName = prefs[Keys.BUSINESS_NAME] ?: "",
            ownerPhone = prefs[Keys.OWNER_PHONE] ?: "",
            currencySymbol = prefs[Keys.CURRENCY_SYMBOL] ?: "৳",
            language = prefs[Keys.LANGUAGE] ?: "BN",
            profilePhotoUri = prefs[Keys.PROFILE_PHOTO_URI] ?: "",
            googlePhotoUrl = (prefs[Keys.GOOGLE_PHOTO_URL] ?: "").ifBlank { fallbackGooglePhoto },
            customCategories = categories,
            useDarkGlassTheme = prefs[Keys.USE_DARK_GLASS] ?: false,
            biometricLockEnabled = prefs[Keys.BIOMETRIC_LOCK_ENABLED] ?: false,
            isSetupCompleted = prefs[Keys.IS_SETUP_COMPLETED] ?: false,
            isSettingsLoaded = true,
            syncedGoogleDocId = prefs[Keys.SYNCED_GOOGLE_DOC_ID] ?: "",
            syncedGoogleSheetId = prefs[Keys.SYNCED_GOOGLE_SHEET_ID] ?: "",
            autoSyncGoogleDocs = prefs[Keys.AUTO_SYNC_GOOGLE_DOCS] ?: true,
            lastSyncedAtMillis = prefs[Keys.LAST_SYNCED_AT_MILLIS] ?: 0L,
            lastSheetSyncedAtMillis = prefs[Keys.LAST_SHEET_SYNCED_AT_MILLIS] ?: 0L,
            hasPendingCloudSync = prefs[Keys.HAS_PENDING_CLOUD_SYNC] ?: false,
            lastSyncError = prefs[Keys.LAST_SYNC_ERROR] ?: ""
        )
    }

    suspend fun getAllCustomersSnapshot(): List<CustomerEntity> = dao.getAllCustomersSnapshot()
    suspend fun getAllTransactionsSnapshot(): List<TransactionEntity> = dao.getAllTransactionsSnapshot()

    val allCustomersFlow: Flow<List<CustomerEntity>> = dao.getAllCustomersFlow()
    val allTransactionsFlow: Flow<List<TransactionEntity>> = dao.getAllTransactionsFlow()

    fun getCustomerByIdFlow(customerId: Long): Flow<CustomerEntity?> =
        dao.getCustomerByIdFlow(customerId)

    fun getTransactionsForCustomerFlow(customerId: Long): Flow<List<TransactionEntity>> =
        dao.getTransactionsForCustomerFlow(customerId)

    suspend fun addCustomer(
        name: String,
        phone: String,
        partyType: String,
        address: String = "",
        avatarColorHex: Long = 0xFF0091EA,
        photoUri: String? = null
    ): Long {
        val now = System.currentTimeMillis()
        val id = dao.insertCustomer(
            CustomerEntity(
                name = name.trim(),
                phone = phone.trim(),
                partyType = partyType,
                address = address.trim(),
                avatarColorHex = avatarColorHex,
                photoUri = photoUri?.takeIf { it.isNotBlank() },
                createdAt = now,
                updatedAt = now
            )
        )
        markPendingCloudSync()
        return id
    }

    suspend fun updateCustomer(customer: CustomerEntity) {
        dao.updateCustomer(customer.copy(updatedAt = System.currentTimeMillis()))
        markPendingCloudSync()
    }

    suspend fun updateCustomerPhoto(customerId: Long, photoUri: String?) {
        val existing = dao.getAllCustomersSnapshot().find { it.id == customerId } ?: return
        dao.updateCustomer(
            existing.copy(
                photoUri = photoUri?.takeIf { it.isNotBlank() },
                updatedAt = System.currentTimeMillis()
            )
        )
        // Note: Customer photos are device-local only and do not need to trigger cloud sync
    }

    suspend fun deleteCustomer(customerId: Long) {
        dao.deleteCustomerById(customerId)
        markPendingCloudSync()
    }

    suspend fun addTransaction(
        customerId: Long,
        type: String,
        amount: Double,
        category: String,
        paymentMethod: String,
        note: String,
        receiptUri: String?,
        timestamp: Long = System.currentTimeMillis()
    ): Long {
        val txId = dao.insertTransaction(
            TransactionEntity(
                customerId = customerId,
                type = type,
                amount = amount,
                category = category,
                paymentMethod = paymentMethod,
                note = note.trim(),
                receiptUri = receiptUri,
                timestamp = timestamp
            )
        )
        // Touch customer's updatedAt so recent customers float to top
        val existing = dao.getAllCustomersSnapshot().find { it.id == customerId }
        if (existing != null) {
            dao.updateCustomer(existing.copy(updatedAt = System.currentTimeMillis()))
        }
        markPendingCloudSync()
        return txId
    }

    suspend fun updateTransaction(
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
        dao.updateTransaction(
            TransactionEntity(
                id = transactionId,
                customerId = customerId,
                type = type,
                amount = amount,
                category = category,
                paymentMethod = paymentMethod,
                note = note.trim(),
                receiptUri = receiptUri,
                timestamp = timestamp
            )
        )
        val existing = dao.getAllCustomersSnapshot().find { it.id == customerId }
        if (existing != null) {
            dao.updateCustomer(existing.copy(updatedAt = System.currentTimeMillis()))
        }
        markPendingCloudSync()
    }

    suspend fun deleteTransaction(transactionId: Long) {
        dao.deleteTransactionById(transactionId)
        markPendingCloudSync()
    }

    private suspend fun markPendingCloudSync() {
        context.dataStore.edit { prefs ->
            val hasDoc = !prefs[Keys.SYNCED_GOOGLE_DOC_ID].isNullOrBlank()
            val hasSheet = !prefs[Keys.SYNCED_GOOGLE_SHEET_ID].isNullOrBlank()
            if (hasDoc || hasSheet) {
                prefs[Keys.HAS_PENDING_CLOUD_SYNC] = true
            }
        }
    }

    suspend fun setPendingCloudSync(pending: Boolean, errorMessage: String = "") {
        context.dataStore.edit { prefs ->
            prefs[Keys.HAS_PENDING_CLOUD_SYNC] = pending
            prefs[Keys.LAST_SYNC_ERROR] = errorMessage
        }
    }

    suspend fun updateLanguage(lang: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.LANGUAGE] = lang
        }
    }

    suspend fun updateOwnerProfilePhoto(photoUri: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.PROFILE_PHOTO_URI] = photoUri.trim()
        }
    }

    suspend fun updateGooglePhotoUrl(googlePhotoUrl: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.GOOGLE_PHOTO_URL] = googlePhotoUrl.trim()
        }
    }

    suspend fun updateBusinessSettings(
        businessName: String,
        ownerPhone: String,
        currencySymbol: String,
        accountType: String? = null
    ) {
        context.dataStore.edit { prefs ->
            if (accountType != null) {
                prefs[Keys.ACCOUNT_TYPE] = accountType
            }
            prefs[Keys.BUSINESS_NAME] = businessName.trim()
            prefs[Keys.OWNER_PHONE] = ownerPhone.trim()
            prefs[Keys.CURRENCY_SYMBOL] = currencySymbol.trim().ifEmpty { "৳" }
            prefs[Keys.IS_SETUP_COMPLETED] = true
        }
    }

    suspend fun updateAccountType(accountType: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.ACCOUNT_TYPE] = accountType
        }
    }

    suspend fun setBiometricLockEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.BIOMETRIC_LOCK_ENABLED] = enabled
        }
    }

    suspend fun completeInitialSetup(
        businessName: String,
        ownerPhone: String,
        currencySymbol: String,
        language: String,
        accountType: String = "BUSINESS"
    ) {
        context.dataStore.edit { prefs ->
            prefs[Keys.ACCOUNT_TYPE] = accountType
            prefs[Keys.BUSINESS_NAME] = businessName.trim()
            prefs[Keys.OWNER_PHONE] = ownerPhone.trim()
            prefs[Keys.CURRENCY_SYMBOL] = currencySymbol.trim().ifEmpty { "৳" }
            prefs[Keys.LANGUAGE] = language
            prefs[Keys.IS_SETUP_COMPLETED] = true
        }
    }

    suspend fun toggleThemeMode(useDarkGlass: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.USE_DARK_GLASS] = useDarkGlass
        }
    }

    suspend fun updateGoogleDocsSyncInfo(docId: String, syncedAtMillis: Long = System.currentTimeMillis()) {
        context.dataStore.edit { prefs ->
            prefs[Keys.SYNCED_GOOGLE_DOC_ID] = docId.trim()
            if (syncedAtMillis > 0L) {
                prefs[Keys.LAST_SYNCED_AT_MILLIS] = syncedAtMillis
                prefs[Keys.HAS_PENDING_CLOUD_SYNC] = false
                prefs[Keys.LAST_SYNC_ERROR] = ""
            }
        }
    }

    suspend fun updateGoogleSheetsSyncInfo(sheetId: String, syncedAtMillis: Long = System.currentTimeMillis()) {
        context.dataStore.edit { prefs ->
            prefs[Keys.SYNCED_GOOGLE_SHEET_ID] = sheetId.trim()
            if (syncedAtMillis > 0L) {
                prefs[Keys.LAST_SHEET_SYNCED_AT_MILLIS] = syncedAtMillis
                prefs[Keys.HAS_PENDING_CLOUD_SYNC] = false
                prefs[Keys.LAST_SYNC_ERROR] = ""
            }
        }
    }

    suspend fun setAutoSyncGoogleDocs(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.AUTO_SYNC_GOOGLE_DOCS] = enabled
        }
    }

    suspend fun addCustomCategory(newCategory: String, currentCategories: List<String>) {
        val trimmed = newCategory.trim()
        if (trimmed.isEmpty() || currentCategories.contains(trimmed)) return
        val updated = currentCategories + trimmed
        context.dataStore.edit { prefs ->
            prefs[Keys.CUSTOM_CATEGORIES] = updated.joinToString("|")
        }
    }

    suspend fun removeCustomCategory(category: String, currentCategories: List<String>) {
        val updated = currentCategories.filterNot { it == category }
        context.dataStore.edit { prefs ->
            prefs[Keys.CUSTOM_CATEGORIES] = if (updated.isEmpty()) "__EMPTY__" else updated.joinToString("|")
        }
    }

    suspend fun clearAllCategories() {
        context.dataStore.edit { prefs ->
            prefs[Keys.CUSTOM_CATEGORIES] = "__EMPTY__"
        }
    }

    suspend fun exportBackupJson(currentSettings: AppSettings): String {
        val customers = dao.getAllCustomersSnapshot()
        val transactions = dao.getAllTransactionsSnapshot()

        val root = JSONObject()
        root.put("app", "TallyKhata Pro")
        root.put("version", 1)
        root.put("exportedAt", System.currentTimeMillis())

        val settingsJson = JSONObject().apply {
            put("accountType", currentSettings.accountType)
            put("businessName", currentSettings.businessName)
            put("ownerPhone", currentSettings.ownerPhone)
            put("currencySymbol", currentSettings.currencySymbol)
            put("language", currentSettings.language)
            put("customCategories", JSONArray(currentSettings.customCategories))
        }
        root.put("settings", settingsJson)

        val customersArray = JSONArray()
        customers.forEach { c ->
            customersArray.put(
                JSONObject().apply {
                    put("id", c.id)
                    put("name", c.name)
                    put("phone", c.phone)
                    put("partyType", c.partyType)
                    put("address", c.address)
                    put("avatarColorHex", c.avatarColorHex)
                    put("createdAt", c.createdAt)
                    put("updatedAt", c.updatedAt)
                }
            )
        }
        root.put("customers", customersArray)

        val transactionsArray = JSONArray()
        transactions.forEach { t ->
            transactionsArray.put(
                JSONObject().apply {
                    put("id", t.id)
                    put("customerId", t.customerId)
                    put("type", t.type)
                    put("amount", t.amount)
                    put("category", t.category)
                    put("paymentMethod", t.paymentMethod)
                    put("note", t.note)
                    put("receiptUri", t.receiptUri ?: JSONObject.NULL)
                    put("timestamp", t.timestamp)
                }
            )
        }
        root.put("transactions", transactionsArray)

        return root.toString(2)
    }

    suspend fun importBackupJson(jsonContent: String): Result<Pair<Int, Int>> {
        return try {
            val cleanJson = run {
                val startMarker = GoogleDocsSyncService.SYNC_START_MARKER
                val endMarker = GoogleDocsSyncService.SYNC_END_MARKER
                val sIdx = jsonContent.indexOf(startMarker)
                val eIdx = jsonContent.indexOf(endMarker)
                if (sIdx >= 0 && eIdx > sIdx) {
                    jsonContent.substring(sIdx + startMarker.length, eIdx).trim()
                } else {
                    val firstBrace = jsonContent.indexOf('{')
                    val lastBrace = jsonContent.lastIndexOf('}')
                    if (firstBrace >= 0 && lastBrace > firstBrace) {
                        jsonContent.substring(firstBrace, lastBrace + 1).trim()
                    } else {
                        jsonContent.trim()
                    }
                }
            }

            val root = JSONObject(cleanJson)
            val customersJson = root.optJSONArray("customers") ?: JSONArray()
            val transactionsJson = root.optJSONArray("transactions") ?: JSONArray()

            // Map existing local photos by ID and by (name, phone) so cloud restore preserves local device avatars
            val existingLocalCustomers = dao.getAllCustomersSnapshot()
            val localPhotoById = existingLocalCustomers.mapNotNull { c ->
                c.photoUri?.takeIf { it.isNotBlank() }?.let { c.id to it }
            }.toMap()
            val localPhotoByIdentity = existingLocalCustomers.mapNotNull { c ->
                c.photoUri?.takeIf { it.isNotBlank() }?.let {
                    "${c.name.trim().lowercase()}_${c.phone.trim()}" to it
                }
            }.toMap()

            val customers = mutableListOf<CustomerEntity>()
            var maxCustomerId = 0L
            for (i in 0 until customersJson.length()) {
                val obj = customersJson.getJSONObject(i)
                val rawId = obj.optLong("id", 0L)
                val resolvedId = if (rawId > 0L) rawId else (maxCustomerId + 1L)
                if (resolvedId > maxCustomerId) maxCustomerId = resolvedId
                val cName = obj.optString("name", "Customer").trim().ifEmpty { "Customer" }
                val cPhone = obj.optString("phone", "").trim()
                val preservedPhoto = localPhotoById[resolvedId]
                    ?: localPhotoByIdentity["${cName.lowercase()}_$cPhone"]
                customers.add(
                    CustomerEntity(
                        id = resolvedId,
                        name = cName,
                        phone = cPhone,
                        partyType = obj.optString("partyType", "CUSTOMER").ifBlank { "CUSTOMER" },
                        address = obj.optString("address", "").trim(),
                        avatarColorHex = obj.optLong("avatarColorHex", 0xFF0091EA),
                        photoUri = preservedPhoto,
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                        updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
                    )
                )
            }

            val transactions = mutableListOf<TransactionEntity>()
            var maxTxId = 0L
            for (i in 0 until transactionsJson.length()) {
                val obj = transactionsJson.getJSONObject(i)
                val rawReceipt = if (obj.isNull("receiptUri")) null else obj.optString("receiptUri")
                val rawTxId = obj.optLong("id", 0L)
                val resolvedTxId = if (rawTxId > 0L) rawTxId else (maxTxId + 1L)
                if (resolvedTxId > maxTxId) maxTxId = resolvedTxId
                transactions.add(
                    TransactionEntity(
                        id = resolvedTxId,
                        customerId = obj.getLong("customerId"),
                        type = obj.optString("type", "DEBIT").ifBlank { "DEBIT" },
                        amount = obj.optDouble("amount", 0.0),
                        category = obj.optString("category", "অন্যান্য").ifBlank { "অন্যান্য" },
                        paymentMethod = obj.optString("paymentMethod", "CASH").ifBlank { "CASH" },
                        note = obj.optString("note", ""),
                        receiptUri = rawReceipt?.takeIf { it.isNotBlank() && it != "null" },
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                    )
                )
            }

            root.optJSONObject("settings")?.let { sObj ->
                val accType = sObj.optString("accountType", "BUSINESS").ifBlank { "BUSINESS" }
                val bName = sObj.optString("businessName")
                val oPhone = sObj.optString("ownerPhone")
                val cSymbol = sObj.optString("currencySymbol")
                val lang = sObj.optString("language")
                val catsArray = sObj.optJSONArray("customCategories")
                val importedCats = mutableListOf<String>()
                if (catsArray != null) {
                    for (idx in 0 until catsArray.length()) {
                        val c = catsArray.optString(idx).trim()
                        if (c.isNotEmpty() && c !in importedCats) importedCats.add(c)
                    }
                }
                context.dataStore.edit { prefs ->
                    prefs[Keys.ACCOUNT_TYPE] = accType
                    if (bName.isNotBlank()) prefs[Keys.BUSINESS_NAME] = bName.trim()
                    if (oPhone.isNotBlank()) prefs[Keys.OWNER_PHONE] = oPhone.trim()
                    if (cSymbol.isNotBlank()) prefs[Keys.CURRENCY_SYMBOL] = cSymbol.trim()
                    if (lang == "BN" || lang == "EN") prefs[Keys.LANGUAGE] = lang
                    if (importedCats.size >= 2) {
                        prefs[Keys.CUSTOM_CATEGORIES] = importedCats.joinToString("|")
                    }
                    prefs[Keys.IS_SETUP_COMPLETED] = true
                    prefs[Keys.HAS_PENDING_CLOUD_SYNC] = false
                    prefs[Keys.LAST_SYNC_ERROR] = ""
                }
            } ?: run {
                context.dataStore.edit { prefs ->
                    prefs[Keys.IS_SETUP_COMPLETED] = true
                    prefs[Keys.HAS_PENDING_CLOUD_SYNC] = false
                    prefs[Keys.LAST_SYNC_ERROR] = ""
                }
            }

            // Preserve any unsynced local offline customers & transactions safely without ID or FK conflicts
            val currentPrefs = context.dataStore.data.first()
            val lastSyncMillis = maxOf(
                currentPrefs[Keys.LAST_SYNCED_AT_MILLIS] ?: 0L,
                currentPrefs[Keys.LAST_SHEET_SYNCED_AT_MILLIS] ?: 0L
            )
            if (lastSyncMillis > 0L) {
                val localCustomers = existingLocalCustomers
                val localTransactions = dao.getAllTransactionsSnapshot()
                val cloudCustomerIds = customers.map { it.id }.toMutableSet()
                val cloudCustomerIdentityMap = customers.associateBy {
                    "${it.name.trim().lowercase()}_${it.phone.trim()}"
                }.toMutableMap()
                val localToMergedCustomerId = mutableMapOf<Long, Long>()

                for (localCustomer in localCustomers) {
                    val identityKey = "${localCustomer.name.trim().lowercase()}_${localCustomer.phone.trim()}"
                    val existingCloud = cloudCustomerIdentityMap[identityKey]
                    if (existingCloud != null) {
                        localToMergedCustomerId[localCustomer.id] = existingCloud.id
                    } else if (localCustomer.createdAt > lastSyncMillis) {
                        val newId = if (localCustomer.id !in cloudCustomerIds) {
                            localCustomer.id
                        } else {
                            ++maxCustomerId
                        }
                        val mergedCust = localCustomer.copy(id = newId)
                        customers.add(mergedCust)
                        cloudCustomerIds.add(newId)
                        cloudCustomerIdentityMap[identityKey] = mergedCust
                        localToMergedCustomerId[localCustomer.id] = newId
                    }
                }

                val cloudTxKeys = transactions.map {
                    "${it.customerId}_${it.type}_${it.amount}_${it.timestamp}_${it.note}"
                }.toMutableSet()
                val cloudTxIds = transactions.map { it.id }.toMutableSet()

                for (localTx in localTransactions) {
                    val targetCustomerId = localToMergedCustomerId[localTx.customerId]
                        ?: localTx.customerId.takeIf { it in cloudCustomerIds }
                        ?: continue
                    val key = "${targetCustomerId}_${localTx.type}_${localTx.amount}_${localTx.timestamp}_${localTx.note}"
                    if (key !in cloudTxKeys && localTx.timestamp > lastSyncMillis) {
                        val ensuredId = if (localTx.id !in cloudTxIds) localTx.id else ++maxTxId
                        transactions.add(localTx.copy(id = ensuredId, customerId = targetCustomerId))
                        cloudTxIds.add(ensuredId)
                        cloudTxKeys.add(key)
                    }
                }
            }

            // Guarantee foreign-key integrity before replacing database
            val validCustomerIds = customers.map { it.id }.toSet()
            val safeTransactions = transactions.filter { it.customerId in validCustomerIds }

            dao.replaceFullDatabase(customers, safeTransactions)
            Result.success(customers.size to safeTransactions.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
