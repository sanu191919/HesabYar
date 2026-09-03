package ir.hesabyar.app.sms

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.Telephony
import androidx.core.content.ContextCompat
import ir.hesabyar.app.data.FinanceRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

data class SmsImportResult(
    val scanned: Int,
    val detected: Int,
    val queued: Int
)

class SmsImporter(
    private val context: Context,
    private val repository: FinanceRepository
) {
    suspend fun scanRecent(days: Int = 90): SmsImportResult = withContext(Dispatchers.IO) {
        check(
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) ==
                PackageManager.PERMISSION_GRANTED
        ) { "مجوز خواندن پیامک داده نشده است" }

        val since = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(days.toLong())
        var scanned = 0
        var detected = 0
        var queued = 0
        val projection = arrayOf(
            Telephony.Sms.ADDRESS,
            Telephony.Sms.BODY,
            Telephony.Sms.DATE
        )

        context.contentResolver.query(
            Telephony.Sms.Inbox.CONTENT_URI,
            projection,
            "${Telephony.Sms.DATE} >= ?",
            arrayOf(since.toString()),
            "${Telephony.Sms.DATE} DESC"
        )?.use { cursor ->
            val addressIndex = cursor.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
            val bodyIndex = cursor.getColumnIndexOrThrow(Telephony.Sms.BODY)
            val dateIndex = cursor.getColumnIndexOrThrow(Telephony.Sms.DATE)

            while (cursor.moveToNext() && scanned < MAX_MESSAGES) {
                scanned++
                val sender = cursor.getString(addressIndex).orEmpty()
                val body = cursor.getString(bodyIndex).orEmpty()
                val date = cursor.getLong(dateIndex)
                val parsed = BankSmsParser.parse(sender, body, date) ?: continue
                detected++
                if (repository.queueSms(parsed, fingerprint(sender, body, date))) queued++
            }
        }
        SmsImportResult(scanned, detected, queued)
    }

    companion object {
        private const val MAX_MESSAGES = 2_000

        fun fingerprint(sender: String, body: String, date: Long): String {
            val value = "$sender|$date|$body".toByteArray(Charsets.UTF_8)
            return MessageDigest.getInstance("SHA-256")
                .digest(value)
                .joinToString("") { "%02x".format(it) }
        }
    }
}
