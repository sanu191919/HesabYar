package ir.hesabyar.app.sms

import ir.hesabyar.app.data.EntryType
import ir.hesabyar.app.util.toEnglishDigits

data class ParsedBankSms(
    val amountTomans: Long,
    val type: EntryType,
    val bankName: String,
    val accountHint: String?,
    val occurredAt: Long,
    val confidence: Int
)

object BankSmsParser {
    private val otpWords = listOf(
        "رمز پویا", "رمز یکبار", "رمز یک بار", "کد تایید", "کد تأیید", "otp", "verification"
    )
    private val expenseWords = listOf(
        "برداشت", "خرید", "پرداخت", "کسر", "بدهکار", "debit", "انتقال به"
    )
    private val incomeWords = listOf(
        "واریز", "واريز", "افزایش موجودی", "بستانکار", "credit", "انتقال از", "دریافت"
    )
    private val transactionWords = expenseWords + incomeWords
    private val balanceWords = listOf("موجودی", "مانده", "balance")
    private val amountWords = listOf("مبلغ", "به مبلغ", "مقدار", "amount")
    private val numberRegex = Regex(
        "(?<![0-9۰-۹٠-٩])([0-9۰-۹٠-٩][0-9۰-۹٠-٩,٬،. ]{2,}[0-9۰-۹٠-٩])(?![0-9۰-۹٠-٩])"
    )
    private val accountRegex = Regex(
        "(?:کارت|حساب|سپرده|card|account)\\s*[:：-]?\\s*(?:[*xX-]+)?([0-9۰-۹٠-٩]{3,4})(?![0-9۰-۹٠-٩])"
    )

    fun parse(
        sender: String,
        body: String,
        occurredAt: Long = System.currentTimeMillis()
    ): ParsedBankSms? {
        val normalized = normalize(body)
        if (otpWords.any(normalized::contains)) return null

        val type = detectType(normalized) ?: return null
        val candidate = findBestAmountLine(normalized) ?: return null
        val currency = detectCurrency(candidate.line, normalized) ?: return null
        val rawAmount = candidate.number.filter(Char::isDigit).toLongOrNull() ?: return null
        if (rawAmount <= 0) return null

        val amountTomans = if (currency == Currency.RIAL) rawAmount / 10 else rawAmount
        if (amountTomans <= 0) return null

        val hasTypeNearAmount = transactionWords.any(candidate.line::contains)
        val confidence = when {
            hasTypeNearAmount && candidate.hasCurrency -> 96
            candidate.hasCurrency -> 88
            else -> 72
        }

        val accountHint = accountRegex.find(normalized)?.groupValues?.getOrNull(1)
            ?.toEnglishDigits()

        return ParsedBankSms(
            amountTomans = amountTomans,
            type = type,
            bankName = detectBank(sender, normalized),
            accountHint = accountHint?.let { "•••• $it" },
            occurredAt = occurredAt,
            confidence = confidence
        )
    }

    private fun normalize(value: String): String = value
        .replace('\u200c', ' ')
        .replace('ي', 'ی')
        .replace('ك', 'ک')
        .lowercase()
        .toEnglishDigits()

    private fun detectType(text: String): EntryType? {
        val incomeIndex = incomeWords.map(text::indexOf).filter { it >= 0 }.minOrNull()
        val expenseIndex = expenseWords.map(text::indexOf).filter { it >= 0 }.minOrNull()
        return when {
            incomeIndex == null && expenseIndex == null -> null
            incomeIndex != null && expenseIndex == null -> EntryType.INCOME
            expenseIndex != null && incomeIndex == null -> EntryType.EXPENSE
            incomeIndex!! < expenseIndex!! -> EntryType.INCOME
            else -> EntryType.EXPENSE
        }
    }

    private data class AmountCandidate(
        val number: String,
        val line: String,
        val hasCurrency: Boolean,
        val score: Int
    )

    private fun findBestAmountLine(text: String): AmountCandidate? {
        return text.lines()
            .flatMap { line ->
                numberRegex.findAll(line).map { match ->
                    val hasCurrency = line.contains("ریال") || line.contains("ريال") ||
                        line.contains("تومان") || line.contains("rial") || line.contains("toman")
                    var score = 0
                    if (transactionWords.any(line::contains)) score += 6
                    if (amountWords.any(line::contains)) score += 4
                    if (hasCurrency) score += 3
                    if (balanceWords.any(line::contains)) score -= 8
                    if (line.contains("کارت") || line.contains("حساب")) score -= 2
                    AmountCandidate(match.groupValues[1], line, hasCurrency, score)
                }
            }
            .filter { candidate ->
                candidate.number.filter(Char::isDigit).length in 3..15
            }
            .maxByOrNull(AmountCandidate::score)
            ?.takeIf { it.score >= 2 }
    }

    private enum class Currency { RIAL, TOMAN }

    private fun detectCurrency(line: String, fullText: String): Currency? {
        val context = if (
            line.contains("ریال") || line.contains("ريال") || line.contains("تومان") ||
            line.contains("rial") || line.contains("toman")
        ) line else fullText
        return when {
            context.contains("تومان") || context.contains("toman") -> Currency.TOMAN
            context.contains("ریال") || context.contains("ريال") || context.contains("rial") -> Currency.RIAL
            else -> null
        }
    }

    private fun detectBank(sender: String, body: String): String {
        val haystack = normalize("$sender $body")
        val banks = linkedMapOf(
            "ملت" to listOf("bankmellat", "mellat", "ملت"),
            "ملی" to listOf("bankmelli", "melli", "بانک ملی"),
            "صادرات" to listOf("bsi", "صادرات"),
            "تجارت" to listOf("tejarat", "تجارت"),
            "پاسارگاد" to listOf("pasargad", "پاسارگاد"),
            "پارسیان" to listOf("parsian", "پارسیان"),
            "سامان" to listOf("saman", "سامان"),
            "کشاورزی" to listOf("bki", "کشاورزی"),
            "مسکن" to listOf("maskan", "مسکن"),
            "رفاه" to listOf("refah", "رفاه"),
            "اقتصاد نوین" to listOf("enbank", "اقتصاد نوین"),
            "آینده" to listOf("ayandeh", "آینده"),
            "بلو" to listOf("blubank", "bluebank", "بلو")
        )
        return banks.entries.firstOrNull { (_, markers) -> markers.any(haystack::contains) }?.key
            ?: "بانک شناسایی‌نشده"
    }
}
