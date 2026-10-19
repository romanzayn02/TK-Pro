package com.example.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.dataStore by preferencesDataStore(name = "tallykhata_settings")

data class AppSettings(
    val businessName: String = "",
    val ownerPhone: String = "",
    val currencySymbol: String = "৳",
    val language: String = "BN", // "BN" (বাংলা) default, or "EN" (English)
    val customCategories: List<String> = listOf(
        "বাকিতে বিক্রি",
        "নগদ জমা",
        "বাকি আদায়",
        "মাল ক্রয়",
        "পরিশোধ",
        "পরিবহন ও ভাড়া",
        "দোকান খরচ",
        "অন্যান্য"
    ),
    val useDarkGlassTheme: Boolean = false,
    val isSetupCompleted: Boolean = false,
    val isSettingsLoaded: Boolean = false,
    val syncedGoogleDocId: String = "",
    val autoSyncGoogleDocs: Boolean = true,
    val lastSyncedAtMillis: Long = 0L
)

class LedgerRepository(
    private val dao: LedgerDao,
    private val context: Context
) {
    private object Keys {
        val BUSINESS_NAME = stringPreferencesKey("business_name")
        val OWNER_PHONE = stringPreferencesKey("owner_phone")
        val CURRENCY_SYMBOL = stringPreferencesKey("currency_symbol")
        val LANGUAGE = stringPreferencesKey("language")
        val CUSTOM_CATEGORIES = stringPreferencesKey("custom_categories")
        val USE_DARK_GLASS = booleanPreferencesKey("use_dark_glass")
        val IS_SETUP_COMPLETED = booleanPreferencesKey("is_setup_completed")
        val SYNCED_GOOGLE_DOC_ID = stringPreferencesKey("synced_google_doc_id")
        val AUTO_SYNC_GOOGLE_DOCS = booleanPreferencesKey("auto_sync_google_docs")
        val LAST_SYNCED_AT_MILLIS = androidx.datastore.preferences.core.longPreferencesKey("last_synced_at_millis")
    }

    val settingsFlow: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        val rawCats = prefs[Keys.CUSTOM_CATEGORIES]
        val categories = if (rawCats.isNullOrBlank()) {
            listOf(
                "বাকিতে বিক্রি",
                "নগদ জমা",
                "বাকি আদায়",
                "মাল ক্রয়",
                "পরিশোধ",
                "পরিবহন ও ভাড়া",
                "দোকান খরচ",
                "অন্যান্য"
            )
        } else {
            rawCats.split("|").filter { it.isNotBlank() }
        }
        AppSettings(
            businessName = prefs[Keys.BUSINESS_NAME] ?: "",
            ownerPhone = prefs[Keys.OWNER_PHONE] ?: "",
            currencySymbol = prefs[Keys.CURRENCY_SYMBOL] ?: "৳",
            language = prefs[Keys.LANGUAGE] ?: "BN",
            customCategories = categories,
            useDarkGlassTheme = prefs[Keys.USE_DARK_GLASS] ?: false,
            isSetupCompleted = prefs[Keys.IS_SETUP_COMPLETED] ?: false,
            isSettingsLoaded = true,
            syncedGoogleDocId = prefs[Keys.SYNCED_GOOGLE_DOC_ID] ?: "",
            autoSyncGoogleDocs = prefs[Keys.AUTO_SYNC_GOOGLE_DOCS] ?: true,
            lastSyncedAtMillis = prefs[Keys.LAST_SYNCED_AT_MILLIS] ?: 0L
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
        avatarColorHex: Long = 0xFF0091EA
    ): Long {
        val now = System.currentTimeMillis()
        return dao.insertCustomer(
            CustomerEntity(
                name = name.trim(),
                phone = phone.trim(),
                partyType = partyType,
                address = address.trim(),
                avatarColorHex = avatarColorHex,
                createdAt = now,
                updatedAt = now
            )
        )
    }

    suspend fun updateCustomer(customer: CustomerEntity) {
        dao.updateCustomer(customer.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun deleteCustomer(customerId: Long) {
        dao.deleteCustomerById(customerId)
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
    }

    suspend fun deleteTransaction(transactionId: Long) {
        dao.deleteTransactionById(transactionId)
    }

    suspend fun updateLanguage(lang: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.LANGUAGE] = lang
        }
    }

    suspend fun updateBusinessSettings(
        businessName: String,
        ownerPhone: String,
        currencySymbol: String
    ) {
        context.dataStore.edit { prefs ->
            prefs[Keys.BUSINESS_NAME] = businessName.trim()
            prefs[Keys.OWNER_PHONE] = ownerPhone.trim()
            prefs[Keys.CURRENCY_SYMBOL] = currencySymbol.trim().ifEmpty { "৳" }
            prefs[Keys.IS_SETUP_COMPLETED] = true
        }
    }

    suspend fun completeInitialSetup(
        businessName: String,
        ownerPhone: String,
        currencySymbol: String,
        language: String
    ) {
        context.dataStore.edit { prefs ->
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
        if (currentCategories.size <= 2) return
        val updated = currentCategories.filterNot { it == category }
        context.dataStore.edit { prefs ->
            prefs[Keys.CUSTOM_CATEGORIES] = updated.joinToString("|")
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
            val root = JSONObject(jsonContent)
            val customersJson = root.optJSONArray("customers") ?: JSONArray()
            val transactionsJson = root.optJSONArray("transactions") ?: JSONArray()

            val customers = mutableListOf<CustomerEntity>()
            for (i in 0 until customersJson.length()) {
                val obj = customersJson.getJSONObject(i)
                customers.add(
                    CustomerEntity(
                        id = obj.getLong("id"),
                        name = obj.optString("name", "Customer"),
                        phone = obj.optString("phone", ""),
                        partyType = obj.optString("partyType", "CUSTOMER"),
                        address = obj.optString("address", ""),
                        avatarColorHex = obj.optLong("avatarColorHex", 0xFF0091EA),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                        updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
                    )
                )
            }

            val transactions = mutableListOf<TransactionEntity>()
            for (i in 0 until transactionsJson.length()) {
                val obj = transactionsJson.getJSONObject(i)
                val rawReceipt = if (obj.isNull("receiptUri")) null else obj.optString("receiptUri")
                transactions.add(
                    TransactionEntity(
                        id = obj.getLong("id"),
                        customerId = obj.getLong("customerId"),
                        type = obj.optString("type", "DEBIT"),
                        amount = obj.optDouble("amount", 0.0),
                        category = obj.optString("category", "অন্যান্য"),
                        paymentMethod = obj.optString("paymentMethod", "CASH"),
                        note = obj.optString("note", ""),
                        receiptUri = rawReceipt?.takeIf { it.isNotBlank() && it != "null" },
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                    )
                )
            }

            root.optJSONObject("settings")?.let { sObj ->
                val bName = sObj.optString("businessName")
                val oPhone = sObj.optString("ownerPhone")
                val cSymbol = sObj.optString("currencySymbol")
                updateBusinessSettings(
                    businessName = bName,
                    ownerPhone = oPhone,
                    currencySymbol = cSymbol.ifBlank { "৳" }
                )
            } ?: run {
                context.dataStore.edit { prefs ->
                    prefs[Keys.IS_SETUP_COMPLETED] = true
                }
            }

            dao.replaceFullDatabase(customers, transactions)
            Result.success(customers.size to transactions.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
