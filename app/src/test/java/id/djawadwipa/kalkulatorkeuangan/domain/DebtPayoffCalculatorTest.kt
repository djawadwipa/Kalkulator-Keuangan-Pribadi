package id.djawadwipa.kalkulatorkeuangan.domain

import id.djawadwipa.kalkulatorkeuangan.model.Debt
import id.djawadwipa.kalkulatorkeuangan.model.DebtType
import id.djawadwipa.kalkulatorkeuangan.model.PayoffStrategy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DebtPayoffCalculatorTest {
    private val startAt = 1_704_067_200_000L

    @Test
    fun overviewAggregatesActiveDebt() {
        val overview = DebtPayoffCalculator.overview(sampleDebts())

        assertEquals(2, overview.activeCount)
        assertEquals(6_000_000L, overview.totalStartingBalance)
        assertEquals(750_000L, overview.totalPaid)
        assertEquals(5_250_000L, overview.totalBalance)
        assertEquals(210_000L, overview.minimumMonthlyPayment)
        assertTrue(overview.progressPercent > 12.0)
    }

    @Test
    fun snowballPrioritizesSmallestBalance() {
        val plan = DebtPayoffCalculator.simulate(
            debts = sampleDebts(),
            monthlyBudget = 600_000L,
            strategy = PayoffStrategy.SNOWBALL,
            startAt = startAt,
        )

        assertTrue(plan.feasible)
        assertEquals("Paylater", plan.steps.first().debtName)
        assertTrue(requireNotNull(plan.monthsToDebtFree) > 0)
        assertTrue(plan.totalInterest > 0L)
    }

    @Test
    fun avalanchePrioritizesHighestInterestRate() {
        val plan = DebtPayoffCalculator.simulate(
            debts = sampleDebts(),
            monthlyBudget = 600_000L,
            strategy = PayoffStrategy.AVALANCHE,
            startAt = startAt,
        )

        assertTrue(plan.feasible)
        assertEquals("Kartu Kredit", plan.steps.first().debtName)
        assertEquals(24.0, plan.steps.first().annualInterestRate, 0.001)
    }

    @Test
    fun budgetBelowMinimumIsRejected() {
        val plan = DebtPayoffCalculator.simulate(
            debts = sampleDebts(),
            monthlyBudget = 200_000L,
            strategy = PayoffStrategy.SNOWBALL,
            startAt = startAt,
        )

        assertFalse(plan.feasible)
        assertEquals(210_000L, plan.minimumRequired)
        assertTrue(plan.monthsToDebtFree == null)
    }

    private fun sampleDebts(): List<Debt> = listOf(
        debt(
            id = 1,
            name = "Paylater",
            startingBalance = 1_000_000L,
            paidAmount = 250_000L,
            currentBalance = 750_000L,
            interestRate = 5.0,
            minimumPayment = 10_000L,
        ),
        debt(
            id = 2,
            name = "Kartu Kredit",
            startingBalance = 5_000_000L,
            paidAmount = 500_000L,
            currentBalance = 4_500_000L,
            interestRate = 24.0,
            minimumPayment = 200_000L,
        ),
    )

    private fun debt(
        id: Long,
        name: String,
        startingBalance: Long,
        paidAmount: Long,
        currentBalance: Long,
        interestRate: Double,
        minimumPayment: Long,
    ): Debt = Debt(
        id = id,
        name = name,
        creditor = "Kreditur",
        type = DebtType.OTHER,
        startingBalance = startingBalance,
        annualInterestRate = interestRate,
        minimumPayment = minimumPayment,
        dueDay = 10,
        startDate = startAt,
        targetPayoffDate = null,
        paidAmount = paidAmount,
        currentBalance = currentBalance,
        createdAt = startAt,
        updatedAt = startAt,
    )
}
