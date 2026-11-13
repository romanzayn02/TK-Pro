package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppDatabase
import com.example.data.AppSettings
import com.example.data.LedgerRepository
import com.example.ui.LocalizationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    private lateinit var context: Context
    private lateinit var database: AppDatabase
    private lateinit var repository: LedgerRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = LedgerRepository(database.ledgerDao(), context)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `read app name and Bengali ledger strings from context`() {
        val appName = context.getString(R.string.app_name)
        assertEquals("TK Pro", appName)
        assertEquals("মোট পাবেন", context.getString(R.string.total_receivable_bn))
        assertEquals("মোট দেবেন", context.getString(R.string.total_payable_bn))
        assertEquals("মোট ব্যালেন্স", context.getString(R.string.net_balance_bn))
        val bnStrings = LocalizationProvider.getStrings("BN")
        assertEquals("খাতা", bnStrings.tabDashboard)
        assertEquals("ইতিহাস", bnStrings.tabHistory)
        assertEquals("সেটিংস", bnStrings.tabSettings)
    }

    @Test
    fun `verify Room ledger debit credit balance and JSON backup restore`() = runBlocking {
        val cId = repository.addCustomer(
            name = "রহিম স্টোর",
            phone = "01711223344",
            partyType = "CUSTOMER",
            address = "ঢাকা"
        )
        // Debit: টাকা পাব / দিয়েছি = 5000
        repository.addTransaction(
            customerId = cId,
            type = "DEBIT",
            amount = 5000.0,
            category = "বাকিতে বিক্রি",
            paymentMethod = "CASH",
            note = "চাল বিক্রি",
            receiptUri = null
        )
        // Credit: টাকা দেব / পেয়েছি = 2000
        repository.addTransaction(
            customerId = cId,
            type = "CREDIT",
            amount = 2000.0,
            category = "নগদ জমা",
            paymentMethod = "BKASH",
            note = "আংশিক আদায়",
            receiptUri = null
        )

        val backupJson = repository.exportBackupJson(AppSettings())
        assertTrue(backupJson.contains("রহিম স্টোর"))
        assertTrue(backupJson.contains("BKASH"))

        val restoreResult = repository.importBackupJson(backupJson)
        assertTrue(restoreResult.isSuccess)
        assertEquals(1 to 2, restoreResult.getOrNull())

        val smsTemplates = LocalizationProvider.generateBengaliSmsTemplates(
            customerName = "রহিম স্টোর",
            balanceAmount = 3000.0,
            currencySymbol = "৳",
            businessName = "বিসমিল্লাহ জেনারেল স্টোর",
            ownerPhone = "01700-000000"
        )
        assertEquals(3, smsTemplates.size)
        assertTrue(smsTemplates.first().second.contains("৩,০০০"))
    }

    @Test
    fun `verify MainActivity launches and renders without error`() {
        val pm = context.packageManager
        val launchIntent = pm.getLaunchIntentForPackage(context.packageName)
        org.junit.Assert.assertNotNull("Launch intent must not be null", launchIntent)
        org.junit.Assert.assertEquals("com.example.MainActivity", launchIntent?.component?.className)

        org.robolectric.Robolectric.buildActivity(MainActivity::class.java).use { controller ->
            controller.create()
            val activity = controller.get()
            org.junit.Assert.assertNotNull(activity)
        }
    }

    @Test
    fun `verify Google Docs sync JSON markers and unicode quote normalization`() {
        val sampleDoc = """
            Customer Khata Ledger
            
            ===TKPRO_SYNC_JSON_START===
            {
              “app”: “TallyKhata Pro”,
              “version”: 1,
              “exportedAt”: 1700000000000,
              “customers”: [
                {
                  “id”: 101,
                  “name”: “Rahim\u200B”,
                  “phone”: “01700000000”
                }
              ],
              “transactions”: []
            }
            ===TKPRO_SYNC_JSON_END===
            
            End of report.
        """.trimIndent()

        val extracted = com.example.data.GoogleDocsSyncService.extractSyncJsonFromText(sampleDoc)
        org.junit.Assert.assertNotNull(extracted)
        val json = org.json.JSONObject(extracted!!)
        assertEquals("TallyKhata Pro", json.getString("app"))
        assertEquals(1, json.getJSONArray("customers").length())
        assertEquals(0, json.getJSONArray("transactions").length())
        val customer = json.getJSONArray("customers").getJSONObject(0)
        assertEquals(101L, customer.getLong("id"))
        assertEquals("Rahim", customer.getString("name"))
    }

    @Test
    fun `verify smart merge excludes deleted customer and transaction IDs`() = runBlocking {
        val cloudJson = """
            ===TKPRO_SYNC_JSON_START===
            {
              "app": "TallyKhata Pro",
              "customers": [
                {"id": 1, "name": "Active Customer", "phone": "01711111111"},
                {"id": 2, "name": "Deleted Customer", "phone": "01722222222"}
              ],
              "transactions": [
                {"id": 10, "customerId": 1, "type": "DEBIT", "amount": 100.0, "timestamp": 1000},
                {"id": 20, "customerId": 1, "type": "CREDIT", "amount": 50.0, "timestamp": 1001}
              ]
            }
            ===TKPRO_SYNC_JSON_END===
        """.trimIndent()

        // Exclude customer 2 (deleted locally) and transaction 20 (deleted locally)
        val result = repository.importBackupJson(
            jsonContent = cloudJson,
            excludedCustomerIds = setOf(2L),
            excludedTxIds = setOf(20L)
        )
        assertTrue(result.isSuccess)
        val (cCount, txCount) = result.getOrThrow()
        assertEquals(1, cCount)
        assertEquals(1, txCount)

        val customers = repository.getAllCustomersSnapshot()
        assertEquals(1, customers.size)
        assertEquals(1L, customers.first().id)
        assertEquals("Active Customer", customers.first().name)

        val transactions = repository.getAllTransactionsSnapshot()
        assertEquals(1, transactions.size)
        assertEquals(10L, transactions.first().id)
    }

    @Test
    fun `verify SMS templates match balance perspective and do not contradict mode`() {
        // Case 1: Customer owes 5000, Credit mode selected -> must NOT claim shop owes customer 5000
        val creditWhenDue = LocalizationProvider.generateBengaliSmsTemplates(
            customerName = "Karim",
            balanceAmount = 5000.0,
            currencySymbol = "৳",
            businessName = "Bismillah Store",
            ownerPhone = "01700000000",
            isCreditMode = true,
            lastTxAmount = 500.0,
            isBn = false,
            lastTxType = "DEBIT"
        )
        assertEquals(3, creditWhenDue.size)
        assertTrue(creditWhenDue.none { it.second.contains("we owe you") })
        // Since lastTxType was DEBIT while Credit mode is selected, must not claim a 500 payment or fake 5000 payment
        assertTrue(creditWhenDue.first().second.contains("updated"))

        // Case 2: Shop owes customer 2000 (balanceAmount = -2000), Debit mode selected -> must NOT demand due payment
        val debitWhenPayable = LocalizationProvider.generateBengaliSmsTemplates(
            customerName = "রহিম",
            balanceAmount = -2000.0,
            currencySymbol = "৳",
            businessName = "বিসমিল্লাহ স্টোর",
            ownerPhone = "",
            isCreditMode = false,
            lastTxAmount = null,
            isBn = true
        )
        assertEquals(3, debitWhenPayable.size)
        assertTrue(debitWhenPayable.first().second.contains("কোনো বকেয়া নেই"))
        assertTrue(debitWhenPayable.none { it.second.contains("পরিশোধ করার জন্য অনুরোধ") })

        // Case 3: Settled (0) in Debit mode -> must NOT ask to pay 0 overdue
        val settledDebit = LocalizationProvider.generateBengaliSmsTemplates(
            customerName = "Sajib",
            balanceAmount = 0.0,
            currencySymbol = "৳",
            businessName = "Test Shop",
            ownerPhone = "",
            isCreditMode = false,
            isBn = false
        )
        assertTrue(settledDebit.first().second.contains("completely settled"))
        assertTrue(settledDebit.none { it.second.contains("overdue balance") })
    }

    @Test
    fun `verify English localization strings contain no stray Bengali characters`() {
        val en = LocalizationProvider.getStrings("EN", "BUSINESS")
        val enInd = LocalizationProvider.getStrings("EN", "INDIVIDUAL")
        val checkedFields = listOf(
            en.totalReceivable,
            en.totalPayable,
            en.netBalance,
            en.receivableBadge,
            en.payableBadge,
            en.debitTitle,
            en.creditTitle,
            en.filterReceivable,
            en.filterPayable,
            en.languageSectionTitle,
            en.noTransactionsEmptyDesc,
            enInd.addCustomer,
            enInd.selectCustomerLabel
        )
        val hasBengaliChar = Regex("[\\u0980-\\u09FF]")
        checkedFields.forEach { text ->
            org.junit.Assert.assertFalse("Unexpected Bengali in EN string: $text", hasBengaliChar.containsMatchIn(text))
        }
    }

    @Test
    fun `verify pure Bengali timestamps payment methods and share messages without language mixing`() {
        val hasEnglishLetters = Regex("[A-Za-z]")
        val hasBengaliChar = Regex("[\\u0980-\\u09F1\\u09F4-\\u09FF]")

        val ts = 1762943400000L // fixed epoch
        val bnTime = LocalizationProvider.formatTimestamp(ts, useBengaliDigits = true)
        val enTime = LocalizationProvider.formatTimestamp(ts, useBengaliDigits = false)
        org.junit.Assert.assertFalse("Bengali timestamp must not contain English letters: $bnTime", hasEnglishLetters.containsMatchIn(bnTime))
        org.junit.Assert.assertFalse("English timestamp must not contain Bengali letters: $enTime", hasBengaliChar.containsMatchIn(enTime))

        listOf("CASH", "BKASH", "NAGAD", "BANK").forEach { method ->
            val bnLabel = LocalizationProvider.paymentMethodLabel(method, isBn = true)
            val enLabel = LocalizationProvider.paymentMethodLabel(method, isBn = false)
            org.junit.Assert.assertFalse("Bengali method must not contain English letters: $bnLabel", hasEnglishLetters.containsMatchIn(bnLabel))
            org.junit.Assert.assertFalse("English method must not contain Bengali letters: $enLabel", hasBengaliChar.containsMatchIn(enLabel))
        }

        val debitTx = com.example.data.TransactionEntity(
            id = 1L,
            customerId = 10L,
            type = "DEBIT",
            amount = 500.0,
            category = "মুদি পণ্য",
            paymentMethod = "CASH",
            note = "চাল ও ডাল",
            timestamp = ts
        )
        val creditTx = com.example.data.TransactionEntity(
            id = 2L,
            customerId = 10L,
            type = "CREDIT",
            amount = 300.0,
            category = "নগদ আদায়",
            paymentMethod = "BKASH",
            note = "",
            timestamp = ts
        )

        // Bengali DEBIT share with blank businessName must contain zero English letters
        val bnDebitMsg = com.example.ui.screens.buildSingleTransactionShareText(
            transaction = debitTx,
            customerName = "করিম সাহেব",
            balanceAfterTransaction = 1500.0,
            currency = "৳",
            isBn = true,
            businessName = "",
            ownerPhone = "01700000000"
        )
        org.junit.Assert.assertFalse("BN debit share must not mix English: $bnDebitMsg", hasEnglishLetters.containsMatchIn(bnDebitMsg))
        assertTrue(bnDebitMsg.contains("প্রিয় করিম সাহেব"))
        assertTrue(bnDebitMsg.contains("আপনার নামে ৳ ৫০০ নতুন বাকি লেখা হয়েছে"))
        assertTrue(bnDebitMsg.contains("এই লেনদেনের পর আপনার মোট বকেয়া: ৳ ১,৫০০"))

        // Bengali CREDIT share with shop name
        val bnCreditMsg = com.example.ui.screens.buildSingleTransactionShareText(
            transaction = creditTx,
            customerName = "করিম সাহেব",
            balanceAfterTransaction = -200.0,
            currency = "৳",
            isBn = true,
            businessName = "বিসমিল্লাহ স্টোর",
            ownerPhone = ""
        )
        org.junit.Assert.assertFalse("BN credit share must not mix English: $bnCreditMsg", hasEnglishLetters.containsMatchIn(bnCreditMsg))
        assertTrue(bnCreditMsg.contains("বিসমিল্লাহ স্টোর — লেনদেনের রশিদ"))
        assertTrue(bnCreditMsg.contains("জমা হিসেবে গ্রহণ করা হয়েছে"))
        assertTrue(bnCreditMsg.contains("আমাদের কাছে আপনার জমা আছে: ৳ ২০০"))

        // English DEBIT share must contain zero Bengali and zero "TK Pro" when blank
        val enDebitMsg = com.example.ui.screens.buildSingleTransactionShareText(
            transaction = debitTx.copy(category = "Groceries", note = "Rice & oil"),
            customerName = "Karim",
            balanceAfterTransaction = 0.0,
            currency = "৳",
            isBn = false,
            businessName = "Bismillah Store",
            ownerPhone = "01700000000"
        )
        org.junit.Assert.assertFalse("EN share must not contain Bengali: $enDebitMsg", hasBengaliChar.containsMatchIn(enDebitMsg))
        assertTrue(enDebitMsg.contains("Bismillah Store — Transaction Receipt"))
        assertTrue(enDebitMsg.contains("added to your due balance"))
        assertTrue(enDebitMsg.contains("Your account is fully settled after this entry"))

        // Statement share in Bengali must not contain English
        val customerDetail = com.example.data.CustomerWithBalance(
            customer = com.example.data.CustomerEntity(id = 10L, name = "রহিম", phone = "01711111111"),
            totalDebit = 500.0,
            totalCredit = 300.0,
            lastTransactionTime = ts,
            transactionCount = 2
        )
        val bnStatement = com.example.ui.screens.buildCustomerLedgerStatementText(
            customerDetail = customerDetail,
            transactions = listOf(creditTx, debitTx),
            currency = "৳",
            isBn = true,
            businessName = "বিসমিল্লাহ স্টোর",
            ownerPhone = "01700000000"
        )
        org.junit.Assert.assertFalse("BN statement must not mix English: $bnStatement", hasEnglishLetters.containsMatchIn(bnStatement))
        assertTrue(bnStatement.contains("আপনার বর্তমান মোট বকেয়া: ৳ ২০০"))
        assertTrue(bnStatement.contains("বাকি: ৳ ৫০০"))
        assertTrue(bnStatement.contains("জমা: ৳ ৩০০"))
    }

    @Test
    fun `verify category পরিশোধ can be added and payment methods managed cleanly`() = runBlocking {
        // Adding "পরিশোধ" and other previously blocked category names must persist cleanly
        repository.addCustomCategory("পরিশোধ")
        repository.addCustomCategory("বাকিতে বিক্রি")
        var settings = repository.settingsFlow.first()
        assertTrue("পরিশোধ should be present in customCategories", settings.customCategories.contains("পরিশোধ"))
        assertTrue("বাকিতে বিক্রি should be present in customCategories", settings.customCategories.contains("বাকিতে বিক্রি"))

        repository.removeCustomCategory("বাকিতে বিক্রি")
        settings = repository.settingsFlow.first()
        org.junit.Assert.assertFalse(settings.customCategories.contains("বাকিতে বিক্রি"))
        assertTrue(settings.customCategories.contains("পরিশোধ"))

        // Payment methods support default list, adding custom, removing, and clearing all
        assertEquals(listOf("CASH", "BKASH", "NAGAD", "BANK"), settings.paymentMethods)
        repository.addPaymentMethod("Rocket")
        settings = repository.settingsFlow.first()
        assertTrue(settings.paymentMethods.contains("Rocket"))

        repository.removePaymentMethod("BANK")
        settings = repository.settingsFlow.first()
        org.junit.Assert.assertFalse(settings.paymentMethods.contains("BANK"))

        repository.clearAllPaymentMethods()
        settings = repository.settingsFlow.first()
        assertTrue(settings.paymentMethods.isEmpty())

        // Blank payment method must be omitted cleanly in share text
        val txNoMethod = com.example.data.TransactionEntity(
            id = 5L,
            customerId = 10L,
            type = "CREDIT",
            amount = 250.0,
            category = "পরিশোধ",
            paymentMethod = "",
            note = "",
            timestamp = 1762943400000L
        )
        val bnMsg = com.example.ui.screens.buildSingleTransactionShareText(
            transaction = txNoMethod,
            customerName = "করিম",
            balanceAfterTransaction = 0.0,
            currency = "৳",
            isBn = true
        )
        org.junit.Assert.assertFalse("Blank method line should be omitted", bnMsg.contains("পেমেন্টের মাধ্যম"))
        assertTrue(bnMsg.contains("🏷️ খাত: পরিশোধ"))
    }

    @Test
    fun `verify 403 help is hidden when signed in and sync is working`() {
        // Signed in + Doc synced + no error -> hide 403 UI
        org.junit.Assert.assertFalse(
            com.example.ui.screens.shouldShowAccess403Help(
                isSignedIn = true,
                isDocConnected = true,
                isSheetConnected = false,
                lastSyncedAtMillis = 1762943400000L,
                lastSheetSyncedAtMillis = 0L,
                lastSyncError = "",
                isSyncFailedOfflineMode = false
            )
        )

        // Not signed in -> show 403 help
        assertTrue(
            com.example.ui.screens.shouldShowAccess403Help(
                isSignedIn = false,
                isDocConnected = false,
                isSheetConnected = false,
                lastSyncedAtMillis = 0L,
                lastSheetSyncedAtMillis = 0L,
                lastSyncError = "",
                isSyncFailedOfflineMode = false
            )
        )

        // Explicit 403 error -> show 403 help even if signed in
        assertTrue(
            com.example.ui.screens.shouldShowAccess403Help(
                isSignedIn = true,
                isDocConnected = true,
                isSheetConnected = false,
                lastSyncedAtMillis = 1762943400000L,
                lastSheetSyncedAtMillis = 0L,
                lastSyncError = "Google Docs update failed (403): access_denied",
                isSyncFailedOfflineMode = true
            )
        )
    }
}


