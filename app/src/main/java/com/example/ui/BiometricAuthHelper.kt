package com.example.ui

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.biometrics.BiometricManager
import android.hardware.biometrics.BiometricPrompt
import android.os.Build
import android.os.CancellationSignal
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.ui.components.LiquidGlassBackdrop
import com.example.ui.components.LiquidGlassCard
import com.example.ui.components.SpringyLiquidButton
import com.example.ui.theme.TallyCyanRefraction
import com.example.ui.theme.TallyEmeraldGreen
import com.example.ui.theme.TallyRubyRed

object BiometricAuthHelper {

    fun isDeviceSecureOrBiometricAvailable(context: Context): Boolean {
        val keyguardManager = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
        val isDeviceSecure = keyguardManager?.isDeviceSecure == true
        if (isDeviceSecure) return true

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val bm = context.getSystemService(BiometricManager::class.java)
            if (bm != null) {
                val res = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    bm.canAuthenticate(
                        BiometricManager.Authenticators.BIOMETRIC_STRONG or
                            BiometricManager.Authenticators.BIOMETRIC_WEAK or
                            BiometricManager.Authenticators.DEVICE_CREDENTIAL
                    )
                } else {
                    @Suppress("DEPRECATION")
                    bm.canAuthenticate()
                }
                if (res == BiometricManager.BIOMETRIC_SUCCESS) return true
            }
        }
        return context.packageManager.hasSystemFeature(PackageManager.FEATURE_FINGERPRINT)
    }

    @Suppress("DEPRECATION")
    fun authenticate(
        context: Context,
        isBn: Boolean,
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
        onLaunchKeyguardCredentialIntent: ((Intent) -> Unit)? = null
    ) {
        val title = if (isBn) "TK Pro বায়োমেট্রিক লগইন" else "TK Pro Biometric Unlock"
        val subtitle = if (isBn) {
            "আপনার হিসাব খাতা আনলক করতে ফিঙ্গারপ্রিন্ট, ফেস বা পিন ব্যবহার করুন"
        } else {
            "Use your fingerprint, face, or screen lock PIN to unlock your ledger"
        }
        val cancelText = if (isBn) "বাতিল" else "Cancel"

        val keyguardManager = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
        val isDeviceSecure = keyguardManager?.isDeviceSecure == true

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                val executor = ContextCompat.getMainExecutor(context)
                val builder = BiometricPrompt.Builder(context)
                    .setTitle(title)
                    .setSubtitle(subtitle)

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    if (isDeviceSecure) {
                        builder.setAllowedAuthenticators(
                            BiometricManager.Authenticators.BIOMETRIC_STRONG or
                                BiometricManager.Authenticators.BIOMETRIC_WEAK or
                                BiometricManager.Authenticators.DEVICE_CREDENTIAL
                        )
                    } else {
                        builder.setNegativeButton(cancelText, executor) { _, _ -> }
                    }
                } else if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
                    if (isDeviceSecure) {
                        builder.setDeviceCredentialAllowed(true)
                    } else {
                        builder.setNegativeButton(cancelText, executor) { _, _ -> }
                    }
                } else {
                    builder.setNegativeButton(cancelText, executor) { _, _ -> }
                }

                val prompt = builder.build()
                val cancellationSignal = CancellationSignal()
                prompt.authenticate(
                    cancellationSignal,
                    executor,
                    object : BiometricPrompt.AuthenticationCallback() {
                        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult?) {
                            super.onAuthenticationSucceeded(result)
                            onSuccess()
                        }

                        override fun onAuthenticationError(errorCode: Int, errString: CharSequence?) {
                            super.onAuthenticationError(errorCode, errString)
                            // If no biometrics or screen lock are enrolled on device/emulator, fallback to Keyguard or allow unlock
                            if (!isDeviceSecure) {
                                onSuccess()
                            } else if (onLaunchKeyguardCredentialIntent != null) {
                                val credIntent = keyguardManager?.createConfirmDeviceCredentialIntent(title, subtitle)
                                if (credIntent != null) {
                                    onLaunchKeyguardCredentialIntent(credIntent)
                                } else {
                                    onError(errString?.toString() ?: "")
                                }
                            } else {
                                onError(errString?.toString() ?: "")
                            }
                        }

                        override fun onAuthenticationFailed() {
                            super.onAuthenticationFailed()
                        }
                    }
                )
                return
            } catch (_: Exception) {
                // Fall through to KeyguardManager below
            }
        }

        if (isDeviceSecure && onLaunchKeyguardCredentialIntent != null) {
            val intent = keyguardManager?.createConfirmDeviceCredentialIntent(title, subtitle)
            if (intent != null) {
                onLaunchKeyguardCredentialIntent(intent)
                return
            }
        }

        // If device has no screen lock or biometric enrolled at all (e.g. cloud emulator)
        onSuccess()
    }
}

@Composable
fun BiometricLockScreen(
    isBn: Boolean,
    isIndividual: Boolean,
    profileName: String,
    currencySymbol: String,
    useDarkGlass: Boolean,
    profilePhotoUri: String = "",
    onUnlocked: () -> Unit
) {
    val context = LocalContext.current
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val credentialLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            onUnlocked()
        } else {
            errorMessage = if (isBn) {
                "যাচাইকরণ সম্পন্ন হয়নি। আবার চেষ্টা করুন।"
            } else {
                "Verification was not completed. Please try again."
            }
        }
    }

    val triggerBiometric = {
        errorMessage = null
        BiometricAuthHelper.authenticate(
            context = context,
            isBn = isBn,
            onSuccess = onUnlocked,
            onError = { err ->
                if (err.isNotBlank()) {
                    errorMessage = err
                }
            },
            onLaunchKeyguardCredentialIntent = { intent ->
                credentialLauncher.launch(intent)
            }
        )
    }

    LaunchedEffect(Unit) {
        triggerBiometric()
    }

    LiquidGlassBackdrop(useDarkGlass = useDarkGlass) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 20.dp)
                .testTag("biometric_lock_screen"),
            contentAlignment = Alignment.Center
        ) {
            LiquidGlassCard(
                useDarkGlass = useDarkGlass,
                accentTint = TallyCyanRefraction,
                cornerRadius = 32.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 460.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    // Glowing Avatar / Fingerprint Badge
                    Box(
                        modifier = Modifier.size(92.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(88.dp)
                                .clip(CircleShape)
                                .background(TallyCyanRefraction.copy(alpha = 0.16f))
                                .border(2.dp, TallyCyanRefraction.copy(alpha = 0.7f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            if (profilePhotoUri.isNotBlank()) {
                                coil.compose.AsyncImage(
                                    model = if (profilePhotoUri.startsWith("/")) java.io.File(profilePhotoUri) else profilePhotoUri,
                                    contentDescription = profileName,
                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape)
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Fingerprint,
                                    contentDescription = if (isBn) "ফিঙ্গারপ্রিন্ট আনলক" else "Fingerprint Unlock",
                                    tint = TallyCyanRefraction,
                                    modifier = Modifier.size(48.dp)
                                )
                            }
                        }
                        if (profilePhotoUri.isNotBlank()) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .background(TallyCyanRefraction)
                                    .border(1.5.dp, Color.White, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Fingerprint,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    // Security Pill
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(TallyEmeraldGreen.copy(alpha = 0.14f))
                            .border(1.dp, TallyEmeraldGreen.copy(alpha = 0.45f), RoundedCornerShape(50))
                            .padding(horizontal = 12.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = TallyEmeraldGreen,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = if (isBn) "বায়োমেট্রিক নিরাপত্তা সক্রিয়" else "Biometric Protection Active",
                            style = MaterialTheme.typography.labelMedium,
                            color = TallyEmeraldGreen,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = profileName.ifBlank { "TK Pro" },
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = if (isBn) {
                                if (isIndividual) {
                                    "আপনার ব্যক্তিগত হিসাব খাতা লক করা আছে"
                                } else {
                                    "আপনার ব্যবসার হিসাব খাতা লক করা আছে"
                                }
                            } else {
                                if (isIndividual) {
                                    "Your personal ledger is locked"
                                } else {
                                    "Your business ledger is locked"
                                }
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }

                    if (!errorMessage.isNullOrBlank()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(TallyRubyRed.copy(alpha = 0.14f))
                                .border(1.dp, TallyRubyRed.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Text(
                                text = errorMessage!!,
                                style = MaterialTheme.typography.bodySmall,
                                color = TallyRubyRed,
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    SpringyLiquidButton(
                        onClick = { triggerBiometric() },
                        accentColor = TallyCyanRefraction,
                        secondaryColor = Color(0xFF0077B6),
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
                        testTag = "unlock_biometric_button"
                    ) {
                        Icon(
                            imageVector = Icons.Default.Fingerprint,
                            contentDescription = null,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = if (isBn) "বায়োমেট্রিক / পিন দিয়ে আনলক করুন" else "Unlock with Biometric / PIN",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
