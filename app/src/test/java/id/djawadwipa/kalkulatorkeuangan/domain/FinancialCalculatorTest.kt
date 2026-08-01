package id.djawadwipa.kalkulatorkeuangan.domain

import id.djawadwipa.kalkulatorkeuangan.model.FinancialHealthInput
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FinancialCalculatorTest {
    @Test
    fun savingsRate_isSafeWhenIncomeIsZero() {
        assertEquals(0.0, FinancialCalculator.savingsRate(0, 1_000_000), 0.0)
    }

    @Test
    fun expenseRatio_isCalculatedCorrectly() {
        assertEquals(75.0, FinancialCalculator.expenseRatio(10_000_000, 7_500_000), 0.001)
    }

    @Test
    fun debtServiceRatio_usesMinimumPaymentsAgainstIncome() {
        assertEquals(
            15.0,
            FinancialCalculator.debtServiceRatio(10_000_000, 1_500_000),
            0.001,
        )
    }

    @Test
    fun investmentAllocation_usesMarketValueAgainstTotalAssets() {
        assertEquals(
            25.0,
            FinancialCalculator.investmentAllocation(50_000_000, 200_000_000),
            0.001,
        )
    }

    @Test
    fun netWorthGrowth_comparesAgainstPreviousSnapshot() {
        assertEquals(
            20.0,
            FinancialCalculator.netWorthGrowth(120_000_000, 100_000_000),
            0.001,
        )
        assertEquals(0.0, FinancialCalculator.netWorthGrowth(120_000_000, null), 0.0)
    }

    @Test
    fun healthScore_staysWithinRange() {
        val score = FinancialCalculator.healthScore(
            FinancialHealthInput(
                income = 10_000_000,
                expense = 7_000_000,
                debtRatio = 10.0,
                emergencyFundMonths = 6.0,
                investmentRate = 10.0,
                netWorthGrowth = 2.0,
                budgetAdherence = 90.0,
            ),
        )
        assertTrue(score in 0..100)
        assertTrue(score >= 80)
    }

    @Test
    fun emptyData_hasNoScore() {
        assertEquals(0, FinancialCalculator.healthScore(FinancialHealthInput(0, 0)))
    }
}
