package ir.hesabyar.app.util

import org.junit.Assert.assertEquals
import org.junit.Test

class JalaliDateTest {
    @Test
    fun convertsKnownDate() {
        assertEquals(JalaliDate(1405, 6, 12), gregorianToJalali(2026, 9, 3))
    }

    @Test
    fun acceptsPersianAndSlashSeparatedAmounts() {
        assertEquals(4_600_000L, "۴/۶۰۰/۰۰۰".toAmountOrNull())
    }
}
