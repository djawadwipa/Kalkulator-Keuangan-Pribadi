package id.djawadwipa.kalkulatorkeuangan.data.local

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import id.djawadwipa.kalkulatorkeuangan.model.FinanceTransaction
import id.djawadwipa.kalkulatorkeuangan.model.TransactionType

class FinanceDatabaseHelper(context: Context) : SQLiteOpenHelper(
    context,
    DATABASE_NAME,
    null,
    DATABASE_VERSION,
) {
    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.setForeignKeyConstraintsEnabled(true)
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE $TABLE_TRANSACTIONS (
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
            "CREATE INDEX index_transactions_occurred_at ON $TABLE_TRANSACTIONS(occurred_at DESC)",
        )
        db.execSQL(
            "CREATE INDEX index_transactions_type ON $TABLE_TRANSACTIONS(type)",
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Migrasi eksplisit akan ditambahkan setiap kali DATABASE_VERSION berubah.
        error("Migrasi database $oldVersion ke $newVersion belum tersedia")
    }

    fun insertTransaction(
        type: TransactionType,
        amount: Long,
        category: String,
        description: String,
        occurredAt: Long,
    ): Long {
        require(amount > 0L) { "Jumlah transaksi harus lebih besar dari nol" }
        require(category.isNotBlank()) { "Kategori tidak boleh kosong" }

        val values = ContentValues().apply {
            put("type", type.name)
            put("amount", amount)
            put("category", category.trim())
            put("description", description.trim())
            put("occurred_at", occurredAt)
        }
        return writableDatabase.insertOrThrow(TABLE_TRANSACTIONS, null, values)
    }

    fun listTransactions(): List<FinanceTransaction> {
        val result = mutableListOf<FinanceTransaction>()
        readableDatabase.query(
            TABLE_TRANSACTIONS,
            arrayOf("id", "type", "amount", "category", "description", "occurred_at"),
            null,
            null,
            null,
            null,
            "occurred_at DESC, id DESC",
        ).use { cursor ->
            val idIndex = cursor.getColumnIndexOrThrow("id")
            val typeIndex = cursor.getColumnIndexOrThrow("type")
            val amountIndex = cursor.getColumnIndexOrThrow("amount")
            val categoryIndex = cursor.getColumnIndexOrThrow("category")
            val descriptionIndex = cursor.getColumnIndexOrThrow("description")
            val occurredAtIndex = cursor.getColumnIndexOrThrow("occurred_at")

            while (cursor.moveToNext()) {
                result += FinanceTransaction(
                    id = cursor.getLong(idIndex),
                    type = TransactionType.valueOf(cursor.getString(typeIndex)),
                    amount = cursor.getLong(amountIndex),
                    category = cursor.getString(categoryIndex),
                    description = cursor.getString(descriptionIndex),
                    occurredAt = cursor.getLong(occurredAtIndex),
                )
            }
        }
        return result
    }

    fun deleteAllTransactions() {
        writableDatabase.delete(TABLE_TRANSACTIONS, null, null)
    }

    companion object {
        private const val DATABASE_NAME = "kalkulator_keuangan.db"
        private const val DATABASE_VERSION = 1
        private const val TABLE_TRANSACTIONS = "transactions"
    }
}
