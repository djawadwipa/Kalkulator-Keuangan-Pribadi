package id.djawadwipa.kalkulatorkeuangan.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import id.djawadwipa.kalkulatorkeuangan.data.NetWorthRepository
import id.djawadwipa.kalkulatorkeuangan.data.local.FinanceDatabase
import id.djawadwipa.kalkulatorkeuangan.model.NetWorthItem
import id.djawadwipa.kalkulatorkeuangan.model.NetWorthItemDraft
import id.djawadwipa.kalkulatorkeuangan.model.NetWorthOverview
import id.djawadwipa.kalkulatorkeuangan.model.NetWorthSnapshot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class NetWorthViewModel(
    private val repository: NetWorthRepository,
) : ViewModel() {
    private val message = MutableStateFlow<String?>(null)

    val uiState = combine(
        repository.items,
        repository.overview,
        repository.snapshots,
        message,
    ) { items, overview, snapshots, notice ->
        NetWorthUiState(
            items = items,
            overview = overview,
            snapshots = snapshots,
            message = notice,
            isLoading = false,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = NetWorthUiState(),
    )

    fun saveItem(id: Long?, draft: NetWorthItemDraft) = runAction("Item neraca berhasil disimpan") {
        repository.saveItem(id, draft)
    }

    fun deleteItem(id: Long) = runAction("Item neraca berhasil dihapus") {
        repository.deleteItem(id)
    }

    fun saveSnapshot() = runAction("Snapshot Net Worth bulan ini berhasil disimpan") {
        repository.saveCurrentMonthSnapshot(uiState.value.overview)
    }

    fun deleteSnapshot(id: Long) = runAction("Snapshot berhasil dihapus") {
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
        private val repository = NetWorthRepository(
            FinanceDatabase.getInstance(context.applicationContext),
        )

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(NetWorthViewModel::class.java))
            return NetWorthViewModel(repository) as T
        }
    }
}

data class NetWorthUiState(
    val items: List<NetWorthItem> = emptyList(),
    val overview: NetWorthOverview = NetWorthOverview(),
    val snapshots: List<NetWorthSnapshot> = emptyList(),
    val message: String? = null,
    val isLoading: Boolean = true,
)
