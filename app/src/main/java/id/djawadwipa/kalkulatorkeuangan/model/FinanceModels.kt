package id.djawadwipa.kalkulatorkeuangan.model

enum class TransactionType(val label: String) {
    INCOME("Pemasukan"),
    EXPENSE("Pengeluaran"),
}

enum class AccountType(val label: String) {
    CASH("Tunai"),
    BANK("Bank"),
    EWALLET("E-Wallet"),
    OTHER("Lainnya"),
}

enum class BudgetStatus(val label: String) {
    SAFE("Aman"),
    WARNING("Mendekati batas"),
    EXCEEDED("Melebihi budget"),
}

data class FinanceAccount(
    val id: Long,
    val name: String,
    val type: AccountType,
    val openingBalance: Long = 0,
    val isArchived: Boolean = false,
)

data class FinanceCategory(
    val id: Long,
    val name: String,
    val type: TransactionType,
    val isDefault: Boolean = false,
)

data class FinanceTransaction(
    val id: Long,
    val type: TransactionType,
    val amount: Long,
    val accountId: Long,
    val accountName: String,
    val categoryId: Long,
    val categoryName: String,
    val description: String,
    val occurredAt: Long,
)

data class TransactionDraft(
    val type: TransactionType,
    val amount: Long,
    val accountId: Long,
    val categoryId: Long,
    val description: String,
    val occurredAt: Long,
)

data class FinancialProfile(
    val displayName: String = "",
    val monthlyIncomeTarget: Long = 0,
    val savingsTargetPercent: Int = 20,
    val currencyCode: String = "IDR",
    val updatedAt: Long = 0,
)

data class FinancialProfileDraft(
    val displayName: String,
    val monthlyIncomeTarget: Long,
    val savingsTargetPercent: Int,
)

data class BudgetItem(
    val id: Long,
    val monthStart: Long,
    val categoryId: Long,
    val categoryName: String,
    val limitAmount: Long,
    val spentAmount: Long,
    val remainingAmount: Long,
    val utilizationPercent: Double,
    val status: BudgetStatus,
)

data class BudgetSummary(
    val monthStart: Long = 0,
    val items: List<BudgetItem> = emptyList(),
    val totalBudget: Long = 0,
    val totalSpent: Long = 0,
    val remaining: Long = 0,
    val utilizationPercent: Double = 0.0,
    val adherencePercent: Double = 0.0,
)

data class ExpenseCategorySummary(
    val categoryId: Long,
    val categoryName: String,
    val amount: Long,
    val sharePercent: Double,
)

data class MonthlyAnalysis(
    val monthStart: Long = 0,
    val totalExpense: Long = 0,
    val averageDailyExpense: Long = 0,
    val topCategoryName: String = "Belum ada data",
    val topCategoryAmount: Long = 0,
    val categories: List<ExpenseCategorySummary> = emptyList(),
    val budget: BudgetSummary = BudgetSummary(),
)

data class DashboardSummary(
    val income: Long = 0,
    val expense: Long = 0,
    val balance: Long = 0,
    val savingsRate: Double = 0.0,
    val expenseRatio: Double = 0.0,
    val healthScore: Int = 0,
    val transactionCount: Int = 0,
    val incomeTargetProgress: Double = 0.0,
    val savingsTargetGap: Double = 0.0,
)

data class FinancialHealthInput(
    val income: Long,
    val expense: Long,
    val debtRatio: Double = 0.0,
    val emergencyFundMonths: Double = 0.0,
    val investmentRate: Double = 0.0,
    val netWorthGrowth: Double = 0.0,
    val budgetAdherence: Double = 0.0,
)
