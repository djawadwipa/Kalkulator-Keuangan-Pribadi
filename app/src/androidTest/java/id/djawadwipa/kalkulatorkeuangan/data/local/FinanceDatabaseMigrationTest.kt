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
    fun migration1To3PreservesTransactionsAndCreatesPlanningTables() {
        createVersionOneDatabase()

        val roomDatabase = Room.databaseBuilder(
            context,
            FinanceDatabase::class.java,
            TEST_DATABASE,
        )
            .addMigrations(FinanceDatabase.MIGRATION_1_2, FinanceDatabase.MIGRATION_2_3)
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
        roomDatabase.openHelper.readableDatabase.query(
            "SELECT savings_target_percent FROM financial_profile WHERE id = 1",
        ).use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(20, cursor.getInt(0))
        }
        roomDatabase.close()
    }

    @Test
    fun migration2To3CreatesBudgetWithCategoryRelation() {
        createVersionTwoDatabase()

        val roomDatabase = Room.databaseBuilder(
            context,
            FinanceDatabase::class.java,
            TEST_DATABASE,
        )
            .addMigrations(FinanceDatabase.MIGRATION_2_3)
            .allowMainThreadQueries()
            .build()

        val db = roomDatabase.openHelper.writableDatabase
        db.execSQL(
            "INSERT INTO budgets(month_start, category_id, limit_amount, updated_at) VALUES (1704067200000, 1, 2000000, 1704067200000)",
        )
        db.query("SELECT limit_amount FROM budgets WHERE category_id = 1").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(2_000_000L, cursor.getLong(0))
        }
        roomDatabase.close()
    }

    private fun createVersionOneDatabase() {
        createDatabase(1) { db ->
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
    }

    private fun createVersionTwoDatabase() {
        createDatabase(2) { db ->
            db.execSQL(
                """
                CREATE TABLE accounts (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    name TEXT NOT NULL,
                    type TEXT NOT NULL,
                    opening_balance INTEGER NOT NULL,
                    is_archived INTEGER NOT NULL
                )
                """.trimIndent(),
            )
            db.execSQL("CREATE UNIQUE INDEX index_accounts_name ON accounts(name)")
            db.execSQL(
                """
                CREATE TABLE categories (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    name TEXT NOT NULL,
                    type TEXT NOT NULL,
                    is_default INTEGER NOT NULL
                )
                """.trimIndent(),
            )
            db.execSQL("CREATE UNIQUE INDEX index_categories_name_type ON categories(name, type)")
            db.execSQL(
                """
                CREATE TABLE transactions (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    type TEXT NOT NULL,
                    amount INTEGER NOT NULL,
                    account_id INTEGER NOT NULL,
                    category_id INTEGER NOT NULL,
                    description TEXT NOT NULL,
                    occurred_at INTEGER NOT NULL,
                    FOREIGN KEY(account_id) REFERENCES accounts(id) ON UPDATE NO ACTION ON DELETE RESTRICT,
                    FOREIGN KEY(category_id) REFERENCES categories(id) ON UPDATE NO ACTION ON DELETE RESTRICT
                )
                """.trimIndent(),
            )
            db.execSQL("CREATE INDEX index_transactions_account_id ON transactions(account_id)")
            db.execSQL("CREATE INDEX index_transactions_category_id ON transactions(category_id)")
            db.execSQL("CREATE INDEX index_transactions_occurred_at ON transactions(occurred_at)")
            db.execSQL("CREATE INDEX index_transactions_type ON transactions(type)")
            db.execSQL("INSERT INTO accounts(id, name, type, opening_balance, is_archived) VALUES (1, 'Dompet Utama', 'CASH', 0, 0)")
            db.execSQL("INSERT INTO categories(id, name, type, is_default) VALUES (1, 'Makanan', 'EXPENSE', 1)")
        }
    }

    private fun createDatabase(version: Int, createSchema: (SupportSQLiteDatabase) -> Unit) {
        val configuration = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(TEST_DATABASE)
            .callback(
                object : SupportSQLiteOpenHelper.Callback(version) {
                    override fun onCreate(db: SupportSQLiteDatabase) = createSchema(db)
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
