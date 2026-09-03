package ir.hesabyar.app.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import ir.hesabyar.app.HesabYarApp
import ir.hesabyar.app.data.EntryType
import ir.hesabyar.app.data.FinanceEntry
import ir.hesabyar.app.data.FinanceSummary
import ir.hesabyar.app.data.Installment
import ir.hesabyar.app.sms.SmsImporter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = (application as HesabYarApp).repository

    val entries: StateFlow<List<FinanceEntry>> = repository.entries.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList()
    )
    val pendingSms: StateFlow<List<FinanceEntry>> = repository.pendingSms.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList()
    )
    val installments: StateFlow<List<Installment>> = repository.installments.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList()
    )
    val summary: StateFlow<FinanceSummary> = repository.summary.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        FinanceSummary()
    )

    private val _notice = MutableStateFlow<String?>(null)
    val notice: StateFlow<String?> = _notice.asStateFlow()

    fun clearNotice() { _notice.value = null }

    fun addEntry(
        type: EntryType,
        amount: Long,
        note: String,
        account: String,
        category: String
    ) = viewModelScope.launch {
        runCatching { repository.addManualEntry(type, amount, note, account, category) }
            .onSuccess { _notice.value = "تراکنش دستی ثبت شد" }
            .onFailure { _notice.value = "ثبت تراکنش انجام نشد" }
    }

    fun deleteEntry(id: Long) = viewModelScope.launch {
        repository.deleteEntry(id)
    }

    fun approveSms(id: Long) = viewModelScope.launch {
        repository.approveSms(id)
        _notice.value = "تراکنش پیامکی تأیید شد"
    }

    fun ignoreSms(id: Long) = viewModelScope.launch {
        repository.ignoreSms(id)
        _notice.value = "پیامک نادیده گرفته شد"
    }

    fun scanSms(context: Context, silent: Boolean = false) = viewModelScope.launch {
        if (!silent) _notice.value = "در حال بررسی پیامک‌های ۹۰ روز اخیر…"
        runCatching { SmsImporter(context.applicationContext, repository).scanRecent() }
            .onSuccess { result ->
                if (!silent || result.queued > 0) {
                    _notice.value = if (result.queued == 0) "مورد جدیدی پیدا نشد"
                    else "${result.queued} تراکنش برای بررسی پیدا شد"
                }
            }
            .onFailure { error ->
                if (!silent) _notice.value = error.message ?: "خواندن پیامک‌ها انجام نشد"
            }
    }

    fun addInstallment(
        title: String,
        lender: String,
        account: String,
        totalAmount: Long,
        eachAmount: Long,
        totalCount: Int,
        dueDay: Int
    ) = viewModelScope.launch {
        val dueAt = nextDueEpoch(dueDay)
        runCatching {
            repository.addInstallment(
                title, lender, account, totalAmount, eachAmount, totalCount, dueAt
            )
        }.onSuccess {
            _notice.value = "قسط جدید ثبت شد"
        }.onFailure {
            _notice.value = "اطلاعات قسط کامل نیست"
        }
    }

    fun markInstallmentPaid(installment: Installment) = viewModelScope.launch {
        repository.markNextInstallmentPaid(installment)
        _notice.value = "یک قسط پرداخت‌شده ثبت شد"
    }

    private fun nextDueEpoch(day: Int): Long {
        val today = LocalDate.now()
        val safeDay = day.coerceIn(1, today.lengthOfMonth())
        var due = today.withDayOfMonth(safeDay)
        if (due.isBefore(today)) {
            val nextMonth = today.plusMonths(1)
            due = nextMonth.withDayOfMonth(day.coerceIn(1, nextMonth.lengthOfMonth()))
        }
        return due.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }
}
