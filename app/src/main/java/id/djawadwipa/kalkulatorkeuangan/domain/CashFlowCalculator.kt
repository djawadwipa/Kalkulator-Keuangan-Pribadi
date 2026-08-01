package id.djawadwipa.kalkulatorkeuangan.domain

import id.djawadwipa.kalkulatorkeuangan.model.BudgetSummary
import id.djawadwipa.kalkulatorkeuangan.model.CashFlowChange
import id.djawadwipa.kalkulatorkeuangan.model.FinanceTransaction
import id.djawadwipa.kalkulatorkeuangan.model.MonthlyCashFlow
import id.djawadwipa.kalkulatorkeuangan.model.MonthlyFinancialReport
import id.djawadwipa.kalkulatorkeuangan.model.ReportAccountSummary
import id.djawadwipa.kalkulatorkeuangan.model.ReportCategorySummary
import id.djawadwipa.kalkulatorkeuangan.model.TransactionType
import java.util.Calendar

object CashFlowCalculator {
    fun monthlyReport(
        transactions: List<FinanceTransaction>,
        selectedMonthStart: Long,
        budget: BudgetSummary,
        trendMonths: Int = 6,
    ): MonthlyFinancialReport {
        require(trendMonths in 2..24) { "Rentang tren harus 2 sampai 24 bulan" }
        val normalizedMonth = monthStart(selectedMonthStart)
        val currentRows = transactions.inMonth(normalizedMonth)
        val previousStart = shiftMonth(normalizedMonth, -1)
        val current = summary(currentRows, normalizedMonth)
        val previous = summary(transactions.inMonth(previousStart), previousStart)

        return MonthlyFinancialReport(
            monthStart = normalizedMonth,
            cashFlow = current,
            previousMonth = previous,
            change = CashFlowChange(
                incomePercent = percentChange(current.income, previous.income),
                expensePercent = percentChange(current.expense, previous.expense),
                netAmount = current.netCashFlow - previous.netCashFlow,
            ),
            trend = (trendMonths - 1 downTo 0).map { offset ->
                val start = shiftMonth(normalizedMonth, -offset)
                summary(transactions.inMonth(start), start)
            },
            incomeCategories = categoryBreakdown(currentRows, TransactionType.INCOME),
            expenseCategories = categoryBreakdown(currentRows, TransactionType.EXPENSE),
            accounts = accountBreakdown(currentRows),
            transactions = currentRows.sortedWith(
                compareByDescending<FinanceTransaction> { it.occurredAt }
                    .thenByDescending { it.id },
            ),
            budget = budget,
        )
    }

    fun summary(
        transactions: List<FinanceTransaction>,
        monthStart: Long,
    ): MonthlyCashFlow {
        val income = transactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
        val expense = transactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
        val net = income - expense
        return MonthlyCashFlow(
            monthStart = monthStart,
            income = income,
            expense = expense,
            netCashFlow = net,
            savingsRate = if (income <= 0L) 0.0 else net.toDouble() / income.toDouble() * 100.0,
            transactionCount = transactions.size,
        )
    }

    private fun categoryBreakdown(
        transactions: List<FinanceTransaction>,
        type: TransactionType,
    ): List<ReportCategorySummary> {
        val typedRows = transactions.filter { it.type == type }
        val total = typedRows.sumOf { it.amount }
        return typedRows
            .groupBy { it.categoryName }
            .map { (name, rows) ->
                val amount = rows.sumOf { it.amount }
                ReportCategorySummary(
                    categoryName = name,
                    amount = amount,
                    sharePercent = if (total <= 0L) 0.0 else amount.toDouble() / total.toDouble() * 100.0,
                )
            }
            .sortedWith(compareByDescending<ReportCategorySummary> { it.amount }.thenBy { it.categoryName })
    }

    private fun accountBreakdown(
        transactions: List<FinanceTransaction>,
    ): List<ReportAccountSummary> = transactions
        .groupBy { it.accountId to it.accountName }
        .map { (account, rows) ->
            val income = rows.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
            val expense = rows.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
            ReportAccountSummary(
                accountId = account.first,
                accountName = account.second,
                income = income,
                expense = expense,
                netCashFlow = income - expense,
            )
        }
        .sortedWith(compareByDescending<ReportAccountSummary> { kotlin.math.abs(it.netCashFlow) }.thenBy { it.accountName })

    private fun percentChange(current: Long, previous: Long): Double? = when {
        previous == 0L && current == 0L -> 0.0
        previous == 0L -> null
        else -> (current - previous).toDouble() / previous.toDouble() * 100.0
    }

    private fun List<FinanceTransaction>.inMonth(start: Long): List<FinanceTransaction> {
        val end = shiftMonth(start, 1)
        return filter { it.occurredAt >= start && it.occurredAt < end }
    }

    private fun monthStart(timestamp: Long): Long = Calendar.getInstance().apply {
        timeInMillis = timestamp
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    private fun shiftMonth(timestamp: Long, amount: Int): Long = Calendar.getInstance().apply {
        timeInMillis = timestamp
        add(Calendar.MONTH, amount)
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}
