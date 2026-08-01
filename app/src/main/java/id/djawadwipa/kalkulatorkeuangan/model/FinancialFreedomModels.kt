package id.djawadwipa.kalkulatorkeuangan.model

data class FinancialFreedomPlan(
    val monthlyExpenses: Long = 0L,
    val withdrawalRatePercent: Double = 4.0,
    val expectedReturnPercent: Double = 8.0,
    val inflationPercent: Double = 3.0,
    val monthlyContribution: Long = 0L,
    val includeSavings: Boolean = true,
)

data class FinancialFreedomProjection(
    val monthlyExpenses: Long = 0L,
    val annualExpenses: Long = 0L,
    val targetAmount: Long = 0L,
    val currentInvestableAssets: Long = 0L,
    val remainingAmount: Long = 0L,
    val progressPercent: Double = 0.0,
    val passiveIncomePerMonth: Long = 0L,
    val realAnnualReturnPercent: Double = 0.0,
    val estimatedMonths: Int? = null,
    val estimatedCompletionAt: Long? = null,
    val milestones: List<FinancialFreedomMilestone> = emptyList(),
)

data class FinancialFreedomMilestone(
    val percent: Int,
    val targetAmount: Long,
    val reached: Boolean,
)
