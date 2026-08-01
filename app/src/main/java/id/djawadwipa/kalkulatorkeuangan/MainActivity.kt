package id.djawadwipa.kalkulatorkeuangan

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import id.djawadwipa.kalkulatorkeuangan.data.AppPreferences
import id.djawadwipa.kalkulatorkeuangan.ui.AppLockScreen
import id.djawadwipa.kalkulatorkeuangan.ui.FinanceApp
import id.djawadwipa.kalkulatorkeuangan.ui.MainViewModel
import id.djawadwipa.kalkulatorkeuangan.ui.theme.KalkulatorKeuanganTheme

class MainActivity : FragmentActivity() {
    private lateinit var preferences: AppPreferences
    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var biometricPromptInfo: BiometricPrompt.PromptInfo

    private var biometricSuccessCallback: (() -> Unit)? = null
    private var biometricErrorCallback: ((String) -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        preferences = AppPreferences(this)
        configureBiometricPrompt()

        val repository = (application as FinanceApplication).repository

        setContent {
            val viewModel: MainViewModel = viewModel(
                factory = MainViewModel.Factory(repository),
            )
            var themeMode by remember { mutableStateOf(preferences.themeMode) }
            var hasPin by remember { mutableStateOf(preferences.hasPin) }
            var biometricEnabled by remember {
                mutableStateOf(preferences.biometricEnabled)
            }
            var unlocked by rememberSaveable(hasPin) {
                mutableStateOf(!hasPin)
            }
            var unlockMessage by rememberSaveable {
                mutableStateOf<String?>(null)
            }

            KalkulatorKeuanganTheme(themeMode = themeMode) {
                if (hasPin && !unlocked) {
                    AppLockScreen(
                        biometricEnabled = biometricEnabled,
                        biometricAvailable = biometricAvailable(),
                        message = unlockMessage,
                        onVerifyPin = { pin ->
                            preferences.verifyPin(pin).also { valid ->
                                if (valid) {
                                    unlocked = true
                                    unlockMessage = null
                                }
                            }
                        },
                        onBiometric = {
                            requestBiometric(
                                onSuccess = {
                                    unlocked = true
                                    unlockMessage = null
                                },
                                onError = { error ->
                                    unlockMessage = error
                                },
                            )
                        },
                    )
                } else {
                    FinanceApp(
                        viewModel = viewModel,
                        themeMode = themeMode,
                        onThemeModeChange = { selected ->
                            themeMode = selected
                            preferences.themeMode = selected
                        },
                        hasPin = hasPin,
                        biometricEnabled = biometricEnabled,
                        biometricAvailable = biometricAvailable(),
                        onSetPin = { pin ->
                            preferences.setPin(pin)
                            hasPin = true
                            unlocked = true
                        },
                        onClearPin = {
                            preferences.clearPin()
                            hasPin = false
                            biometricEnabled = false
                            unlocked = true
                        },
                        onVerifyPin = preferences::verifyPin,
                        onBiometricEnabledChange = { enabled ->
                            preferences.biometricEnabled = enabled
                            biometricEnabled = preferences.biometricEnabled
                        },
                    )
                }
            }
        }
    }

    private fun configureBiometricPrompt() {
        biometricPrompt = BiometricPrompt(
            this,
            ContextCompat.getMainExecutor(this),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(
                    result: BiometricPrompt.AuthenticationResult,
                ) {
                    super.onAuthenticationSucceeded(result)
                    biometricSuccessCallback?.invoke()
                    clearBiometricCallbacks()
                }

                override fun onAuthenticationError(
                    errorCode: Int,
                    errString: CharSequence,
                ) {
                    super.onAuthenticationError(errorCode, errString)
                    if (
                        errorCode != BiometricPrompt.ERROR_USER_CANCELED &&
                        errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON
                    ) {
                        biometricErrorCallback?.invoke(errString.toString())
                    }
                    clearBiometricCallbacks()
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    biometricErrorCallback?.invoke(
                        "Biometrik tidak dikenali. Silakan coba lagi.",
                    )
                }
            },
        )

        biometricPromptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Buka Kalkulator Keuangan Pribadi")
            .setSubtitle("Gunakan biometrik perangkat")
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG,
            )
            .setNegativeButtonText("Gunakan PIN")
            .build()
    }

    private fun requestBiometric(
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
    ) {
        if (!biometricAvailable()) {
            onError("Biometrik belum tersedia atau belum didaftarkan pada perangkat")
            return
        }

        biometricSuccessCallback = onSuccess
        biometricErrorCallback = onError
        biometricPrompt.authenticate(biometricPromptInfo)
    }

    private fun biometricAvailable(): Boolean =
        BiometricManager.from(this).canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG,
        ) == BiometricManager.BIOMETRIC_SUCCESS

    private fun clearBiometricCallbacks() {
        biometricSuccessCallback = null
        biometricErrorCallback = null
    }
}
