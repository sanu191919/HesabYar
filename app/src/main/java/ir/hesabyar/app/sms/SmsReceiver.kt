package ir.hesabyar.app.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import ir.hesabyar.app.HesabYarApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (messages.isEmpty()) return

        val pendingResult = goAsync()
        val sender = messages.first().originatingAddress.orEmpty()
        val body = messages.joinToString(separator = "") { it.messageBody.orEmpty() }
        val occurredAt = messages.first().timestampMillis

        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val parsed = BankSmsParser.parse(sender, body, occurredAt) ?: return@launch
                val app = context.applicationContext as HesabYarApp
                app.repository.queueSms(
                    parsed,
                    SmsImporter.fingerprint(sender, body, occurredAt)
                )
            } finally {
                pendingResult.finish()
            }
        }
    }
}
