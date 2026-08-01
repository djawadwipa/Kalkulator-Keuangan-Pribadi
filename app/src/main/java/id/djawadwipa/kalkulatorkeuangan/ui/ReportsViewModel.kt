package id.djawadwipa.kalkulatorkeuangan.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import id.djawadwipa.kalkulatorkeuangan.data.ReportsRepository
import id.djawadwipa.kalkulatorkeuangan.data.local.FinanceDatabase
import id.djawadwipa.kalkulatorkeuangan.model.MonthlyFinancialReport
import id.djawadwipa.kalkulatorkeuangan.model.MonthlyReportSnapshot
import id.djawadwipa.kalkulatorkeuangan.model.MonthlyReview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ReportsViewModel(
    private val repository: ReportsRepository,
) : ViewModel() {
    private val message = MutableStateFlow<String?>(null)

    val uiState = combine(
        repository.report,
        repository.review,
        repository.snapshots,
        repository.selectedMonthStart,
        message,
    ) { report, review, snapshots, selectedMonth, notice ->
        ReportsUiState(
            report = report,
            review = review,
            snapshots = snapshots,
            selectedMonthStart = selectedMonth,
            message = notice,
            isLoading = false,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ReportsUiState(),
    )

    fun previousMonth() = repository.previousMonth()

    fun nextMonth() = repository.nextMonth()

    fun selectCurrentMonth() = repository.selectCurrentMonth()

    fun saveReview(score: Int, highlight: String, improvement: String) =
        runAction("Evaluasi bulanan berhasil disimpan") {
            repository.saveReview(score, highlight, improvement)
        }

    fun saveSnapshot() = runAction("Snapshot laporan berhasil disimpan") {
        repository.saveSnapshot()
    }

    fun deleteSnapshot(id: Long) = runAction("Snapshot laporan berhasil dihapus") {
        repository.deleteSnapshot(id)
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

    class Factory(context: Context) : ViewModelProvider.Factory {
        private val repository = ReportsRepository(
            FinanceDatabase.getInstance(context.applicationContext).financeDao(),
        )

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(ReportsViewModel::class.java))
            return ReportsViewModel(repository) as T
        }
    }
}

data class ReportsUiState(
    val report: MonthlyFinancialReport = MonthlyFinancialReport(),
    val review: MonthlyReview? = null,
    val snapshots: List<MonthlyReportSnapshot> = emptyList(),
    val selectedMonthStart: Long = 0,
    val message: String? = null,
    val isLoading: Boolean = true,
)
