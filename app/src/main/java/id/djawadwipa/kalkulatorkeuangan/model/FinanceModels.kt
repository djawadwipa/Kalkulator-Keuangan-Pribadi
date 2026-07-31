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
