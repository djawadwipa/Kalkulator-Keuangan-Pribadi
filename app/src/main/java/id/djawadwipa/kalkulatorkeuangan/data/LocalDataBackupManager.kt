package id.djawadwipa.kalkulatorkeuangan.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.Process
import id.djawadwipa.kalkulatorkeuangan.data.local.FinanceDatabase
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LocalDataBackupManager(
    private val context: Context,
) {
    data class Result(
        val byteCount: Int,
        val databaseVersion: Int,
    )

    suspend fun exportPlainDatabase(uri: Uri): Result = withContext(Dispatchers.IO) {
        val database = snapshotDatabase()
        writeUri(uri, database)
        Result(database.size, databaseVersion(database))
    }

    suspend fun importPlainDatabase(uri: Uri): Result = withContext(Dispatchers.IO) {
        val database = readUri(uri)
        replaceDatabase(database)
        Result(database.size, databaseVersion(database))
    }

    suspend fun exportEncryptedBackup(uri: Uri, password: String): Result = withContext(Dispatchers.IO) {
        val passwordChars = password.toCharArray()
        try {
            val database = snapshotDatabase()
            val encrypted = EncryptedBackupCodec.encrypt(database, passwordChars)
            writeUri(uri, encrypted)
            Result(database.size, databaseVersion(database))
        } finally {
            passwordChars.fill('\u0000')
        }
    }

    suspend fun importEncryptedBackup(uri: Uri, password: String): Result = withContext(Dispatchers.IO) {
        val passwordChars = password.toCharArray()
        try {
            val encrypted = readUri(uri)
            val database = EncryptedBackupCodec.decrypt(encrypted, passwordChars)
            replaceDatabase(database)
            Result(database.size, databaseVersion(database))
        } finally {
            passwordChars.fill('\u0000')
        }
    }

    private fun snapshotDatabase(): ByteArray {
        val database = FinanceDatabase.getInstance(context.applicationContext)
        database.openHelper.writableDatabase
            .query("PRAGMA wal_checkpoint(FULL)")
            .use { cursor -> while (cursor.moveToNext()) Unit }
        val bytes = databaseFile().readBytes()
        validateDatabase(bytes)
        return bytes
    }

    private fun replaceDatabase(bytes: ByteArray) {
        validateDatabase(bytes)
        val target = databaseFile()
        target.parentFile?.mkdirs()
        val temporary = File(target.parentFile, "${target.name}.importing")
        val previous = File(target.parentFile, "${target.name}.preimport")
        temporary.delete()
        temporary.writeBytes(bytes)

        val database = FinanceDatabase.getInstance(context.applicationContext)
        database.openHelper.writableDatabase
            .query("PRAGMA wal_checkpoint(TRUNCATE)")
            .use { cursor -> while (cursor.moveToNext()) Unit }
        database.close()
        File("${target.path}-wal").delete()
        File("${target.path}-shm").delete()
        previous.delete()
        if (target.exists() && !target.renameTo(previous)) {
            target.copyTo(previous, overwrite = true)
            require(target.delete()) { "Database aktif tidak dapat diganti" }
        }
        val installed = temporary.renameTo(target) || runCatching {
            temporary.copyTo(target, overwrite = true)
            temporary.delete()
            true
        }.getOrDefault(false)
        if (!installed) {
            if (previous.exists()) previous.renameTo(target)
            throw IllegalStateException("File impor tidak dapat dipasang")
        }
    }

    private fun writeUri(uri: Uri, bytes: ByteArray) {
        require(bytes.size <= MAX_FILE_BYTES) { "Ukuran file melebihi batas keamanan" }
        val output = requireNotNull(context.contentResolver.openOutputStream(uri, "wt")) {
            "Dokumen tujuan tidak dapat dibuka"
        }
        output.use { stream ->
            stream.write(bytes)
            stream.flush()
        }
    }

    private fun readUri(uri: Uri): ByteArray {
        val input = requireNotNull(context.contentResolver.openInputStream(uri)) {
            "Dokumen tidak dapat dibuka"
        }
        return input.use { stream ->
            val output = ByteArrayOutputStream()
            val buffer = ByteArray(32 * 1_024)
            var total = 0
            while (true) {
                val count = stream.read(buffer)
                if (count < 0) break
                total += count
                require(total <= MAX_FILE_BYTES) { "Ukuran file melebihi batas keamanan" }
                output.write(buffer, 0, count)
            }
            output.toByteArray()
        }
    }

    private fun validateDatabase(bytes: ByteArray) {
        require(bytes.size >= SQLITE_HEADER_SIZE) { "File database terlalu pendek" }
        require(bytes.copyOfRange(0, SQLITE_MAGIC.size).contentEquals(SQLITE_MAGIC)) {
            "File bukan database Kalkulator Keuangan Pribadi"
        }
        val version = databaseVersion(bytes)
        require(version in MIN_SUPPORTED_DATABASE_VERSION..MAX_SUPPORTED_DATABASE_VERSION) {
            "Versi database $version belum didukung"
        }
    }

    private fun databaseVersion(bytes: ByteArray): Int = ByteBuffer
        .wrap(bytes, USER_VERSION_OFFSET, Int.SIZE_BYTES)
        .order(ByteOrder.BIG_ENDIAN)
        .int

    private fun databaseFile(): File = context.applicationContext
        .getDatabasePath(DATABASE_NAME)

    companion object {
        private const val DATABASE_NAME = "kalkulator_keuangan.db"
        private const val MAX_FILE_BYTES = 256 * 1_024 * 1_024
        private const val SQLITE_HEADER_SIZE = 100
        private const val USER_VERSION_OFFSET = 60
        private const val MIN_SUPPORTED_DATABASE_VERSION = 1
        private const val MAX_SUPPORTED_DATABASE_VERSION = 8
        private val SQLITE_MAGIC = "SQLite format 3\u0000".toByteArray(Charsets.US_ASCII)

        fun restartApplication(context: Context) {
            val launchIntent = context.packageManager
                .getLaunchIntentForPackage(context.packageName)
                ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                ?: throw IllegalStateException("Aplikasi tidak dapat dimulai ulang")
            context.startActivity(launchIntent)
            Handler(Looper.getMainLooper()).postDelayed(
                { Process.killProcess(Process.myPid()) },
                250L,
            )
        }
    }
}
