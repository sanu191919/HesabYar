package ir.hesabyar.app.sms

import ir.hesabyar.app.data.EntryType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BankSmsParserTest {
    @Test
    fun parsesRialExpenseAndConvertsToToman() {
        val text = """
            برداشت از حساب ۱۲۳۴
            مبلغ: ۱۲,۳۴۰,۰۰۰ ریال
            مانده: ۹۸,۰۰۰,۰۰۰ ریال
        """.trimIndent()

        val parsed = BankSmsParser.parse("BankMellat", text, 100L)!!

        assertEquals(EntryType.EXPENSE, parsed.type)
        assertEquals(1_234_000L, parsed.amountTomans)
        assertEquals("ملت", parsed.bankName)
        assertEquals(100L, parsed.occurredAt)
    }

    @Test
    fun parsesTomanIncome() {
        val text = "واریز به حساب شما به مبلغ ۴,۶۰۰,۰۰۰ تومان انجام شد"

        val parsed = BankSmsParser.parse("BlueBank", text)!!

        assertEquals(EntryType.INCOME, parsed.type)
        assertEquals(4_600_000L, parsed.amountTomans)
        assertEquals("بلو", parsed.bankName)
    }

    @Test
    fun choosesTransactionAmountInsteadOfBalance() {
        val text = "خرید مبلغ 2,500,000 ریال\nموجودی 125,000,000 ریال"

        val parsed = BankSmsParser.parse("BSI", text)!!

        assertEquals(250_000L, parsed.amountTomans)
        assertEquals("صادرات", parsed.bankName)
    }

    @Test
    fun rejectsOtpMessages() {
        val text = "رمز پویا 123456 برای خرید مبلغ 2,000,000 ریال"
        assertNull(BankSmsParser.parse("Bank", text))
    }

    @Test
    fun rejectsMessagesWithoutCurrencyToAvoidWrongUnit() {
        val text = "برداشت مبلغ 500000 از حساب"
        assertNull(BankSmsParser.parse("Bank", text))
    }

    @Test
    fun confidenceIsHighWhenTypeAndCurrencyAreOnAmountLine() {
        val parsed = BankSmsParser.parse("Saman", "برداشت 1,000,000 ریال")!!
        assertTrue(parsed.confidence >= 90)
    }
}
