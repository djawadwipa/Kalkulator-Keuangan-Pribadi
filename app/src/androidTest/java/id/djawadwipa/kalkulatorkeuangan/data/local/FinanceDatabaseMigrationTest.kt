package id.djawadwipa.kalkulatorkeuangan.data.local

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FinanceDatabaseMigrationTest {
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.deleteDatabase(TEST_DATABASE)
    }

    @After
    fun tearDown() {
        context.deleteDatabase(TEST_DATABASE)
    }

    @Test
    fun migration1To2PreservesTransactionsAndCreatesRelations() {
        createVersionOneDatabase()

        val roomDatabase = Room.databaseBuilder(
            context,
            FinanceDatabase::class.java,
            TEST_DATABASE,
        )
            .addMigrations(FinanceDatabase.MIGRATION_1_2)
            .allowMainThreadQueries()
            .build()

        roomDatabase.openHelper.writableDatabase
        roomDatabase.openHelper.readableDatabase.query(
            """
            SELECT t.amount, a.name, c.name
            FROM transactions t
            INNER JOIN accounts a ON a.id = t.account_id
            INNER JOIN categories c ON c.id = t.category_id
            """.trimIndent(),
        ).use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(125_000L, cursor.getLong(0))
            assertEquals("Dompet Utama", cursor.getString(1))
            assertEquals("Makanan", cursor.getString(2))
        }
        roomDatabase.close()
    }

    private fun createVersionOneDatabase() {
        val configuration = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(TEST_DATABASE)
            .callback(
                object : SupportSQLiteOpenHelper.Callback(1) {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        db.execSQL(
                            """
                            CREATE TABLE transactions (
                                id INTEGER PRIMARY KEY AUTOINCREMENT,
                                type TEXT NOT NULL CHECK(type IN ('INCOME', 'EXPENSE')),
                                amount INTEGER NOT NULL CHECK(amount > 0),
                                category TEXT NOT NULL,
                                description TEXT NOT NULL DEFAULT '',
                                occurred_at INTEGER NOT NULL
                            )
                            """.trimIndent(),
                        )
                        db.execSQL(
                            """
                            INSERT INTO transactions(type, amount, category, description, occurred_at)
                            VALUES ('EXPENSE', 125000, 'Makanan', 'Belanja', 1700000000000)
                            """.trimIndent(),
                        )
                    }

                    override fun onUpgrade(
                        db: SupportSQLiteDatabase,
                        oldVersion: Int,
                        newVersion: Int,
                    ) = Unit
                },
            )
            .build()

        FrameworkSQLiteOpenHelperFactory().create(configuration).use { helper ->
            helper.writableDatabase
        }
    }

    companion object {
        private const val TEST_DATABASE = "finance-migration-test.db"
    }
}
