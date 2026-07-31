package id.djawadwipa.kalkulatorkeuangan.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        AccountEntity::class,
        CategoryEntity::class,
        TransactionEntity::class,
        FinancialProfileEntity::class,
        BudgetEntity::class,
    ],
    version = 3,
    exportSchema = true,
)
abstract class FinanceDatabase : RoomDatabase() {
    abstract fun financeDao(): FinanceDao

    companion object {
        private const val DATABASE_NAME = "kalkulator_keuangan.db"

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `accounts` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `type` TEXT NOT NULL,
                        `opening_balance` INTEGER NOT NULL,
                        `is_archived` INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_accounts_name` ON `accounts` (`name`)")
                db.execSQL(
                    """
                    INSERT OR IGNORE INTO accounts(id, name, type, opening_balance, is_archived)
                    VALUES (1, 'Dompet Utama', 'CASH', 0, 0)
                    """.trimIndent(),
                )

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `categories` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `type` TEXT NOT NULL,
                        `is_default` INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_categories_name_type` ON `categories` (`name`, `type`)",
                )
                db.execSQL(
                    """
                    INSERT OR IGNORE INTO categories(name, type, is_default)
                    SELECT category, type, 0 FROM transactions
                    """.trimIndent(),
                )

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `transactions_room` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `type` TEXT NOT NULL,
                        `amount` INTEGER NOT NULL,
                        `account_id` INTEGER NOT NULL,
                        `category_id` INTEGER NOT NULL,
                        `description` TEXT NOT NULL,
                        `occurred_at` INTEGER NOT NULL,
                        FOREIGN KEY(`account_id`) REFERENCES `accounts`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT,
                        FOREIGN KEY(`category_id`) REFERENCES `categories`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    INSERT INTO transactions_room(
                        id, type, amount, account_id, category_id, description, occurred_at
                    )
                    SELECT
                        t.id,
                        t.type,
                        t.amount,
                        1,
                        c.id,
                        t.description,
                        t.occurred_at
                    FROM transactions t
                    INNER JOIN categories c ON c.name = t.category AND c.type = t.type
                    """.trimIndent(),
                )
                db.execSQL("DROP TABLE transactions")
                db.execSQL("ALTER TABLE transactions_room RENAME TO transactions")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_account_id` ON `transactions` (`account_id`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_category_id` ON `transactions` (`category_id`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_occurred_at` ON `transactions` (`occurred_at`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_type` ON `transactions` (`type`)")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `financial_profile` (
                        `id` INTEGER NOT NULL,
                        `display_name` TEXT NOT NULL,
                        `monthly_income_target` INTEGER NOT NULL,
                        `savings_target_percent` INTEGER NOT NULL,
                        `currency_code` TEXT NOT NULL,
                        `updated_at` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    INSERT OR IGNORE INTO financial_profile(
                        id, display_name, monthly_income_target, savings_target_percent, currency_code, updated_at
                    ) VALUES (1, '', 0, 20, 'IDR', 0)
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `budgets` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `month_start` INTEGER NOT NULL,
                        `category_id` INTEGER NOT NULL,
                        `limit_amount` INTEGER NOT NULL,
                        `updated_at` INTEGER NOT NULL,
                        FOREIGN KEY(`category_id`) REFERENCES `categories`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT
                    )
                    """.trimIndent(),
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_budgets_category_id` ON `budgets` (`category_id`)")
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_budgets_month_start_category_id` ON `budgets` (`month_start`, `category_id`)",
                )
            }
        }

        @Volatile
        private var instance: FinanceDatabase? = null

        fun getInstance(context: Context): FinanceDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                FinanceDatabase::class.java,
                DATABASE_NAME,
            )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .build()
                .also { instance = it }
        }
    }
}
