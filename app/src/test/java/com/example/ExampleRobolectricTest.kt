package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppDatabase
import com.example.data.AppSettings
import com.example.data.LedgerRepository
import com.example.ui.LocalizationProvider
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
}

