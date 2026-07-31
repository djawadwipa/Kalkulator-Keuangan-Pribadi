package id.djawadwipa.kalkulatorkeuangan.domain

import id.djawadwipa.kalkulatorkeuangan.model.BudgetStatus
import id.djawadwipa.kalkulatorkeuangan.model.FinancialProfileDraft
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BudgetCalculatorTest {
    @Test
    fun budgetStatusUsesSafeWarningAndExceededThresholds() {
        assertEquals(
            BudgetStatus.SAFE,
            BudgetCalculator.budgetItem(1, 1, 1, "Makanan", 1_000, 700).status,
        )
        assertEquals(
            BudgetStatus.WARNING,
            BudgetCalculator.budgetItem(1, 1, 1, "Makanan", 1_000, 750).status,
        )
        assertEquals(
            BudgetStatus.EXCEEDED,
            BudgetCalculator.budgetItem(1, 1, 1, "Makanan", 1_000, 1_001).status,
        )
    }

    @Test
    fun summaryCalculatesUtilizationAndAdherence() {
        val items = listOf(
            BudgetCalculator.budgetItem(1, 1, 1, "Makanan", 1_000, 800),
            BudgetCalculator.budgetItem(2, 1, 2, "Transportasi", 500, 600),
        )
        val summary = BudgetCalculator.summary(1, items)

        assertEquals(1_500L, summary.totalBudget)
        assertEquals(1_400L, summary.totalSpent)
        assertEquals(100L, summary.remaining)
        assertEquals(50.0, summary.adherencePercent, 0.01)
    }

    @Test
    fun profileValidationAcceptsNormalValues() {
        assertNull(
            BudgetCalculator.validateProfile(
                FinancialProfileDraft(
                    displayName = "Djawad",
                    monthlyIncomeTarget = 10_000_000,
                    savingsTargetPercent = 20,
                ),
            ),
        )
    }
}
