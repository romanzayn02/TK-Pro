package com.example.data

import android.accounts.Account
import android.accounts.AccountManager
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
import java.net.URLEncoder
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

data class DiscoveredCloudBackup(
    val id: String,
    val isSpreadsheet: Boolean,
    val jsonPayload: String
)

/**
 * Handles Google OAuth2 authorization (`documents`, `spreadsheets`, & `drive.file` scopes) via
 * Android AccountManager + GoogleAuthUtil + Play Services Identity AuthorizationClient, and performs
 * live two-way sync and auto-discovery restore with Google Docs and Google Sheets.
 */
object GoogleDocsSyncService {

    private const val SCOPE_DOCS = "https://www.googleapis.com/auth/documents"
    private const val SCOPE_SPREADSHEETS = "https://www.googleapis.com/auth/spreadsheets"
    private const val SCOPE_DRIVE_FILE = "https://www.googleapis.com/auth/drive.file"
    private const val OAUTH_SCOPE_STRING = "oauth2:$SCOPE_DOCS $SCOPE_SPREADSHEETS $SCOPE_DRIVE_FILE"

    const val SYNC_START_MARKER = "===TALLYKHATA_SYNC_JSON_START==="
    const val SYNC_END_MARKER = "===TALLYKHATA_SYNC_JSON_END==="
    const val SHEET_SYNC_ROW_MARKER = "===TALLYKHATA_SHEET_SYNC_JSON==="

    private const val AUTH_PREFS_NAME = "tkpro_google_auth_prefs"
    private const val KEY_SIGNED_IN_EMAIL = "signed_in_google_email"

    private val httpClient = OkHttpClient()
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    private fun buildAuthRequest(account: Account? = null): AuthorizationRequest {
        val builder = AuthorizationRequest.builder()
            .setRequestedScopes(
                listOf(
                    Scope(SCOPE_DOCS),
                    Scope(SCOPE_SPREADSHEETS),
                    Scope(SCOPE_DRIVE_FILE)
                )
            )
        if (account != null) {
            builder.setAccount(account)
        }
        return builder.build()
    }

    private fun saveSignedInEmail(context: Context, email: String?) {
        val prefs = context.getSharedPreferences(AUTH_PREFS_NAME, Context.MODE_PRIVATE)
        if (email.isNullOrBlank()) {
            prefs.edit().remove(KEY_SIGNED_IN_EMAIL).apply()
        } else {
            prefs.edit().putString(KEY_SIGNED_IN_EMAIL, email.trim()).apply()
        }
    }

    @Suppress("DEPRECATION")
    fun getSignedInEmail(context: Context): String? {
        val prefs = context.getSharedPreferences(AUTH_PREFS_NAME, Context.MODE_PRIVATE)
        val saved = prefs.getString(KEY_SIGNED_IN_EMAIL, null)?.trim()
        if (!saved.isNullOrEmpty()) {
            return saved
        }
        val gsoEmail = GoogleSignIn.getLastSignedInAccount(context)?.email?.trim()
        if (!gsoEmail.isNullOrEmpty()) {
            saveSignedInEmail(context, gsoEmail)
            return gsoEmail
        }
        return null
    }

    @Suppress("DEPRECATION")
    private fun getSavedGoogleAccount(context: Context): Account? {
        val savedEmail = getSignedInEmail(context)
        if (!savedEmail.isNullOrBlank()) {
            return Account(savedEmail, "com.google")
        }
        val lastAccount = GoogleSignIn.getLastSignedInAccount(context)?.account
        if (lastAccount != null && !lastAccount.name.isNullOrBlank()) {
            saveSignedInEmail(context, lastAccount.name)
            return lastAccount
        }
        return null
    }

    /**
     * Uses Android's native system Google Account Picker (`AccountManager.newChooseAccountIntent`)
     * so account selection never fails with `GoogleSignIn` ApiException code 10.
     */
    @Suppress("DEPRECATION")
    fun buildGoogleSignInIntent(context: Context): Intent {
        return try {
            AccountManager.newChooseAccountIntent(
                null,
                null,
                arrayOf("com.google"),
                null,
                null,
                null,
                null
            )
        } catch (_: Exception) {
            val gsoBuilder = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
            GoogleSignIn.getClient(context, gsoBuilder.build()).signInIntent
        }
    }

    @Suppress("DEPRECATION")
    suspend fun signOutAndBuildSignInIntent(context: Context): Intent = withContext(Dispatchers.IO) {
        try {
            val gsoBuilder = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
            val client = GoogleSignIn.getClient(context, gsoBuilder.build())
            suspendCancellableCoroutine<Unit> { cont ->
                client.signOut()
                    .addOnCompleteListener {
                        if (cont.isActive) cont.resume(Unit)
                    }
            }
        } catch (_: Exception) {
        }
        buildGoogleSignInIntent(context)
    }

    private suspend fun requestTokenForAccount(
        context: Context,
        account: Account
    ): GoogleDocsAuthOutcome = withContext(Dispatchers.IO) {
        saveSignedInEmail(context, account.name)

        // 1. Primary path: GoogleAuthUtil with explicit selected Account
        try {
            val token = GoogleAuthUtil.getToken(context, account, OAUTH_SCOPE_STRING)
            if (!token.isNullOrBlank()) {
                return@withContext GoogleDocsAuthOutcome.Authorized(token)
            }
        } catch (recoverable: UserRecoverableAuthException) {
            val recoverIntent = recoverable.intent
            if (recoverIntent != null) {
                return@withContext GoogleDocsAuthOutcome.NeedsSignInIntent(recoverIntent)
            }
        } catch (_: Exception) {
            // Fall through to GIS AuthorizationClient
        }

        // 2. Fallback path: Play Services Identity AuthorizationClient
        return@withContext try {
            val client: AuthorizationClient = Identity.getAuthorizationClient(context)
            val result = awaitAuthorization(client, buildAuthRequest(account))
            if (result.hasResolution()) {
                val pendingIntent = result.pendingIntent
                if (pendingIntent != null) {
                    GoogleDocsAuthOutcome.NeedsResolution(pendingIntent.intentSender)
                } else {
                    GoogleDocsAuthOutcome.Error("Google Cloud permission consent required.")
                }
            } else {
                val token = result.accessToken
                if (!token.isNullOrBlank()) {
                    GoogleDocsAuthOutcome.Authorized(token)
                } else {
                    GoogleDocsAuthOutcome.Error("Could not obtain Google Cloud access token.")
                }
            }
        } catch (e: Exception) {
            GoogleDocsAuthOutcome.Error(
                e.localizedMessage ?: "Google Cloud authorization failed."
            )
        }
    }

    suspend fun authorizeGoogleDocs(context: Context): GoogleDocsAuthOutcome = withContext(Dispatchers.IO) {
        // 1. Check if we already have a saved or signed-in Google account
        val savedAccount = getSavedGoogleAccount(context)
        if (savedAccount != null) {
            return@withContext requestTokenForAccount(context, savedAccount)
        }

        // 2. Try silent/GIS AuthorizationClient if the user already authorized this app on device
        try {
            val client: AuthorizationClient = Identity.getAuthorizationClient(context)
            val result = awaitAuthorization(client, buildAuthRequest(null))
            if (!result.hasResolution() && !result.accessToken.isNullOrBlank()) {
                return@withContext GoogleDocsAuthOutcome.Authorized(result.accessToken!!)
            }
        } catch (_: Exception) {
        }

        // 3. Otherwise prompt the user to pick their Google account via system AccountPicker
        return@withContext GoogleDocsAuthOutcome.NeedsSignInIntent(buildGoogleSignInIntent(context))
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
            }
        }

        val savedAccount = getSavedGoogleAccount(context)
        if (savedAccount != null) {
            try {
                val token = GoogleAuthUtil.getToken(context, savedAccount, OAUTH_SCOPE_STRING)
                if (!token.isNullOrBlank()) {
                    return@withContext GoogleDocsAuthOutcome.Authorized(token)
                }
            } catch (_: Exception) {
            }
        }

        try {
            val checkResult = awaitAuthorization(client, buildAuthRequest(savedAccount))
            if (!checkResult.hasResolution() && !checkResult.accessToken.isNullOrBlank()) {
                return@withContext GoogleDocsAuthOutcome.Authorized(checkResult.accessToken!!)
            }
        } catch (_: Exception) {
        }

        return@withContext GoogleDocsAuthOutcome.NeedsSignInIntent(buildGoogleSignInIntent(context))
    }

    @Suppress("DEPRECATION")
    suspend fun extractTokenFromGoogleSignInIntent(
        context: Context,
        data: Intent?
    ): GoogleDocsAuthOutcome = withContext(Dispatchers.IO) {
        // 1. Check if data returned directly from UserRecoverableAuthException consent screen
        if (data?.extras?.containsKey("authtoken") == true) {
            val directToken = data.getStringExtra("authtoken")
            val chosenEmail = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME)
            if (!chosenEmail.isNullOrBlank()) {
                saveSignedInEmail(context, chosenEmail)
            }
            if (!directToken.isNullOrBlank()) {
                return@withContext GoogleDocsAuthOutcome.Authorized(directToken)
            }
        }

        // 2. Check if data returned from Android's native AccountManager.newChooseAccountIntent
        val pickedAccountName = data?.getStringExtra(AccountManager.KEY_ACCOUNT_NAME)?.trim()
        if (!pickedAccountName.isNullOrEmpty()) {
            val pickedAccountType = data.getStringExtra(AccountManager.KEY_ACCOUNT_TYPE)
                ?.takeIf { it.isNotBlank() } ?: "com.google"
            saveSignedInEmail(context, pickedAccountName)
            val account = Account(pickedAccountName, pickedAccountType)
            return@withContext requestTokenForAccount(context, account)
        }

        // 3. Check if data returned from GoogleSignIn intent
        val existingAccount = getSavedGoogleAccount(context)
        val signInAccount: Account? = if (data != null) {
            try {
                val task = GoogleSignIn.getSignedInAccountFromIntent(data)
                val gAccount = task.getResult(ApiException::class.java)
                gAccount?.account?.also { acc ->
                    if (!acc.name.isNullOrBlank()) {
                        saveSignedInEmail(context, acc.name)
                    }
                } ?: existingAccount
            } catch (_: Exception) {
                existingAccount
            }
        } else {
            existingAccount
        }

        if (signInAccount != null) {
            return@withContext requestTokenForAccount(context, signInAccount)
        }

        // 4. Last fallback: Try Identity.getAuthorizationClient directly
        return@withContext try {
            val client = Identity.getAuthorizationClient(context)
            val result = awaitAuthorization(client, buildAuthRequest(null))
            if (result.hasResolution() && result.pendingIntent != null) {
                GoogleDocsAuthOutcome.NeedsResolution(result.pendingIntent!!.intentSender)
            } else if (!result.accessToken.isNullOrBlank()) {
                GoogleDocsAuthOutcome.Authorized(result.accessToken!!)
            } else {
                GoogleDocsAuthOutcome.Error(
                    "No Google account selected. Please select or add a Google account on your device."
                )
            }
        } catch (_: Exception) {
            GoogleDocsAuthOutcome.Error(
                "Google Sign-In was cancelled or no Google account is signed in on this device."
            )
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
     * Searches the user's Google Drive for the most recently modified Google Doc created by TK Pro / TallyKhata
     * that contains valid embedded sync JSON.
     */
    suspend fun findLatestBackupDocId(accessToken: String): String? = withContext(Dispatchers.IO) {
        try {
            val query = URLEncoder.encode(
                "mimeType='application/vnd.google-apps.document' and trashed=false",
                "UTF-8"
            )
            val url = "https://www.googleapis.com/drive/v3/files?q=$query&orderBy=modifiedTime%20desc&pageSize=15&fields=files(id,name,modifiedTime)"
            val req = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $accessToken")
                .get()
                .build()

            val candidateIds = mutableListOf<String>()
            httpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext null
                val bodyStr = resp.body?.string().orEmpty()
                val files = JSONObject(bodyStr).optJSONArray("files") ?: return@withContext null
                // Prioritize docs with Ledger/Khata/TK Pro in title, then check all recent docs
                val priorityIds = mutableListOf<String>()
                val otherIds = mutableListOf<String>()
                for (i in 0 until files.length()) {
                    val fileObj = files.optJSONObject(i) ?: continue
                    val id = fileObj.optString("id").trim()
                    val name = fileObj.optString("name", "").lowercase()
                    if (id.isNotEmpty()) {
                        if (name.contains("ledger") || name.contains("khata") || name.contains("tk pro") || name.contains("tally")) {
                            priorityIds.add(id)
                        } else {
                            otherIds.add(id)
                        }
                    }
                }
                candidateIds.addAll(priorityIds)
                candidateIds.addAll(otherIds)
            }

            for (docId in candidateIds) {
                val pullRes = pullLedgerFromGoogleDoc(accessToken, docId)
                if (pullRes.isSuccess) {
                    return@withContext docId
                }
            }
            null
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Searches the user's Google Drive for the most recently modified Google Sheet created by TK Pro / TallyKhata
     * that contains valid embedded sync JSON.
     */
    suspend fun findLatestBackupSheetId(accessToken: String): String? = withContext(Dispatchers.IO) {
        try {
            val query = URLEncoder.encode(
                "mimeType='application/vnd.google-apps.spreadsheet' and trashed=false",
                "UTF-8"
            )
            val url = "https://www.googleapis.com/drive/v3/files?q=$query&orderBy=modifiedTime%20desc&pageSize=15&fields=files(id,name,modifiedTime)"
            val req = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $accessToken")
                .get()
                .build()

            val candidateIds = mutableListOf<String>()
            httpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext null
                val bodyStr = resp.body?.string().orEmpty()
                val files = JSONObject(bodyStr).optJSONArray("files") ?: return@withContext null
                val priorityIds = mutableListOf<String>()
                val otherIds = mutableListOf<String>()
                for (i in 0 until files.length()) {
                    val fileObj = files.optJSONObject(i) ?: continue
                    val id = fileObj.optString("id").trim()
                    val name = fileObj.optString("name", "").lowercase()
                    if (id.isNotEmpty()) {
                        if (name.contains("ledger") || name.contains("khata") || name.contains("tk pro") || name.contains("tally") || name.contains("sheet")) {
                            priorityIds.add(id)
                        } else {
                            otherIds.add(id)
                        }
                    }
                }
                candidateIds.addAll(priorityIds)
                candidateIds.addAll(otherIds)
            }

            for (sheetId in candidateIds) {
                val pullRes = pullLedgerFromGoogleSheet(accessToken, sheetId)
                if (pullRes.isSuccess) {
                    return@withContext sheetId
                }
            }
            null
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Automatically finds the newest valid cloud backup across Google Sheets and Google Docs
     * and returns its ID, type, and JSON payload.
     */
    suspend fun findAndPullLatestCloudBackup(
        accessToken: String,
        preferredDocId: String? = null,
        preferredSheetId: String? = null
    ): Result<DiscoveredCloudBackup> = withContext(Dispatchers.IO) {
        try {
            val cleanSheetId = preferredSheetId?.let { extractSheetIdFromInput(it) }.orEmpty()
            if (cleanSheetId.isNotEmpty()) {
                val res = pullLedgerFromGoogleSheet(accessToken, cleanSheetId)
                if (res.isSuccess) {
                    return@withContext Result.success(
                        DiscoveredCloudBackup(
                            id = cleanSheetId,
                            isSpreadsheet = true,
                            jsonPayload = res.getOrThrow()
                        )
                    )
                }
            }

            val cleanDocId = preferredDocId?.let { extractDocIdFromInput(it) }.orEmpty()
            if (cleanDocId.isNotEmpty()) {
                val res = pullLedgerFromGoogleDoc(accessToken, cleanDocId)
                if (res.isSuccess) {
                    return@withContext Result.success(
                        DiscoveredCloudBackup(
                            id = cleanDocId,
                            isSpreadsheet = false,
                            jsonPayload = res.getOrThrow()
                        )
                    )
                }
            }

            // Query Google Drive for all non-trashed Docs and Spreadsheets ordered by modifiedTime desc
            val query = URLEncoder.encode(
                "(mimeType='application/vnd.google-apps.spreadsheet' or mimeType='application/vnd.google-apps.document') and trashed=false",
                "UTF-8"
            )
            val url = "https://www.googleapis.com/drive/v3/files?q=$query&orderBy=modifiedTime%20desc&pageSize=20&fields=files(id,name,mimeType,modifiedTime)"
            val req = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $accessToken")
                .get()
                .build()

            data class CandidateFile(val id: String, val isSheet: Boolean, val isPriority: Boolean)
            val candidates = mutableListOf<CandidateFile>()

            httpClient.newCall(req).execute().use { resp ->
                if (resp.isSuccessful) {
                    val bodyStr = resp.body?.string().orEmpty()
                    val files = JSONObject(bodyStr).optJSONArray("files")
                    if (files != null) {
                        for (i in 0 until files.length()) {
                            val f = files.optJSONObject(i) ?: continue
                            val id = f.optString("id").trim()
                            val mime = f.optString("mimeType")
                            val name = f.optString("name", "").lowercase()
                            if (id.isNotEmpty()) {
                                val isSheet = mime == "application/vnd.google-apps.spreadsheet"
                                val isPriority = name.contains("ledger") ||
                                    name.contains("khata") ||
                                    name.contains("tk pro") ||
                                    name.contains("tally")
                                candidates.add(CandidateFile(id, isSheet, isPriority))
                            }
                        }
                    }
                }
            }

            val sortedCandidates = candidates.sortedByDescending { it.isPriority }
            for (cand in sortedCandidates) {
                if (cand.isSheet) {
                    val res = pullLedgerFromGoogleSheet(accessToken, cand.id)
                    if (res.isSuccess) {
                        return@withContext Result.success(
                            DiscoveredCloudBackup(
                                id = cand.id,
                                isSpreadsheet = true,
                                jsonPayload = res.getOrThrow()
                            )
                        )
                    }
                } else {
                    val res = pullLedgerFromGoogleDoc(accessToken, cand.id)
                    if (res.isSuccess) {
                        return@withContext Result.success(
                            DiscoveredCloudBackup(
                                id = cand.id,
                                isSpreadsheet = false,
                                jsonPayload = res.getOrThrow()
                            )
                        )
                    }
                }
            }

            Result.failure(
                IllegalStateException("No TK Pro cloud backup found in this Google account's Drive.")
            )
        } catch (e: Exception) {
            Result.failure(e)
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
            var targetDocId = existingDocId?.trim()?.let { extractDocIdFromInput(it) }.orEmpty()
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
                // Check if a backup Google Doc already exists in Drive before creating a duplicate
                val existingInDrive = findLatestBackupDocId(accessToken)
                if (!existingInDrive.isNullOrBlank()) {
                    val existingEndIdx = fetchDocumentEndIndex(accessToken, existingInDrive)
                    if (existingEndIdx != null) {
                        targetDocId = existingInDrive
                        endIndex = existingEndIdx
                    }
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
        val docRegex = Regex("/document/d/([a-zA-Z0-9-_]+)")
        docRegex.find(trimmed)?.groupValues?.getOrNull(1)?.let { return it }
        val genericIdRegex = Regex("id=([a-zA-Z0-9-_]+)")
        genericIdRegex.find(trimmed)?.groupValues?.getOrNull(1)?.let { return it }
        return trimmed
    }

    fun extractSheetIdFromInput(rawInput: String): String {
        val trimmed = rawInput.trim()
        val sheetRegex = Regex("/spreadsheets/d/([a-zA-Z0-9-_]+)")
        sheetRegex.find(trimmed)?.groupValues?.getOrNull(1)?.let { return it }
        val genericIdRegex = Regex("id=([a-zA-Z0-9-_]+)")
        genericIdRegex.find(trimmed)?.groupValues?.getOrNull(1)?.let { return it }
        return trimmed
    }

    /**
     * Pushes the current ledger (structured customer summary rows + transaction rows + embedded sync row)
     * to a live Google Sheet. If [existingSheetId] is blank or inaccessible, reuses an existing sheet in Drive or creates a new one.
     */
    suspend fun syncLedgerToGoogleSheet(
        accessToken: String,
        existingSheetId: String?,
        settings: AppSettings,
        customers: List<CustomerEntity>,
        transactions: List<TransactionEntity>,
        rawBackupJson: String
    ): Result<GoogleDocsSyncResult> = withContext(Dispatchers.IO) {
        try {
            val sheetTitle = "${settings.businessName.ifBlank { "TK Pro" }} - Live Ledger Sheet"
            var targetSheetId = existingSheetId?.trim()?.let { extractSheetIdFromInput(it) }.orEmpty()

            if (targetSheetId.isNotEmpty()) {
                if (!checkSpreadsheetAccessible(accessToken, targetSheetId)) {
                    targetSheetId = ""
                }
            }

            if (targetSheetId.isEmpty()) {
                val existingInDrive = findLatestBackupSheetId(accessToken)
                if (!existingInDrive.isNullOrBlank() && checkSpreadsheetAccessible(accessToken, existingInDrive)) {
                    targetSheetId = existingInDrive
                }
            }

            if (targetSheetId.isEmpty()) {
                val createdId = createNewGoogleSheet(accessToken, sheetTitle)
                    ?: return@withContext Result.failure(
                        IllegalStateException("Could not create Google Sheet. Verify Google Sheets API is enabled.")
                    )
                targetSheetId = createdId
            }

            // Clear existing sheet values in A1:Z5000
            val clearReq = Request.Builder()
                .url("https://sheets.googleapis.com/v4/spreadsheets/$targetSheetId/values/A1:Z5000:clear")
                .addHeader("Authorization", "Bearer $accessToken")
                .post("{}".toRequestBody(jsonMediaType))
                .build()
            httpClient.newCall(clearReq).execute().close()

            // Build 2D values array
            val valuesArray = buildGoogleSheetValuesArray(
                settings = settings,
                customers = customers,
                transactions = transactions,
                rawBackupJson = rawBackupJson
            )
            val bodyJson = JSONObject()
                .put("majorDimension", "ROWS")
                .put("values", valuesArray)
                .toString()

            val updateReq = Request.Builder()
                .url("https://sheets.googleapis.com/v4/spreadsheets/$targetSheetId/values/A1?valueInputOption=USER_ENTERED")
                .addHeader("Authorization", "Bearer $accessToken")
                .put(bodyJson.toRequestBody(jsonMediaType))
                .build()

            httpClient.newCall(updateReq).execute().use { resp ->
                if (!resp.isSuccessful) {
                    val errBody = resp.body?.string().orEmpty()
                    return@withContext Result.failure(
                        IllegalStateException("Google Sheets update failed (${resp.code}): $errBody")
                    )
                }
            }

            val now = System.currentTimeMillis()
            Result.success(
                GoogleDocsSyncResult(
                    documentId = targetSheetId,
                    documentUrl = "https://docs.google.com/spreadsheets/d/$targetSheetId/edit",
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
     * Reads the live Google Sheet and extracts the embedded JSON sync row to restore customers,
     * transactions, and business settings into the app.
     */
    suspend fun pullLedgerFromGoogleSheet(
        accessToken: String,
        spreadsheetId: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val cleanId = extractSheetIdFromInput(spreadsheetId)
            if (cleanId.isEmpty()) {
                return@withContext Result.failure(IllegalArgumentException("Invalid Google Sheet ID"))
            }

            val req = Request.Builder()
                .url("https://sheets.googleapis.com/v4/spreadsheets/$cleanId/values/A1:Z5000")
                .addHeader("Authorization", "Bearer $accessToken")
                .get()
                .build()

            httpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) {
                    val errBody = resp.body?.string().orEmpty()
                    return@withContext Result.failure(
                        IllegalStateException("Could not read Google Sheet (${resp.code}): $errBody")
                    )
                }

                val bodyStr = resp.body?.string().orEmpty()
                val sheetJson = JSONObject(bodyStr)
                val rows = sheetJson.optJSONArray("values")
                    ?: return@withContext Result.failure(
                        IllegalStateException("Google Sheet is empty.")
                    )

                for (i in 0 until rows.length()) {
                    val row = rows.optJSONArray(i) ?: continue
                    if (row.length() >= 2 && row.optString(0) == SHEET_SYNC_ROW_MARKER) {
                        val jsonPayload = row.optString(1).trim()
                        if (jsonPayload.isNotEmpty()) {
                            return@withContext Result.success(jsonPayload)
                        }
                    }
                }

                return@withContext Result.failure(
                    IllegalStateException("No TK Pro sync data found in this Google Sheet.")
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun checkSpreadsheetAccessible(accessToken: String, sheetId: String): Boolean {
        val req = Request.Builder()
            .url("https://sheets.googleapis.com/v4/spreadsheets/$sheetId?includeGridData=false")
            .addHeader("Authorization", "Bearer $accessToken")
            .get()
            .build()
        httpClient.newCall(req).execute().use { resp ->
            return resp.isSuccessful
        }
    }

    private fun createNewGoogleSheet(accessToken: String, title: String): String? {
        val payload = JSONObject()
            .put("properties", JSONObject().put("title", title))
            .toString()
        val req = Request.Builder()
            .url("https://sheets.googleapis.com/v4/spreadsheets")
            .addHeader("Authorization", "Bearer $accessToken")
            .post(payload.toRequestBody(jsonMediaType))
            .build()

        httpClient.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) return null
            val str = resp.body?.string() ?: return null
            val json = JSONObject(str)
            return json.optString("spreadsheetId").takeIf { it.isNotBlank() }
        }
    }

    private fun buildGoogleSheetValuesArray(
        settings: AppSettings,
        customers: List<CustomerEntity>,
        transactions: List<TransactionEntity>,
        rawBackupJson: String
    ): JSONArray {
        val rows = JSONArray()
        val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
        val nowStr = dateFormat.format(Date(System.currentTimeMillis()))
        val txByCustomer = transactions.groupBy { it.customerId }
        val customerMap = customers.associateBy { it.id }

        var totalReceivable = 0.0
        var totalPayable = 0.0
        customers.forEach { c ->
            val cTxs = txByCustomer[c.id].orEmpty()
            val debit = cTxs.filter { it.type == "DEBIT" }.sumOf { it.amount }
            val credit = cTxs.filter { it.type == "CREDIT" }.sumOf { it.amount }
            val net = debit - credit
            if (net > 0.001) totalReceivable += net
            if (net < -0.001) totalPayable += abs(net)
        }
        val netOverall = totalReceivable - totalPayable

        // Header & Summary Rows
        rows.put(
            JSONArray()
                .put("TK Pro Live Ledger")
                .put(settings.businessName.ifBlank { "TK Pro" })
                .put("Phone")
                .put(settings.ownerPhone)
                .put("Last Synced")
                .put(nowStr)
        )
        rows.put(
            JSONArray()
                .put("SUMMARY")
                .put("Total Receivable (মোট পাবেন)")
                .put(totalReceivable)
                .put("Total Payable (মোট দেবেন)")
                .put(totalPayable)
                .put("Net Balance (মোট ব্যালেন্স)")
                .put(netOverall)
        )
        rows.put(JSONArray())

        // Customer Table Header
        rows.put(
            JSONArray()
                .put("CUSTOMER ID")
                .put("NAME (নাম)")
                .put("PHONE (মোবাইল)")
                .put("PARTY TYPE")
                .put("ADDRESS (ঠিকানা)")
                .put("TOTAL DEBIT (টাকা পাব)")
                .put("TOTAL CREDIT (টাকা দেব)")
                .put("NET BALANCE (ব্যালেন্স)")
                .put("STATUS")
        )
        customers.forEach { c ->
            val cTxs = txByCustomer[c.id].orEmpty()
            val debit = cTxs.filter { it.type == "DEBIT" }.sumOf { it.amount }
            val credit = cTxs.filter { it.type == "CREDIT" }.sumOf { it.amount }
            val net = debit - credit
            val status = when {
                net > 0.001 -> "RECEIVABLE (পাবেন)"
                net < -0.001 -> "PAYABLE (দেবেন)"
                else -> "SETTLED (পরিশোধিত)"
            }
            rows.put(
                JSONArray()
                    .put(c.id)
                    .put(c.name)
                    .put(c.phone)
                    .put(c.partyType)
                    .put(c.address)
                    .put(debit)
                    .put(credit)
                    .put(net)
                    .put(status)
            )
        }
        rows.put(JSONArray())

        // Transactions Table Header
        rows.put(
            JSONArray()
                .put("TX ID")
                .put("DATE & TIME")
                .put("CUSTOMER NAME")
                .put("CUSTOMER PHONE")
                .put("TYPE (ধরন)")
                .put("AMOUNT (পরিমাণ)")
                .put("CATEGORY (ক্যাটাগরি)")
                .put("PAYMENT METHOD")
                .put("NOTE (নোট)")
        )
        transactions.sortedByDescending { it.timestamp }.forEach { t ->
            val cust = customerMap[t.customerId]
            rows.put(
                JSONArray()
                    .put(t.id)
                    .put(dateFormat.format(Date(t.timestamp)))
                    .put(cust?.name ?: "")
                    .put(cust?.phone ?: "")
                    .put(t.type)
                    .put(t.amount)
                    .put(t.category)
                    .put(t.paymentMethod)
                    .put(t.note)
            )
        }
        rows.put(JSONArray())

        // Machine-readable restore row
        rows.put(
            JSONArray()
                .put(SHEET_SYNC_ROW_MARKER)
                .put(rawBackupJson)
        )
        return rows
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
