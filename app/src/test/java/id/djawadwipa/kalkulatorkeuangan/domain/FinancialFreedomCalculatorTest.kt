package id.djawadwipa.kalkulatorkeuangan.domain

import id.djawadwipa.kalkulatorkeuangan.model.FinancialFreedomPlan
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FinancialFreedomCalculatorTest {
    @Test
    fun targetUsesAnnualExpensesAndWithdrawalRate() {
        val result = FinancialFreedomCalculator.calculate(
            plan = FinancialFreedomPlan(
                monthlyExpenses = 10_000_000L,
                withdrawalRatePercent = 4.0,
                expectedReturnPercent = 8.0,
                inflationPercent = 3.0,
                monthlyContribution = 5_000_000L,
            ),
            automaticMonthlyExpenses = 0L,
            currentInvestableAssets = 300_000_000L,
            now = 0L,
        )

        assertEquals(3_000_000_000L, result.targetAmount)
        assertEquals(10.0, result.progressPercent, 0.001)
        assertEquals(1_000_000L, result.passiveIncomePerMonth)
        assertTrue(result.estimatedMonths != null && result.estimatedMonths!! > 0)
    }

    @Test
    fun zeroManualExpenseUsesAutomaticExpense() {
        val result = FinancialFreedomCalculator.calculate(
            plan = FinancialFreedomPlan(monthlyExpenses = 0L),
            automaticMonthlyExpenses = 6_000_000L,
            currentInvestableAssets = 0L,
        )

        assertEquals(6_000_000L, result.monthlyExpenses)
        assertEquals(1_800_000_000L, result.targetAmount)
    }

    @Test
    fun targetAlreadyReachedHasZeroMonths() {
        val result = FinancialFreedomCalculator.calculate(
            plan = FinancialFreedomPlan(monthlyExpenses = 1_000_000L),
            automaticMonthlyExpenses = 0L,
            currentInvestableAssets = 500_000_000L,
        )

        assertEquals(0, result.estimatedMonths)
        assertEquals(100.0, result.progressPercent, 0.001)
        assertTrue(result.milestones.all { it.reached })
    }

    @Test
    fun noContributionAndNonPositiveRealReturnCannotEstimate() {
        val result = FinancialFreedomCalculator.calculate(
            plan = FinancialFreedomPlan(
                monthlyExpenses = 5_000_000L,
                expectedReturnPercent = 2.0,
                inflationPercent = 4.0,
                monthlyContribution = 0L,
            ),
            automaticMonthlyExpenses = 0L,
            currentInvestableAssets = 100_000_000L,
        )

        assertNull(result.estimatedMonths)
    }
}
