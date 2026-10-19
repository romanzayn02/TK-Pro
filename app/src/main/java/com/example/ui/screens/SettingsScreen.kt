package com.example.ui.screens

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Sync
import com.example.data.GoogleDocsSyncService
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.AppSettings
import com.example.ui.AppStrings
import com.example.ui.GoogleDocsSyncAction
import com.example.ui.LedgerViewModel
import com.example.ui.LocalizationProvider
import com.example.ui.components.LiquidFilterChip
import com.example.ui.components.LiquidGlassCard
import com.example.ui.components.SpringyLiquidButton
import com.example.ui.theme.TallyCyanRefraction
import com.example.ui.theme.TallyEmeraldGreen
import com.example.ui.theme.TallyGoldAccent
import com.example.ui.theme.TallyRubyRed

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    settings: AppSettings,
    strings: AppStrings,
    viewModel: LedgerViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val useDarkGlass = settings.useDarkGlassTheme
    val isSyncingDocs = uiState.isSyncingGoogleDocs

    var businessName by remember(settings.businessName) { mutableStateOf(settings.businessName) }
    var ownerPhone by remember(settings.ownerPhone) { mutableStateOf(settings.ownerPhone) }
    var currencySymbol by remember(settings.currencySymbol) { mutableStateOf(settings.currencySymbol) }
    var newCategoryText by remember { mutableStateOf("") }
    var customDocInput by remember(settings.syncedGoogleDocId) {
        mutableStateOf(settings.syncedGoogleDocId)
    }

    var pendingSyncAction by remember { mutableStateOf(GoogleDocsSyncAction.PUSH_TO_DOCS) }
    var pendingCustomDocId by remember { mutableStateOf<String?>(null) }
    var signedInEmail by remember(isSyncingDocs) {
        mutableStateOf(GoogleDocsSyncService.getSignedInEmail(context))
    }
    var requestAccessEmail by remember(signedInEmail) {
        mutableStateOf(signedInEmail ?: "")
    }
    var showAccessErrorHelp by remember { mutableStateOf(false) }

    val scopeConsentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        signedInEmail = GoogleDocsSyncService.getSignedInEmail(context)
        viewModel.onGoogleSignInActivityResult(
            context = context,
            intent = result.data,
            action = pendingSyncAction,
            customDocIdInput = pendingCustomDocId,
            onNeedsSignInIntent = { _, _, _ -> }
        )
    }

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        signedInEmail = GoogleDocsSyncService.getSignedInEmail(context)
        viewModel.onGoogleSignInActivityResult(
            context = context,
            intent = result.data,
            action = pendingSyncAction,
            customDocIdInput = pendingCustomDocId,
            onNeedsSignInIntent = { recoverIntent, action, docId ->
                pendingSyncAction = action
                pendingCustomDocId = docId
                scopeConsentLauncher.launch(recoverIntent)
            }
        )
    }

    val googleDocsConsentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        signedInEmail = GoogleDocsSyncService.getSignedInEmail(context)
        viewModel.onGoogleDocsConsentResult(
            context = context,
            intent = result.data,
            action = pendingSyncAction,
            customDocIdInput = pendingCustomDocId,
            onNeedsSignInIntent = { signInIntent, action, docId ->
                pendingSyncAction = action
                pendingCustomDocId = docId
                googleSignInLauncher.launch(signInIntent)
            }
        )
    }

    val exportJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.exportDatabaseToUri(context, uri)
        }
    }

    val importJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.importDatabaseFromUri(context, uri)
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("settings_lazy_column"),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 12.dp,
            bottom = 110.dp
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item(key = "settings_title") {
            Text(
                text = strings.settingsTitle,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }

        // 0. Live Google Docs Cloud Sync Card
        item(key = "google_docs_live_sync_card") {
            LiquidGlassCard(
                useDarkGlass = useDarkGlass,
                accentTint = TallyCyanRefraction,
                cornerRadius = 24.dp,
                testTag = "google_docs_sync_card",
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
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
                            Icon(
                                imageVector = Icons.Default.Description,
                                contentDescription = null,
                                tint = TallyCyanRefraction
                            )
                            Column {
                                Text(
                                    text = if (strings.isBn) {
                                        "লাইভ Google Docs সিঙ্ক"
                                    } else {
                                        "Live Google Docs Sync"
                                    },
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (strings.isBn) {
                                        "আপনার সম্পূর্ণ হিসাব খাতা সরাসরি Google Docs ফাইলে লাইভ সেভ ও রিস্টোর করুন"
                                    } else {
                                        "Sync your ledger live with a readable & restorable Google Docs file"
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        if (isSyncingDocs) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.5.dp,
                                color = TallyCyanRefraction
                            )
                        }
                    }

                    // Auto-sync toggle row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (strings.isBn) {
                                    "অটোমেটিক লাইভ সিঙ্ক"
                                } else {
                                    "Auto-Sync on Every Change"
                                },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (strings.isBn) {
                                    "নতুন কাস্টমার বা লেনদেন যোগ করলেই Google Doc আপডেট হবে"
                                } else {
                                    "Automatically update linked Google Doc after adding entries"
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = settings.autoSyncGoogleDocs,
                            onCheckedChange = { viewModel.setAutoSyncGoogleDocs(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = TallyEmeraldGreen
                            ),
                            modifier = Modifier.testTag("auto_sync_google_docs_switch")
                        )
                    }

                    // Connected Google Doc status & Open button
                    if (settings.syncedGoogleDocId.isNotBlank()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(TallyEmeraldGreen.copy(alpha = 0.12f))
                                .border(1.dp, TallyEmeraldGreen.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                                .padding(12.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CloudDone,
                                            contentDescription = null,
                                            tint = TallyEmeraldGreen,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            text = if (strings.isBn) "যুক্ত Google Doc" else "Linked Google Doc",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = TallyEmeraldGreen,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    LiquidFilterChip(
                                        label = if (strings.isBn) "ডক খুলুন" else "Open Doc",
                                        selected = true,
                                        onClick = {
                                            try {
                                                val docUrl = "https://docs.google.com/document/d/${settings.syncedGoogleDocId}/edit"
                                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(docUrl)))
                                            } catch (_: Exception) {
                                            }
                                        },
                                        useDarkGlass = useDarkGlass,
                                        accentColor = TallyEmeraldGreen,
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                                contentDescription = null,
                                                modifier = Modifier.size(14.dp),
                                                tint = Color.White
                                            )
                                        },
                                        testTag = "open_synced_google_doc_button"
                                    )
                                }

                                Text(
                                    text = "ID: ${settings.syncedGoogleDocId}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                if (settings.lastSyncedAtMillis > 0L) {
                                    val timeStr = LocalizationProvider.formatTimestamp(
                                        settings.lastSyncedAtMillis,
                                        strings.isBn
                                    )
                                    Text(
                                        text = if (strings.isBn) "সর্বশেষ সিঙ্ক: $timeStr" else "Last synced: $timeStr",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    // Input for existing Google Doc URL or ID
                    OutlinedTextField(
                        value = customDocInput,
                        onValueChange = { customDocInput = it },
                        label = {
                            Text(
                                if (strings.isBn) {
                                    "Google Doc লিংক বা ID (নতুন তৈরির জন্য খালি রাখুন)"
                                } else {
                                    "Google Doc URL or ID (Leave blank to create new)"
                                }
                            )
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("google_doc_id_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TallyCyanRefraction,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )

                    // Push & Pull Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        SpringyLiquidButton(
                            onClick = {
                                if (!isSyncingDocs) {
                                    viewModel.performGoogleDocsSync(
                                        context = context,
                                        action = GoogleDocsSyncAction.PUSH_TO_DOCS,
                                        customDocIdInput = customDocInput,
                                        onNeedsConsent = { intentSender, action, docId ->
                                            pendingSyncAction = action
                                            pendingCustomDocId = docId
                                            googleDocsConsentLauncher.launch(
                                                IntentSenderRequest.Builder(intentSender).build()
                                            )
                                        },
                                        onNeedsSignInIntent = { signInIntent, action, docId ->
                                            pendingSyncAction = action
                                            pendingCustomDocId = docId
                                            googleSignInLauncher.launch(signInIntent)
                                        }
                                    )
                                }
                            },
                            accentColor = TallyCyanRefraction,
                            secondaryColor = Color(0xFF0077B6),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 12.dp),
                            testTag = "push_google_docs_sync_button"
                        ) {
                            Icon(
                                imageVector = Icons.Default.Sync,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (strings.isBn) {
                                    "Google Docs-এ সিঙ্ক"
                                } else {
                                    "Sync to Google Docs"
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        SpringyLiquidButton(
                            onClick = {
                                if (!isSyncingDocs) {
                                    viewModel.performGoogleDocsSync(
                                        context = context,
                                        action = GoogleDocsSyncAction.PULL_FROM_DOCS,
                                        customDocIdInput = customDocInput,
                                        onNeedsConsent = { intentSender, action, docId ->
                                            pendingSyncAction = action
                                            pendingCustomDocId = docId
                                            googleDocsConsentLauncher.launch(
                                                IntentSenderRequest.Builder(intentSender).build()
                                            )
                                        },
                                        onNeedsSignInIntent = { signInIntent, action, docId ->
                                            pendingSyncAction = action
                                            pendingCustomDocId = docId
                                            googleSignInLauncher.launch(signInIntent)
                                        }
                                    )
                                }
                            },
                            accentColor = TallyEmeraldGreen,
                            secondaryColor = Color(0xFF007E33),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 12.dp),
                            testTag = "pull_google_docs_sync_button"
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudSync,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (strings.isBn) {
                                    "ডক থেকে রিস্টোর"
                                } else {
                                    "Restore from Doc"
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Active Google Account & Switch Account Row + 403 Fix Helper
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountCircle,
                                contentDescription = null,
                                tint = TallyCyanRefraction,
                                modifier = Modifier.size(20.dp)
                            )
                            Column {
                                Text(
                                    text = if (strings.isBn) "যুক্ত Google অ্যাকাউন্ট" else "Google Account",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = signedInEmail ?: if (strings.isBn) "নির্বাচন করা হয়নি" else "Not selected yet",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        LiquidFilterChip(
                            label = if (strings.isBn) "অ্যাকাউন্ট পরিবর্তন" else "Switch Account",
                            selected = false,
                            onClick = {
                                viewModel.switchGoogleAccountForDocsSync(
                                    context = context,
                                    action = GoogleDocsSyncAction.PUSH_TO_DOCS,
                                    customDocIdInput = customDocInput,
                                    onLaunchSignInIntent = { signInIntent, action, docId ->
                                        pendingSyncAction = action
                                        pendingCustomDocId = docId
                                        googleSignInLauncher.launch(signInIntent)
                                    }
                                )
                            },
                            useDarkGlass = useDarkGlass,
                            accentColor = TallyGoldAccent,
                            testTag = "switch_google_account_button"
                        )
                    }

                    // Expandable 403 Access Blocked Request Access via WhatsApp Section
                    LiquidFilterChip(
                        label = if (strings.isBn) {
                            "Error 403: access_denied সমাধান (WhatsApp অ্যাক্সেস রিকোয়েস্ট)"
                        } else {
                            "How to fix Error 403: Request Access via WhatsApp (@RomanZayn)"
                        },
                        selected = showAccessErrorHelp,
                        onClick = { showAccessErrorHelp = !showAccessErrorHelp },
                        useDarkGlass = useDarkGlass,
                        accentColor = TallyGoldAccent,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.HelpOutline,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = if (showAccessErrorHelp) Color.White else TallyGoldAccent
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "toggle_403_help_chip"
                    )

                    if (showAccessErrorHelp) {
                        val effectiveEmail = requestAccessEmail.trim().ifBlank { "[Your Gmail Address]" }
                        val automatedDraftMessage = if (strings.isBn) {
                            "হ্যালো @RomanZayn,\nআমি TK Pro অ্যাপে Google Docs লাইভ সিঙ্ক ব্যবহার করতে চাই কিন্তু Error 403 (access_denied) দেখাচ্ছে।\nঅনুগ্রহ করে আমার জিমেইল অ্যাকাউন্টে অ্যাক্সেস দিন:\n📧 ইমেইল: $effectiveEmail"
                        } else {
                            "Hello @RomanZayn,\nI would like to use Google Docs Live Sync in TK Pro, but I am getting Error 403 (access_denied).\nPlease grant access to my Gmail account:\n📧 Email: $effectiveEmail"
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(TallyGoldAccent.copy(alpha = 0.12f))
                                .border(1.dp, TallyGoldAccent.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = if (strings.isBn) {
                                    "কেন অ্যাক্সেস ব্লক দেখায়?\n" +
                                        "নিরাপত্তার জন্য শুধুমাত্র অনুমোদিত জিমেইল অ্যাকাউন্টগুলো Google Docs সিঙ্ক ব্যবহার করতে পারে। অ্যাক্সেস পেতে নিচে আপনার জিমেইল লিখুন এবং @RomanZayn-কে WhatsApp-এ অটোমেটিক রিকোয়েস্ট মেসেজ পাঠান।"
                                } else {
                                    "How to fix Error 403 (Access blocked):\n" +
                                        "Google Docs sync requires developer approval for your Gmail account. Enter your Gmail below and tap the WhatsApp button to send an automated access request to @RomanZayn on WhatsApp."
                                },
                                style = MaterialTheme.typography.bodyMedium
                            )

                            OutlinedTextField(
                                value = requestAccessEmail,
                                onValueChange = { requestAccessEmail = it },
                                label = {
                                    Text(
                                        if (strings.isBn) {
                                            "আপনার জিমেইল অ্যাড্রেস লিখুন (যেমন: name@gmail.com)"
                                        } else {
                                            "Enter your Gmail address (e.g. name@gmail.com)"
                                        }
                                    )
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("request_access_email_input"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = TallyGoldAccent,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                                )
                            )

                            // Automated Draft Message Preview Box
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.75f))
                                    .border(
                                        1.dp,
                                        MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                                        RoundedCornerShape(10.dp)
                                    )
                                    .padding(10.dp)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        text = if (strings.isBn) {
                                            "অটোমেটিক ড্রাফট মেসেজ (@RomanZayn):"
                                        } else {
                                            "Automated WhatsApp Draft (@RomanZayn):"
                                        },
                                        style = MaterialTheme.typography.labelMedium,
                                        color = TallyCyanRefraction,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = automatedDraftMessage,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                SpringyLiquidButton(
                                    onClick = {
                                        if (requestAccessEmail.trim().isBlank() || !requestAccessEmail.contains("@")) {
                                            viewModel.showStatusMessage(
                                                if (strings.isBn) {
                                                    "⚠️ অনুগ্রহ করে প্রথমে আপনার জিমেইল অ্যাড্রেসটি লিখুন"
                                                } else {
                                                    "⚠️ Please enter your Gmail address first"
                                                }
                                            )
                                        } else {
                                            try {
                                                val waUri = Uri.parse(
                                                    "https://api.whatsapp.com/send?text=${Uri.encode(automatedDraftMessage)}"
                                                )
                                                val waIntent = Intent(Intent.ACTION_VIEW, waUri)
                                                context.startActivity(waIntent)
                                            } catch (_: Exception) {
                                                try {
                                                    val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                                        type = "text/plain"
                                                        putExtra(Intent.EXTRA_TEXT, automatedDraftMessage)
                                                    }
                                                    context.startActivity(
                                                        Intent.createChooser(sendIntent, "Send via WhatsApp")
                                                    )
                                                } catch (_: Exception) {
                                                }
                                            }
                                        }
                                    },
                                    accentColor = TallyEmeraldGreen,
                                    secondaryColor = Color(0xFF007E33),
                                    modifier = Modifier.weight(1.5f),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                                    testTag = "send_whatsapp_access_request_button"
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Send,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (strings.isBn) {
                                            "WhatsApp করুন (@RomanZayn)"
                                        } else {
                                            "WhatsApp @RomanZayn"
                                        },
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                LiquidFilterChip(
                                    label = if (strings.isBn) "মেসেজ কপি" else "Copy Draft",
                                    selected = false,
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                        clipboard?.setPrimaryClip(
                                            ClipData.newPlainText("TK Pro Access Request", automatedDraftMessage)
                                        )
                                        viewModel.showStatusMessage(
                                            if (strings.isBn) {
                                                "✅ ড্রাফট মেসেজ কপি হয়েছে (@RomanZayn-কে পাঠান)"
                                            } else {
                                                "✅ Draft message copied for @RomanZayn"
                                            }
                                        )
                                    },
                                    useDarkGlass = useDarkGlass,
                                    accentColor = TallyCyanRefraction,
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.ContentCopy,
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp),
                                            tint = TallyCyanRefraction
                                        )
                                    },
                                    modifier = Modifier.weight(1f),
                                    testTag = "copy_whatsapp_draft_button"
                                )
                            }
                        }
                    }
                }
            }
        }

        // 1. Language Toggle (বাংলা Default vs English) & Theme Mode
        item(key = "language_and_theme_card") {
            LiquidGlassCard(
                useDarkGlass = useDarkGlass,
                accentTint = TallyCyanRefraction,
                cornerRadius = 24.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Language,
                            contentDescription = null,
                            tint = TallyCyanRefraction
                        )
                        Text(
                            text = strings.languageSectionTitle,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        LiquidFilterChip(
                            label = "বাংলা (Default)",
                            selected = settings.language == "BN",
                            onClick = { viewModel.setLanguage("BN") },
                            useDarkGlass = useDarkGlass,
                            accentColor = TallyEmeraldGreen,
                            modifier = Modifier.weight(1f),
                            testTag = "lang_bn_chip"
                        )
                        LiquidFilterChip(
                            label = "English",
                            selected = settings.language == "EN",
                            onClick = { viewModel.setLanguage("EN") },
                            useDarkGlass = useDarkGlass,
                            accentColor = TallyCyanRefraction,
                            modifier = Modifier.weight(1f),
                            testTag = "lang_en_chip"
                        )
                    }

                    Text(
                        text = strings.glassThemeSectionTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        LiquidFilterChip(
                            label = strings.darkGlassModeLabel,
                            selected = settings.useDarkGlassTheme,
                            onClick = { viewModel.toggleGlassThemeMode(true) },
                            useDarkGlass = useDarkGlass,
                            accentColor = TallyCyanRefraction,
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.DarkMode,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            modifier = Modifier.weight(1f),
                            testTag = "theme_dark_glass_chip"
                        )
                        LiquidFilterChip(
                            label = strings.lightGlassModeLabel,
                            selected = !settings.useDarkGlassTheme,
                            onClick = { viewModel.toggleGlassThemeMode(false) },
                            useDarkGlass = useDarkGlass,
                            accentColor = TallyGoldAccent,
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.LightMode,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            modifier = Modifier.weight(1f),
                            testTag = "theme_light_glass_chip"
                        )
                    }
                }
            }
        }

        // 2. Business Branding & Currency Customization (৳ default)
        item(key = "business_branding_card") {
            LiquidGlassCard(
                useDarkGlass = useDarkGlass,
                accentTint = TallyEmeraldGreen,
                cornerRadius = 24.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Storefront,
                            contentDescription = null,
                            tint = TallyEmeraldGreen
                        )
                        Text(
                            text = strings.businessBrandingTitle,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    OutlinedTextField(
                        value = businessName,
                        onValueChange = { businessName = it },
                        label = { Text(strings.businessNameLabel) },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_business_name_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TallyEmeraldGreen,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )

                    OutlinedTextField(
                        value = ownerPhone,
                        onValueChange = { ownerPhone = it },
                        label = { Text(strings.ownerPhoneLabel) },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_owner_phone_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TallyEmeraldGreen,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )

                    // Currency presets + Custom input
                    Text(
                        text = strings.currencySymbolLabel,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf("৳", "Tk", "₹", "$").forEach { sym ->
                            LiquidFilterChip(
                                label = sym,
                                selected = currencySymbol == sym,
                                onClick = { currencySymbol = sym },
                                useDarkGlass = useDarkGlass,
                                accentColor = TallyEmeraldGreen,
                                testTag = "currency_preset_$sym"
                            )
                        }
                        OutlinedTextField(
                            value = currencySymbol,
                            onValueChange = { if (it.length <= 4) currencySymbol = it },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("settings_currency_input")
                        )
                    }

                    SpringyLiquidButton(
                        onClick = {
                            viewModel.updateBusinessSettings(
                                businessName = businessName,
                                ownerPhone = ownerPhone,
                                currencySymbol = currencySymbol
                            )
                        },
                        accentColor = TallyEmeraldGreen,
                        secondaryColor = Color(0xFF007E33),
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "save_branding_button"
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = strings.saveBrandingButton,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // 3. Customizable Categories Management
        item(key = "custom_categories_card") {
            LiquidGlassCard(
                useDarkGlass = useDarkGlass,
                accentTint = TallyGoldAccent,
                cornerRadius = 24.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Category,
                            contentDescription = null,
                            tint = TallyGoldAccent
                        )
                        Text(
                            text = strings.categoriesSectionTitle,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        settings.customCategories.forEach { cat ->
                            LiquidFilterChip(
                                label = "$cat  ✕",
                                selected = false,
                                onClick = { viewModel.removeCustomCategory(cat) },
                                useDarkGlass = useDarkGlass,
                                accentColor = TallyGoldAccent
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = newCategoryText,
                            onValueChange = { newCategoryText = it },
                            placeholder = { Text(strings.newCategoryPlaceholder) },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("new_category_input")
                        )
                        SpringyLiquidButton(
                            onClick = {
                                if (newCategoryText.isNotBlank()) {
                                    viewModel.addCustomCategory(newCategoryText)
                                    newCategoryText = ""
                                }
                            },
                            accentColor = TallyGoldAccent,
                            secondaryColor = Color(0xFFE65100),
                            testTag = "add_category_button"
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = strings.addCategoryButton
                            )
                        }
                    }
                }
            }
        }

        // 4. Full JSON Database Export & Import for Backup
        item(key = "json_backup_restore_card") {
            LiquidGlassCard(
                useDarkGlass = useDarkGlass,
                accentTint = TallyRubyRed,
                cornerRadius = 24.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = strings.backupSectionTitle,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = strings.backupSubtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        SpringyLiquidButton(
                            onClick = {
                                val fileName = "tallykhata_backup_${System.currentTimeMillis()}.json"
                                exportJsonLauncher.launch(fileName)
                            },
                            accentColor = TallyCyanRefraction,
                            secondaryColor = Color(0xFF0077B6),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 12.dp),
                            testTag = "export_json_backup_button"
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudUpload,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = strings.exportJsonButton,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        SpringyLiquidButton(
                            onClick = {
                                importJsonLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
                            },
                            accentColor = TallyRubyRed,
                            secondaryColor = Color(0xFFB71C1C),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 12.dp),
                            testTag = "import_json_backup_button"
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudDownload,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = strings.importJsonButton,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

