package id.djawadwipa.kalkulatorkeuangan.model

enum class DebtType(val label: String) {
    CREDIT_CARD("Kartu kredit"),
    PERSONAL_LOAN("Pinjaman pribadi"),
    VEHICLE("Kendaraan"),
    MORTGAGE("KPR/properti"),
    PAYLATER("Paylater"),
    OTHER("Lainnya"),
}

enum class PayoffStrategy(val label: String, val description: String) {
    SNOWBALL(
        label = "Snowball",
        description = "Prioritaskan saldo terkecil untuk kemenangan lebih cepat.",
    ),
    AVALANCHE(
        label = "Avalanche",
        description = "Prioritaskan bunga tertinggi untuk menekan total bunga.",
    ),
}

data class Debt(
    val id: Long,
    val name: String,
    val creditor: String,
    val type: DebtType,
    val startingBalance: Long,
    val annualInterestRate: Double,
    val minimumPayment: Long,
    val dueDay: Int,
    val startDate: Long,
    val targetPayoffDate: Long?,
    val paidAmount: Long,
    val currentBalance: Long,
    val createdAt: Long,
    val updatedAt: Long,
) {
    val progressPercent: Double
        get() = if (startingBalance <= 0L) 0.0 else {
            (paidAmount.toDouble() / startingBalance * 100.0).coerceIn(0.0, 100.0)
        }

    val isPaidOff: Boolean
        get() = currentBalance <= 0L
}

data class DebtPayment(
    val id: Long,
    val debtId: Long,
    val debtName: String,
    val amount: Long,
    val paidAt: Long,
    val note: String,
)

data class DebtDraft(
    val name: String,
    val creditor: String,
    val type: DebtType,
    val startingBalance: Long,
    val annualInterestRate: Double,
    val minimumPayment: Long,
    val dueDay: Int,
    val startDate: Long,
    val targetPayoffDate: Long?,
)

data class DebtPaymentDraft(
    val debtId: Long,
    val amount: Long,
    val paidAt: Long,
    val note: String,
)

data class DebtOverview(
    val activeCount: Int = 0,
    val totalStartingBalance: Long = 0,
    val totalPaid: Long = 0,
    val totalBalance: Long = 0,
    val minimumMonthlyPayment: Long = 0,
    val progressPercent: Double = 0.0,
)

data class DebtPayoffStep(
    val order: Int,
    val debtId: Long,
    val debtName: String,
    val startingBalance: Long,
    val annualInterestRate: Double,
    val payoffMonth: Int,
    val payoffDate: Long,
    val interestPaid: Long,
    val totalPaid: Long,
)

data class DebtPayoffPlan(
    val strategy: PayoffStrategy,
    val monthlyBudget: Long,
    val minimumRequired: Long,
    val totalBalance: Long,
    val monthsToDebtFree: Int?,
    val debtFreeAt: Long?,
    val totalInterest: Long,
    val feasible: Boolean,
    val message: String,
    val steps: List<DebtPayoffStep>,
)