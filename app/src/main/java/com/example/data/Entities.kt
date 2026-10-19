package com.example.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "customers")
data class CustomerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phone: String,
    val partyType: String = "CUSTOMER", // "CUSTOMER" (কাস্টমার) or "SUPPLIER" (সাপ্লায়ার)
    val address: String = "",
    val avatarColorHex: Long = 0xFF00E5FF,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = CustomerEntity::class,
            parentColumns = ["id"],
            childColumns = ["customerId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["customerId"]), Index(value = ["timestamp"])]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val customerId: Long,
    val type: String, // "DEBIT" (টাকা পাব / দিয়েছি) or "CREDIT" (টাকা দেব / পেয়েছি)
    val amount: Double,
    val category: String, // e.g., বাকিতে বিক্রি, নগদ জমা, মাল ক্রয়, পরিবহন, অন্যান্য
    val paymentMethod: String, // "CASH" (নগদ), "BKASH" (বিকাশ), "NAGAD" (নগদ অ্যাপ), "BANK" (ব্যাংক)
    val note: String = "",
    val receiptUri: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

data class CustomerWithBalance(
    val customer: CustomerEntity,
    val totalDebit: Double,   // টাকা পাব / দিয়েছি
    val totalCredit: Double,  // টাকা দেব / পেয়েছি
    val lastTransactionTime: Long?,
    val transactionCount: Int
) {
    // Positive netBalance => Customer owes business (মোট পাবেন / Receivable)
    // Negative netBalance => Business owes customer/supplier (মোট দেবেন / Payable)
    val netBalance: Double
        get() = totalDebit - totalCredit
}

data class TransactionWithCustomer(
    val transaction: TransactionEntity,
    val customerName: String,
    val customerPhone: String
)

data class LedgerSummary(
    val totalReceivable: Double = 0.0, // মোট পাবেন
    val totalPayable: Double = 0.0,    // মোট দেবেন
    val netBalance: Double = 0.0,      // মোট ব্যালেন্স
    val totalDebitVolume: Double = 0.0,
    val totalCreditVolume: Double = 0.0,
    val customerCount: Int = 0,
    val transactionCount: Int = 0
)
