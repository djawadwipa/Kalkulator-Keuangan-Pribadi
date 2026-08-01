package id.djawadwipa.kalkulatorkeuangan.data

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class EncryptedBackupCodecTest {
    @Test
    fun encryptAndDecryptRoundTrip() {
        val plain = "SQLite format 3\u0000private finance data".toByteArray()
        val password = "correct horse battery staple".toCharArray()

        val encrypted = EncryptedBackupCodec.encrypt(plain, password)
        val decrypted = EncryptedBackupCodec.decrypt(encrypted, password)

        assertTrue(encrypted.size > plain.size)
        assertArrayEquals(plain, decrypted)
    }

    @Test
    fun wrongPasswordIsRejected() {
        val encrypted = EncryptedBackupCodec.encrypt(
            "private data".toByteArray(),
            "password-one".toCharArray(),
        )

        assertThrows(IllegalArgumentException::class.java) {
            EncryptedBackupCodec.decrypt(encrypted, "password-two".toCharArray())
        }
    }

    @Test
    fun changedCipherTextIsRejected() {
        val password = "strong-password".toCharArray()
        val encrypted = EncryptedBackupCodec.encrypt("private data".toByteArray(), password)
        encrypted[encrypted.lastIndex] = (encrypted.last().toInt() xor 1).toByte()

        assertThrows(IllegalArgumentException::class.java) {
            EncryptedBackupCodec.decrypt(encrypted, password)
        }
    }
}
