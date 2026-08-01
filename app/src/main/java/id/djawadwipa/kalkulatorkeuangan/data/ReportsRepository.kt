package id.djawadwipa.kalkulatorkeuangan.data

import id.djawadwipa.kalkulatorkeuangan.data.local.BudgetRecord
import id.djawadwipa.kalkulatorkeuangan.data.local.FinanceDao
import id.djawadwipa.kalkulatorkeuangan.data.local.MonthlyReportSnapshotEntity
import id.djawadwipa.kalkulatorkeuangan.data.local.MonthlyReviewEntity
import id.djawadwipa.kalkulatorkeuangan.data.local.TransactionRecord
import id.djawadwipa.kalkulatorkeuangan.domain.BudgetCalculator
import id.djawadwipa.kalkulatorkeuangan.domain.CashFlowCalculator
import id.djawadwipa.kalkulatorkeuangan.domain.FinancialCalculator
import id.djawadwipa.kalkulatorkeuangan.domain.SavingsCalculator
import id.djawadwipa.kalkulatorkeuangan.model.BudgetItem
import id.djawadwipa.kalkulatorkeuangan.model.FinanceTransaction
import id.djawadwipa.kalkulatorkeuangan.model.FinancialHealthInput
import id.djawadwipa.kalkulatorkeuangan.model.MonthlyFinancialReport
import id.djawadwipa.kalkulatorkeuangan.model.MonthlyReportSnapshot
import id.djawadwipa.kalkulatorkeuangan.model.MonthlyReview
import id.djawadwipa.kalkulatorkeuangan.model.TransactionType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map

class ReportsRepository(
    private val dao: FinanceDao,
) {
    private val _selectedMonthStart = MutableStateFlow(
        CashFlowCalculator.monthStart(System.currentTimeMillis()),
    )

    val selectedMonthStart: StateFlow<Long> = _selectedMonthStart.asStateFlow()

    private val transactions: Flow<List<FinanceTransaction>> =
        dao.observeTransactions(query = "", type = null).map { rows ->
            rows.map(TransactionRecord::toReportModel)
        }

    private val savingsGoals = dao.observeSavingsGoals().map { rows ->
        val now = System.currentTimeMillis()
        rows.map { it.toModel(now) }
    }

    val report: Flow<MonthlyFinancialReport> = _selectedMonthStart.flatMapLatest { month ->
        val nextMonth = CashFlowCalculator.shiftMonth(month, 1)
        combine(
            transactions,
            dao.observeBudgets(month, nextMonth),
            savingsGoals,
        ) { allTransactions, budgetRows, goals ->
            val budget = BudgetCalculator.summary(month, budgetRows.map(BudgetRecord::toReportModel))
            val selectedRows = allTransactions.filter { it.occurredAt >= month && it.occurredAt < nextMonth }
            val income = selectedRows.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
            val expense = selectedRows.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
            val emergencyFundMonths = SavingsCalculator.overview(goals, expense).emergencyFundMonths
            val healthScore = FinancialCalculator.healthScore(
                FinancialHealthInput(
                    income = income,
                    expense = expense,
                    emergencyFundMonths = emergencyFundMonths,
                    budgetAdherence = budget.adherencePercent,
                ),
            )
            CashFlowCalculator.monthlyReport(
                transactions = allTransactions,
                selectedMonthStart = month,
                budget = budget,
                healthScore = healthScore,
                emergencyFundMonths = emergencyFundMonths,
                trendMonths = 12,
            )
        }
    }

    val review: Flow<MonthlyReview?> = _selectedMonthStart.flatMapLatest { month ->
        dao.observeMonthlyReview(month).map { it?.toModel() }
    }

    val snapshots: Flow<List<MonthlyReportSnapshot>> =
        dao.observeMonthlyReportSnapshots().map { rows -> rows.map(MonthlyReportSnapshotEntity::toModel) }

    fun previousMonth() {
        _selectedMonthStart.value = CashFlowCalculator.shiftMonth(_selectedMonthStart.value, -1)
    }

    fun nextMonth() {
        _selectedMonthStart.value = CashFlowCalculator.shiftMonth(_selectedMonthStart.value, 1)
    }

    fun selectCurrentMonth() {
        _selectedMonthStart.value = CashFlowCalculator.monthStart(System.currentTimeMillis())
    }

    suspend fun saveReview(score: Int, highlight: String, improvement: String) {
        require(score in 0..100) { "Nilai evaluasi harus 0 sampai 100" }
        require(highlight.length <= 240) { "Sorotan maksimal 240 karakter" }
        require(improvement.length <= 240) { "Rencana perbaikan maksimal 240 karakter" }
        dao.saveMonthlyReview(
            MonthlyReviewEntity(
                monthStart = _selectedMonthStart.value,
                score = score,
                highlight = highlight.trim(),
                improvement = improvement.trim(),
                updatedAt = System.currentTimeMillis(),
            ),
        )
    }

    suspend fun saveSnapshot() {
        val current = report.first()
        dao.saveMonthlyReportSnapshot(
            MonthlyReportSnapshotEntity(
                monthStart = current.monthStart,
                income = current.cashFlow.income,
                expense = current.cashFlow.expense,
                netCashFlow = current.cashFlow.netCashFlow,
                savingsRate = current.cashFlow.savingsRate,
                budgetAdherence = current.budget.adherencePercent,
                healthScore = current.healthScore,
                generatedAt = System.currentTimeMillis(),
            ),
        )
    }

    suspend fun deleteSnapshot(id: Long) {
        require(id > 0L) { "ID snapshot tidak valid" }
        dao.deleteMonthlyReportSnapshotById(id)
    }
}

private fun TransactionRecord.toReportModel(): FinanceTransaction = FinanceTransaction(
    id = id,
    type = TransactionType.valueOf(type),
    amount = amount,
    accountId = accountId,
    accountName = accountName,
    categoryId = categoryId,
    categoryName = categoryName,
    description = description,
    occurredAt = occurredAt,
)

private fun BudgetRecord.toReportModel(): BudgetItem = BudgetCalculator.budgetItem(
    id = id,
    monthStart = monthStart,
    categoryId = categoryId,
    categoryName = categoryName,
    limitAmount = limitAmount,
    spentAmount = spentAmount,
)

private fun MonthlyReviewEntity.toModel(): MonthlyReview = MonthlyReview(
    monthStart = monthStart,
    score = score,
    highlight = highlight,
    improvement = improvement,
    updatedAt = updatedAt,
)

private fun MonthlyReportSnapshotEntity.toModel(): MonthlyReportSnapshot = MonthlyReportSnapshot(
    id = id,
    monthStart = monthStart,
    income = income,
    expense = expense,
    netCashFlow = netCashFlow,
    savingsRate = savingsRate,
    budgetAdherence = budgetAdherence,
    healthScore = healthScore,
    generatedAt = generatedAt,
)
