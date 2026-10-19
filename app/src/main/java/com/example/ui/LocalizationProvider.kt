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
                tabDashboard = "হিসাব খাতা",
                tabHistory = "লেনদেন তালিকা",
                tabSettings = "সেটিংস ও ব্যাকআপ",
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
                tabDashboard = "Ledger Book",
                tabHistory = "Transactions",
                tabSettings = "Settings & Backup",
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
        ownerPhone: String
    ): List<Pair<String, String>> {
        val cleanAmount = toBengaliDigits(numberFormat.format(abs(balanceAmount)))
        val politeTemplate =
            "প্রিয় $customerName, আসসালামু আলাইকুম। আমাদের '$businessName'-এ আপনার বর্তমান বকেয়া হিসাব $currencySymbol $cleanAmount। সুবিধাজনক সময়ে বকেয়া পরিশোধ করার জন্য বিনীত অনুরোধ করা হলো। যোগাযোগ: $ownerPhone।"

        val urgentTemplate =
            "সম্মানিত $customerName, '$businessName' থেকে আপনার পূর্বের বাকি $currencySymbol $cleanAmount দ্রুত পরিশোধ করার জন্য অনুরোধ জানানো হচ্ছে। বিকাশ/নগদ বা সরাসরি দোকানে জমা দিতে পারেন ($ownerPhone)। ধন্যবাদ।"

        val confirmationTemplate =
            "প্রিয় $customerName, '$businessName'-এর হিসাব খাতা অনুযায়ী আপনার বর্তমান ব্যালেন্স $currencySymbol $cleanAmount বাকি রয়েছে। বিস্তারিত জানতে কল করুন: $ownerPhone।"

        return listOf(
            "বিনীত তাগাদা (Polite Reminder)" to politeTemplate,
            "জরুরি তাগাদা (Urgent Reminder)" to urgentTemplate,
            "হিসাব বিবরণী (Balance Statement)" to confirmationTemplate
        )
    }
}
