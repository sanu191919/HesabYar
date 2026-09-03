package ir.hesabyar.app

import android.app.KeyguardManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import ir.hesabyar.app.ui.FinanceApp
import ir.hesabyar.app.ui.HesabYarTheme
import ir.hesabyar.app.ui.MainViewModel

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )
        setContent {
            HesabYarTheme {
                val vm: MainViewModel = viewModel()
                BiometricGate { FinanceApp(vm) }
            }
        }
    }
}

@Composable
private fun FragmentActivity.BiometricGate(content: @Composable () -> Unit) {
    var unlocked by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("برای ورود، هویت خود را تأیید کنید") }

    fun authenticate() {
        val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or
            BiometricManager.Authenticators.DEVICE_CREDENTIAL
        val deviceIsSecure = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            BiometricManager.from(this).canAuthenticate(authenticators) ==
                BiometricManager.BIOMETRIC_SUCCESS
        } else {
            (getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager).isDeviceSecure
        }
        if (!deviceIsSecure) {
            message = "ابتدا برای گوشی اثر انگشت، تشخیص چهره یا رمز صفحه فعال کنید."
            return
        }
        val prompt = BiometricPrompt(
            this,
            ContextCompat.getMainExecutor(this),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    unlocked = true
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    message = errString.toString()
                }

                override fun onAuthenticationFailed() {
                    message = "تأیید نشد؛ دوباره تلاش کنید"
                }
            }
        )
        val promptInfoBuilder = BiometricPrompt.PromptInfo.Builder()
                .setTitle("ورود امن به حساب‌یار")
                .setSubtitle("اطلاعات مالی فقط روی همین گوشی نگهداری می‌شود")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            promptInfoBuilder.setAllowedAuthenticators(authenticators)
        } else {
            @Suppress("DEPRECATION")
            promptInfoBuilder.setDeviceCredentialAllowed(true)
        }
        prompt.authenticate(promptInfoBuilder.build())
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START && !unlocked) authenticate()
            if (event == Lifecycle.Event.ON_STOP && unlocked) unlocked = false
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    if (unlocked) {
        content()
    } else {
        Column(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp, Alignment.CenterVertically)
        ) {
            Icon(
                Icons.Rounded.Lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Text("حساب‌یار قفل است", style = MaterialTheme.typography.headlineSmall)
            Text(message, style = MaterialTheme.typography.bodyLarge)
            Button(onClick = ::authenticate) { Text("باز کردن برنامه") }
        }
    }
}
