package com.example.ui

import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs

data class AppStrings(
    val isBn: Boolean,
    val isIndividual: Boolean,
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

    fun getStrings(lang: String, accountType: String = "BUSINESS"): AppStrings {
        val isBn = lang != "EN"
        val isInd = accountType.equals("INDIVIDUAL", ignoreCase = true)
        return if (isBn) {
            AppStrings(
                isBn = true,
                isIndividual = isInd,
                appTitle = "TK Pro",
                appSubtitle = if (isInd) "ব্যক্তিগত ও ব্যবসায়িক লেনদেন খাতা" else "ডিজিটাল হিসাব ও বাকি খাতা",
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
                addCustomer = if (isInd) "নতুন ব্যক্তি / প্রতিষ্ঠান" else "নতুন কাস্টমার / পার্টি",
                importContact = "ফোন কন্টাক্ট থেকে নিন",
                customerNameLabel = if (isInd) "ব্যক্তি বা প্রতিষ্ঠানের নাম" else "কাস্টমার বা পার্টির নাম",
                phoneNumberLabel = "মোবাইল নম্বর",
                addressLabel = "ঠিকানা বা নোট (ঐচ্ছিক)",
                partyTypeCustomer = if (isInd) "ব্যক্তি" else "কাস্টমার",
                partyTypeSupplier = if (isInd) "ব্যবসা / শপ" else "সাপ্লায়ার",
                searchPlaceholder = "নাম বা মোবাইল নম্বর দিয়ে খুঁজুন...",
                filterAll = if (isInd) "সব হিসাব" else "সব পার্টি",
                filterReceivable = "পাবেন (বাকি)",
                filterPayable = "দেবেন",
                filterSupplier = if (isInd) "ব্যবসা / শপ" else "সাপ্লায়ার",
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
                smsTemplateSheetTitle = "মেসেজ ও রিমাইন্ডার",
                sendSmsAction = "মেসেজ অ্যাপে পাঠান",
                copySmsAction = "টেক্সট কপি করুন",
                settingsTitle = if (isInd) "প্রোফাইল ও অ্যাপ সেটিংস" else "ব্যবসা ও অ্যাপ সেটিংস",
                businessBrandingTitle = if (isInd) "প্রোফাইল পরিচিতি ও কারেন্সি" else "ব্যবসার পরিচিতি ও কারেন্সি",
                businessNameLabel = if (isInd) "আপনার নাম" else "ব্যবসা প্রতিষ্ঠানের নাম",
                ownerPhoneLabel = if (isInd) "আপনার মোবাইল নম্বর" else "ব্যবসার মোবাইল নম্বর",
                currencySymbolLabel = "মুদ্রার চিহ্ন (৳, Tk, $)",
                saveBrandingButton = "তথ্য আপডেট করুন",
                categoriesSectionTitle = "কাস্টম হিসাবের খাতসমূহ",
                newCategoryPlaceholder = "নতুন খাতের নাম লিখুন...",
                addCategoryButton = "খাত যোগ করুন",
                backupSectionTitle = "ডাটাবেস ব্যাকআপ ও রিস্টোর (JSON)",
                backupSubtitle = "আপনার সব হিসাব ও লেনদেন নিরাপদে JSON ফাইলে এক্সপোর্ট বা ইমপোর্ট করুন।",
                exportJsonButton = "JSON ব্যাকআপ এক্সপোর্ট করুন",
                importJsonButton = "JSON ব্যাকআপ ইমপোর্ট করুন",
                languageSectionTitle = "অ্যাপের ভাষা",
                glassThemeSectionTitle = "থিম মোড",
                darkGlassModeLabel = "ডার্ক গ্লাস থিম",
                lightGlassModeLabel = "লাইট গ্লাস থিম",
                noCustomersEmptyTitle = if (isInd) "কোনো ব্যক্তি বা প্রতিষ্ঠান যুক্ত নেই" else "কোনো কাস্টমার পাওয়া যায়নি",
                noCustomersEmptyDesc = if (isInd) {
                    "নতুন ব্যক্তি বা ব্যবসা প্রতিষ্ঠানের হিসাব যোগ করতে নিচের '+' বাটনে চাপুন অথবা ফোন কন্টাক্ট থেকে ইমপোর্ট করুন।"
                } else {
                    "নতুন কাস্টমার যোগ করতে নিচের '+' বাটনে চাপুন অথবা ফোন কন্টাক্ট থেকে ইমপোর্ট করুন।"
                },
                noTransactionsEmptyTitle = "এখনো কোনো লেনদেন নেই",
                noTransactionsEmptyDesc = "'টাকা পাব / দিয়েছি' অথবা 'টাকা দেব / পেয়েছি' বাটনে চেপে প্রথম হিসাব লিখুন।",
                quickNewEntry = "দ্রুত হিসাব যোগ",
                selectCustomerLabel = if (isInd) "ব্যক্তি বা প্রতিষ্ঠান নির্বাচন করুন" else "কাস্টমার নির্বাচন করুন",
                dateTimeLabel = "সময় ও তারিখ",
                receiptAttachedBadge = "রশিদ যুক্ত আছে",
                viewReceiptTitle = "ভাউচার / রশিদের ছবি",
                closeButton = "বন্ধ করুন",
                balanceAfterTxLabel = "হিসাব শেষে দেনা/পাওনা"
            )
        } else {
            AppStrings(
                isBn = false,
                isIndividual = isInd,
                appTitle = "TK Pro",
                appSubtitle = if (isInd) "Personal & Party Ledger" else "Digital Ledger & Business Khata",
                tabDashboard = "Ledger",
                tabHistory = "History",
                tabSettings = "Settings",
                totalReceivable = "Total Receivable",
                totalPayable = "Total Payable",
                netBalance = "Net Balance",
                receivableBadge = "Receivable",
                payableBadge = "Payable",
                settledBadge = "Settled",
                debitTitle = "You Gave (Debit)",
                debitShort = "You Gave",
                creditTitle = "You Got (Credit)",
                creditShort = "You Got",
                addCustomer = if (isInd) "Add Person / Business" else "Add Customer / Party",
                importContact = "Pick from Contacts",
                customerNameLabel = if (isInd) "Person or Business Name" else "Customer or Party Name",
                phoneNumberLabel = "Mobile Number",
                addressLabel = "Address or Note (Optional)",
                partyTypeCustomer = if (isInd) "Person" else "Customer",
                partyTypeSupplier = if (isInd) "Business / Shop" else "Supplier",
                searchPlaceholder = "Search by name or phone...",
                filterAll = if (isInd) "All Accounts" else "All Parties",
                filterReceivable = "Receivable",
                filterPayable = "Payable",
                filterSupplier = if (isInd) "Businesses" else "Suppliers",
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
                smsReminder = "Send Message",
                smsTemplateSheetTitle = "Messages & Reminders",
                sendSmsAction = "Open in SMS App",
                copySmsAction = "Copy Message",
                settingsTitle = if (isInd) "Profile & App Settings" else "Business & App Settings",
                businessBrandingTitle = if (isInd) "Personal Profile & Currency" else "Business Profile & Currency",
                businessNameLabel = if (isInd) "Your Full Name" else "Business Name",
                ownerPhoneLabel = if (isInd) "Your Phone Number" else "Business Phone Number",
                currencySymbolLabel = "Currency Symbol (৳, Tk, $)",
                saveBrandingButton = if (isInd) "Update Profile" else "Update Business Info",
                categoriesSectionTitle = "Custom Transaction Categories",
                newCategoryPlaceholder = "Enter new category...",
                addCategoryButton = "Add Category",
                backupSectionTitle = "Database Backup & Restore (JSON)",
                backupSubtitle = "Export or import all accounts, transactions, and settings as a portable JSON file.",
                exportJsonButton = "Export JSON Backup",
                importJsonButton = "Import JSON Backup",
                languageSectionTitle = "App Language",
                glassThemeSectionTitle = "Theme Mode",
                darkGlassModeLabel = "Dark Glass Mode",
                lightGlassModeLabel = "Light Glass Mode",
                noCustomersEmptyTitle = if (isInd) "No Person or Business Found" else "No Customers Found",
                noCustomersEmptyDesc = if (isInd) {
                    "Tap '+' below to add a person or business, or import directly from your phone contacts."
                } else {
                    "Tap '+' below to add a customer or import directly from your phone contacts."
                },
                noTransactionsEmptyTitle = "No Transactions Recorded",
                noTransactionsEmptyDesc = "Tap 'You Gave (Debit)' or 'You Got (Credit)' to record your first entry.",
                quickNewEntry = "Quick Ledger Entry",
                selectCustomerLabel = if (isInd) "Select Person or Business" else "Select Customer",
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
        if (!useBengaliDigits) {
            val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.ENGLISH)
            return sdf.format(Date(timestamp))
        }
        val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
        val day = cal.get(Calendar.DAY_OF_MONTH).toString().padStart(2, '0')
        val monthNamesBn = arrayOf(
            "জানু", "ফেব্রু", "মার্চ", "এপ্রিল", "মে", "জুন",
            "জুলাই", "আগস্ট", "সেপ্টে", "অক্টো", "নভে", "ডিসে"
        )
        val monthBn = monthNamesBn[cal.get(Calendar.MONTH).coerceIn(0, 11)]
        val year = cal.get(Calendar.YEAR).toString()
        val hour24 = cal.get(Calendar.HOUR_OF_DAY)
        val hour12 = when (val h = hour24 % 12) {
            0 -> 12
            else -> h
        }.toString().padStart(2, '0')
        val minute = cal.get(Calendar.MINUTE).toString().padStart(2, '0')
        val periodBn = when (hour24) {
            in 5..11 -> "সকাল"
            in 12..15 -> "দুপুর"
            in 16..17 -> "বিকাল"
            in 18..19 -> "সন্ধ্যা"
            else -> "রাত"
        }
        return toBengaliDigits("$day $monthBn $year, $periodBn $hour12:$minute")
    }

    fun paymentMethodLabel(method: String, isBn: Boolean): String {
        val trimmed = method.trim()
        return when (trimmed.uppercase()) {
            "" -> ""
            "CASH" -> if (isBn) "নগদ" else "Cash"
            "BKASH" -> if (isBn) "বিকাশ" else "bKash"
            "NAGAD" -> if (isBn) "নগদ অ্যাপ" else "Nagad"
            "BANK" -> if (isBn) "ব্যাংক" else "Bank"
            else -> trimmed
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
        isBn: Boolean = true,
        lastTxType: String? = null
    ): List<Pair<String, String>> {
        val bnShopAt = if (businessName.isNotBlank()) "'$businessName'-এ" else "আমাদের হিসাবে"
        val bnShopPossessive = if (businessName.isNotBlank()) "'$businessName'-এর" else "আমাদের"
        val enShopAt = if (businessName.isNotBlank()) "at '$businessName'" else "with us"
        val enShopWith = if (businessName.isNotBlank()) "with '$businessName'" else "with us"
        val enShopFrom = if (businessName.isNotBlank()) "from '$businessName'" else "from us"
        val enShopRecords = if (businessName.isNotBlank()) "per '$businessName' records" else "per our records"

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

        val expectedType = if (isCreditMode) "CREDIT" else "DEBIT"
        val matchingTxAmount = lastTxAmount
            ?.takeIf { it > 0.0 && (lastTxType == null || lastTxType.equals(expectedType, ignoreCase = true)) }
            ?.let { amt ->
                if (isBn) toBengaliDigits(numberFormat.format(abs(amt))) else numberFormat.format(abs(amt))
            }

        // Written strictly from the recipient's perspective
        val currentStatusText = when {
            balanceAmount > 0.001 -> if (isBn) {
                "আপনার বর্তমান বকেয়া: $currencySymbol $cleanBalance।"
            } else {
                "Your current due balance is $currencySymbol $cleanBalance."
            }
            balanceAmount < -0.001 -> if (isBn) {
                "আমাদের কাছে আপনার জমা রয়েছে: $currencySymbol $cleanBalance।"
            } else {
                "Your advance credit balance with us is $currencySymbol $cleanBalance."
            }
            else -> if (isBn) {
                "বর্তমানে আপনার হিসাব সম্পূর্ণ পরিশোধিত (বকেয়া: $currencySymbol ০)।"
            } else {
                "Your account is currently settled (Due: $currencySymbol 0)."
            }
        }

        return if (!isCreditMode) {
            // DEBIT / DUE TEMPLATES
            if (isBn) {
                val primaryMsg = when {
                    balanceAmount > 0.001 ->
                        "প্রিয় $customerName, আসসালামু আলাইকুম। $bnShopAt আপনার বর্তমান বকেয়া রয়েছে $currencySymbol $cleanBalance। সুবিধামতো সময়ে বকেয়া পরিশোধ করার জন্য বিনীত অনুরোধ করা হলো।$contactSuffix"
                    balanceAmount < -0.001 ->
                        "প্রিয় $customerName, আসসালামু আলাইকুম। $bnShopPossessive হিসাব অনুযায়ী বর্তমানে আপনার কোনো বকেয়া নেই; বরং আমাদের কাছে আপনার $currencySymbol $cleanBalance জমা রয়েছে।$contactSuffix"
                    else ->
                        "প্রিয় $customerName, আসসালামু আলাইকুম। $bnShopAt আপনার সকল হিসাব বর্তমানে পরিশোধিত রয়েছে (বকেয়া: $currencySymbol ০)। ধন্যবাদ!$contactSuffix"
                }

                val entryMsg = if (matchingTxAmount != null) {
                    "প্রিয় $customerName, $bnShopAt আপনার নামে $currencySymbol $matchingTxAmount নতুন বাকি লেখা হয়েছে। $currentStatusText$contactSuffix"
                } else {
                    "প্রিয় $customerName, $bnShopAt আপনার হিসাব আপডেট করা হয়েছে। $currentStatusText$contactSuffix"
                }

                val thirdTitleAndMsg = if (balanceAmount > 0.001) {
                    "জরুরি তাগাদা" to
                        "সম্মানিত $customerName, $bnShopAt আপনার বকেয়া $currencySymbol $cleanBalance দ্রুত পরিশোধ করার জন্য অনুরোধ জানানো হচ্ছে। নগদ, বিকাশ বা নগদ অ্যাপে পরিশোধ করতে পারেন।$contactSuffix ধন্যবাদ।"
                } else {
                    "হিসাব বিবরণী" to
                        "প্রিয় $customerName, $bnShopPossessive সাথে আপনার বর্তমান হিসাব আপডেট: $currentStatusText আমাদের সাথে লেনদেনের জন্য ধন্যবাদ।$contactSuffix"
                }

                val primaryTitle = when {
                    balanceAmount > 0.001 -> "বকেয়া তাগাদা"
                    balanceAmount < -0.001 -> "হিসাব তথ্য"
                    else -> "হিসাব পরিশোধিত"
                }

                listOf(
                    primaryTitle to primaryMsg,
                    "নতুন বাকি হিসাব" to entryMsg,
                    thirdTitleAndMsg
                )
            } else {
                val primaryMsg = when {
                    balanceAmount > 0.001 ->
                        "Dear $customerName, your current due balance $enShopWith is $currencySymbol $cleanBalance. Kindly settle it at your convenience.$contactSuffix"
                    balanceAmount < -0.001 ->
                        "Dear $customerName, $enShopRecords, you have no due balance — you currently have an advance credit of $currencySymbol $cleanBalance with us.$contactSuffix"
                    else ->
                        "Dear $customerName, your account $enShopWith is completely settled with no due balance ($currencySymbol 0). Thank you!$contactSuffix"
                }

                val entryMsg = if (matchingTxAmount != null) {
                    "Dear $customerName, an amount of $currencySymbol $matchingTxAmount has been added to your due balance $enShopAt. $currentStatusText$contactSuffix"
                } else {
                    "Dear $customerName, your account ledger $enShopAt has been updated. $currentStatusText$contactSuffix"
                }

                val thirdTitleAndMsg = if (balanceAmount > 0.001) {
                    "Urgent Reminder" to
                        "Dear $customerName, please settle your overdue balance of $currencySymbol $cleanBalance $enShopWith as soon as possible.$contactSuffix Thank you."
                } else {
                    "Account Summary" to
                        "Dear $customerName, here is your ledger summary $enShopFrom: $currentStatusText Thank you!$contactSuffix"
                }

                val primaryTitle = when {
                    balanceAmount > 0.001 -> "Due Reminder"
                    balanceAmount < -0.001 -> "Balance Notice"
                    else -> "Account Settled"
                }

                listOf(
                    primaryTitle to primaryMsg,
                    "New Due Entry" to entryMsg,
                    thirdTitleAndMsg
                )
            }
        } else {
            // CREDIT / PAYMENT RECEIVED TEMPLATES
            if (isBn) {
                val paymentReceivedMsg = if (matchingTxAmount != null) {
                    "প্রিয় $customerName, $bnShopAt আপনার দেওয়া $currencySymbol $matchingTxAmount জমা হিসেবে গ্রহণ করা হয়েছে। ধন্যবাদ! $currentStatusText$contactSuffix"
                } else {
                    "প্রিয় $customerName, $bnShopAt আপনার জমা ও লেনদেন হিসাব আপডেট করা হয়েছে। ধন্যবাদ! $currentStatusText$contactSuffix"
                }

                val secondTitleAndMsg = when {
                    balanceAmount < -0.001 ->
                        "আপনার জমা তথ্য" to
                            "প্রিয় $customerName, আসসালামু আলাইকুম। $bnShopPossessive হিসাব অনুযায়ী আমাদের কাছে আপনার $currencySymbol $cleanBalance জমা রয়েছে। শীঘ্রই তা পরিশোধ করা হবে।$contactSuffix"
                    balanceAmount > 0.001 ->
                        "অবশিষ্ট বকেয়া" to
                            "প্রিয় $customerName, জমার পর $bnShopAt আপনার অবশিষ্ট বকেয়া রয়েছে $currencySymbol $cleanBalance। সুবিধামতো সময়ে বাকি টাকা পরিশোধ করার অনুরোধ রইল।$contactSuffix"
                    else ->
                        "হিসাব পরিশোধিত" to
                            "প্রিয় $customerName, আপনার সকল বকেয়া পরিশোধ হওয়ায় $bnShopAt আপনার বর্তমান বকেয়া $currencySymbol ০। আমাদের সাথে থাকার জন্য ধন্যবাদ!$contactSuffix"
                }

                val summaryMsg =
                    "প্রিয় $customerName, $bnShopAt আপনার সর্বশেষ হিসাব বিবরণী: $currentStatusText আমাদের সাথে লেনদেনের জন্য ধন্যবাদ।$contactSuffix"

                listOf(
                    "জমা প্রাপ্তি" to paymentReceivedMsg,
                    secondTitleAndMsg,
                    "হিসাব বিবরণী" to summaryMsg
                )
            } else {
                val paymentReceivedMsg = if (matchingTxAmount != null) {
                    "Dear $customerName, thank you! We have received your payment of $currencySymbol $matchingTxAmount $enShopAt. $currentStatusText$contactSuffix"
                } else {
                    "Dear $customerName, your payment and ledger records $enShopAt have been updated. Thank you! $currentStatusText$contactSuffix"
                }

                val secondTitleAndMsg = when {
                    balanceAmount < -0.001 ->
                        "Credit Balance Notice" to
                            "Dear $customerName, $enShopRecords, we hold an advance credit balance of $currencySymbol $cleanBalance for you, which will be settled soon.$contactSuffix"
                    balanceAmount > 0.001 ->
                        "Remaining Due" to
                            "Dear $customerName, following your recent payment, your remaining due balance $enShopAt is $currencySymbol $cleanBalance. Thank you!$contactSuffix"
                    else ->
                        "Account Settled" to
                            "Dear $customerName, all dues are cleared and your current due balance $enShopAt is $currencySymbol 0. Thank you!$contactSuffix"
                }

                val summaryMsg =
                    "Dear $customerName, here is your latest balance update $enShopFrom: $currentStatusText Thank you!$contactSuffix"

                listOf(
                    "Payment Received" to paymentReceivedMsg,
                    secondTitleAndMsg,
                    "Account Summary" to summaryMsg
                )
            }
        }
    }
}
