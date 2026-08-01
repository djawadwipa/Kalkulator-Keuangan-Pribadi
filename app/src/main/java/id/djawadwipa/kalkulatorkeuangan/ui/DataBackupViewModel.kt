package id.djawadwipa.kalkulatorkeuangan.ui

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import id.djawadwipa.kalkulatorkeuangan.data.LocalDataBackupManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DataBackupViewModel(
    private val manager: LocalDataBackupManager,
) : ViewModel() {
    private val mutableState = MutableStateFlow(DataBackupUiState())
    val uiState: StateFlow<DataBackupUiState> = mutableState.asStateFlow()

    fun exportPlain(uri: Uri) = runAction("Database berhasil diekspor tanpa enkripsi") {
        manager.exportPlainDatabase(uri)
    }

    fun importPlain(uri: Uri) = runImport {
        manager.importPlainDatabase(uri)
    }

    fun exportEncrypted(uri: Uri, password: String) = runAction("Backup terenkripsi berhasil dibuat") {
        manager.exportEncryptedBackup(uri, password)
    }

    fun importEncrypted(uri: Uri, password: String) = runImport {
        manager.importEncryptedBackup(uri, password)
    }

    fun clearMessage() {
        mutableState.value = mutableState.value.copy(message = null)
    }

    fun restartApplication(context: Context) {
        LocalDataBackupManager.restartApplication(context.applicationContext)
    }

    private fun runAction(
        successMessage: String,
        action: suspend () -> LocalDataBackupManager.Result,
    ) {
        if (mutableState.value.isBusy) return
        mutableState.value = mutableState.value.copy(isBusy = true, message = null)
        viewModelScope.launch {
            runCatching { action() }
                .onSuccess { result ->
                    mutableState.value = DataBackupUiState(
                        message = "$successMessage • ${formatBytes(result.byteCount)} • database v${result.databaseVersion}",
                    )
                }
                .onFailure { error ->
                    mutableState.value = DataBackupUiState(
                        message = error.message?.takeIf(String::isNotBlank)
                            ?: "Operasi data gagal",
                    )
                }
        }
    }

    private fun runImport(action: suspend () -> LocalDataBackupManager.Result) {
        if (mutableState.value.isBusy) return
        mutableState.value = mutableState.value.copy(isBusy = true, message = null)
        viewModelScope.launch {
            runCatching { action() }
                .onSuccess { result ->
                    mutableState.value = DataBackupUiState(
                        message = "Data berhasil diimpor • ${formatBytes(result.byteCount)} • database v${result.databaseVersion}",
                        restartRequired = true,
                    )
                }
                .onFailure { error ->
                    mutableState.value = DataBackupUiState(
                        message = error.message?.takeIf(String::isNotBlank)
                            ?: "File tidak dapat diimpor",
                    )
                }
        }
    }

    private fun formatBytes(bytes: Int): String = when {
        bytes >= 1_048_576 -> String.format(java.util.Locale.US, "%.1f MB", bytes / 1_048_576.0)
        bytes >= 1_024 -> String.format(java.util.Locale.US, "%.1f KB", bytes / 1_024.0)
        else -> "$bytes byte"
    }

    class Factory(context: Context) : ViewModelProvider.Factory {
        private val manager = LocalDataBackupManager(context.applicationContext)

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(DataBackupViewModel::class.java))
            return DataBackupViewModel(manager) as T
        }
    }
}

data class DataBackupUiState(
    val isBusy: Boolean = false,
    val message: String? = null,
    val restartRequired: Boolean = false,
)
