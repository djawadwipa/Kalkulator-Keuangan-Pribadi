package id.djawadwipa.kalkulatorkeuangan.model

enum class TransactionType {
    INCOME,
    EXPENSE,
}

data class FinanceTransaction(
    val id: Long,
    val type: TransactionType,
    val amount: Long,
    val category: String,
    val description: String,
    val occurredAt: Long,
)

data class DashboardSummary(
    val income: Long = 0,
    val expense: Long = 0,
    val balance: Long = 0,
    val savingsRate: Double = 0.0,
    val expenseRatio: Double = 0.0,
    val healthScore: Int = 0,
    val transactionCount: Int = 0,
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
