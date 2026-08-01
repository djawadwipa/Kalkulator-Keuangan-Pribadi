package id.djawadwipa.kalkulatorkeuangan.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import id.djawadwipa.kalkulatorkeuangan.data.FinanceRepository
import id.djawadwipa.kalkulatorkeuangan.model.AccountType
import id.djawadwipa.kalkulatorkeuangan.model.BudgetSummary
import id.djawadwipa.kalkulatorkeuangan.model.DashboardSummary
import id.djawadwipa.kalkulatorkeuangan.model.FinanceAccount
import id.djawadwipa.kalkulatorkeuangan.model.FinanceCategory
import id.djawadwipa.kalkulatorkeuangan.model.FinanceTransaction
import id.djawadwipa.kalkulatorkeuangan.model.FinancialProfile
import id.djawadwipa.kalkulatorkeuangan.model.FinancialProfileDraft
import id.djawadwipa.kalkulatorkeuangan.model.MonthlyAnalysis
import id.djawadwipa.kalkulatorkeuangan.model.TransactionDraft
import id.djawadwipa.kalkulatorkeuangan.model.TransactionType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(
    private val repository: FinanceRepository,
) : ViewModel() {
    private val searchQuery = MutableStateFlow("")
    private val typeFilter = MutableStateFlow<TransactionType?>(null)
    private val message = MutableStateFlow<String?>(null)

    private val coreData = combine(
        repository.transactions,
        repository.allTransactions,
        repository.summary,
        repository.accounts,
        repository.categories,
    ) { transactions, allTransactions, summary, accounts, categories ->
        CoreFinanceData(
            transactions = transactions,
            allTransactions = allTransactions,
            summary = summary,
            accounts = accounts,
            categories = categories,
        )
    }

    private val planningData = combine(
        repository.profile,
        repository.budgetSummary,
        repository.monthlyAnalysis,
        repository.selectedMonthStart,
    ) { profile, budget, analysis, selectedMonth ->
        PlanningData(profile, budget, analysis, selectedMonth)
    }

    val uiState = combine(
        coreData,
        planningData,
        searchQuery,
        typeFilter,
        message,
    ) { core, planning, query, filter, notice ->
        FinanceUiState(
            transactions = core.transactions,
            recentTransactions = core.allTransactions,
            summary = core.summary,
            accounts = core.accounts,
            categories = core.categories,
            profile = planning.profile,
            budgetSummary = planning.budget,
            monthlyAnalysis = planning.analysis,
            selectedMonthStart = planning.selectedMonth,
            searchQuery = query,
            typeFilter = filter,
            message = notice,
            isLoading = false,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = FinanceUiState(),
    )

    fun setSearchQuery(value: String) {
        val normalized = value.take(80)
        searchQuery.value = normalized
        repository.setSearchQuery(normalized)
    }

    fun setTypeFilter(value: TransactionType?) {
        typeFilter.value = value
        repository.setTypeFilter(value)
    }

    fun previousMonth() = repository.previousMonth()

    fun nextMonth() = repository.nextMonth()

    fun selectCurrentMonth() = repository.selectCurrentMonth()

    fun saveProfile(draft: FinancialProfileDraft) = runAction("Profil keuangan berhasil disimpan") {
        repository.saveProfile(draft)
    }

    fun saveBudget(categoryId: Long, limitAmount: Long) = runAction("Budget berhasil disimpan") {
        repository.saveBudget(uiState.value.selectedMonthStart, categoryId, limitAmount)
    }

    fun deleteBudget(id: Long) = runAction("Budget berhasil dihapus") {
        repository.deleteBudget(id)
    }

    fun addTransaction(draft: TransactionDraft) = runAction("Transaksi berhasil disimpan") {
        repository.addTransaction(draft)
    }

    fun updateTransaction(id: Long, draft: TransactionDraft) = runAction("Transaksi berhasil diperbarui") {
        repository.updateTransaction(id, draft)
    }

    fun deleteTransaction(id: Long) = runAction("Transaksi berhasil dihapus") {
        repository.deleteTransaction(id)
    }

    fun addAccount(name: String, type: AccountType) = runAction("Rekening berhasil ditambahkan") {
        repository.addAccount(name, type)
    }

    fun updateAccount(id: Long, name: String, type: AccountType) =
        runAction("Rekening berhasil diperbarui") {
            repository.updateAccount(id, name, type)
        }

    fun addCategory(name: String, type: TransactionType) = runAction("Kategori berhasil ditambahkan") {
        repository.addCategory(name, type)
    }

    fun updateCategory(id: Long, name: String, type: TransactionType) =
        runAction("Kategori berhasil diperbarui") {
            repository.updateCategory(id, name, type)
        }

    fun addDemoData() = runAction("Data contoh berhasil ditambahkan") {
        repository.addDemoData()
    }

    fun clearAllData() = runAction("Seluruh transaksi lokal berhasil dihapus") {
        repository.clearAllTransactions()
    }

    fun clearMessage() {
        message.value = null
    }

    private fun runAction(successMessage: String, action: suspend () -> Unit) {
        viewModelScope.launch {
            runCatching { action() }
                .onSuccess { message.value = successMessage }
                .onFailure { error ->
                    message.value = error.message?.takeIf(String::isNotBlank)
                        ?: "Terjadi kesalahan. Silakan coba lagi."
                }
        }
    }

    class Factory(
        private val repository: FinanceRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(MainViewModel::class.java))
            return MainViewModel(repository) as T
        }
    }
}

private data class CoreFinanceData(
    val transactions: List<FinanceTransaction>,
    val allTransactions: List<FinanceTransaction>,
    val summary: DashboardSummary,
    val accounts: List<FinanceAccount>,
    val categories: List<FinanceCategory>,
)

private data class PlanningData(
    val profile: FinancialProfile,
    val budget: BudgetSummary,
    val analysis: MonthlyAnalysis,
    val selectedMonth: Long,
)

data class FinanceUiState(
    val transactions: List<FinanceTransaction> = emptyList(),
    val recentTransactions: List<FinanceTransaction> = emptyList(),
    val summary: DashboardSummary = DashboardSummary(),
    val accounts: List<FinanceAccount> = emptyList(),
    val categories: List<FinanceCategory> = emptyList(),
    val profile: FinancialProfile = FinancialProfile(),
    val budgetSummary: BudgetSummary = BudgetSummary(),
    val monthlyAnalysis: MonthlyAnalysis = MonthlyAnalysis(),
    val selectedMonthStart: Long = 0,
    val searchQuery: String = "",
    val typeFilter: TransactionType? = null,
    val message: String? = null,
    val isLoading: Boolean = true,
)
