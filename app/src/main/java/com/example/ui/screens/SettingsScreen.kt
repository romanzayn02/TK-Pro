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
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.TableChart
import com.example.data.GoogleDocsSyncService
import com.example.ui.BiometricAuthHelper
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.activity.result.PickVisualMediaRequest
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.AppSettings
import com.example.ui.AppStrings
import com.example.ui.GoogleDocsSyncAction
import com.example.ui.LedgerViewModel
import com.example.ui.LocalizationProvider
import com.example.ui.components.ConfirmActionDialog
import com.example.ui.components.LiquidFilterChip
import com.example.ui.components.LiquidGlassCard
import com.example.ui.components.SpringyLiquidButton
import com.example.ui.theme.TallyCyanRefraction
import com.example.ui.theme.TallyEmeraldGreen
import com.example.ui.theme.TallyGoldAccent
import com.example.ui.theme.TallyRubyRed
import java.io.File

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    settings: AppSettings,
    strings: AppStrings,
    viewModel: LedgerViewModel,
    onLockAppNow: () -> Unit = {},
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
    var customSheetInput by remember(settings.syncedGoogleSheetId) {
        mutableStateOf(settings.syncedGoogleSheetId)
    }

    var pendingSyncAction by remember { mutableStateOf(GoogleDocsSyncAction.PUSH_TO_DOCS) }
    var pendingCustomDocId by remember { mutableStateOf<String?>(null) }
    var signedInEmail by remember(isSyncingDocs) {
        mutableStateOf(GoogleDocsSyncService.getSignedInEmail(context))
    }
    var requestAccessEmail by remember(signedInEmail) {
        mutableStateOf(signedInEmail ?: "")
    }
    var showDocsSection by remember { mutableStateOf(false) }
    var showSheetsSection by remember { mutableStateOf(false) }
    var showAccessErrorHelp by remember { mutableStateOf(false) }
    var categoryToDelete by remember { mutableStateOf<String?>(null) }

    val pendingWelcomeRestore by viewModel.pendingWelcomeCloudRestore.collectAsStateWithLifecycle()

    val googleDocsConsentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        signedInEmail = GoogleDocsSyncService.getSignedInEmail(context)
        viewModel.onGoogleDocsConsentResult(
            context = context,
            intent = result.data,
            action = pendingSyncAction,
            customDocIdInput = pendingCustomDocId,
            onNeedsSignInIntent = { _, _, _ -> }
        )
    }

    val scopeConsentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        signedInEmail = GoogleDocsSyncService.getSignedInEmail(context)
        viewModel.onGoogleSignInActivityResult(
            context = context,
            intent = result.data,
            action = pendingSyncAction,
            customDocIdInput = pendingCustomDocId,
            onNeedsConsent = { intentSender, action, docId ->
                pendingSyncAction = action
                pendingCustomDocId = docId
                googleDocsConsentLauncher.launch(
                    IntentSenderRequest.Builder(intentSender).build()
                )
            },
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
            onNeedsConsent = { intentSender, action, docId ->
                pendingSyncAction = action
                pendingCustomDocId = docId
                googleDocsConsentLauncher.launch(
                    IntentSenderRequest.Builder(intentSender).build()
                )
            },
            onNeedsSignInIntent = { recoverIntent, action, docId ->
                pendingSyncAction = action
                pendingCustomDocId = docId
                scopeConsentLauncher.launch(recoverIntent)
            }
        )
    }

    LaunchedEffect(pendingWelcomeRestore) {
        if (pendingWelcomeRestore) {
            viewModel.consumePendingWelcomeCloudRestore()
            viewModel.performGoogleDocsSync(
                context = context,
                action = GoogleDocsSyncAction.AUTO_RESTORE,
                customDocIdInput = null,
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

    val ownerPhotoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.updateOwnerProfilePhoto(context, uri)
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

        // 0. Compact Expandable Google Docs & Sheets Cloud Sync Card
        item(key = "google_docs_live_sync_card") {
            val isDocConnected = settings.syncedGoogleDocId.isNotBlank()
            val isSheetConnected = settings.syncedGoogleSheetId.isNotBlank()

            LiquidGlassCard(
                useDarkGlass = useDarkGlass,
                accentTint = TallyCyanRefraction,
                cornerRadius = 22.dp,
                testTag = "google_docs_sync_card",
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Compact Top Header + Auto-Sync Switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudSync,
                                contentDescription = null,
                                tint = TallyCyanRefraction,
                                modifier = Modifier.size(20.dp)
                            )
                            Column {
                                Text(
                                    text = if (strings.isBn) {
                                        "Google ক্লাউড সিঙ্ক"
                                    } else {
                                        "Google Cloud Sync"
                                    },
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (strings.isBn) {
                                        "অটো-সিঙ্ক এবং Docs / Sheets অপশন"
                                    } else {
                                        "Tap Docs or Sheets to expand options"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (isSyncingDocs) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = TallyCyanRefraction
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
                    }

                    // Offline Mode / Pending Cloud Sync pill inside Settings when sync fails or offline
                    AnimatedVisibility(visible = uiState.isOfflineMode) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(TallyGoldAccent.copy(alpha = 0.14f))
                                .border(1.dp, TallyGoldAccent.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
                                .clickable { viewModel.retryOfflineCloudSync() }
                                .padding(horizontal = 10.dp, vertical = 8.dp)
                                .testTag("settings_offline_mode_badge"),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloudOff,
                                    contentDescription = null,
                                    tint = TallyGoldAccent,
                                    modifier = Modifier.size(16.dp)
                                )
                                Column {
                                    Text(
                                        text = if (strings.isBn) {
                                            "অফলাইন মোড (Offline Mode)"
                                        } else {
                                            "Offline Mode Active"
                                        },
                                        style = MaterialTheme.typography.labelLarge,
                                        color = TallyGoldAccent,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = if (settings.lastSyncError.isNotBlank()) {
                                            settings.lastSyncError
                                        } else if (strings.isBn) {
                                            "অফলাইনে করা লেনদেনগুলো ইন্টারনেট চালু হলে অটো-সিঙ্ক হবে (সিঙ্ক করতে ট্যাপ করুন)"
                                        } else {
                                            "Offline transactions are queued and will sync when online (Tap to retry)"
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            Icon(
                                imageVector = Icons.Default.Sync,
                                contentDescription = if (strings.isBn) "পুনরায় সিঙ্ক" else "Retry Sync",
                                tint = TallyGoldAccent,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // 1. Compact Clickable Google Docs Row
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                if (isDocConnected) {
                                    TallyEmeraldGreen.copy(alpha = 0.10f)
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                                }
                            )
                            .border(
                                1.dp,
                                if (isDocConnected) {
                                    TallyEmeraldGreen.copy(alpha = 0.4f)
                                } else {
                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                                },
                                RoundedCornerShape(14.dp)
                            )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showDocsSection = !showDocsSection }
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                                .testTag("toggle_google_docs_section"),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Description,
                                    contentDescription = null,
                                    tint = if (isDocConnected) TallyEmeraldGreen else TallyCyanRefraction,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "Google Docs",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                if (isDocConnected) {
                                    Text(
                                        text = if (strings.isBn) "✅ যুক্ত" else "✅ Connected",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = TallyEmeraldGreen,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                if (isDocConnected) {
                                    IconButton(
                                        onClick = {
                                            try {
                                                val docUrl = "https://docs.google.com/document/d/${settings.syncedGoogleDocId}/edit"
                                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(docUrl)))
                                            } catch (_: Exception) {
                                            }
                                        },
                                        modifier = Modifier
                                            .size(30.dp)
                                            .clip(CircleShape)
                                            .background(TallyEmeraldGreen.copy(alpha = 0.18f))
                                            .testTag("open_synced_google_doc_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                            contentDescription = if (strings.isBn) "ডক খুলুন" else "Open Doc",
                                            tint = TallyEmeraldGreen,
                                            modifier = Modifier.size(15.dp)
                                        )
                                    }
                                }
                                Icon(
                                    imageVector = if (showDocsSection) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        AnimatedVisibility(
                            visible = showDocsSection,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (isDocConnected) {
                                    val timeStr = if (settings.lastSyncedAtMillis > 0L) {
                                        " • " + LocalizationProvider.formatTimestamp(
                                            settings.lastSyncedAtMillis,
                                            strings.isBn
                                        )
                                    } else {
                                        ""
                                    }
                                    Text(
                                        text = "ID: ${settings.syncedGoogleDocId}$timeStr",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                OutlinedTextField(
                                    value = customDocInput,
                                    onValueChange = { customDocInput = it },
                                    label = {
                                        Text(
                                            if (strings.isBn) {
                                                "Google Doc লিংক বা ID (নতুন তৈরির জন্য খালি রাখুন)"
                                            } else {
                                                "Google Doc URL or ID (Leave blank for new)"
                                            }
                                        )
                                    },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("google_doc_id_input"),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = TallyCyanRefraction,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                                    )
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
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
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                                        testTag = "push_google_docs_sync_button"
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Sync,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (strings.isBn) "Docs সিঙ্ক" else "Sync Docs",
                                            style = MaterialTheme.typography.bodySmall,
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
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                                        testTag = "pull_google_docs_sync_button"
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CloudSync,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (strings.isBn) "ডক রিস্টোর" else "Restore Doc",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 2. Compact Clickable Google Sheets Row
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                if (isSheetConnected) {
                                    TallyEmeraldGreen.copy(alpha = 0.10f)
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                                }
                            )
                            .border(
                                1.dp,
                                if (isSheetConnected) {
                                    TallyEmeraldGreen.copy(alpha = 0.4f)
                                } else {
                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                                },
                                RoundedCornerShape(14.dp)
                            )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showSheetsSection = !showSheetsSection }
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                                .testTag("toggle_google_sheets_section"),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.TableChart,
                                    contentDescription = null,
                                    tint = TallyEmeraldGreen,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "Google Sheets",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                if (isSheetConnected) {
                                    Text(
                                        text = if (strings.isBn) "✅ যুক্ত" else "✅ Connected",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = TallyEmeraldGreen,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                if (isSheetConnected) {
                                    IconButton(
                                        onClick = {
                                            try {
                                                val sheetUrl = "https://docs.google.com/spreadsheets/d/${settings.syncedGoogleSheetId}/edit"
                                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(sheetUrl)))
                                            } catch (_: Exception) {
                                            }
                                        },
                                        modifier = Modifier
                                            .size(30.dp)
                                            .clip(CircleShape)
                                            .background(TallyEmeraldGreen.copy(alpha = 0.18f))
                                            .testTag("open_synced_google_sheet_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                            contentDescription = if (strings.isBn) "শিট খুলুন" else "Open Sheet",
                                            tint = TallyEmeraldGreen,
                                            modifier = Modifier.size(15.dp)
                                        )
                                    }
                                }
                                Icon(
                                    imageVector = if (showSheetsSection) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        AnimatedVisibility(
                            visible = showSheetsSection,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (isSheetConnected) {
                                    val timeStr = if (settings.lastSheetSyncedAtMillis > 0L) {
                                        " • " + LocalizationProvider.formatTimestamp(
                                            settings.lastSheetSyncedAtMillis,
                                            strings.isBn
                                        )
                                    } else {
                                        ""
                                    }
                                    Text(
                                        text = "ID: ${settings.syncedGoogleSheetId}$timeStr",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                OutlinedTextField(
                                    value = customSheetInput,
                                    onValueChange = { customSheetInput = it },
                                    label = {
                                        Text(
                                            if (strings.isBn) {
                                                "Google Sheet লিংক বা ID (নতুন তৈরির জন্য খালি রাখুন)"
                                            } else {
                                                "Google Sheet URL or ID (Leave blank for new)"
                                            }
                                        )
                                    },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("google_sheet_id_input"),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = TallyEmeraldGreen,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                                    )
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    SpringyLiquidButton(
                                        onClick = {
                                            if (!isSyncingDocs) {
                                                viewModel.performGoogleDocsSync(
                                                    context = context,
                                                    action = GoogleDocsSyncAction.PUSH_TO_SHEETS,
                                                    customDocIdInput = customSheetInput,
                                                    onNeedsConsent = { intentSender, action, sheetId ->
                                                        pendingSyncAction = action
                                                        pendingCustomDocId = sheetId
                                                        googleDocsConsentLauncher.launch(
                                                            IntentSenderRequest.Builder(intentSender).build()
                                                        )
                                                    },
                                                    onNeedsSignInIntent = { signInIntent, action, sheetId ->
                                                        pendingSyncAction = action
                                                        pendingCustomDocId = sheetId
                                                        googleSignInLauncher.launch(signInIntent)
                                                    }
                                                )
                                            }
                                        },
                                        accentColor = TallyEmeraldGreen,
                                        secondaryColor = Color(0xFF007E33),
                                        modifier = Modifier.weight(1f),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                                        testTag = "push_google_sheets_sync_button"
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.TableChart,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (strings.isBn) "Sheets সিঙ্ক" else "Sync Sheets",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    SpringyLiquidButton(
                                        onClick = {
                                            if (!isSyncingDocs) {
                                                viewModel.performGoogleDocsSync(
                                                    context = context,
                                                    action = GoogleDocsSyncAction.PULL_FROM_SHEETS,
                                                    customDocIdInput = customSheetInput,
                                                    onNeedsConsent = { intentSender, action, sheetId ->
                                                        pendingSyncAction = action
                                                        pendingCustomDocId = sheetId
                                                        googleDocsConsentLauncher.launch(
                                                            IntentSenderRequest.Builder(intentSender).build()
                                                        )
                                                    },
                                                    onNeedsSignInIntent = { signInIntent, action, sheetId ->
                                                        pendingSyncAction = action
                                                        pendingCustomDocId = sheetId
                                                        googleSignInLauncher.launch(signInIntent)
                                                    }
                                                )
                                            }
                                        },
                                        accentColor = TallyCyanRefraction,
                                        secondaryColor = Color(0xFF0077B6),
                                        modifier = Modifier.weight(1f),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                                        testTag = "pull_google_sheets_sync_button"
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CloudSync,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (strings.isBn) "শিট রিস্টোর" else "Restore Sheet",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 3. Compact Account Login, 1-Tap Cloud Auto-Restore & 403 Fix Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LiquidFilterChip(
                            label = signedInEmail ?: if (strings.isBn) "Google লগইন" else "Google Login",
                            selected = !signedInEmail.isNullOrBlank(),
                            onClick = {
                                if (!isSyncingDocs) {
                                    viewModel.switchGoogleAccountForDocsSync(
                                        context = context,
                                        action = GoogleDocsSyncAction.SIGN_IN_ONLY,
                                        customDocIdInput = null,
                                        onLaunchSignInIntent = { signInIntent, action, docId ->
                                            pendingSyncAction = action
                                            pendingCustomDocId = docId
                                            googleSignInLauncher.launch(signInIntent)
                                        }
                                    )
                                }
                            },
                            useDarkGlass = useDarkGlass,
                            accentColor = if (!signedInEmail.isNullOrBlank()) TallyEmeraldGreen else TallyCyanRefraction,
                            leadingIcon = {
                                if (settings.googlePhotoUrl.isNotBlank()) {
                                    AsyncImage(
                                        model = settings.googlePhotoUrl,
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clip(CircleShape)
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.AccountCircle,
                                        contentDescription = null,
                                        modifier = Modifier.size(15.dp),
                                        tint = if (!signedInEmail.isNullOrBlank()) Color.White else TallyCyanRefraction
                                    )
                                }
                            },
                            modifier = Modifier.weight(1.1f),
                            testTag = "switch_google_account_button"
                        )

                        LiquidFilterChip(
                            label = if (strings.isBn) "অটো রিস্টোর" else "Auto Restore",
                            selected = false,
                            onClick = {
                                if (!isSyncingDocs) {
                                    viewModel.performGoogleDocsSync(
                                        context = context,
                                        action = GoogleDocsSyncAction.AUTO_RESTORE,
                                        customDocIdInput = null,
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
                            useDarkGlass = useDarkGlass,
                            accentColor = TallyEmeraldGreen,
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.CloudDownload,
                                    contentDescription = null,
                                    modifier = Modifier.size(15.dp),
                                    tint = TallyEmeraldGreen
                                )
                            },
                            modifier = Modifier.weight(1f),
                            testTag = "auto_cloud_restore_button"
                        )

                        LiquidFilterChip(
                            label = if (strings.isBn) "403" else "403 Fix",
                            selected = showAccessErrorHelp,
                            onClick = { showAccessErrorHelp = !showAccessErrorHelp },
                            useDarkGlass = useDarkGlass,
                            accentColor = TallyGoldAccent,
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.HelpOutline,
                                    contentDescription = null,
                                    modifier = Modifier.size(15.dp),
                                    tint = if (showAccessErrorHelp) Color.White else TallyGoldAccent
                                )
                            },
                            testTag = "toggle_403_help_chip"
                        )
                    }

                    AnimatedVisibility(
                        visible = showAccessErrorHelp,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        val effectiveEmail = requestAccessEmail.trim().ifBlank {
                            if (strings.isBn) "[এখানে আপনার জিমেইল লিখুন]" else "[Enter your Gmail here]"
                        }
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
                                        "নিরাপত্তার জন্য শুধুমাত্র অনুমোদিত জিমেইল অ্যাকাউন্টগুলো Google Docs সিঙ্ক ব্যবহার করতে পারে। অ্যাক্সেস পেতে নিচে আপনার জিমেইল লিখুন এবং নিচের বাটনে ট্যাপ করে সরাসরি @RomanZayn-কে WhatsApp মেসেজ পাঠান।"
                                } else {
                                    "How to fix Error 403 (Access blocked):\n" +
                                        "Google Docs sync requires developer approval for your Gmail account. Enter your Gmail below and tap the button to open @RomanZayn's chat on WhatsApp with the automated request message."
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
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                        clipboard?.setPrimaryClip(
                                            ClipData.newPlainText("TK Pro Access Request", automatedDraftMessage)
                                        )

                                        val encodedText = Uri.encode(automatedDraftMessage)
                                        val directUserUri = Uri.parse("https://wa.me/RomanZayn?text=$encodedText")
                                        var launched = false

                                        for (pkg in listOf("com.whatsapp", "com.whatsapp.w4b")) {
                                            if (!launched) {
                                                try {
                                                    val pkgIntent = Intent(Intent.ACTION_VIEW, directUserUri).apply {
                                                        setPackage(pkg)
                                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                    }
                                                    context.startActivity(pkgIntent)
                                                    launched = true
                                                } catch (_: Exception) {
                                                }
                                            }
                                        }

                                        if (!launched) {
                                            try {
                                                val universalIntent = Intent(Intent.ACTION_VIEW, directUserUri).apply {
                                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                }
                                                context.startActivity(universalIntent)
                                            } catch (_: Exception) {
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

        // 1.5 Biometric Login & App Lock Card
        item(key = "biometric_login_card") {
            val biometricCredentialLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.StartActivityForResult()
            ) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    viewModel.setBiometricLockEnabled(true)
                }
            }

            LiquidGlassCard(
                useDarkGlass = useDarkGlass,
                accentTint = TallyCyanRefraction,
                cornerRadius = 24.dp,
                testTag = "biometric_login_card",
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
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(TallyCyanRefraction.copy(alpha = 0.18f))
                                    .border(1.dp, TallyCyanRefraction.copy(alpha = 0.45f), RoundedCornerShape(14.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Fingerprint,
                                    contentDescription = null,
                                    tint = TallyCyanRefraction,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = if (strings.isBn) {
                                        "বায়োমেট্রিক লগইন (Biometric Lock)"
                                    } else {
                                        "Biometric Login & App Lock"
                                    },
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (strings.isBn) {
                                        "ফিঙ্গারপ্রিন্ট, ফেস আনলক বা ফোনের পিন দিয়ে হিসাব নিরাপদ রাখুন"
                                    } else {
                                        "Protect your ledger with Fingerprint, Face Unlock, or device PIN"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Switch(
                            checked = settings.biometricLockEnabled,
                            onCheckedChange = { shouldEnable ->
                                if (shouldEnable) {
                                    BiometricAuthHelper.authenticate(
                                        context = context,
                                        isBn = strings.isBn,
                                        onSuccess = {
                                            viewModel.setBiometricLockEnabled(true)
                                        },
                                        onError = { err ->
                                            if (err.isNotBlank()) {
                                                viewModel.showStatusMessage(err)
                                            }
                                        },
                                        onLaunchKeyguardCredentialIntent = { intent ->
                                            biometricCredentialLauncher.launch(intent)
                                        }
                                    )
                                } else {
                                    viewModel.setBiometricLockEnabled(false)
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = TallyEmeraldGreen
                            ),
                            modifier = Modifier.testTag("biometric_login_switch")
                        )
                    }

                    AnimatedVisibility(visible = settings.biometricLockEnabled) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (strings.isBn) {
                                    "✅ অ্যাপ চালুর সময় বায়োমেট্রিক লগইন সক্রিয় আছে"
                                } else {
                                    "✅ Biometric unlock required on app launch"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = TallyEmeraldGreen,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )

                            LiquidFilterChip(
                                label = if (strings.isBn) "এখনই লক করুন" else "Lock Now",
                                selected = false,
                                onClick = onLockAppNow,
                                useDarkGlass = useDarkGlass,
                                accentColor = TallyCyanRefraction,
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = TallyCyanRefraction
                                    )
                                },
                                testTag = "lock_app_now_button"
                            )
                        }
                    }
                }
            }
        }

        // 2. Profile / Business Branding, Account Mode (Business vs Individual) & Currency Customization
        item(key = "business_branding_card") {
            val isIndividual = settings.accountType.equals("INDIVIDUAL", ignoreCase = true)

            LiquidGlassCard(
                useDarkGlass = useDarkGlass,
                accentTint = if (isIndividual) TallyCyanRefraction else TallyEmeraldGreen,
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
                            imageVector = if (isIndividual) Icons.Default.Person else Icons.Default.Storefront,
                            contentDescription = null,
                            tint = if (isIndividual) TallyCyanRefraction else TallyEmeraldGreen
                        )
                        Text(
                            text = strings.businessBrandingTitle,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Owner Profile Picture Section (Default: Google Profile Photo, Override: Local Device Photo with No Sync)
                    val effectivePhoto = settings.effectiveProfilePhoto
                    val hasCustomLocalPhoto = settings.profilePhotoUri.isNotBlank()
                    val hasGooglePhoto = settings.googlePhotoUrl.isNotBlank()
                    val profileAccent = if (isIndividual) TallyCyanRefraction else TallyEmeraldGreen

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(profileAccent.copy(alpha = 0.10f))
                            .border(1.dp, profileAccent.copy(alpha = 0.35f), RoundedCornerShape(18.dp))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clickable {
                                    ownerPhotoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                }
                                .testTag("settings_owner_avatar_picker"),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(CircleShape)
                                    .background(profileAccent.copy(alpha = 0.22f))
                                    .border(2.dp, profileAccent, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                if (effectivePhoto.isNotBlank()) {
                                    AsyncImage(
                                        model = if (effectivePhoto.startsWith("/")) File(effectivePhoto) else effectivePhoto,
                                        contentDescription = settings.businessName.ifBlank { strings.appTitle },
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(CircleShape)
                                    )
                                } else {
                                    Icon(
                                        imageVector = if (isIndividual) Icons.Default.Person else Icons.Default.Storefront,
                                        contentDescription = null,
                                        tint = profileAccent,
                                        modifier = Modifier.size(30.dp)
                                    )
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .background(profileAccent)
                                    .border(1.5.dp, Color.White, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AddAPhoto,
                                    contentDescription = if (strings.isBn) "ছবি আপলোড" else "Upload Photo",
                                    tint = Color.White,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = if (strings.isBn) {
                                    when {
                                        hasCustomLocalPhoto -> "প্রোফাইল ছবি (ডিভাইস গ্যালারি)"
                                        hasGooglePhoto -> "প্রোফাইল ছবি (Google অ্যাকাউন্ট ডিফল্ট)"
                                        else -> "প্রোফাইল ছবি যুক্ত করুন"
                                    }
                                } else {
                                    when {
                                        hasCustomLocalPhoto -> "Profile Photo (Custom Device Image)"
                                        hasGooglePhoto -> "Profile Photo (Google Account Default)"
                                        else -> "Profile Picture"
                                    }
                                },
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (strings.isBn) {
                                    "ডিফল্টভাবে Google ছবি দেখায়। গ্যালারি থেকে আপলোড করা ছবি ক্লাউডে সিঙ্ক ছাড়াই শুধু এই ফোনে থাকে।"
                                } else {
                                    "Uses your Google photo by default. Uploaded photos stay on this device only (no cloud sync)."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                LiquidFilterChip(
                                    label = if (strings.isBn) "📷 গ্যালারি থেকে" else "📷 Upload Photo",
                                    selected = false,
                                    onClick = {
                                        ownerPhotoPickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    },
                                    useDarkGlass = useDarkGlass,
                                    accentColor = profileAccent,
                                    testTag = "upload_owner_photo_button"
                                )
                                if (hasCustomLocalPhoto) {
                                    LiquidFilterChip(
                                        label = if (hasGooglePhoto) {
                                            if (strings.isBn) "Google ছবি" else "Use Google Photo"
                                        } else {
                                            if (strings.isBn) "রিসেট" else "Reset"
                                        },
                                        selected = false,
                                        onClick = {
                                            viewModel.updateOwnerProfilePhoto(context, null)
                                        },
                                        useDarkGlass = useDarkGlass,
                                        accentColor = TallyGoldAccent,
                                        testTag = "reset_owner_photo_to_google_button"
                                    )
                                }
                            }
                        }
                    }

                    // Account Mode Selector: Business vs Individual
                    Text(
                        text = if (strings.isBn) "অ্যাকাউন্টের ধরন (Account Mode)" else "Account Type (Mode)",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        LiquidFilterChip(
                            label = if (strings.isBn) "🏪 ব্যবসা (Business)" else "🏪 Business",
                            selected = !isIndividual,
                            onClick = { viewModel.setAccountType("BUSINESS") },
                            useDarkGlass = useDarkGlass,
                            accentColor = TallyEmeraldGreen,
                            modifier = Modifier.weight(1f),
                            testTag = "settings_account_type_business_chip"
                        )
                        LiquidFilterChip(
                            label = if (strings.isBn) "👤 ব্যক্তিগত (Individual)" else "👤 Individual",
                            selected = isIndividual,
                            onClick = { viewModel.setAccountType("INDIVIDUAL") },
                            useDarkGlass = useDarkGlass,
                            accentColor = TallyCyanRefraction,
                            modifier = Modifier.weight(1f),
                            testTag = "settings_account_type_individual_chip"
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
                            focusedBorderColor = if (isIndividual) TallyCyanRefraction else TallyEmeraldGreen,
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
                            focusedBorderColor = if (isIndividual) TallyCyanRefraction else TallyEmeraldGreen,
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
                                currencySymbol = currencySymbol,
                                accountType = settings.accountType
                            )
                        },
                        accentColor = if (isIndividual) TallyCyanRefraction else TallyEmeraldGreen,
                        secondaryColor = if (isIndividual) Color(0xFF0077B6) else Color(0xFF007E33),
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
                                onClick = { categoryToDelete = cat },
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

    categoryToDelete?.let { cat ->
        ConfirmActionDialog(
            title = if (strings.isBn) "খাত মুছে ফেলবেন?" else "Remove Category?",
            message = if (strings.isBn) {
                "\"$cat\" খাতটি তালিকা থেকে মুছে ফেলতে চান?"
            } else {
                "Do you want to remove \"$cat\" from your custom categories?"
            },
            confirmLabel = if (strings.isBn) "হ্যাঁ, মুছুন" else "Remove",
            cancelLabel = strings.cancelButton,
            accentColor = TallyGoldAccent,
            useDarkGlass = useDarkGlass,
            onConfirm = {
                viewModel.removeCustomCategory(cat)
                categoryToDelete = null
            },
            onDismiss = { categoryToDelete = null },
            confirmTestTag = "confirm_remove_category_button",
            cancelTestTag = "cancel_remove_category_button"
        )
    }
}

