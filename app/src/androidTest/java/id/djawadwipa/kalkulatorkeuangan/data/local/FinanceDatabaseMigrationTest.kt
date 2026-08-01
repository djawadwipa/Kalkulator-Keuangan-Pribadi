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
    fun migration1To4PreservesTransactionsAndCreatesPlanningTables() {
        createVersionOneDatabase()

        val roomDatabase = Room.databaseBuilder(
            context,
            FinanceDatabase::class.java,
            TEST_DATABASE,
        )
            .addMigrations(
                FinanceDatabase.MIGRATION_1_2,
                FinanceDatabase.MIGRATION_2_3,
                FinanceDatabase.MIGRATION_3_4,
            )
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
        roomDatabase.openHelper.readableDatabase.query(
            "SELECT COUNT(*) FROM savings_goals",
        ).use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(0, cursor.getInt(0))
        }
        roomDatabase.close()
    }

    @Test
    fun migration3To4CreatesSavingsRelationsAndCascadeDelete() {
        createVersionThreeDatabase()

        val roomDatabase = Room.databaseBuilder(
            context,
            FinanceDatabase::class.java,
            TEST_DATABASE,
        )
            .addMigrations(FinanceDatabase.MIGRATION_3_4)
            .allowMainThreadQueries()
            .build()

        val db = roomDatabase.openHelper.writableDatabase
        db.execSQL(
            """
            INSERT INTO savings_goals(
                id, name, type, target_amount, target_date,
                monthly_contribution_target, is_archived, created_at, updated_at
            ) VALUES (1, 'Dana Darurat', 'EMERGENCY_FUND', 12000000, NULL, 1000000, 0, 1, 1)
            """.trimIndent(),
        )
        db.execSQL(
            "INSERT INTO savings_contributions(goal_id, amount, contributed_at, note) VALUES (1, 2000000, 2, 'Saldo awal')",
        )
        db.query("SELECT amount FROM savings_contributions WHERE goal_id = 1").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(2_000_000L, cursor.getLong(0))
        }
        db.execSQL("DELETE FROM savings_goals WHERE id = 1")
        db.query("SELECT COUNT(*) FROM savings_contributions").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(0, cursor.getInt(0))
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

    private fun createVersionThreeDatabase() {
        createDatabase(3) { db ->
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
            db.execSQL(
                """
                CREATE TABLE financial_profile (
                    id INTEGER NOT NULL PRIMARY KEY,
                    display_name TEXT NOT NULL,
                    monthly_income_target INTEGER NOT NULL,
                    savings_target_percent INTEGER NOT NULL,
                    currency_code TEXT NOT NULL,
                    updated_at INTEGER NOT NULL
                )
                """.trimIndent(),
            )
            db.execSQL(
                """
                CREATE TABLE budgets (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    month_start INTEGER NOT NULL,
                    category_id INTEGER NOT NULL,
                    limit_amount INTEGER NOT NULL,
                    updated_at INTEGER NOT NULL,
                    FOREIGN KEY(category_id) REFERENCES categories(id) ON UPDATE NO ACTION ON DELETE RESTRICT
                )
                """.trimIndent(),
            )
            db.execSQL("CREATE INDEX index_budgets_category_id ON budgets(category_id)")
            db.execSQL("CREATE UNIQUE INDEX index_budgets_month_start_category_id ON budgets(month_start, category_id)")
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
