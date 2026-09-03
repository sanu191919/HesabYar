package ir.hesabyar.app.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverter

enum class EntryType { INCOME, EXPENSE }
enum class EntrySource { MANUAL, SMS }
enum class ReviewStatus { CONFIRMED, PENDING, IGNORED }

@Entity(
    tableName = "transactions",
    indices = [Index(value = ["smsFingerprint"], unique = true)]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: EntryType,
    val source: EntrySource,
    val reviewStatus: ReviewStatus,
    val amountCipher: String,
    val noteCipher: String,
    val accountCipher: String,
    val categoryCipher: String,
    val bankCipher: String? = null,
    val occurredAt: Long,
    val createdAt: Long = System.currentTimeMillis(),
    val smsFingerprint: String? = null,
    val parserConfidence: Int = 100
)

@Entity(tableName = "installments")
data class InstallmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val titleCipher: String,
    val lenderCipher: String,
    val accountCipher: String,
    val totalAmountCipher: String,
    val eachAmountCipher: String,
    val totalCount: Int,
    val paidCount: Int = 0,
    val firstDueAt: Long,
    val active: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

class DbConverters {
    @TypeConverter fun entryTypeToString(value: EntryType) = value.name
    @TypeConverter fun stringToEntryType(value: String) = EntryType.valueOf(value)
    @TypeConverter fun entrySourceToString(value: EntrySource) = value.name
    @TypeConverter fun stringToEntrySource(value: String) = EntrySource.valueOf(value)
    @TypeConverter fun reviewStatusToString(value: ReviewStatus) = value.name
    @TypeConverter fun stringToReviewStatus(value: String) = ReviewStatus.valueOf(value)
}

data class FinanceEntry(
    val id: Long,
    val type: EntryType,
    val source: EntrySource,
    val reviewStatus: ReviewStatus,
    val amount: Long,
    val note: String,
    val account: String,
    val category: String,
    val bank: String?,
    val occurredAt: Long,
    val confidence: Int
)

data class Installment(
    val id: Long,
    val title: String,
    val lender: String,
    val account: String,
    val totalAmount: Long,
    val eachAmount: Long,
    val totalCount: Int,
    val paidCount: Int,
    val firstDueAt: Long,
    val active: Boolean
)

data class SourceSummary(
    val income: Long = 0,
    val expense: Long = 0
) {
    val balance: Long get() = income - expense
}

data class FinanceSummary(
    val manual: SourceSummary = SourceSummary(),
    val sms: SourceSummary = SourceSummary(),
    val activeInstallments: Int = 0,
    val pendingSms: Int = 0
)
