package com.example.data

import android.accounts.Account
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import com.example.ui.LocalizationProvider
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.UserRecoverableAuthException
import com.google.android.gms.auth.api.identity.AuthorizationClient
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.abs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

sealed class GoogleDocsAuthOutcome {
    data class Authorized(val accessToken: String) : GoogleDocsAuthOutcome()
    data class NeedsResolution(val intentSender: IntentSender) : GoogleDocsAuthOutcome()
    data class NeedsSignInIntent(val signInIntent: Intent) : GoogleDocsAuthOutcome()
    data class Error(val message: String) : GoogleDocsAuthOutcome()
}

data class GoogleDocsSyncResult(
    val documentId: String,
    val documentUrl: String,
    val syncedAtMillis: Long,
    val customerCount: Int,
    val transactionCount: Int
)

/**
 * Handles Google OAuth2 authorization (`documents` & `drive.file` scopes) via Play Services
 * AuthorizationClient with an automatic GoogleSignIn + GoogleAuthUtil fallback, and performs
 * live two-way sync with a Google Docs document.
 */
object GoogleDocsSyncService {

    private const val SCOPE_DOCS = "https://www.googleapis.com/auth/documents"
    private const val SCOPE_DRIVE_FILE = "https://www.googleapis.com/auth/drive.file"
    private const val OAUTH_SCOPE_STRING = "oauth2:$SCOPE_DOCS $SCOPE_DRIVE_FILE"

    private const val SYNC_START_MARKER = "===TALLYKHATA_SYNC_JSON_START==="
    private const val SYNC_END_MARKER = "===TALLYKHATA_SYNC_JSON_END==="

    private val httpClient = OkHttpClient()
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    private fun buildAuthRequest(account: Account? = null): AuthorizationRequest {
        val builder = AuthorizationRequest.builder()
            .setRequestedScopes(
                listOf(
                    Scope(SCOPE_DOCS),
                    Scope(SCOPE_DRIVE_FILE)
                )
            )
        if (account != null) {
            builder.setAccount(account)
        }
        return builder.build()
    }

    @Suppress("DEPRECATION")
    fun getSignedInEmail(context: Context): String? {
        return GoogleSignIn.getLastSignedInAccount(context)?.email
    }

    @Suppress("DEPRECATION")
    suspend fun signOutAndBuildSignInIntent(context: Context): Intent = withContext(Dispatchers.IO) {
        val gsoBuilder = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(SCOPE_DOCS), Scope(SCOPE_DRIVE_FILE))
        val client = GoogleSignIn.getClient(context, gsoBuilder.build())
        try {
            suspendCancellableCoroutine<Unit> { cont ->
                client.signOut()
                    .addOnCompleteListener {
                        if (cont.isActive) cont.resume(Unit)
                    }
            }
        } catch (_: Exception) {
        }
        client.signInIntent
    }

    @Suppress("DEPRECATION")
    fun buildGoogleSignInIntent(context: Context): Intent {
        val gsoBuilder = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(SCOPE_DOCS), Scope(SCOPE_DRIVE_FILE))
        val client = GoogleSignIn.getClient(context, gsoBuilder.build())
        return client.signInIntent
    }

    @Suppress("DEPRECATION")
    suspend fun authorizeGoogleDocs(context: Context): GoogleDocsAuthOutcome = withContext(Dispatchers.IO) {
        // 1. Check if there is already a last signed-in Google account we can get a token for
        val lastAccount = GoogleSignIn.getLastSignedInAccount(context)?.account
        if (lastAccount != null) {
            try {
                val token = GoogleAuthUtil.getToken(context, lastAccount, OAUTH_SCOPE_STRING)
                if (!token.isNullOrBlank()) {
                    return@withContext GoogleDocsAuthOutcome.Authorized(token)
                }
            } catch (recoverable: UserRecoverableAuthException) {
                val recoverIntent = recoverable.intent
                if (recoverIntent != null) {
                    return@withContext GoogleDocsAuthOutcome.NeedsSignInIntent(recoverIntent)
                }
            } catch (_: Exception) {
                // Fall through to AuthorizationClient with the selected account
            }
        } else {
            // Prompt the user to choose which Google account to use first so it doesn't
            // auto-pick an unauthorized account on multi-account phones
            return@withContext GoogleDocsAuthOutcome.NeedsSignInIntent(buildGoogleSignInIntent(context))
        }

        // 2. Try Play Services Identity AuthorizationClient with the user's chosen account
        val client: AuthorizationClient = Identity.getAuthorizationClient(context)
        val request = buildAuthRequest(lastAccount)
        return@withContext try {
            val result = awaitAuthorization(client, request)
            if (result.hasResolution()) {
                val pendingIntent = result.pendingIntent
                if (pendingIntent != null) {
                    GoogleDocsAuthOutcome.NeedsResolution(pendingIntent.intentSender)
                } else {
                    GoogleDocsAuthOutcome.NeedsSignInIntent(buildGoogleSignInIntent(context))
                }
            } else {
                val token = result.accessToken
                if (!token.isNullOrBlank()) {
                    GoogleDocsAuthOutcome.Authorized(token)
                } else {
                    GoogleDocsAuthOutcome.NeedsSignInIntent(buildGoogleSignInIntent(context))
                }
            }
        } catch (_: Exception) {
            GoogleDocsAuthOutcome.NeedsSignInIntent(buildGoogleSignInIntent(context))
        }
    }

    suspend fun extractTokenFromResolution(
        context: Context,
        data: Intent?
    ): GoogleDocsAuthOutcome = withContext(Dispatchers.IO) {
        val client = Identity.getAuthorizationClient(context)
        if (data != null) {
            try {
                val result = client.getAuthorizationResultFromIntent(data)
                val token = result.accessToken
                if (!token.isNullOrBlank()) {
                    return@withContext GoogleDocsAuthOutcome.Authorized(token)
                }
            } catch (_: Exception) {
                // Continue to silent re-check or GoogleSignIn fallback
            }
        }

        // Check if authorization succeeded on the backend even if result data was empty
        try {
            val checkResult = awaitAuthorization(client, buildAuthRequest())
            if (!checkResult.hasResolution() && !checkResult.accessToken.isNullOrBlank()) {
                return@withContext GoogleDocsAuthOutcome.Authorized(checkResult.accessToken!!)
            }
        } catch (_: Exception) {
        }

        // Fall back to standard GoogleSignIn intent if GIS PendingIntent was cancelled/dismissed
        return@withContext GoogleDocsAuthOutcome.NeedsSignInIntent(buildGoogleSignInIntent(context))
    }

    @Suppress("DEPRECATION")
    suspend fun extractTokenFromGoogleSignInIntent(
        context: Context,
        data: Intent?
    ): GoogleDocsAuthOutcome = withContext(Dispatchers.IO) {
        // 1. Check if data came from UserRecoverableAuthException consent screen
        val existingAccount = GoogleSignIn.getLastSignedInAccount(context)?.account
        if (existingAccount != null && data?.extras?.containsKey("authtoken") == true) {
            val directToken = data.getStringExtra("authtoken")
            if (!directToken.isNullOrBlank()) {
                return@withContext GoogleDocsAuthOutcome.Authorized(directToken)
            }
        }

        // 2. Parse GoogleSignInAccount from sign-in result
        val account: Account? = try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            val signInAccount = task.getResult(ApiException::class.java)
            signInAccount?.account ?: existingAccount
        } catch (e: ApiException) {
            existingAccount ?: return@withContext GoogleDocsAuthOutcome.Error(
                "Google Sign-In failed (code ${e.statusCode}). Please ensure a Google account is signed in on this device."
            )
        } catch (e: Exception) {
            existingAccount ?: return@withContext GoogleDocsAuthOutcome.Error(
                e.localizedMessage ?: "Google Sign-In was not completed"
            )
        }

        if (account == null) {
            return@withContext GoogleDocsAuthOutcome.Error(
                "No Google account selected. Please sign in with a Google account on your device."
            )
        }

        // 3. Exchange account for OAuth2 access token with Docs + Drive scopes
        return@withContext try {
            val token = GoogleAuthUtil.getToken(context, account, OAUTH_SCOPE_STRING)
            if (!token.isNullOrBlank()) {
                GoogleDocsAuthOutcome.Authorized(token)
            } else {
                GoogleDocsAuthOutcome.Error("Failed to obtain Google Docs access token")
            }
        } catch (recoverable: UserRecoverableAuthException) {
            val recoverIntent = recoverable.intent
            if (recoverIntent != null) {
                GoogleDocsAuthOutcome.NeedsSignInIntent(recoverIntent)
            } else {
                GoogleDocsAuthOutcome.Error("Additional Google Docs permission required")
            }
        } catch (e: Exception) {
            GoogleDocsAuthOutcome.Error(e.localizedMessage ?: "Failed to obtain access token")
        }
    }

    private suspend fun awaitAuthorization(
        client: AuthorizationClient,
        request: AuthorizationRequest
    ): AuthorizationResult = suspendCancellableCoroutine { cont ->
        client.authorize(request)
            .addOnSuccessListener { result ->
                if (cont.isActive) cont.resume(result)
            }
            .addOnFailureListener { err ->
                if (cont.isActive) cont.resumeWithException(err)
            }
    }

    /**
     * Pushes the current ledger (formatted human-readable report + machine-readable JSON sync block)
     * to a live Google Doc. If [existingDocId] is blank or no longer accessible, creates a new Google Doc.
     */
    suspend fun syncLedgerToGoogleDoc(
        accessToken: String,
        existingDocId: String?,
        settings: AppSettings,
        customers: List<CustomerEntity>,
        transactions: List<TransactionEntity>,
        rawBackupJson: String
    ): Result<GoogleDocsSyncResult> = withContext(Dispatchers.IO) {
        try {
            val docTitle = "${settings.businessName.ifBlank { "TK Pro" }} - Live Ledger Khata"

            // 1. Resolve or create target Google Doc
            var targetDocId = existingDocId?.trim().orEmpty()
            var endIndex = 1

            if (targetDocId.isNotEmpty()) {
                val getDocInfo = fetchDocumentEndIndex(accessToken, targetDocId)
                if (getDocInfo != null) {
                    endIndex = getDocInfo
                } else {
                    // Document deleted or inaccessible; create a fresh one
                    targetDocId = ""
                }
            }

            if (targetDocId.isEmpty()) {
                val createdId = createNewGoogleDoc(accessToken, docTitle)
                    ?: return@withContext Result.failure(
                        IllegalStateException("Could not create Google Doc. Verify Google Docs API is enabled.")
                    )
                targetDocId = createdId
                endIndex = 1
            }

            // 2. Build formatted ledger text + embedded JSON sync block
            val docContent = buildFormattedGoogleDocContent(
                settings = settings,
                customers = customers,
                transactions = transactions,
                rawBackupJson = rawBackupJson
            )

            // 3. Build batchUpdate requests: clear existing content if any, then insert fresh ledger
            val requestsArray = JSONArray()
            if (endIndex > 2) {
                requestsArray.put(
                    JSONObject().put(
                        "deleteContentRange",
                        JSONObject().put(
                            "range",
                            JSONObject()
                                .put("startIndex", 1)
                                .put("endIndex", endIndex - 1)
                        )
                    )
                )
            }

            requestsArray.put(
                JSONObject().put(
                    "insertText",
                    JSONObject()
                        .put("location", JSONObject().put("index", 1))
                        .put("text", docContent)
                )
            )

            val batchBody = JSONObject().put("requests", requestsArray).toString()
            val updateReq = Request.Builder()
                .url("https://docs.googleapis.com/v1/documents/$targetDocId:batchUpdate")
                .addHeader("Authorization", "Bearer $accessToken")
                .post(batchBody.toRequestBody(jsonMediaType))
                .build()

            httpClient.newCall(updateReq).execute().use { resp ->
                if (!resp.isSuccessful) {
                    val errBody = resp.body?.string().orEmpty()
                    return@withContext Result.failure(
                        IllegalStateException("Google Docs update failed (${resp.code}): $errBody")
                    )
                }
            }

            val now = System.currentTimeMillis()
            Result.success(
                GoogleDocsSyncResult(
                    documentId = targetDocId,
                    documentUrl = "https://docs.google.com/document/d/$targetDocId/edit",
                    syncedAtMillis = now,
                    customerCount = customers.size,
                    transactionCount = transactions.size
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Reads the live Google Doc and extracts the embedded JSON sync block to restore customers,
     * transactions, and business settings into the app.
     */
    suspend fun pullLedgerFromGoogleDoc(
        accessToken: String,
        documentId: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val cleanId = extractDocIdFromInput(documentId)
            if (cleanId.isEmpty()) {
                return@withContext Result.failure(IllegalArgumentException("Invalid Google Doc ID"))
            }

            val req = Request.Builder()
                .url("https://docs.googleapis.com/v1/documents/$cleanId")
                .addHeader("Authorization", "Bearer $accessToken")
                .get()
                .build()

            httpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) {
                    val errBody = resp.body?.string().orEmpty()
                    return@withContext Result.failure(
                        IllegalStateException("Could not read Google Doc (${resp.code}): $errBody")
                    )
                }

                val bodyStr = resp.body?.string().orEmpty()
                val docJson = JSONObject(bodyStr)
                val fullText = extractPlainTextFromGoogleDoc(docJson)

                val startIdx = fullText.indexOf(SYNC_START_MARKER)
                val endIdx = fullText.indexOf(SYNC_END_MARKER)

                if (startIdx >= 0 && endIdx > startIdx) {
                    val jsonPayload = fullText
                        .substring(startIdx + SYNC_START_MARKER.length, endIdx)
                        .trim()
                    return@withContext Result.success(jsonPayload)
                } else {
                    return@withContext Result.failure(
                        IllegalStateException("No TK Pro sync data found in this Google Doc.")
                    )
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun extractDocIdFromInput(rawInput: String): String {
        val trimmed = rawInput.trim()
        val regex = Regex("/document/d/([a-zA-Z0-9-_]+)")
        val match = regex.find(trimmed)
        return match?.groupValues?.getOrNull(1) ?: trimmed
    }

    private fun createNewGoogleDoc(accessToken: String, title: String): String? {
        val payload = JSONObject().put("title", title).toString()
        val req = Request.Builder()
            .url("https://docs.googleapis.com/v1/documents")
            .addHeader("Authorization", "Bearer $accessToken")
            .post(payload.toRequestBody(jsonMediaType))
            .build()

        httpClient.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) return null
            val str = resp.body?.string() ?: return null
            val json = JSONObject(str)
            return json.optString("documentId").takeIf { it.isNotBlank() }
        }
    }

    private fun fetchDocumentEndIndex(accessToken: String, docId: String): Int? {
        val req = Request.Builder()
            .url("https://docs.googleapis.com/v1/documents/$docId")
            .addHeader("Authorization", "Bearer $accessToken")
            .get()
            .build()

        httpClient.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) return null
            val str = resp.body?.string() ?: return null
            val json = JSONObject(str)
            val content = json.optJSONObject("body")?.optJSONArray("content") ?: return 1
            if (content.length() == 0) return 1
            val lastElem = content.getJSONObject(content.length() - 1)
            return lastElem.optInt("endIndex", 1)
        }
    }

    private fun extractPlainTextFromGoogleDoc(docJson: JSONObject): String {
        val sb = StringBuilder()
        val content = docJson.optJSONObject("body")?.optJSONArray("content") ?: return ""
        for (i in 0 until content.length()) {
            val structuralElement = content.getJSONObject(i)
            val paragraph = structuralElement.optJSONObject("paragraph") ?: continue
            val elements = paragraph.optJSONArray("elements") ?: continue
            for (j in 0 until elements.length()) {
                val elem = elements.getJSONObject(j)
                val textRun = elem.optJSONObject("textRun") ?: continue
                val text = textRun.optString("content", "")
                sb.append(text)
            }
        }
        return sb.toString()
    }

    private fun buildFormattedGoogleDocContent(
        settings: AppSettings,
        customers: List<CustomerEntity>,
        transactions: List<TransactionEntity>,
        rawBackupJson: String
    ): String {
        val isBn = settings.language != "EN"
        val currency = settings.currencySymbol.ifBlank { "৳" }
        val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
        val nowStr = dateFormat.format(Date(System.currentTimeMillis()))

        val txByCustomer = transactions.groupBy { it.customerId }
        var totalReceivable = 0.0
        var totalPayable = 0.0

        val customerLines = StringBuilder()
        customers.forEachIndexed { index, c ->
            val cTxs = txByCustomer[c.id].orEmpty()
                .sortedWith(compareBy<TransactionEntity> { it.timestamp }.thenBy { it.id })
            val debitSum = cTxs.filter { it.type == "DEBIT" }.sumOf { it.amount }
            val creditSum = cTxs.filter { it.type == "CREDIT" }.sumOf { it.amount }
            val net = debitSum - creditSum
            if (net > 0.001) totalReceivable += net
            if (net < -0.001) totalPayable += abs(net)

            val statusLabel = when {
                net > 0.001 -> if (isBn) "পাবেন (Receivable)" else "Receivable"
                net < -0.001 -> if (isBn) "দেবেন (Payable)" else "Payable"
                else -> if (isBn) "পরিশোধিত (Settled)" else "Settled"
            }

            customerLines.append("${index + 1}. ${c.name} (${c.phone.ifBlank { "N/A" }})\n")
            customerLines.append(
                "   • Balance: ${LocalizationProvider.formatAmount(abs(net), currency, isBn)} [$statusLabel]\n"
            )

            if (cTxs.isNotEmpty()) {
                var running = 0.0
                cTxs.forEach { t ->
                    running += if (t.type == "DEBIT") t.amount else -t.amount
                    val tTypeLabel = if (t.type == "DEBIT") {
                        if (isBn) "টাকা পাব / দিয়েছি (Debit)" else "Debit"
                    } else {
                        if (isBn) "টাকা দেব / পেয়েছি (Credit)" else "Credit"
                    }
                    val tDate = dateFormat.format(Date(t.timestamp))
                    val runStatus = when {
                        running > 0.001 -> if (isBn) "পাবেন" else "Receivable"
                        running < -0.001 -> if (isBn) "দেবেন" else "Payable"
                        else -> if (isBn) "পরিশোধিত" else "Settled"
                    }
                    customerLines.append(
                        "     - [$tDate] $tTypeLabel: ${
                            LocalizationProvider.formatAmount(t.amount, currency, isBn)
                        } | (${t.category} • ${t.paymentMethod}) -> Balance After: ${
                            LocalizationProvider.formatAmount(abs(running), currency, isBn)
                        } ($runStatus)${if (t.note.isNotBlank()) " | Note: ${t.note}" else ""}\n"
                    )
                }
            }
            customerLines.append("\n")
        }

        val netOverall = totalReceivable - totalPayable

        return buildString {
            append("====================================================\n")
            append("   ${settings.businessName.ifBlank { "TK Pro" }} — ডিজিটাল হিসাব ও বাকি খাতা\n")
            if (settings.ownerPhone.isNotBlank()) {
                append("   Phone: ${settings.ownerPhone}\n")
            }
            append("   Last Synced: $nowStr\n")
            append("====================================================\n\n")
            append("📊 LEDGER SUMMARY (সারসংক্ষেপ)\n")
            append("• মোট পাবেন (Total Receivable): ${LocalizationProvider.formatAmount(totalReceivable, currency, isBn)}\n")
            append("• মোট দেবেন (Total Payable): ${LocalizationProvider.formatAmount(totalPayable, currency, isBn)}\n")
            append("• মোট ব্যালেন্স (Net Balance): ${LocalizationProvider.formatAmount(netOverall, currency, isBn)}\n")
            append("• Total Customers/Parties: ${customers.size} | Total Transactions: ${transactions.size}\n\n")
            append("----------------------------------------------------\n")
            append("👥 CUSTOMER LEDGERS & RUNNING BALANCES (কাস্টমার ও লেনদেন বিবরণী)\n")
            append("----------------------------------------------------\n\n")
            if (customers.isEmpty()) {
                append("(No customers recorded yet)\n\n")
            } else {
                append(customerLines.toString())
            }
            append("\n----------------------------------------------------\n")
            append("🔒 MACHINE-READABLE SYNC DATA (DO NOT EDIT BELOW THIS LINE)\n")
            append("$SYNC_START_MARKER\n")
            append(rawBackupJson)
            append("\n$SYNC_END_MARKER\n")
        }
    }
}
