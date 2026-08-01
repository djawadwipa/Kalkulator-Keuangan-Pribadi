package id.djawadwipa.kalkulatorkeuangan.domain

import id.djawadwipa.kalkulatorkeuangan.model.FinancialFreedomMilestone
import id.djawadwipa.kalkulatorkeuangan.model.FinancialFreedomPlan
import id.djawadwipa.kalkulatorkeuangan.model.FinancialFreedomProjection
import java.util.Calendar
import kotlin.math.pow
import kotlin.math.roundToLong

object FinancialFreedomCalculator {
    private const val MAX_MONTHS = 1_200

    fun calculate(
        plan: FinancialFreedomPlan,
        automaticMonthlyExpenses: Long,
        currentInvestableAssets: Long,
        now: Long = System.currentTimeMillis(),
    ): FinancialFreedomProjection {
        require(plan.monthlyExpenses >= 0L) { "Pengeluaran bulanan tidak boleh negatif" }
        require(automaticMonthlyExpenses >= 0L) { "Pengeluaran otomatis tidak boleh negatif" }
        require(currentInvestableAssets >= 0L) { "Aset investasi tidak boleh negatif" }
        require(plan.withdrawalRatePercent in 0.1..20.0) { "Safe withdrawal rate harus 0,1 sampai 20 persen" }
        require(plan.expectedReturnPercent in -50.0..100.0) { "Estimasi return harus -50 sampai 100 persen" }
        require(plan.inflationPercent in -20.0..100.0) { "Inflasi harus -20 sampai 100 persen" }
        require(plan.monthlyContribution >= 0L) { "Setoran bulanan tidak boleh negatif" }

        val monthlyExpenses = plan.monthlyExpenses.takeIf { it > 0L }
            ?: automaticMonthlyExpenses
        val annualExpenses = saturatedMultiply(monthlyExpenses, 12L)
        val targetAmount = if (annualExpenses == 0L) {
            0L
        } else {
            (annualExpenses.toDouble() / (plan.withdrawalRatePercent / 100.0))
                .coerceAtMost(Long.MAX_VALUE.toDouble())
                .roundToLong()
        }
        val remainingAmount = (targetAmount - currentInvestableAssets).coerceAtLeast(0L)
        val progress = when {
            targetAmount <= 0L -> 0.0
            else -> currentInvestableAssets.toDouble() / targetAmount.toDouble() * 100.0
        }.coerceIn(0.0, 100.0)
        val passiveIncome = (currentInvestableAssets.toDouble() *
            (plan.withdrawalRatePercent / 100.0) / 12.0)
            .coerceAtMost(Long.MAX_VALUE.toDouble())
            .roundToLong()

        val nominal = plan.expectedReturnPercent / 100.0
        val inflation = plan.inflationPercent / 100.0
        val realAnnualReturn = if (1.0 + inflation <= 0.0) {
            -1.0
        } else {
            (1.0 + nominal) / (1.0 + inflation) - 1.0
        }
        val estimatedMonths = estimateMonths(
            currentAssets = currentInvestableAssets,
            monthlyContribution = plan.monthlyContribution,
            targetAmount = targetAmount,
            realAnnualReturn = realAnnualReturn,
        )
        val completionAt = estimatedMonths?.let { months ->
            Calendar.getInstance().apply {
                timeInMillis = now
                add(Calendar.MONTH, months)
            }.timeInMillis
        }

        return FinancialFreedomProjection(
            monthlyExpenses = monthlyExpenses,
            annualExpenses = annualExpenses,
            targetAmount = targetAmount,
            currentInvestableAssets = currentInvestableAssets,
            remainingAmount = remainingAmount,
            progressPercent = progress,
            passiveIncomePerMonth = passiveIncome,
            realAnnualReturnPercent = realAnnualReturn * 100.0,
            estimatedMonths = estimatedMonths,
            estimatedCompletionAt = completionAt,
            milestones = listOf(25, 50, 75, 100).map { percent ->
                FinancialFreedomMilestone(
                    percent = percent,
                    targetAmount = (targetAmount.toDouble() * percent / 100.0).roundToLong(),
                    reached = progress + 1e-9 >= percent.toDouble(),
                )
            },
        )
    }

    private fun estimateMonths(
        currentAssets: Long,
        monthlyContribution: Long,
        targetAmount: Long,
        realAnnualReturn: Double,
    ): Int? {
        if (targetAmount <= 0L || currentAssets >= targetAmount) return 0
        val monthlyRate = when {
            realAnnualReturn <= -1.0 -> -1.0
            else -> (1.0 + realAnnualReturn).pow(1.0 / 12.0) - 1.0
        }
        if (monthlyContribution == 0L && monthlyRate <= 0.0) return null

        var balance = currentAssets.toDouble()
        repeat(MAX_MONTHS) { index ->
            balance = (balance * (1.0 + monthlyRate) + monthlyContribution.toDouble())
                .coerceAtLeast(0.0)
            if (!balance.isFinite()) return index + 1
            if (balance >= targetAmount.toDouble()) return index + 1
        }
        return null
    }

    private fun saturatedMultiply(left: Long, right: Long): Long {
        if (left == 0L || right == 0L) return 0L
        return if (left > Long.MAX_VALUE / right) Long.MAX_VALUE else left * right
    }
}
