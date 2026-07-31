package id.djawadwipa.kalkulatorkeuangan.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import id.djawadwipa.kalkulatorkeuangan.data.FinanceRepository
import id.djawadwipa.kalkulatorkeuangan.model.AccountType
import id.djawadwipa.kalkulatorkeuangan.model.DashboardSummary
import id.djawadwipa.kalkulatorkeuangan.model.FinanceAccount
import id.djawadwipa.kalkulatorkeuangan.model.FinanceCategory
import id.djawadwipa.kalkulatorkeuangan.model.FinanceTransaction
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

    private val financeData = combine(
        repository.transactions,
        repository.summary,
        repository.accounts,
        repository.categories,
    ) { transactions, summary, accounts, categories ->
        FinanceData(transactions, summary, accounts, categories)
    }

    private val completeFinanceData = combine(financeData, repository.allTransactions) { data, allTransactions ->
        data.copy(allTransactions = allTransactions)
    }

    val uiState = combine(completeFinanceData, searchQuery, typeFilter, message) { data, query, filter, notice ->
        FinanceUiState(
            transactions = data.transactions,
            recentTransactions = data.allTransactions,
            summary = data.summary,
            accounts = data.accounts,
            categories = data.categories,
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

    fun addCategory(name: String, type: TransactionType) = runAction("Kategori berhasil ditambahkan") {
        repository.addCategory(name, type)
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

private data class FinanceData(
    val transactions: List<FinanceTransaction>,
    val summary: DashboardSummary,
    val accounts: List<FinanceAccount>,
    val categories: List<FinanceCategory>,
    val allTransactions: List<FinanceTransaction> = emptyList(),
)

data class FinanceUiState(
    val transactions: List<FinanceTransaction> = emptyList(),
    val recentTransactions: List<FinanceTransaction> = emptyList(),
    val summary: DashboardSummary = DashboardSummary(),
    val accounts: List<FinanceAccount> = emptyList(),
    val categories: List<FinanceCategory> = emptyList(),
    val searchQuery: String = "",
    val typeFilter: TransactionType? = null,
    val message: String? = null,
    val isLoading: Boolean = true,
)
