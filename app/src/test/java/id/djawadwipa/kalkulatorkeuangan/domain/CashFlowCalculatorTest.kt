package id.djawadwipa.kalkulatorkeuangan.domain

import id.djawadwipa.kalkulatorkeuangan.model.BudgetSummary
import id.djawadwipa.kalkulatorkeuangan.model.FinanceTransaction
import id.djawadwipa.kalkulatorkeuangan.model.TransactionType
import java.util.Calendar
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CashFlowCalculatorTest {
    @Test
    fun monthlyReportBuildsComparisonTrendAndBreakdowns() {
        val august = monthStart(2026, Calendar.AUGUST)
        val july = monthStart(2026, Calendar.JULY)
        val transactions = listOf(
            transaction(1, TransactionType.INCOME, 10_000_000, "Gaji", "Bank", august + DAY),
            transaction(2, TransactionType.EXPENSE, 3_000_000, "Tempat Tinggal", "Bank", august + DAY * 2),
            transaction(3, TransactionType.EXPENSE, 1_000_000, "Makanan", "Tunai", august + DAY * 3),
            transaction(4, TransactionType.INCOME, 8_000_000, "Gaji", "Bank", july + DAY),
            transaction(5, TransactionType.EXPENSE, 5_000_000, "Makanan", "Tunai", july + DAY * 2),
        )

        val report = CashFlowCalculator.monthlyReport(
            transactions = transactions,
            selectedMonthStart = august,
            budget = BudgetSummary(monthStart = august, adherencePercent = 85.0),
            healthScore = 78,
            emergencyFundMonths = 3.5,
            trendMonths = 2,
        )

        assertEquals(10_000_000L, report.cashFlow.income)
        assertEquals(4_000_000L, report.cashFlow.expense)
        assertEquals(6_000_000L, report.cashFlow.netCashFlow)
        assertEquals(25.0, report.change.incomePercent!!, 0.01)
        assertEquals(-20.0, report.change.expensePercent!!, 0.01)
        assertEquals(3_000_000L, report.change.netAmount)
        assertEquals(2, report.trend.size)
        assertEquals("Tempat Tinggal", report.expenseCategories.first().categoryName)
        assertEquals("Bank", report.accounts.first().accountName)
        assertEquals(78, report.healthScore)
        assertEquals(3.5, report.emergencyFundMonths, 0.01)
    }

    @Test
    fun percentChangeIsUndefinedWhenPreviousMonthHasNoValue() {
        val august = monthStart(2026, Calendar.AUGUST)
        val report = CashFlowCalculator.monthlyReport(
            transactions = listOf(
                transaction(1, TransactionType.INCOME, 1_000_000, "Bonus", "Bank", august + DAY),
            ),
            selectedMonthStart = august,
            budget = BudgetSummary(monthStart = august),
            healthScore = 50,
            emergencyFundMonths = 0.0,
            trendMonths = 2,
        )

        assertNull(report.change.incomePercent)
        assertEquals(100.0, report.cashFlow.savingsRate, 0.01)
    }

    private fun transaction(
        id: Long,
        type: TransactionType,
        amount: Long,
        category: String,
        account: String,
        occurredAt: Long,
    ) = FinanceTransaction(
        id = id,
        type = type,
        amount = amount,
        accountId = if (account == "Bank") 1 else 2,
        accountName = account,
        categoryId = id,
        categoryName = category,
        description = "",
        occurredAt = occurredAt,
    )

    private fun monthStart(year: Int, month: Int): Long = Calendar.getInstance().apply {
        clear()
        set(year, month, 1, 0, 0, 0)
    }.timeInMillis

    companion object {
        private const val DAY = 86_400_000L
    }
}
