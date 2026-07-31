package id.djawadwipa.kalkulatorkeuangan.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import id.djawadwipa.kalkulatorkeuangan.data.FinanceRepository
import id.djawadwipa.kalkulatorkeuangan.model.DashboardSummary
import id.djawadwipa.kalkulatorkeuangan.model.FinanceTransaction
import id.djawadwipa.kalkulatorkeuangan.model.TransactionType
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(
    private val repository: FinanceRepository,
) : ViewModel() {
    val uiState = combine(repository.transactions, repository.summary) { transactions, summary ->
        FinanceUiState(
            transactions = transactions,
            summary = summary,
            isLoading = false,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = FinanceUiState(),
    )

    fun addTransaction(
        type: TransactionType,
        amount: Long,
        category: String,
        description: String,
    ) {
        viewModelScope.launch {
            repository.addTransaction(type, amount, category, description)
        }
    }

    fun addDemoData() {
        viewModelScope.launch { repository.addDemoData() }
    }

    fun clearAllData() {
        viewModelScope.launch { repository.clearAllData() }
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

data class FinanceUiState(
    val transactions: List<FinanceTransaction> = emptyList(),
    val summary: DashboardSummary = DashboardSummary(),
    val isLoading: Boolean = true,
)
