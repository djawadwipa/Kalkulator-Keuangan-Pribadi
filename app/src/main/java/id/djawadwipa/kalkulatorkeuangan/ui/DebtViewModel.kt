package id.djawadwipa.kalkulatorkeuangan.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import id.djawadwipa.kalkulatorkeuangan.data.DebtRepository
import id.djawadwipa.kalkulatorkeuangan.data.local.FinanceDatabase
import id.djawadwipa.kalkulatorkeuangan.domain.DebtPayoffCalculator
import id.djawadwipa.kalkulatorkeuangan.model.Debt
import id.djawadwipa.kalkulatorkeuangan.model.DebtDraft
import id.djawadwipa.kalkulatorkeuangan.model.DebtOverview
import id.djawadwipa.kalkulatorkeuangan.model.DebtPayment
import id.djawadwipa.kalkulatorkeuangan.model.DebtPaymentDraft
import id.djawadwipa.kalkulatorkeuangan.model.DebtPayoffPlan
import id.djawadwipa.kalkulatorkeuangan.model.PayoffStrategy
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DebtViewModel(
    private val repository: DebtRepository,
) : ViewModel() {
    private val monthlyBudget = MutableStateFlow(0L)
    private val strategy = MutableStateFlow(PayoffStrategy.SNOWBALL)
    private val message = MutableStateFlow<String?>(null)

    val uiState = combine(
        repository.debts,
        repository.payments,
        monthlyBudget,
        strategy,
        message,
    ) { debts, payments, budget, selectedStrategy, notice ->
        DebtUiState(
            debts = debts,
            payments = payments,
            overview = DebtPayoffCalculator.overview(debts),
            strategy = selectedStrategy,
            monthlyBudget = budget,
            plan = DebtPayoffCalculator.simulate(debts, budget, selectedStrategy),
            message = notice,
            isLoading = false,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DebtUiState(),
    )

    fun setMonthlyBudget(value: Long) {
        monthlyBudget.value = value.coerceAtLeast(0L)
    }

    fun setStrategy(value: PayoffStrategy) {
        strategy.value = value
    }

    fun saveDebt(id: Long?, draft: DebtDraft) = runAction("Data utang berhasil disimpan") {
        repository.saveDebt(id, draft)
    }

    fun deleteDebt(id: Long) = runAction("Utang dan riwayat pembayarannya berhasil dihapus") {
        repository.deleteDebt(id)
    }

    fun addPayment(draft: DebtPaymentDraft) = runAction("Pembayaran berhasil dicatat") {
        repository.addPayment(draft)
    }

    fun deletePayment(id: Long) = runAction("Pembayaran berhasil dihapus") {
        repository.deletePayment(id)
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
        private val repository = DebtRepository(
            FinanceDatabase.getInstance(context.applicationContext).financeDao(),
        )

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(DebtViewModel::class.java))
            return DebtViewModel(repository) as T
        }
    }
}

data class DebtUiState(
    val debts: List<Debt> = emptyList(),
    val payments: List<DebtPayment> = emptyList(),
    val overview: DebtOverview = DebtOverview(),
    val strategy: PayoffStrategy = PayoffStrategy.SNOWBALL,
    val monthlyBudget: Long = 0,
    val plan: DebtPayoffPlan = DebtPayoffPlan(
        strategy = PayoffStrategy.SNOWBALL,
        monthlyBudget = 0,
        minimumRequired = 0,
        totalBalance = 0,
        monthsToDebtFree = 0,
        debtFreeAt = null,
        totalInterest = 0,
        feasible = true,
        message = "Tidak ada utang aktif.",
        steps = emptyList(),
    ),
    val message: String? = null,
    val isLoading: Boolean = true,
)
