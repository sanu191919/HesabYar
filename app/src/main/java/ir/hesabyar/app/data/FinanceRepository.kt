package ir.hesabyar.app.data

import ir.hesabyar.app.sms.ParsedBankSms
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class FinanceRepository(
    private val dao: FinanceDao,
    private val crypto: CryptoManager
) {
    val entries: Flow<List<FinanceEntry>> = dao.observeConfirmedTransactions()
        .map { list -> list.mapNotNull(::decryptTransactionSafely) }

    val pendingSms: Flow<List<FinanceEntry>> = dao.observePendingTransactions()
        .map { list -> list.mapNotNull(::decryptTransactionSafely) }

    val installments: Flow<List<Installment>> = dao.observeInstallments()
        .map { list -> list.mapNotNull(::decryptInstallmentSafely) }

    val summary: Flow<FinanceSummary> = combine(entries, pendingSms, installments) {
            confirmed, pending, installmentList ->
        val manual = confirmed.filter { it.source == EntrySource.MANUAL }
        val sms = confirmed.filter { it.source == EntrySource.SMS }
        FinanceSummary(
            manual = SourceSummary(
                income = manual.filter { it.type == EntryType.INCOME }.sumOf { it.amount },
                expense = manual.filter { it.type == EntryType.EXPENSE }.sumOf { it.amount }
            ),
            sms = SourceSummary(
                income = sms.filter { it.type == EntryType.INCOME }.sumOf { it.amount },
                expense = sms.filter { it.type == EntryType.EXPENSE }.sumOf { it.amount }
            ),
            activeInstallments = installmentList.count { it.active },
            pendingSms = pending.size
        )
    }

    suspend fun addManualEntry(
        type: EntryType,
        amount: Long,
        note: String,
        account: String,
        category: String,
        occurredAt: Long = System.currentTimeMillis()
    ) {
        require(amount > 0)
        dao.insertTransaction(
            TransactionEntity(
                type = type,
                source = EntrySource.MANUAL,
                reviewStatus = ReviewStatus.CONFIRMED,
                amountCipher = crypto.encrypt(amount.toString()),
                noteCipher = crypto.encrypt(note.trim()),
                accountCipher = crypto.encrypt(account.trim()),
                categoryCipher = crypto.encrypt(category.trim().ifBlank { "بدون دسته‌بندی" }),
                occurredAt = occurredAt
            )
        )
    }

    suspend fun queueSms(parsed: ParsedBankSms, fingerprint: String): Boolean {
        val id = dao.insertTransaction(
            TransactionEntity(
                type = parsed.type,
                source = EntrySource.SMS,
                reviewStatus = ReviewStatus.PENDING,
                amountCipher = crypto.encrypt(parsed.amountTomans.toString()),
                noteCipher = crypto.encrypt("تراکنش شناسایی‌شده از پیامک"),
                accountCipher = crypto.encrypt(parsed.accountHint.orEmpty()),
                categoryCipher = crypto.encrypt("نیازمند بررسی"),
                bankCipher = crypto.encrypt(parsed.bankName),
                occurredAt = parsed.occurredAt,
                smsFingerprint = fingerprint,
                parserConfidence = parsed.confidence
            )
        )
        return id != -1L
    }

    suspend fun approveSms(id: Long) = dao.updateReviewStatus(id, ReviewStatus.CONFIRMED)

    suspend fun ignoreSms(id: Long) = dao.updateReviewStatus(id, ReviewStatus.IGNORED)

    suspend fun deleteEntry(id: Long) = dao.deleteTransaction(id)

    suspend fun addInstallment(
        title: String,
        lender: String,
        account: String,
        totalAmount: Long,
        eachAmount: Long,
        totalCount: Int,
        firstDueAt: Long
    ) {
        require(title.isNotBlank() && eachAmount > 0 && totalCount > 0)
        dao.insertInstallment(
            InstallmentEntity(
                titleCipher = crypto.encrypt(title.trim()),
                lenderCipher = crypto.encrypt(lender.trim()),
                accountCipher = crypto.encrypt(account.trim()),
                totalAmountCipher = crypto.encrypt(totalAmount.toString()),
                eachAmountCipher = crypto.encrypt(eachAmount.toString()),
                totalCount = totalCount,
                firstDueAt = firstDueAt
            )
        )
    }

    suspend fun markNextInstallmentPaid(installment: Installment) {
        val newPaidCount = (installment.paidCount + 1).coerceAtMost(installment.totalCount)
        dao.updateInstallment(
            InstallmentEntity(
                id = installment.id,
                titleCipher = crypto.encrypt(installment.title),
                lenderCipher = crypto.encrypt(installment.lender),
                accountCipher = crypto.encrypt(installment.account),
                totalAmountCipher = crypto.encrypt(installment.totalAmount.toString()),
                eachAmountCipher = crypto.encrypt(installment.eachAmount.toString()),
                totalCount = installment.totalCount,
                paidCount = newPaidCount,
                firstDueAt = installment.firstDueAt,
                active = newPaidCount < installment.totalCount
            )
        )
    }

    private fun decryptTransactionSafely(entity: TransactionEntity): FinanceEntry? = runCatching {
        FinanceEntry(
            id = entity.id,
            type = entity.type,
            source = entity.source,
            reviewStatus = entity.reviewStatus,
            amount = crypto.decrypt(entity.amountCipher).toLong(),
            note = crypto.decrypt(entity.noteCipher),
            account = crypto.decrypt(entity.accountCipher),
            category = crypto.decrypt(entity.categoryCipher),
            bank = entity.bankCipher?.let(crypto::decrypt),
            occurredAt = entity.occurredAt,
            confidence = entity.parserConfidence
        )
    }.getOrNull()

    private fun decryptInstallmentSafely(entity: InstallmentEntity): Installment? = runCatching {
        Installment(
            id = entity.id,
            title = crypto.decrypt(entity.titleCipher),
            lender = crypto.decrypt(entity.lenderCipher),
            account = crypto.decrypt(entity.accountCipher),
            totalAmount = crypto.decrypt(entity.totalAmountCipher).toLong(),
            eachAmount = crypto.decrypt(entity.eachAmountCipher).toLong(),
            totalCount = entity.totalCount,
            paidCount = entity.paidCount,
            firstDueAt = entity.firstDueAt,
            active = entity.active
        )
    }.getOrNull()
}
