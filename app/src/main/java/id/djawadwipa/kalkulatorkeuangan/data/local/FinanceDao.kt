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

    @Query("SELECT * FROM financial_profile WHERE id = 1 LIMIT 1")
    fun observeProfile(): Flow<FinancialProfileEntity?>

    @Query("SELECT * FROM financial_profile WHERE id = 1 LIMIT 1")
    suspend fun getProfile(): FinancialProfileEntity?

    @Transaction
    @Query(
        """
        SELECT t.id, t.type, t.amount, t.account_id, a.name AS account_name,
            t.category_id, c.name AS category_name, t.description, t.occurred_at
        FROM transactions t
        INNER JOIN accounts a ON a.id = t.account_id
        INNER JOIN categories c ON c.id = t.category_id
        WHERE (:query = '' OR LOWER(c.name) LIKE '%' || LOWER(:query) || '%'
            OR LOWER(a.name) LIKE '%' || LOWER(:query) || '%'
            OR LOWER(t.description) LIKE '%' || LOWER(:query) || '%')
            AND (:type IS NULL OR t.type = :type)
        ORDER BY t.occurred_at DESC, t.id DESC
        """,
    )
    fun observeTransactions(query: String, type: String?): Flow<List<TransactionRecord>>

    @Query(
        """
        SELECT b.id, b.month_start, b.category_id, c.name AS category_name,
            b.limit_amount, COALESCE(SUM(t.amount), 0) AS spent_amount
        FROM budgets b
        INNER JOIN categories c ON c.id = b.category_id
        LEFT JOIN transactions t ON t.category_id = b.category_id
            AND t.type = 'EXPENSE'
            AND t.occurred_at >= :monthStart
            AND t.occurred_at < :nextMonthStart
        WHERE b.month_start = :monthStart
        GROUP BY b.id, b.month_start, b.category_id, c.name, b.limit_amount
        ORDER BY c.name COLLATE NOCASE
        """,
    )
    fun observeBudgets(monthStart: Long, nextMonthStart: Long): Flow<List<BudgetRecord>>

    @Query(
        """
        SELECT c.id AS category_id, c.name AS category_name, COALESCE(SUM(t.amount), 0) AS amount
        FROM transactions t
        INNER JOIN categories c ON c.id = t.category_id
        WHERE t.type = 'EXPENSE'
            AND t.occurred_at >= :monthStart
            AND t.occurred_at < :nextMonthStart
        GROUP BY c.id, c.name
        HAVING amount > 0
        ORDER BY amount DESC, c.name COLLATE NOCASE
        """,
    )
    fun observeCategorySpending(monthStart: Long, nextMonthStart: Long): Flow<List<CategorySpendingRecord>>

    @Query(
        """
        SELECT g.id, g.name, g.type, g.target_amount, g.target_date,
            g.monthly_contribution_target, COALESCE(SUM(c.amount), 0) AS current_amount,
            g.created_at, g.updated_at
        FROM savings_goals g
        LEFT JOIN savings_contributions c ON c.goal_id = g.id
        WHERE g.is_archived = 0
        GROUP BY g.id, g.name, g.type, g.target_amount, g.target_date,
            g.monthly_contribution_target, g.created_at, g.updated_at
        ORDER BY CASE g.type WHEN 'EMERGENCY_FUND' THEN 0 WHEN 'SAVINGS' THEN 1 ELSE 2 END,
            g.updated_at DESC, g.name COLLATE NOCASE
        """,
    )
    fun observeSavingsGoals(): Flow<List<SavingsGoalRecord>>

    @Query(
        """
        SELECT c.id, c.goal_id, g.name AS goal_name, c.amount, c.contributed_at, c.note
        FROM savings_contributions c
        INNER JOIN savings_goals g ON g.id = c.goal_id
        ORDER BY c.contributed_at DESC, c.id DESC
        """,
    )
    fun observeSavingsContributions(): Flow<List<SavingsContributionRecord>>

    @Query(
        """
        SELECT d.id, d.name, d.creditor, d.type, d.starting_balance,
            d.annual_interest_rate, d.minimum_payment, d.due_day,
            d.start_date, d.target_payoff_date,
            COALESCE(SUM(p.amount), 0) AS paid_amount,
            CASE
                WHEN d.starting_balance - COALESCE(SUM(p.amount), 0) > 0
                THEN d.starting_balance - COALESCE(SUM(p.amount), 0)
                ELSE 0
            END AS current_balance,
            d.created_at, d.updated_at
        FROM debts d
        LEFT JOIN debt_payments p ON p.debt_id = d.id
        WHERE d.is_archived = 0
        GROUP BY d.id, d.name, d.creditor, d.type, d.starting_balance,
            d.annual_interest_rate, d.minimum_payment, d.due_day,
            d.start_date, d.target_payoff_date, d.created_at, d.updated_at
        ORDER BY current_balance DESC, d.updated_at DESC, d.name COLLATE NOCASE
        """,
    )
    fun observeDebts(): Flow<List<DebtRecord>>

    @Query(
        """
        SELECT p.id, p.debt_id, d.name AS debt_name, p.amount, p.paid_at, p.note
        FROM debt_payments p
        INNER JOIN debts d ON d.id = p.debt_id
        ORDER BY p.paid_at DESC, p.id DESC
        """,
    )
    fun observeDebtPayments(): Flow<List<DebtPaymentRecord>>

    @Query("SELECT * FROM monthly_reviews WHERE month_start = :monthStart LIMIT 1")
    fun observeMonthlyReview(monthStart: Long): Flow<MonthlyReviewEntity?>

    @Query("SELECT * FROM monthly_report_snapshots ORDER BY month_start DESC")
    fun observeMonthlyReportSnapshots(): Flow<List<MonthlyReportSnapshotEntity>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAccount(account: AccountEntity): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertCategory(category: CategoryEntity): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertTransaction(transaction: TransactionEntity): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertBudget(budget: BudgetEntity): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSavingsGoal(goal: SavingsGoalEntity): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSavingsContribution(contribution: SavingsContributionEntity): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertDebt(debt: DebtEntity): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertDebtPayment(payment: DebtPaymentEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveProfile(profile: FinancialProfileEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveMonthlyReview(review: MonthlyReviewEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveMonthlyReportSnapshot(snapshot: MonthlyReportSnapshotEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAccounts(accounts: List<AccountEntity>): List<Long>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCategories(categories: List<CategoryEntity>): List<Long>

    @Update
    suspend fun updateTransaction(transaction: TransactionEntity)

    @Update
    suspend fun updateBudget(budget: BudgetEntity)

    @Update
    suspend fun updateSavingsGoal(goal: SavingsGoalEntity)

    @Update
    suspend fun updateDebt(debt: DebtEntity)

    @Query("SELECT id FROM budgets WHERE month_start = :monthStart AND category_id = :categoryId LIMIT 1")
    suspend fun findBudgetId(monthStart: Long, categoryId: Long): Long?

    @Query("SELECT * FROM savings_goals WHERE id = :id AND is_archived = 0 LIMIT 1")
    suspend fun findSavingsGoalById(id: Long): SavingsGoalEntity?

    @Query("SELECT * FROM debts WHERE id = :id AND is_archived = 0 LIMIT 1")
    suspend fun findDebtById(id: Long): DebtEntity?

    @Query("SELECT COALESCE(SUM(amount), 0) FROM debt_payments WHERE debt_id = :debtId")
    suspend fun debtPaidAmount(debtId: Long): Long

    @Query("DELETE FROM budgets WHERE id = :id")
    suspend fun deleteBudgetById(id: Long)

    @Query("DELETE FROM savings_contributions WHERE id = :id")
    suspend fun deleteSavingsContributionById(id: Long)

    @Query("DELETE FROM savings_goals WHERE id = :id")
    suspend fun deleteSavingsGoalById(id: Long)

    @Query("DELETE FROM debt_payments WHERE id = :id")
    suspend fun deleteDebtPaymentById(id: Long)

    @Query("DELETE FROM debts WHERE id = :id")
    suspend fun deleteDebtById(id: Long)

    @Query("DELETE FROM monthly_report_snapshots WHERE id = :id")
    suspend fun deleteMonthlyReportSnapshotById(id: Long)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteTransactionById(id: Long)

    @Query("DELETE FROM transactions")
    suspend fun deleteAllTransactions()

    @Query("SELECT COUNT(*) FROM accounts")
    suspend fun accountCount(): Int

    @Query("SELECT COUNT(*) FROM categories")
    suspend fun categoryCount(): Int

    @Query("SELECT COUNT(*) FROM savings_goals WHERE is_archived = 0")
    suspend fun savingsGoalCount(): Int

    @Query("SELECT * FROM accounts WHERE is_archived = 0 ORDER BY id LIMIT 1")
    suspend fun firstActiveAccount(): AccountEntity?

    @Query("SELECT * FROM categories WHERE name = :name AND type = :type LIMIT 1")
    suspend fun findCategory(name: String, type: String): CategoryEntity?

    @Query("SELECT * FROM categories WHERE id = :id LIMIT 1")
    suspend fun findCategoryById(id: Long): CategoryEntity?

    @Query("SELECT COUNT(*) FROM transactions")
    suspend fun transactionCount(): Int
}
