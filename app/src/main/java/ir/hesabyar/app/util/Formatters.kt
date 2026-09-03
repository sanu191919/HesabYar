package ir.hesabyar.app.util

import java.text.DecimalFormat
import java.time.Instant
import java.time.ZoneId

private val persianDigits = charArrayOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')

fun String.toEnglishDigits(): String = buildString(length) {
    for (char in this@toEnglishDigits) {
        append(
            when (char) {
                in '۰'..'۹' -> '0' + (char - '۰')
                in '٠'..'٩' -> '0' + (char - '٠')
                else -> char
            }
        )
    }
}

fun String.toAmountOrNull(): Long? = toEnglishDigits()
    .filter(Char::isDigit)
    .takeIf(String::isNotBlank)
    ?.toLongOrNull()

fun String.toPersianDigits(): String = buildString(length) {
    for (char in this@toPersianDigits) {
        append(if (char in '0'..'9') persianDigits[char - '0'] else char)
    }
}

fun formatMoney(amount: Long): String =
    (DecimalFormat("#,###").format(amount) + " تومان").toPersianDigits()

data class JalaliDate(val year: Int, val month: Int, val day: Int)

fun formatJalali(epochMillis: Long): String {
    val date = Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).toLocalDate()
    val j = gregorianToJalali(date.year, date.monthValue, date.dayOfMonth)
    return "%04d/%02d/%02d".format(j.year, j.month, j.day).toPersianDigits()
}

fun gregorianToJalali(year: Int, month: Int, day: Int): JalaliDate {
    val gMonthDays = intArrayOf(31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
    var gy = year - 1600
    val gm = month - 1
    val gd = day - 1
    var gDayNo = 365 * gy + (gy + 3) / 4 - (gy + 99) / 100 + (gy + 399) / 400
    for (i in 0 until gm) gDayNo += gMonthDays[i]
    if (gm > 1 && (gy % 4 == 0 && gy % 100 != 0 || gy % 400 == 0)) gDayNo++
    gDayNo += gd

    var jDayNo = gDayNo - 79
    val jNp = jDayNo / 12053
    jDayNo %= 12053
    var jy = 979 + 33 * jNp + 4 * (jDayNo / 1461)
    jDayNo %= 1461
    if (jDayNo >= 366) {
        jy += (jDayNo - 1) / 365
        jDayNo = (jDayNo - 1) % 365
    }
    val jm: Int
    val jd: Int
    if (jDayNo < 186) {
        jm = 1 + jDayNo / 31
        jd = 1 + jDayNo % 31
    } else {
        jm = 7 + (jDayNo - 186) / 30
        jd = 1 + (jDayNo - 186) % 30
    }
    return JalaliDate(jy, jm, jd)
}
