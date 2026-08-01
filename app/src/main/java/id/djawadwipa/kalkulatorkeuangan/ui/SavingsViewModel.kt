package id.djawadwipa.kalkulatorkeuangan.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import id.djawadwipa.kalkulatorkeuangan.data.SavingsRepository
import id.djawadwipa.kalkulatorkeuangan.data.local.FinanceDatabase
import id.djawadwipa.kalkulatorkeuangan.model.SavingsContribution
import id.djawadwipa.kalkulatorkeuangan.model.SavingsContributionDraft
import id.djawadwipa.kalkulatorkeuangan.model.SavingsGoal
import id.djawadwipa.kalkulatorkeuangan.model.SavingsGoalDraft
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SavingsViewModel(
    private val repository: SavingsRepository,
) : ViewModel() {
    private val message = MutableStateFlow<String?>(null)

    val uiState = combine(
        repository.goals,
        repository.contributions,
        message,
    ) { goals, contributions, notice ->
        SavingsUiState(
            goals = goals,
            contributions = contributions,
            message = notice,
            isLoading = false,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SavingsUiState(),
    )

    fun saveGoal(id: Long?, draft: SavingsGoalDraft) = runAction("Target berhasil disimpan") {
        repository.saveGoal(id, draft)
    }

    fun deleteGoal(id: Long) = runAction("Target dan riwayat setorannya berhasil dihapus") {
        repository.deleteGoal(id)
    }

    fun addContribution(draft: SavingsContributionDraft) = runAction("Setoran berhasil dicatat") {
        repository.addContribution(draft)
    }

    fun deleteContribution(id: Long) = runAction("Setoran berhasil dihapus") {
        repository.deleteContribution(id)
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
        private val repository = SavingsRepository(
            FinanceDatabase.getInstance(context.applicationContext).financeDao(),
        )

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(SavingsViewModel::class.java))
            return SavingsViewModel(repository) as T
        }
    }
}

data class SavingsUiState(
    val goals: List<SavingsGoal> = emptyList(),
    val contributions: List<SavingsContribution> = emptyList(),
    val message: String? = null,
    val isLoading: Boolean = true,
)
