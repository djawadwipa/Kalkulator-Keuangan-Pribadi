package id.djawadwipa.kalkulatorkeuangan.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "accounts",
    indices = [Index(value = ["name"], unique = true)],
)
data class AccountEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val type: String,
    @ColumnInfo(name = "opening_balance")
    val openingBalance: Long = 0,
    @ColumnInfo(name = "is_archived")
    val isArchived: Boolean = false,
)

@Entity(
    tableName = "categories",
    indices = [Index(value = ["name", "type"], unique = true)],
)
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val type: String,
    @ColumnInfo(name = "is_default")
    val isDefault: Boolean = false,
)

@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["account_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["category_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [
        Index(value = ["account_id"]),
        Index(value = ["category_id"]),
        Index(value = ["occurred_at"]),
        Index(value = ["type"]),
    ],
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val type: String,
    val amount: Long,
    @ColumnInfo(name = "account_id")
    val accountId: Long,
    @ColumnInfo(name = "category_id")
    val categoryId: Long,
    val description: String = "",
    @ColumnInfo(name = "occurred_at")
    val occurredAt: Long,
)

@Entity(tableName = "financial_profile")
data class FinancialProfileEntity(
    @PrimaryKey
    val id: Int = 1,
    @ColumnInfo(name = "display_name")
    val displayName: String = "",
    @ColumnInfo(name = "monthly_income_target")
    val monthlyIncomeTarget: Long = 0,
    @ColumnInfo(name = "savings_target_percent")
    val savingsTargetPercent: Int = 20,
    @ColumnInfo(name = "currency_code")
    val currencyCode: String = "IDR",
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,
)

@Entity(
    tableName = "budgets",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["category_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [
        Index(value = ["category_id"]),
        Index(value = ["month_start", "category_id"], unique = true),
    ],
)
data class BudgetEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "month_start")
    val monthStart: Long,
    @ColumnInfo(name = "category_id")
    val categoryId: Long,
    @ColumnInfo(name = "limit_amount")
    val limitAmount: Long,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,
)

@Entity(
    tableName = "savings_goals",
    indices = [
        Index(value = ["type"]),
        Index(value = ["is_archived"]),
    ],
)
data class SavingsGoalEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val type: String,
    @ColumnInfo(name = "target_amount")
    val targetAmount: Long,
    @ColumnInfo(name = "target_date")
    val targetDate: Long? = null,
    @ColumnInfo(name = "monthly_contribution_target")
    val monthlyContributionTarget: Long = 0,
    @ColumnInfo(name = "is_archived")
    val isArchived: Boolean = false,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,
)

@Entity(
    tableName = "savings_contributions",
    foreignKeys = [
        ForeignKey(
            entity = SavingsGoalEntity::class,
            parentColumns = ["id"],
            childColumns = ["goal_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["goal_id"]),
        Index(value = ["contributed_at"]),
    ],
)
data class SavingsContributionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "goal_id")
    val goalId: Long,
    val amount: Long,
    @ColumnInfo(name = "contributed_at")
    val contributedAt: Long,
    val note: String = "",
)

@Entity(
    tableName = "monthly_report_snapshots",
    indices = [Index(value = ["month_start"], unique = true)],
)
data class MonthlyReportSnapshotEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "month_start")
    val monthStart: Long,
    val income: Long,
    val expense: Long,
    @ColumnInfo(name = "net_cash_flow")
    val netCashFlow: Long,
    @ColumnInfo(name = "savings_rate")
    val savingsRate: Double,
    @ColumnInfo(name = "budget_adherence")
    val budgetAdherence: Double,
    @ColumnInfo(name = "health_score")
    val healthScore: Int,
    @ColumnInfo(name = "generated_at")
    val generatedAt: Long,
)

data class TransactionRecord(
    val id: Long,
    val type: String,
    val amount: Long,
    @ColumnInfo(name = "account_id")
    val accountId: Long,
    @ColumnInfo(name = "account_name")
    val accountName: String,
    @ColumnInfo(name = "category_id")
    val categoryId: Long,
    @ColumnInfo(name = "category_name")
    val categoryName: String,
    val description: String,
    @ColumnInfo(name = "occurred_at")
    val occurredAt: Long,
)

data class BudgetRecord(
    val id: Long,
    @ColumnInfo(name = "month_start")
    val monthStart: Long,
    @ColumnInfo(name = "category_id")
    val categoryId: Long,
    @ColumnInfo(name = "category_name")
    val categoryName: String,
    @ColumnInfo(name = "limit_amount")
    val limitAmount: Long,
    @ColumnInfo(name = "spent_amount")
    val spentAmount: Long,
)

data class CategorySpendingRecord(
    @ColumnInfo(name = "category_id")
    val categoryId: Long,
    @ColumnInfo(name = "category_name")
    val categoryName: String,
    val amount: Long,
)

data class SavingsGoalRecord(
    val id: Long,
    val name: String,
    val type: String,
    @ColumnInfo(name = "target_amount")
    val targetAmount: Long,
    @ColumnInfo(name = "target_date")
    val targetDate: Long?,
    @ColumnInfo(name = "monthly_contribution_target")
    val monthlyContributionTarget: Long,
    @ColumnInfo(name = "current_amount")
    val currentAmount: Long,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,
)

data class SavingsContributionRecord(
    val id: Long,
    @ColumnInfo(name = "goal_id")
    val goalId: Long,
    @ColumnInfo(name = "goal_name")
    val goalName: String,
    val amount: Long,
    @ColumnInfo(name = "contributed_at")
    val contributedAt: Long,
    val note: String,
)
