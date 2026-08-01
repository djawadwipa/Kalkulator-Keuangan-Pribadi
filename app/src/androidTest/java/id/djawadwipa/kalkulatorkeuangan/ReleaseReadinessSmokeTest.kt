package id.djawadwipa.kalkulatorkeuangan

import android.content.Context
import android.net.Uri
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import id.djawadwipa.kalkulatorkeuangan.data.LocalDataBackupManager
import id.djawadwipa.kalkulatorkeuangan.data.local.FinanceDatabase
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReleaseReadinessSmokeTest {
    @Test
    fun appLaunchesAndEncryptedBackupRestoresDatabaseOnDevice() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()

        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                assertFalse(activity.isFinishing)
                assertFalse(activity.isDestroyed)
            }
        }

        val database = FinanceDatabase.getInstance(context)
        val writable = database.openHelper.writableDatabase
        writable.execSQL(
            """
            INSERT OR IGNORE INTO accounts(id, name, type, opening_balance, is_archived)
            VALUES ($ACCOUNT_ID, 'Rekening Instrumentasi', 'CASH', 0, 0)
            """.trimIndent(),
        )
        writable.execSQL(
            """
            INSERT OR IGNORE INTO categories(id, name, type, is_default)
            VALUES ($CATEGORY_ID, 'Kategori Instrumentasi', 'EXPENSE', 0)
            """.trimIndent(),
        )
        writable.execSQL("DELETE FROM transactions WHERE id = $TRANSACTION_ID")
        writable.execSQL(
            """
            INSERT INTO transactions(
                id, type, amount, account_id, category_id, description, occurred_at
            ) VALUES (
                $TRANSACTION_ID, 'EXPENSE', $EXPECTED_AMOUNT,
                $ACCOUNT_ID, $CATEGORY_ID, 'Smoke test backup', 1704067200000
            )
            """.trimIndent(),
        )

        val manager = LocalDataBackupManager(context)
        val plainUri = Uri.parse("content://$TEST_AUTHORITY/plain-export.kkpdb")
        val encryptedUri = Uri.parse("content://$TEST_AUTHORITY/encrypted-backup.kkpbak")
        val plainResult = manager.exportPlainDatabase(plainUri)
        val encryptedResult = manager.exportEncryptedBackup(encryptedUri, TEST_PASSWORD)

        assertEquals(8, plainResult.databaseVersion)
        assertEquals(8, encryptedResult.databaseVersion)
        assertTrue(plainResult.byteCount >= 100)
        assertEquals(plainResult.byteCount, encryptedResult.byteCount)

        val plainHeader = requireNotNull(context.contentResolver.openInputStream(plainUri)).use { input ->
            ByteArray(SQLITE_MAGIC.size).also { header ->
                var offset = 0
                while (offset < header.size) {
                    val count = input.read(header, offset, header.size - offset)
                    assertTrue(count > 0)
                    offset += count
                }
            }
        }
        assertTrue(plainHeader.contentEquals(SQLITE_MAGIC))

        writable.execSQL(
            "UPDATE transactions SET amount = $MUTATED_AMOUNT WHERE id = $TRANSACTION_ID",
        )
        manager.importEncryptedBackup(encryptedUri, TEST_PASSWORD)

        openImportedDatabase(context).use { helper ->
            helper.readableDatabase.query(
                "SELECT amount FROM transactions WHERE id = $TRANSACTION_ID",
            ).use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals(EXPECTED_AMOUNT, cursor.getLong(0))
            }
            helper.readableDatabase.query("PRAGMA quick_check").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("ok", cursor.getString(0))
            }
        }
    }

    private fun openImportedDatabase(context: Context): SupportSQLiteOpenHelper {
        val configuration = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(DATABASE_NAME)
            .callback(
                object : SupportSQLiteOpenHelper.Callback(8) {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        error("Database hasil impor tidak boleh dibuat ulang")
                    }

                    override fun onUpgrade(
                        db: SupportSQLiteDatabase,
                        oldVersion: Int,
                        newVersion: Int,
                    ) {
                        error("Database hasil impor memiliki versi tidak terduga: $oldVersion → $newVersion")
                    }
                },
            )
            .build()
        return FrameworkSQLiteOpenHelperFactory().create(configuration)
    }

    companion object {
        private const val DATABASE_NAME = "kalkulator_keuangan.db"
        private const val TEST_AUTHORITY = "id.djawadwipa.kalkulatorkeuangan.test.documents"
        private const val TEST_PASSWORD = "Sprint9-Backup-Test"
        private const val ACCOUNT_ID = 91_001L
        private const val CATEGORY_ID = 91_001L
        private const val TRANSACTION_ID = 91_001L
        private const val EXPECTED_AMOUNT = 456_789L
        private const val MUTATED_AMOUNT = 999L
        private val SQLITE_MAGIC = "SQLite format 3\u0000".toByteArray(Charsets.US_ASCII)
    }
}
