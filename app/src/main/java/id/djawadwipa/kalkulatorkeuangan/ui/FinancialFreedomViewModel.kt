package id.djawadwipa.kalkulatorkeuangan.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import id.djawadwipa.kalkulatorkeuangan.data.FinancialFreedomRepository
import id.djawadwipa.kalkulatorkeuangan.model.FinancialFreedomPlan
import id.djawadwipa.kalkulatorkeuangan.model.FinancialFreedomProjection
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class FinancialFreedomViewModel(
    private val repository: FinancialFreedomRepository,
) : ViewModel() {
    private val message = MutableStateFlow<String?>(null)

    val uiState = combine(
        repository.plan,
        repository.projection,
        message,
    ) { plan, projection, notice ->
        FinancialFreedomUiState(
            plan = plan,
            projection = projection,
            message = notice,
            isLoading = false,
        )
    }.stateIn(
        scope = androidx.lifecycle.viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = FinancialFreedomUiState(),
    )

    fun savePlan(plan: FinancialFreedomPlan) {
        runCatching { repository.savePlan(plan) }
            .onSuccess { message.value = "Asumsi Financial Freedom berhasil disimpan" }
            .onFailure { error ->
                message.value = error.message?.takeIf(String::isNotBlank)
                    ?: "Asumsi tidak dapat disimpan"
            }
    }

    fun clearMessage() {
        message.value = null
    }

    class Factory(context: Context) : ViewModelProvider.Factory {
        private val repository = FinancialFreedomRepository(context.applicationContext)

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(FinancialFreedomViewModel::class.java))
            return FinancialFreedomViewModel(repository) as T
        }
    }
}

data class FinancialFreedomUiState(
    val plan: FinancialFreedomPlan = FinancialFreedomPlan(),
    val projection: FinancialFreedomProjection = FinancialFreedomProjection(),
    val message: String? = null,
    val isLoading: Boolean = true,
)
