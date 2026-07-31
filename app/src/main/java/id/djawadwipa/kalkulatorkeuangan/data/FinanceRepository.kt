package id.djawadwipa.kalkulatorkeuangan.data

import id.djawadwipa.kalkulatorkeuangan.data.local.FinanceDatabaseHelper
import id.djawadwipa.kalkulatorkeuangan.domain.FinancialCalculator
import id.djawadwipa.kalkulatorkeuangan.model.DashboardSummary
import id.djawadwipa.kalkulatorkeuangan.model.FinanceTransaction
import id.djawadwipa.kalkulatorkeuangan.model.FinancialHealthInput
import id.djawadwipa.kalkulatorkeuangan.model.TransactionType
import java.util.Calendar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FinanceRepository(
    private val database: FinanceDatabaseHelper,
) {
    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _transactions = MutableStateFlow<List<FinanceTransaction>>(emptyList())
    private val _summary = MutableStateFlow(DashboardSummary())

    val transactions: StateFlow<List<FinanceTransaction>> = _transactions.asStateFlow()
    val summary: StateFlow<DashboardSummary> = _summary.asStateFlow()

    init {
        repositoryScope.launch { refreshInternal() }
    }

    suspend fun addTransaction(
        type: TransactionType,
        amount: Long,
        category: String,
        description: String,
    ) = withContext(Dispatchers.IO) {
        database.insertTransaction(
            type = type,
            amount = amount,
            category = category,
            description = description,
            occurredAt = System.currentTimeMillis(),
        )
        refreshInternal()
    }

    suspend fun addDemoData() = withContext(Dispatchers.IO) {
        if (database.listTransactions().isNotEmpty()) return@withContext
        val now = System.currentTimeMillis()
        database.insertTransaction(TransactionType.INCOME, 12_500_000, "Gaji", "Pemasukan bulanan", now - 86_400_000L * 5)
        database.insertTransaction(TransactionType.EXPENSE, 2_500_000, "Tempat tinggal", "Sewa/KPR", now - 86_400_000L * 4)
        database.insertTransaction(TransactionType.EXPENSE, 1_250_000, "Kebutuhan pokok", "Belanja bulanan", now - 86_400_000L * 3)
        database.insertTransaction(TransactionType.EXPENSE, 650_000, "Transportasi", "BBM dan parkir", now - 86_400_000L * 2)
        database.insertTransaction(TransactionType.EXPENSE, 500_000, "Tabungan", "Dana darurat", now - 86_400_000L)
        refreshInternal()
    }

    suspend fun clearAllData() = withContext(Dispatchers.IO) {
        database.deleteAllTransactions()
        refreshInternal()
    }

    private fun refreshInternal() {
        val allTransactions = database.listTransactions()
        _transactions.value = allTransactions

        val monthStart = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val currentMonth = allTransactions.filter { it.occurredAt >= monthStart }
        val income = currentMonth
            .filter { it.type == TransactionType.INCOME }
            .sumOf { it.amount }
        val expense = currentMonth
            .filter { it.type == TransactionType.EXPENSE }
            .sumOf { it.amount }

        _summary.value = DashboardSummary(
            income = income,
            expense = expense,
            balance = income - expense,
            savingsRate = FinancialCalculator.savingsRate(income, expense),
            expenseRatio = FinancialCalculator.expenseRatio(income, expense),
            healthScore = FinancialCalculator.healthScore(
                FinancialHealthInput(
                    income = income,
                    expense = expense,
                ),
            ),
            transactionCount = currentMonth.size,
        )
    }
}
