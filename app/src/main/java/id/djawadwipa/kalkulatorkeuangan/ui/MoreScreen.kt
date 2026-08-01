package id.djawadwipa.kalkulatorkeuangan.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import id.djawadwipa.kalkulatorkeuangan.BuildConfig
import id.djawadwipa.kalkulatorkeuangan.data.AppThemeMode
import id.djawadwipa.kalkulatorkeuangan.model.AccountType
import id.djawadwipa.kalkulatorkeuangan.model.FinanceAccount
import id.djawadwipa.kalkulatorkeuangan.model.FinanceCategory
import id.djawadwipa.kalkulatorkeuangan.model.FinancialProfile
import id.djawadwipa.kalkulatorkeuangan.model.FinancialProfileDraft
import id.djawadwipa.kalkulatorkeuangan.model.TransactionType

@Composable
internal fun MoreScreen(
    state: FinanceUiState,
    themeMode: AppThemeMode,
    onThemeModeChange: (AppThemeMode) -> Unit,
    hasPin: Boolean,
    biometricEnabled: Boolean,
    biometricAvailable: Boolean,
    onSetPin: (String) -> Unit,
    onClearPin: () -> Unit,
    onVerifyPin: (String) -> Boolean,
    onBiometricEnabledChange: (Boolean) -> Unit,
    onSaveProfile: (FinancialProfileDraft) -> Unit,
    onAddAccount: (String, AccountType) -> Unit,
    onUpdateAccount: (Long, String, AccountType) -> Unit,
    onAddCategory: (String, TransactionType) -> Unit,
    onUpdateCategory: (Long, String, TransactionType) -> Unit,
    onDemo: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier,
) {
    var confirmClear by rememberSaveable { mutableStateOf(false) }
    var showProfileDialog by rememberSaveable { mutableStateOf(false) }
    var showAccountDialog by rememberSaveable { mutableStateOf(false) }
    var showCategoryDialog by rememberSaveable { mutableStateOf(false) }
    var editingAccount by remember { mutableStateOf<FinanceAccount?>(null) }
    var editingCategory by remember { mutableStateOf<FinanceCategory?>(null) }
    var showDebtPlanner by rememberSaveable { mutableStateOf(false) }
    var showInvestmentPlanner by rememberSaveable { mutableStateOf(false) }
    var showNetWorthPlanner by rememberSaveable { mutableStateOf(false) }
    var showFreedomPlanner by rememberSaveable { mutableStateOf(false) }
    var showDataBackup by rememberSaveable { mutableStateOf(false) }

    when {
        showDebtPlanner -> {
            DebtRouteScreen(onBack = { showDebtPlanner = false }, modifier = modifier)
            return
        }
        showInvestmentPlanner -> {
            InvestmentRouteScreen(onBack = { showInvestmentPlanner = false }, modifier = modifier)
            return
        }
        showNetWorthPlanner -> {
            NetWorthRouteScreen(onBack = { showNetWorthPlanner = false }, modifier = modifier)
            return
        }
        showFreedomPlanner -> {
            FinancialFreedomRouteScreen(onBack = { showFreedomPlanner = false }, modifier = modifier)
            return
        }
        showDataBackup -> {
            DataBackupRouteScreen(onBack = { showDataBackup = false }, modifier = modifier)
            return
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text("Lainnya", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("Offline-first • tanpa iklan • tanpa analytics • tanpa permission sensitif")
            SectionTitle("Tampilan")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = themeMode == AppThemeMode.SYSTEM,
                    onClick = { onThemeModeChange(AppThemeMode.SYSTEM) },
                    label = { Text("Ikuti sistem") },
                )
                FilterChip(
                    selected = themeMode == AppThemeMode.LIGHT,
                    onClick = { onThemeModeChange(AppThemeMode.LIGHT) },
                    label = { Text("Terang") },
                )
                FilterChip(
                    selected = themeMode == AppThemeMode.DARK,
                    onClick = { onThemeModeChange(AppThemeMode.DARK) },
                    label = { Text("Gelap") },
                )
            }
        }
        item {
            SecuritySettingsSection(
                hasPin = hasPin,
                biometricEnabled = biometricEnabled,
                biometricAvailable = biometricAvailable,
                onSetPin = onSetPin,
                onClearPin = onClearPin,
                onVerifyPin = onVerifyPin,
                onBiometricEnabledChange = onBiometricEnabledChange,
            )
        }
        item {
            SectionTitle("Profil keuangan")
            MasterDataRow(
                title = state.profile.displayName.ifBlank { "Profil belum dilengkapi" },
                subtitle = "Target pemasukan ${rupiah(state.profile.monthlyIncomeTarget)} • tabungan ${state.profile.savingsTargetPercent}%",
                onClick = { showProfileDialog = true },
            )
            OutlinedButton(onClick = { showProfileDialog = true }, modifier = Modifier.fillMaxWidth()) {
                Text("Ubah profil dan target")
            }
        }
        item {
            SectionTitle("Tabungan, dana darurat, dan target")
            SavingsPlannerSection(
                totalSavings = state.summary.totalSavings,
                emergencyFundMonths = state.summary.emergencyFundMonths,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            SectionTitle("Cicilan dan utang")
            MasterDataRow(
                title = "Debt payoff planner",
                subtitle = "Kelola pembayaran dan bandingkan strategi Snowball dengan Avalanche.",
                onClick = { showDebtPlanner = true },
            )
            OutlinedButton(onClick = { showDebtPlanner = true }, modifier = Modifier.fillMaxWidth()) {
                Text("Buka cicilan, utang, dan strategi pelunasan")
            }
        }
        item {
            SectionTitle("Investasi dan portofolio")
            MasterDataRow(
                title = "Portfolio tracker",
                subtitle = "Catat posisi, alokasi, keuntungan/rugi, dan proyeksi investasi secara lokal.",
                onClick = { showInvestmentPlanner = true },
            )
            OutlinedButton(onClick = { showInvestmentPlanner = true }, modifier = Modifier.fillMaxWidth()) {
                Text("Buka investasi, portofolio, dan simulasi")
            }
        }
        item {
            SectionTitle("Aset, liabilitas, dan Net Worth")
            MasterDataRow(
                title = "Net Worth tracker",
                subtitle = "Gabungkan aset manual dengan tabungan, investasi, dan sisa utang secara otomatis.",
                onClick = { showNetWorthPlanner = true },
            )
            OutlinedButton(onClick = { showNetWorthPlanner = true }, modifier = Modifier.fillMaxWidth()) {
                Text("Buka aset, liabilitas, dan Net Worth")
            }
        }
        item {
            SectionTitle("Financial Freedom")
            MasterDataRow(
                title = "Proyeksi kebebasan finansial",
                subtitle = "Hitung target modal, pendapatan pasif, milestone, dan estimasi tanggal tercapai.",
                onClick = { showFreedomPlanner = true },
            )
            OutlinedButton(onClick = { showFreedomPlanner = true }, modifier = Modifier.fillMaxWidth()) {
                Text("Buka proyeksi Financial Freedom")
            }
        }
        item {
            SectionTitle("Ekspor, impor, dan backup")
            MasterDataRow(
                title = "Portabilitas data lokal",
                subtitle = "Ekspor database atau buat backup AES-256-GCM dengan password melalui pemilih dokumen Android.",
                onClick = { showDataBackup = true },
            )
            OutlinedButton(onClick = { showDataBackup = true }, modifier = Modifier.fillMaxWidth()) {
                Text("Kelola ekspor, impor, dan backup")
            }
        }
        item {
            SectionTitle("Rekening")
            OutlinedButton(onClick = { showAccountDialog = true }, modifier = Modifier.fillMaxWidth()) {
                Text("Tambah rekening")
            }
        }
        items(state.accounts, key = { "account-${it.id}" }) { account ->
            MasterDataRow(
                title = account.name,
                subtitle = account.type.label,
                onClick = { editingAccount = account },
            )
        }
        item {
            SectionTitle("Kategori")
            OutlinedButton(onClick = { showCategoryDialog = true }, modifier = Modifier.fillMaxWidth()) {
                Text("Tambah kategori")
            }
        }
        items(state.categories, key = { "category-${it.id}" }) { category ->
            MasterDataRow(
                title = category.name,
                subtitle = category.type.label,
                onClick = { editingCategory = category },
            )
        }
        item {
            SectionTitle("Data lokal")
            Button(
                onClick = onDemo,
                enabled = state.recentTransactions.isEmpty(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (state.recentTransactions.isEmpty()) "Isi data contoh" else "Data contoh hanya untuk database kosong")
            }
        }
        item {
            OutlinedButton(
                onClick = { confirmClear = true },
                enabled = state.recentTransactions.isNotEmpty(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Hapus seluruh transaksi lokal")
            }
        }
        item {
            Text(
                text = "Versi ${BuildConfig.VERSION_NAME} • Djawa Dwipa • fanyagung@gmail.com",
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
            )
        }
    }

    if (showProfileDialog) {
        ProfileDialog(
            profile = state.profile,
            onDismiss = { showProfileDialog = false },
            onSave = {
                onSaveProfile(it)
                showProfileDialog = false
            },
        )
    }
    if (showAccountDialog) {
        AddAccountDialog(
            onDismiss = { showAccountDialog = false },
            onSave = { name, type ->
                onAddAccount(name, type)
                showAccountDialog = false
            },
        )
    }
    editingAccount?.let { account ->
        AddAccountDialog(
            account = account,
            onDismiss = { editingAccount = null },
            onSave = { name, type ->
                onUpdateAccount(account.id, name, type)
                editingAccount = null
            },
        )
    }
    if (showCategoryDialog) {
        AddCategoryDialog(
            onDismiss = { showCategoryDialog = false },
            onSave = { name, type ->
                onAddCategory(name, type)
                showCategoryDialog = false
            },
        )
    }
    editingCategory?.let { category ->
        AddCategoryDialog(
            category = category,
            onDismiss = { editingCategory = null },
            onSave = { name, type ->
                onUpdateCategory(category.id, name, type)
                editingCategory = null
            },
        )
    }
    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("Hapus semua transaksi?") },
            text = {
                Text("Profil, rekening, kategori, budget, target, setoran, utang, investasi, item Net Worth, dan data Financial Freedom tetap tersimpan. Tindakan ini tidak dapat dibatalkan.")
            },
            confirmButton = {
                Button(onClick = {
                    onClear()
                    confirmClear = false
                }) { Text("Hapus") }
            },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("Batal") } },
        )
    }
}

@Composable
private fun ProfileDialog(
    profile: FinancialProfile,
    onDismiss: () -> Unit,
    onSave: (FinancialProfileDraft) -> Unit,
) {
    var displayName by rememberSaveable { mutableStateOf(profile.displayName) }
    var incomeTarget by rememberSaveable {
        mutableStateOf(profile.monthlyIncomeTarget.takeIf { it > 0L }?.toString().orEmpty())
    }
    var savingsTarget by rememberSaveable { mutableStateOf(profile.savingsTargetPercent.toString()) }
    val parsedIncome = incomeTarget.toLongOrNull() ?: 0L
    val parsedSavings = savingsTarget.toIntOrNull()
    val valid = displayName.length <= 50 && parsedIncome >= 0L && parsedSavings != null && parsedSavings in 0..100

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Profil keuangan") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it.take(50) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Nama atau panggilan") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = incomeTarget,
                    onValueChange = { incomeTarget = it.filter(Char::isDigit).take(15) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Target pemasukan bulanan (Rp)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                )
                OutlinedTextField(
                    value = savingsTarget,
                    onValueChange = { savingsTarget = it.filter(Char::isDigit).take(3) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Target tabungan (%)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    supportingText = { Text("Gunakan nilai 0 sampai 100") },
                )
            }
        },
        confirmButton = {
            Button(
                enabled = valid,
                onClick = {
                    onSave(
                        FinancialProfileDraft(
                            displayName = displayName.trim(),
                            monthlyIncomeTarget = parsedIncome,
                            savingsTargetPercent = requireNotNull(parsedSavings),
                        ),
                    )
                },
            ) { Text("Simpan") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } },
    )
}

@Composable
private fun AddAccountDialog(
    account: FinanceAccount? = null,
    onDismiss: () -> Unit,
    onSave: (String, AccountType) -> Unit,
) {
    var name by rememberSaveable(account?.id) { mutableStateOf(account?.name.orEmpty()) }
    var type by rememberSaveable(account?.id) { mutableStateOf(account?.type ?: AccountType.CASH) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (account == null) "Tambah rekening" else "Ubah rekening") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(40) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Nama rekening") },
                    singleLine = true,
                )
                AccountType.entries.chunked(2).forEach { options ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        options.forEach { option ->
                            FilterChip(
                                selected = type == option,
                                onClick = { type = option },
                                label = { Text(option.label) },
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(enabled = name.isNotBlank(), onClick = { onSave(name.trim(), type) }) { Text("Simpan") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } },
    )
}

@Composable
private fun AddCategoryDialog(
    category: FinanceCategory? = null,
    onDismiss: () -> Unit,
    onSave: (String, TransactionType) -> Unit,
) {
    var name by rememberSaveable(category?.id) { mutableStateOf(category?.name.orEmpty()) }
    var type by rememberSaveable(category?.id) {
        mutableStateOf(category?.type ?: TransactionType.EXPENSE)
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (category == null) "Tambah kategori" else "Ubah kategori") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(40) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Nama kategori") },
                    singleLine = true,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TransactionType.entries.forEach { option ->
                        FilterChip(
                            selected = type == option,
                            onClick = { type = option },
                            label = { Text(option.label) },
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(enabled = name.isNotBlank(), onClick = { onSave(name.trim(), type) }) { Text("Simpan") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } },
    )
}
