package id.djawadwipa.kalkulatorkeuangan.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface FinanceDao {
    @Query("SELECT * FROM accounts WHERE is_archived = 0 ORDER BY name COLLATE NOCASE")
    fun observeActiveAccounts(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM categories ORDER BY type, name COLLATE NOCASE")
    fun observeCategories(): Flow<List<CategoryEntity>>

    @Transaction
    @Query(
        """
        SELECT
            t.id,
            t.type,
            t.amount,
            t.account_id,
            a.name AS account_name,
            t.category_id,
            c.name AS category_name,
            t.description,
            t.occurred_at
        FROM transactions t
        INNER JOIN accounts a ON a.id = t.account_id
        INNER JOIN categories c ON c.id = t.category_id
        WHERE
            (:query = '' OR
                LOWER(c.name) LIKE '%' || LOWER(:query) || '%' OR
                LOWER(a.name) LIKE '%' || LOWER(:query) || '%' OR
                LOWER(t.description) LIKE '%' || LOWER(:query) || '%')
            AND (:type IS NULL OR t.type = :type)
        ORDER BY t.occurred_at DESC, t.id DESC
        """,
    )
    fun observeTransactions(query: String, type: String?): Flow<List<TransactionRecord>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAccount(account: AccountEntity): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertCategory(category: CategoryEntity): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertTransaction(transaction: TransactionEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAccounts(accounts: List<AccountEntity>): List<Long>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCategories(categories: List<CategoryEntity>): List<Long>

    @Update
    suspend fun updateTransaction(transaction: TransactionEntity)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteTransactionById(id: Long)

    @Query("DELETE FROM transactions")
    suspend fun deleteAllTransactions()

    @Query("SELECT COUNT(*) FROM accounts")
    suspend fun accountCount(): Int

    @Query("SELECT COUNT(*) FROM categories")
    suspend fun categoryCount(): Int

    @Query("SELECT * FROM accounts WHERE is_archived = 0 ORDER BY id LIMIT 1")
    suspend fun firstActiveAccount(): AccountEntity?

    @Query("SELECT * FROM categories WHERE name = :name AND type = :type LIMIT 1")
    suspend fun findCategory(name: String, type: String): CategoryEntity?

    @Query("SELECT COUNT(*) FROM transactions")
    suspend fun transactionCount(): Int
}
