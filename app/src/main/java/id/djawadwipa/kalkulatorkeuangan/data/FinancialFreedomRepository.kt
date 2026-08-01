package id.djawadwipa.kalkulatorkeuangan.data

import android.content.Context
import id.djawadwipa.kalkulatorkeuangan.data.local.FinanceDatabase
import id.djawadwipa.kalkulatorkeuangan.domain.FinancialFreedomCalculator
import id.djawadwipa.kalkulatorkeuangan.model.FinancialFreedomPlan
import id.djawadwipa.kalkulatorkeuangan.model.FinancialFreedomProjection
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlin.math.roundToLong

class FinancialFreedomRepository(
    context: Context,
    database: FinanceDatabase = FinanceDatabase.getInstance(context.applicationContext),
) {
    private val preferences = context.applicationContext.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE,
    )
    private val financeDao = database.financeDao()
    private val investmentDao = database.investmentDao()
    private val planState = MutableStateFlow(loadPlan())

    val plan: Flow<FinancialFreedomPlan> = planState

    val projection: Flow<FinancialFreedomProjection> = combine(
        planState,
        financeDao.observeTransactions(query = "", type = null),
        financeDao.observeSavingsGoals(),
        investmentDao.observeAssets(),
    ) { currentPlan, transactions, savings, investments ->
        val ninetyDaysAgo = System.currentTimeMillis() - 90L * 24L * 60L * 60L * 1_000L
        val trailingExpense = transactions.asSequence()
            .filter { it.type == "EXPENSE" && it.occurredAt >= ninetyDaysAgo }
            .sumOf { it.amount }
        val automaticMonthlyExpenses = trailingExpense / 3L
        val savingsValue = if (currentPlan.includeSavings) {
            savings.sumOf { it.currentAmount.coerceAtLeast(0L) }
        } else {
            0L
        }
        val investmentValue = investments.sumOf { asset ->
            (asset.units.coerceAtLeast(0.0) * asset.currentPrice.coerceAtLeast(0L).toDouble())
                .roundToLong()
                .coerceAtLeast(0L)
        }
        FinancialFreedomCalculator.calculate(
            plan = currentPlan,
            automaticMonthlyExpenses = automaticMonthlyExpenses,
            currentInvestableAssets = safeAdd(savingsValue, investmentValue),
        )
    }

    fun savePlan(plan: FinancialFreedomPlan) {
        FinancialFreedomCalculator.calculate(
            plan = plan,
            automaticMonthlyExpenses = 0L,
            currentInvestableAssets = 0L,
        )
        preferences.edit()
            .putLong(KEY_MONTHLY_EXPENSES, plan.monthlyExpenses)
            .putLong(KEY_WITHDRAWAL_RATE, plan.withdrawalRatePercent.toRawBits())
            .putLong(KEY_EXPECTED_RETURN, plan.expectedReturnPercent.toRawBits())
            .putLong(KEY_INFLATION, plan.inflationPercent.toRawBits())
            .putLong(KEY_MONTHLY_CONTRIBUTION, plan.monthlyContribution)
            .putBoolean(KEY_INCLUDE_SAVINGS, plan.includeSavings)
            .apply()
        planState.value = plan
    }

    private fun loadPlan(): FinancialFreedomPlan = FinancialFreedomPlan(
        monthlyExpenses = preferences.getLong(KEY_MONTHLY_EXPENSES, 0L).coerceAtLeast(0L),
        withdrawalRatePercent = preferences.double(KEY_WITHDRAWAL_RATE, 4.0),
        expectedReturnPercent = preferences.double(KEY_EXPECTED_RETURN, 8.0),
        inflationPercent = preferences.double(KEY_INFLATION, 3.0),
        monthlyContribution = preferences.getLong(KEY_MONTHLY_CONTRIBUTION, 0L).coerceAtLeast(0L),
        includeSavings = preferences.getBoolean(KEY_INCLUDE_SAVINGS, true),
    )

    private fun android.content.SharedPreferences.double(key: String, default: Double): Double =
        if (contains(key)) Double.fromBits(getLong(key, default.toRawBits())) else default

    private fun safeAdd(left: Long, right: Long): Long =
        if (Long.MAX_VALUE - left < right) Long.MAX_VALUE else left + right

    companion object {
        private const val PREFERENCES_NAME = "financial_freedom_preferences"
        private const val KEY_MONTHLY_EXPENSES = "monthly_expenses"
        private const val KEY_WITHDRAWAL_RATE = "withdrawal_rate"
        private const val KEY_EXPECTED_RETURN = "expected_return"
        private const val KEY_INFLATION = "inflation"
        private const val KEY_MONTHLY_CONTRIBUTION = "monthly_contribution"
        private const val KEY_INCLUDE_SAVINGS = "include_savings"
    }
}
