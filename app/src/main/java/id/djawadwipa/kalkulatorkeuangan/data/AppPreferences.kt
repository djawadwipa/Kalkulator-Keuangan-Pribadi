package id.djawadwipa.kalkulatorkeuangan.data

import android.content.Context
import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

enum class AppThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
}

class AppPreferences(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE,
    )

    var themeMode: AppThemeMode
        get() {
            val stored = preferences.getString(KEY_THEME_MODE, null)
            return runCatching {
                stored?.let(AppThemeMode::valueOf) ?: AppThemeMode.SYSTEM
            }.getOrDefault(AppThemeMode.SYSTEM)
        }
        set(value) {
            preferences.edit()
                .putString(KEY_THEME_MODE, value.name)
                .apply()
        }

    val hasPin: Boolean
        get() = preferences.contains(KEY_PIN_HASH) &&
            preferences.contains(KEY_PIN_SALT)

    var biometricEnabled: Boolean
        get() = hasPin && preferences.getBoolean(KEY_BIOMETRIC_ENABLED, false)
        set(value) {
            preferences.edit()
                .putBoolean(KEY_BIOMETRIC_ENABLED, value && hasPin)
                .apply()
        }

    fun setPin(pin: String) {
        require(pin.matches(PIN_PATTERN)) {
            "PIN harus terdiri dari 4 sampai 8 angka"
        }

        val salt = ByteArray(SALT_SIZE_BYTES).also(SecureRandom()::nextBytes)
        val hash = derivePinHash(pin, salt)

        preferences.edit()
            .putString(KEY_PIN_SALT, salt.toBase64())
            .putString(KEY_PIN_HASH, hash.toBase64())
            .apply()
    }

    fun verifyPin(pin: String): Boolean {
        if (!pin.matches(PIN_PATTERN)) return false

        val salt = preferences.getString(KEY_PIN_SALT, null)
            ?.fromBase64()
            ?: return false
        val expectedHash = preferences.getString(KEY_PIN_HASH, null)
            ?.fromBase64()
            ?: return false
        val actualHash = derivePinHash(pin, salt)

        return MessageDigest.isEqual(expectedHash, actualHash)
    }

    fun clearPin() {
        preferences.edit()
            .remove(KEY_PIN_SALT)
            .remove(KEY_PIN_HASH)
            .putBoolean(KEY_BIOMETRIC_ENABLED, false)
            .apply()
    }

    private fun derivePinHash(pin: String, salt: ByteArray): ByteArray {
        val specification = PBEKeySpec(
            pin.toCharArray(),
            salt,
            PBKDF2_ITERATIONS,
            HASH_SIZE_BITS,
        )
        return try {
            SecretKeyFactory.getInstance(PBKDF2_ALGORITHM)
                .generateSecret(specification)
                .encoded
        } finally {
            specification.clearPassword()
        }
    }

    private fun ByteArray.toBase64(): String =
        Base64.encodeToString(this, Base64.NO_WRAP)

    private fun String.fromBase64(): ByteArray =
        Base64.decode(this, Base64.NO_WRAP)

    private companion object {
        const val PREFERENCES_NAME = "app_preferences"
        const val KEY_THEME_MODE = "theme_mode"
        const val KEY_PIN_SALT = "pin_salt"
        const val KEY_PIN_HASH = "pin_hash"
        const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"

        const val SALT_SIZE_BYTES = 16
        const val HASH_SIZE_BITS = 256
        const val PBKDF2_ITERATIONS = 120_000
        const val PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA1"

        val PIN_PATTERN = Regex("""\d{4,8}""")
    }
}
