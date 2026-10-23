package com.example.ui

import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

data class AppStrings(
    val isBn: Boolean,
    val appTitle: String,
    val appSubtitle: String,
    val tabDashboard: String,
    val tabHistory: String,
    val tabSettings: String,
    val totalReceivable: String,
    val totalPayable: String,
    val netBalance: String,
    val receivableBadge: String,
    val payableBadge: String,
    val settledBadge: String,
    val debitTitle: String,
    val debitShort: String,
    val creditTitle: String,
    val creditShort: String,
    val addCustomer: String,
    val importContact: String,
    val customerNameLabel: String,
    val phoneNumberLabel: String,
    val addressLabel: String,
    val partyTypeCustomer: String,
    val partyTypeSupplier: String,
    val searchPlaceholder: String,
    val filterAll: String,
    val filterReceivable: String,
    val filterPayable: String,
    val filterSupplier: String,
    val addEntryButton: String,
    val amountLabel: String,
    val categoryLabel: String,
    val paymentMethodLabel: String,
    val noteLabel: String,
    val attachReceiptLabel: String,
    val cameraReceiptButton: String,
    val galleryReceiptButton: String,
    val saveButton: String,
    val cancelButton: String,
    val deleteButton: String,
    val callDirect: String,
    val smsReminder: String,
    val smsTemplateSheetTitle: String,
    val sendSmsAction: String,
    val copySmsAction: String,
    val settingsTitle: String,
    val businessBrandingTitle: String,
    val businessNameLabel: String,
    val ownerPhoneLabel: String,
    val currencySymbolLabel: String,
    val saveBrandingButton: String,
    val categoriesSectionTitle: String,
    val newCategoryPlaceholder: String,
    val addCategoryButton: String,
    val backupSectionTitle: String,
    val backupSubtitle: String,
    val exportJsonButton: String,
    val importJsonButton: String,
    val languageSectionTitle: String,
    val glassThemeSectionTitle: String,
    val darkGlassModeLabel: String,
    val lightGlassModeLabel: String,
    val noCustomersEmptyTitle: String,
    val noCustomersEmptyDesc: String,
    val noTransactionsEmptyTitle: String,
    val noTransactionsEmptyDesc: String,
    val quickNewEntry: String,
    val selectCustomerLabel: String,
    val dateTimeLabel: String,
    val receiptAttachedBadge: String,
    val viewReceiptTitle: String,
    val closeButton: String,
    val balanceAfterTxLabel: String
)

object LocalizationProvider {

    fun getStrings(lang: String): AppStrings {
        val isBn = lang != "EN"
        return if (isBn) {
            AppStrings(
                isBn = true,
                appTitle = "TK Pro",
                appSubtitle = "ডিজিটাল হিসাব ও বাকি খাতা",
                tabDashboard = "খাতা",
                tabHistory = "ইতিহাস",
                tabSettings = "সেটিংস",
                totalReceivable = "মোট পাবেন",
                totalPayable = "মোট দেবেন",
                netBalance = "মোট ব্যালেন্স",
                receivableBadge = "পাবেন",
                payableBadge = "দেবেন",
                settledBadge = "পরিশোধিত",
                debitTitle = "টাকা পাব / দিয়েছি",
                debitShort = "দিয়েছি / পাব",
                creditTitle = "টাকা দেব / পেয়েছি",
                creditShort = "পেয়েছি / দেব",
                addCustomer = "নতুন কাস্টমার / পার্টি",
                importContact = "ফোন কন্টাক্ট থেকে নিন",
                customerNameLabel = "কাস্টমার বা পার্টির নাম",
                phoneNumberLabel = "মোবাইল নম্বর",
                addressLabel = "ঠিকানা বা নোট (ঐচ্ছিক)",
                partyTypeCustomer = "কাস্টমার",
                partyTypeSupplier = "সাপ্লায়ার",
                searchPlaceholder = "নাম বা মোবাইল নম্বর দিয়ে খুঁজুন...",
                filterAll = "সব পার্টি",
                filterReceivable = "পাবেন (বাকি)",
                filterPayable = "দেবেন",
                filterSupplier = "সাপ্লায়ার",
                addEntryButton = "নতুন হিসাব এন্ট্রি",
                amountLabel = "টাকার পরিমাণ",
                categoryLabel = "খাত / ক্যাটাগরি নির্বাচন করুন",
                paymentMethodLabel = "পেমেন্ট মাধ্যম",
                noteLabel = "বিবরণ বা পণ্যের তালিকা লিখুন",
                attachReceiptLabel = "ভাউচার বা রশিদের ছবি যুক্ত করুন",
                cameraReceiptButton = "ক্যামেরায় ছবি তুলুন",
                galleryReceiptButton = "গ্যালারি থেকে নিন",
                saveButton = "হিসাব সেভ করুন",
                cancelButton = "বাতিল",
                deleteButton = "মুছে ফেলুন",
                callDirect = "কল করুন",
                smsReminder = "তাগাদা মেসেজ",
                smsTemplateSheetTitle = "বকেয়া তাগাদা মেসেজ (SMS Reminder)",
                sendSmsAction = "মেসেজ অ্যাপে পাঠান",
                copySmsAction = "টেক্সট কপি করুন",
                settingsTitle = "ব্যবসা ও অ্যাপ সেটিংস",
                businessBrandingTitle = "ব্যবসার পরিচিতি ও কারেন্সি",
                businessNameLabel = "ব্যবসা প্রতিষ্ঠানের নাম",
                ownerPhoneLabel = "ব্যবসার মোবাইল নম্বর",
                currencySymbolLabel = "মুদ্রার চিহ্ন (৳, Tk, $)",
                saveBrandingButton = "তথ্য আপডেট করুন",
                categoriesSectionTitle = "কাস্টম হিসাবের খাতসমূহ",
                newCategoryPlaceholder = "নতুন খাতের নাম লিখুন...",
                addCategoryButton = "খাত যোগ করুন",
                backupSectionTitle = "ডাটাবেস ব্যাকআপ ও রিস্টোর (JSON)",
                backupSubtitle = "আপনার সব কাস্টমার ও লেনদেন নিরাপদে JSON ফাইলে এক্সপোর্ট বা ইমপোর্ট করুন।",
                exportJsonButton = "JSON ব্যাকআপ এক্সপোর্ট করুন",
                importJsonButton = "JSON ব্যাকআপ ইমপোর্ট করুন",
                languageSectionTitle = "ভাষা পরিবর্তন (Language)",
                glassThemeSectionTitle = "থিম মোড (Theme Mode)",
                darkGlassModeLabel = "ডার্ক থিম (Dark Mode)",
                lightGlassModeLabel = "লাইট থিম (Light Mode)",
                noCustomersEmptyTitle = "কোনো কাস্টমার পাওয়া যায়নি",
                noCustomersEmptyDesc = "নতুন কাস্টমার যোগ করতে নিচের '+' বাটনে চাপুন অথবা ফোন কন্টাক্ট থেকে ইমপোর্ট করুন।",
                noTransactionsEmptyTitle = "এখনো কোনো লেনদেন নেই",
                noTransactionsEmptyDesc = "'টাকা পাব / দিয়েছি' অথবা 'টাকা দেব / পেয়েছি' বাটনে চেপে প্রথম হিসাব লিখুন।",
                quickNewEntry = "দ্রুত হিসাব যোগ",
                selectCustomerLabel = "কাস্টমার নির্বাচন করুন",
                dateTimeLabel = "সময় ও তারিখ",
                receiptAttachedBadge = "রশিদ যুক্ত আছে",
                viewReceiptTitle = "ভাউচার / রশিদের ছবি",
                closeButton = "বন্ধ করুন",
                balanceAfterTxLabel = "হিসাব শেষে দেনা/পাওনা"
            )
        } else {
            AppStrings(
                isBn = false,
                appTitle = "TK Pro",
                appSubtitle = "Digital Ledger & Business Khata",
                tabDashboard = "Ledger",
                tabHistory = "History",
                tabSettings = "Settings",
                totalReceivable = "মোট পাবেন (Receivable)",
                totalPayable = "মোট দেবেন (Payable)",
                netBalance = "মোট ব্যালেন্স (Net Balance)",
                receivableBadge = "Receivable (পাবেন)",
                payableBadge = "Payable (দেবেন)",
                settledBadge = "Settled",
                debitTitle = "টাকা পাব / দিয়েছি (Debit)",
                debitShort = "Debit (পাব)",
                creditTitle = "টাকা দেব / পেয়েছি (Credit)",
                creditShort = "Credit (দেব)",
                addCustomer = "Add Customer / Party",
                importContact = "Pick from Contacts",
                customerNameLabel = "Customer or Party Name",
                phoneNumberLabel = "Mobile Number",
                addressLabel = "Address or Note (Optional)",
                partyTypeCustomer = "Customer",
                partyTypeSupplier = "Supplier",
                searchPlaceholder = "Search by name or phone...",
                filterAll = "All Parties",
                filterReceivable = "Receivable (পাবেন)",
                filterPayable = "Payable (দেবেন)",
                filterSupplier = "Suppliers",
                addEntryButton = "Add Ledger Entry",
                amountLabel = "Amount",
                categoryLabel = "Select Category",
                paymentMethodLabel = "Payment Method",
                noteLabel = "Notes or Item Details",
                attachReceiptLabel = "Attach Receipt / Voucher Photo",
                cameraReceiptButton = "Take Camera Photo",
                galleryReceiptButton = "Pick from Gallery",
                saveButton = "Save Entry",
                cancelButton = "Cancel",
                deleteButton = "Delete",
                callDirect = "Direct Call",
                smsReminder = "SMS Reminder",
                smsTemplateSheetTitle = "Overdue Balance SMS Reminder",
                sendSmsAction = "Open in SMS App",
                copySmsAction = "Copy Message",
                settingsTitle = "Business & App Settings",
                businessBrandingTitle = "Business Branding & Currency",
                businessNameLabel = "Business Name",
                ownerPhoneLabel = "Business Phone Number",
                currencySymbolLabel = "Currency Symbol (৳, Tk, $)",
                saveBrandingButton = "Update Branding",
                categoriesSectionTitle = "Custom Transaction Categories",
                newCategoryPlaceholder = "Enter new category...",
                addCategoryButton = "Add Category",
                backupSectionTitle = "Database Backup & Restore (JSON)",
                backupSubtitle = "Export or import all customers, transactions, and settings as a portable JSON file.",
                exportJsonButton = "Export JSON Backup",
                importJsonButton = "Import JSON Backup",
                languageSectionTitle = "App Language (ভাষা)",
                glassThemeSectionTitle = "Liquid Glass Surface Mode",
                darkGlassModeLabel = "Dark Liquid Glass",
                lightGlassModeLabel = "Light Frosted Glass",
                noCustomersEmptyTitle = "No Customers Found",
                noCustomersEmptyDesc = "Tap '+' below to add a customer or import directly from your phone contacts.",
                noTransactionsEmptyTitle = "No Transactions Recorded",
                noTransactionsEmptyDesc = "Tap 'টাকা পাব / দিয়েছি' (Debit) or 'টাকা দেব / পেয়েছি' (Credit) to record an entry.",
                quickNewEntry = "Quick Ledger Entry",
                selectCustomerLabel = "Select Customer",
                dateTimeLabel = "Timestamp",
                receiptAttachedBadge = "Receipt Attached",
                viewReceiptTitle = "Attached Receipt / Voucher",
                closeButton = "Close",
                balanceAfterTxLabel = "Balance after entry"
            )
        }
    }

    private val numberFormat = DecimalFormat("#,##,##0.##")

    fun formatAmount(amount: Double, currencySymbol: String, useBengaliDigits: Boolean): String {
        val clean = abs(amount)
        val formatted = numberFormat.format(clean)
        val localizedDigits = if (useBengaliDigits) toBengaliDigits(formatted) else formatted
        return "$currencySymbol $localizedDigits"
    }

    fun toBengaliDigits(input: String): String {
        val bnDigits = charArrayOf('০', '১', '২', '৩', '৪', '৫', '৬', '৭', '৮', '৯')
        val sb = StringBuilder(input.length)
        for (ch in input) {
            if (ch in '0'..'9') {
                sb.append(bnDigits[ch - '0'])
            } else {
                sb.append(ch)
            }
        }
        return sb.toString()
    }

    fun formatTimestamp(timestamp: Long, useBengaliDigits: Boolean): String {
        val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.ENGLISH)
        val raw = sdf.format(Date(timestamp))
        return if (useBengaliDigits) toBengaliDigits(raw) else raw
    }

    fun paymentMethodLabel(method: String, isBn: Boolean): String {
        return when (method.uppercase()) {
            "CASH" -> if (isBn) "নগদ (Cash)" else "Cash"
            "BKASH" -> if (isBn) "বিকাশ (bKash)" else "bKash"
            "NAGAD" -> if (isBn) "নগদ অ্যাপ (Nagad)" else "Nagad"
            "BANK" -> if (isBn) "ব্যাংক (Bank)" else "Bank"
            else -> method
        }
    }

    fun generateBengaliSmsTemplates(
        customerName: String,
        balanceAmount: Double,
        currencySymbol: String,
        businessName: String,
        ownerPhone: String,
        isCreditMode: Boolean = balanceAmount < -0.001,
        lastTxAmount: Double? = null,
        isBn: Boolean = true
    ): List<Pair<String, String>> {
        val effectiveShop = businessName.ifBlank { "TK Pro" }
        val contactSuffix = if (ownerPhone.isNotBlank()) {
            if (isBn) " যোগাযোগ: $ownerPhone।" else " Contact: $ownerPhone."
        } else {
            ""
        }
        val cleanBalance = if (isBn) {
            toBengaliDigits(numberFormat.format(abs(balanceAmount)))
        } else {
            numberFormat.format(abs(balanceAmount))
        }
        val cleanLastTx = lastTxAmount?.takeIf { it > 0.0 }?.let {
            if (isBn) toBengaliDigits(numberFormat.format(abs(it))) else numberFormat.format(abs(it))
        } ?: cleanBalance

        val currentStatusText = when {
            balanceAmount > 0.001 -> if (isBn) {
                "বর্তমান বকেয়া: $currencySymbol $cleanBalance (পাব)"
            } else {
                "Current Due: $currencySymbol $cleanBalance (Receivable)"
            }
            balanceAmount < -0.001 -> if (isBn) {
                "আপনার পাওনা/জমা: $currencySymbol $cleanBalance (দেব)"
            } else {
                "Your Credit Balance: $currencySymbol $cleanBalance (Payable)"
            }
            else -> if (isBn) {
                "বর্তমান ব্যালেন্স: $currencySymbol ০ (পরিশোধিত)"
            } else {
                "Current Balance: $currencySymbol 0 (Settled)"
            }
        }

        return if (!isCreditMode) {
            // DEBIT / RECEIVABLE TEMPLATES (টাকা পাব / দিয়েছি — বাকি ও তাগাদা মেসেজ)
            if (isBn) {
                val debitEntryMsg =
                    "প্রিয় $customerName, '$effectiveShop'-এ আপনার নামে $currencySymbol $cleanLastTx বাকি/প্রদান (Debit) হিসাব যোগ হয়েছে। $currentStatusText।$contactSuffix"
                val politeReminder =
                    "প্রিয় $customerName, আসসালামু আলাইকুম। আমাদের '$effectiveShop'-এ আপনার বর্তমান বকেয়া হিসাব $currencySymbol $cleanBalance। সুবিধাজনক সময়ে বকেয়া পরিশোধ করার জন্য বিনীত অনুরোধ করা হলো।$contactSuffix"
                val urgentReminder =
                    "সম্মানিত $customerName, '$effectiveShop' থেকে আপনার পূর্বের বাকি $currencySymbol $cleanBalance দ্রুত পরিশোধ করার জন্য অনুরোধ জানানো হচ্ছে। বিকাশ/নগদ বা সরাসরি দোকানে জমা দিতে পারেন।$contactSuffix ধন্যবাদ।"

                listOf(
                    "বাকি তাগাদা (Due Reminder)" to politeReminder,
                    "নতুন বাকি (Debit Entry)" to debitEntryMsg,
                    "জরুরি তাগাদা (Urgent Due)" to urgentReminder
                )
            } else {
                val debitEntryMsg =
                    "Dear $customerName, a Debit entry of $currencySymbol $cleanLastTx has been recorded in '$effectiveShop'. $currentStatusText.$contactSuffix"
                val politeReminder =
                    "Dear $customerName, your current due balance at '$effectiveShop' is $currencySymbol $cleanBalance. Kindly clear the due at your convenience.$contactSuffix"
                val urgentReminder =
                    "Dear $customerName, please settle your overdue balance of $currencySymbol $cleanBalance at '$effectiveShop' as soon as possible.$contactSuffix Thank you."

                listOf(
                    "Due Reminder" to politeReminder,
                    "Debit Entry" to debitEntryMsg,
                    "Urgent Reminder" to urgentReminder
                )
            }
        } else {
            // CREDIT / PAYMENT RECEIVED / PAYABLE TEMPLATES (টাকা দেব / পেয়েছি — জমা ও পরিশোধ মেসেজ)
            if (isBn) {
                val paymentReceivedMsg =
                    "প্রিয় $customerName, '$effectiveShop'-এ আপনার $currencySymbol $cleanLastTx জমা/পরিশোধ (Credit) সফলভাবে গৃহীত হয়েছে। ধন্যবাদ! $currentStatusText।$contactSuffix"
                val payableAckMsg =
                    "প্রিয় $customerName, আসসালামু আলাইকুম। আমাদের '$effectiveShop'-এর হিসাব অনুযায়ী আপনার $currencySymbol $cleanBalance আমাদের কাছে পাওনা/জমা (Credit) রয়েছে। ইনশাআল্লাহ শীঘ্রই পরিশোধ করা হবে।$contactSuffix"
                val settledOrStatementMsg =
                    "প্রিয় $customerName, '$effectiveShop'-এ আপনার জমা হিসাব আপডেট করা হয়েছে। $currentStatusText। আমাদের সাথে লেনদেনের জন্য ধন্যবাদ।$contactSuffix"

                listOf(
                    "জমা প্রাপ্তি (Payment Received)" to paymentReceivedMsg,
                    "দেবেন / পাওনা (Payable Info)" to payableAckMsg,
                    "জমা বিবরণী (Credit Summary)" to settledOrStatementMsg
                )
            } else {
                val paymentReceivedMsg =
                    "Dear $customerName, we have received your payment/credit of $currencySymbol $cleanLastTx at '$effectiveShop'. Thank you! $currentStatusText.$contactSuffix"
                val payableAckMsg =
                    "Dear $customerName, according to '$effectiveShop' records, a credit/payable balance of $currencySymbol $cleanBalance is due to you.$contactSuffix"
                val settledOrStatementMsg =
                    "Dear $customerName, your credit ledger at '$effectiveShop' has been updated. $currentStatusText. Thank you for doing business with us!$contactSuffix"

                listOf(
                    "Payment Received" to paymentReceivedMsg,
                    "Payable Info" to payableAckMsg,
                    "Credit Summary" to settledOrStatementMsg
                )
            }
        }
    }
}
