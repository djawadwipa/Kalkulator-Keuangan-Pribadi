package id.djawadwipa.kalkulatorkeuangan.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import id.djawadwipa.kalkulatorkeuangan.data.InvestmentRepository
import id.djawadwipa.kalkulatorkeuangan.data.local.FinanceDatabase
import id.djawadwipa.kalkulatorkeuangan.domain.InvestmentCalculator
import id.djawadwipa.kalkulatorkeuangan.model.InvestmentAsset
import id.djawadwipa.kalkulatorkeuangan.model.InvestmentAssetDraft
import id.djawadwipa.kalkulatorkeuangan.model.InvestmentProjection
import id.djawadwipa.kalkulatorkeuangan.model.InvestmentTransaction
import id.djawadwipa.kalkulatorkeuangan.model.InvestmentTransactionDraft
import id.djawadwipa.kalkulatorkeuangan.model.PortfolioOverview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class InvestmentViewModel(
    private val repository: InvestmentRepository,
) : ViewModel() {
    private val message = MutableStateFlow<String?>(null)
    private val simulationInput = MutableStateFlow(InvestmentSimulationInput())

    val uiState = combine(
        repository.assets,
        repository.transactions,
        simulationInput,
        message,
    ) { assets, transactions, simulation, notice ->
        InvestmentUiState(
            assets = assets,
            transactions = transactions,
            overview = InvestmentCalculator.overview(assets, transactions),
            simulation = simulation,
            projection = runCatching {
                InvestmentCalculator.project(
                    initialInvestment = simulation.initialInvestment,
                    monthlyContribution = simulation.monthlyContribution,
                    annualReturnPercent = simulation.annualReturnPercent,
                    years = simulation.years,
                    annualInflationPercent = simulation.annualInflationPercent,
                )
            }.getOrNull(),
            message = notice,
            isLoading = false,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = InvestmentUiState(),
    )

    fun saveAsset(id: Long?, draft: InvestmentAssetDraft) = runAction("Aset investasi berhasil disimpan") {
        repository.saveAsset(id, draft)
    }

    fun updateMarketPrice(assetId: Long, price: Long) = runAction("Harga pasar berhasil diperbarui") {
        repository.updateMarketPrice(assetId, price)
    }

    fun deleteAsset(id: Long) = runAction("Aset dan seluruh riwayatnya berhasil dihapus") {
        repository.deleteAsset(id)
    }

    fun addTransaction(draft: InvestmentTransactionDraft) = runAction("Transaksi investasi berhasil dicatat") {
        repository.addTransaction(draft)
    }

    fun deleteTransaction(id: Long) = runAction("Transaksi investasi berhasil dihapus") {
        repository.deleteTransaction(id)
    }

    fun updateSimulation(input: InvestmentSimulationInput) {
        simulationInput.value = input
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
        private val database = FinanceDatabase.getInstance(context.applicationContext)
        private val repository = InvestmentRepository(database)

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(InvestmentViewModel::class.java))
            return InvestmentViewModel(repository) as T
        }
    }
}

data class InvestmentSimulationInput(
    val initialInvestment: Long = 10_000_000L,
    val monthlyContribution: Long = 1_000_000L,
    val annualReturnPercent: Double = 8.0,
    val years: Int = 10,
    val annualInflationPercent: Double = 3.0,
)

data class InvestmentUiState(
    val assets: List<InvestmentAsset> = emptyList(),
    val transactions: List<InvestmentTransaction> = emptyList(),
    val overview: PortfolioOverview = PortfolioOverview(),
    val simulation: InvestmentSimulationInput = InvestmentSimulationInput(),
    val projection: InvestmentProjection? = null,
    val message: String? = null,
    val isLoading: Boolean = true,
)
