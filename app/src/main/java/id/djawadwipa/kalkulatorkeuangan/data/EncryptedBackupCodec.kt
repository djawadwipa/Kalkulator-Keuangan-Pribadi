package id.djawadwipa.kalkulatorkeuangan.data

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.SecureRandom
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object EncryptedBackupCodec {
    const val MIN_PASSWORD_LENGTH = 8
    private const val MAGIC = "KKPBACKUP"
    private const val VERSION: Byte = 1
    private const val SALT_SIZE = 16
    private const val IV_SIZE = 12
    private const val ITERATIONS = 150_000
    private const val KEY_BITS = 256
    private const val TAG_BITS = 128
    private val magicBytes = MAGIC.toByteArray(Charsets.US_ASCII)
    private val random = SecureRandom()

    fun encrypt(plainText: ByteArray, password: CharArray): ByteArray {
        require(password.size >= MIN_PASSWORD_LENGTH) {
            "Password backup minimal $MIN_PASSWORD_LENGTH karakter"
        }
        val salt = ByteArray(SALT_SIZE).also(random::nextBytes)
        val iv = ByteArray(IV_SIZE).also(random::nextBytes)
        val header = ByteBuffer.allocate(magicBytes.size + 1 + Int.SIZE_BYTES + SALT_SIZE + IV_SIZE)
            .order(ByteOrder.BIG_ENDIAN)
            .put(magicBytes)
            .put(VERSION)
            .putInt(ITERATIONS)
            .put(salt)
            .put(iv)
            .array()
        val key = deriveKey(password, salt, ITERATIONS)
        return try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(TAG_BITS, iv))
            cipher.updateAAD(header)
            header + cipher.doFinal(plainText)
        } finally {
            key.encoded?.fill(0)
            salt.fill(0)
        }
    }

    fun decrypt(payload: ByteArray, password: CharArray): ByteArray {
        require(password.size >= MIN_PASSWORD_LENGTH) {
            "Password backup minimal $MIN_PASSWORD_LENGTH karakter"
        }
        val headerSize = magicBytes.size + 1 + Int.SIZE_BYTES + SALT_SIZE + IV_SIZE
        require(payload.size > headerSize + 16) { "File backup terlalu pendek atau rusak" }
        val header = payload.copyOfRange(0, headerSize)
        val buffer = ByteBuffer.wrap(header).order(ByteOrder.BIG_ENDIAN)
        val actualMagic = ByteArray(magicBytes.size).also(buffer::get)
        require(actualMagic.contentEquals(magicBytes)) { "Format backup tidak dikenali" }
        require(buffer.get() == VERSION) { "Versi backup belum didukung" }
        val iterations = buffer.int
        require(iterations in 50_000..1_000_000) { "Parameter backup tidak valid" }
        val salt = ByteArray(SALT_SIZE).also(buffer::get)
        val iv = ByteArray(IV_SIZE).also(buffer::get)
        val key = deriveKey(password, salt, iterations)
        return try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(TAG_BITS, iv))
            cipher.updateAAD(header)
            cipher.doFinal(payload, headerSize, payload.size - headerSize)
        } catch (_: AEADBadTagException) {
            throw IllegalArgumentException("Password salah atau file backup telah berubah")
        } finally {
            key.encoded?.fill(0)
            salt.fill(0)
        }
    }

    private fun deriveKey(password: CharArray, salt: ByteArray, iterations: Int): SecretKeySpec {
        val spec = PBEKeySpec(password, salt, iterations, KEY_BITS)
        return try {
            val encoded = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1")
                .generateSecret(spec)
                .encoded
            SecretKeySpec(encoded, "AES").also { encoded.fill(0) }
        } finally {
            spec.clearPassword()
        }
    }
}
