package id.djawadwipa.kalkulatorkeuangan.domain

import id.djawadwipa.kalkulatorkeuangan.model.FinancialHealthInput
import kotlin.math.abs
import kotlin.math.roundToInt

object FinancialCalculator {
    fun savingsRate(income: Long, expense: Long): Double {
        if (income <= 0L) return 0.0
        return ((income - expense).toDouble() / income.toDouble() * 100.0)
            .coerceIn(-999.0, 100.0)
    }

    fun expenseRatio(income: Long, expense: Long): Double {
        if (income <= 0L) return if (expense == 0L) 0.0 else 100.0
        return (expense.toDouble() / income.toDouble() * 100.0)
            .coerceAtLeast(0.0)
    }

    fun debtServiceRatio(income: Long, minimumPayments: Long): Double {
        require(minimumPayments >= 0L) { "Pembayaran minimum tidak boleh negatif" }
        if (income <= 0L) return if (minimumPayments == 0L) 0.0 else 100.0
        return (minimumPayments.toDouble() / income.toDouble() * 100.0)
            .coerceIn(0.0, 999.0)
    }

    fun investmentAllocation(investmentValue: Long, totalAssets: Long): Double {
        require(investmentValue >= 0L) { "Nilai investasi tidak boleh negatif" }
        require(totalAssets >= 0L) { "Total aset tidak boleh negatif" }
        if (totalAssets == 0L) return 0.0
        return (investmentValue.toDouble() / totalAssets.toDouble() * 100.0)
            .coerceIn(0.0, 100.0)
    }

    fun netWorthGrowth(currentNetWorth: Long, previousNetWorth: Long?): Double {
        if (previousNetWorth == null) return 0.0
        if (previousNetWorth == 0L) {
            return when {
                currentNetWorth > 0L -> 100.0
                currentNetWorth < 0L -> -100.0
                else -> 0.0
            }
        }
        return ((currentNetWorth - previousNetWorth).toDouble() /
            abs(previousNetWorth.toDouble()) * 100.0)
            .coerceIn(-999.0, 999.0)
    }

    fun healthScore(input: FinancialHealthInput): Int {
        if (input.income == 0L && input.expense == 0L) return 0

        val weighted = listOf(
            cashFlowScore(input.income, input.expense) to 0.15,
            expenseRatioScore(expenseRatio(input.income, input.expense)) to 0.15,
            savingsRateScore(savingsRate(input.income, input.expense)) to 0.15,
            debtRatioScore(input.debtRatio) to 0.15,
            emergencyFundScore(input.emergencyFundMonths) to 0.15,
            investmentScore(input.investmentRate) to 0.10,
            netWorthScore(input.netWorthGrowth) to 0.10,
            budgetScore(input.budgetAdherence) to 0.05,
        )

        return weighted.sumOf { (score, weight) -> score * weight }
            .roundToInt()
            .coerceIn(0, 100)
    }

    private fun cashFlowScore(income: Long, expense: Long): Double {
        if (income <= 0L) return 20.0
        val margin = (income - expense).toDouble() / income.toDouble()
        return when {
            margin >= 0.30 -> 100.0
            margin >= 0.20 -> 90.0
            margin >= 0.10 -> 75.0
            margin >= 0.0 -> 60.0
            else -> 20.0
        }
    }

    private fun expenseRatioScore(ratio: Double): Double = when {
        ratio <= 60.0 -> 100.0
        ratio <= 70.0 -> 85.0
        ratio <= 80.0 -> 70.0
        ratio <= 100.0 -> 45.0
        else -> 20.0
    }

    private fun savingsRateScore(rate: Double): Double = when {
        rate >= 30.0 -> 100.0
        rate >= 20.0 -> 90.0
        rate >= 10.0 -> 70.0
        rate > 0.0 -> 45.0
        else -> 20.0
    }

    private fun debtRatioScore(ratio: Double): Double = when {
        ratio <= 10.0 -> 100.0
        ratio <= 20.0 -> 85.0
        ratio <= 30.0 -> 70.0
        ratio <= 40.0 -> 45.0
        else -> 20.0
    }

    private fun emergencyFundScore(months: Double): Double = when {
        months >= 12.0 -> 100.0
        months >= 6.0 -> 90.0
        months >= 3.0 -> 70.0
        months > 0.0 -> 45.0
        else -> 20.0
    }

    private fun investmentScore(rate: Double): Double = when {
        rate >= 20.0 -> 100.0
        rate >= 10.0 -> 80.0
        rate > 0.0 -> 55.0
        else -> 20.0
    }

    private fun netWorthScore(growth: Double): Double = when {
        growth >= 10.0 -> 100.0
        growth > 0.0 -> 80.0
        growth == 0.0 -> 60.0
        else -> 20.0
    }

    private fun budgetScore(adherence: Double): Double = when {
        adherence >= 90.0 -> 100.0
        adherence >= 75.0 -> 80.0
        adherence >= 50.0 -> 60.0
        adherence > 0.0 -> 40.0
        else -> 20.0
    }
}
