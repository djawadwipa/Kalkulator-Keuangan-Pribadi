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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import id.djawadwipa.kalkulatorkeuangan.model.AccountType
import id.djawadwipa.kalkulatorkeuangan.model.FinancialProfile
import id.djawadwipa.kalkulatorkeuangan.model.FinancialProfileDraft
import id.djawadwipa.kalkulatorkeuangan.model.TransactionType

@Composable
internal fun MoreScreen(
    state: FinanceUiState,
    onSaveProfile: (FinancialProfileDraft) -> Unit,
    onAddAccount: (String, AccountType) -> Unit,
    onAddCategory: (String, TransactionType) -> Unit,
    onDemo: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier,
) {
    var confirmClear by rememberSaveable { mutableStateOf(false) }
    var showProfileDialog by rememberSaveable { mutableStateOf(false) }
    var showAccountDialog by rememberSaveable { mutableStateOf(false) }
    var showCategoryDialog by rememberSaveable { mutableStateOf(false) }
    var showDebtPlanner by rememberSaveable { mutableStateOf(false) }
    var showInvestmentPlanner by rememberSaveable { mutableStateOf(false) }
    var showNetWorthPlanner by rememberSaveable { mutableStateOf(false) }

    if (showDebtPlanner) {
        DebtRouteScreen(
            onBack = { showDebtPlanner = false },
            modifier = modifier,
        )
        return
    }
    if (showInvestmentPlanner) {
        InvestmentRouteScreen(
            onBack = { showInvestmentPlanner = false },
            modifier = modifier,
        )
        return
    }
    if (showNetWorthPlanner) {
        NetWorthRouteScreen(
            onBack = { showNetWorthPlanner = false },
            modifier = modifier,
        )
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                "Lainnya",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Text("Offline-first • tanpa iklan • tanpa analytics • tanpa permission sensitif")
        }
        item {
            SectionTitle("Profil keuangan")
            MasterDataRow(
                title = state.profile.displayName.ifBlank { "Profil belum dilengkapi" },
                subtitle = "Target pemasukan ${rupiah(state.profile.monthlyIncomeTarget)} • tabungan ${state.profile.savingsTargetPercent}%",
            )
            OutlinedButton(
                onClick = { showProfileDialog = true },
                modifier = Modifier.fillMaxWidth(),
            ) {
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
            )
            OutlinedButton(
                onClick = { showDebtPlanner = true },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Buka cicilan, utang, dan strategi pelunasan")
            }
        }
        item {
            SectionTitle("Investasi dan portofolio")
            MasterDataRow(
                title = "Portfolio tracker",
                subtitle = "Catat posisi, alokasi, keuntungan/rugi, dan proyeksi investasi secara lokal.",
            )
            OutlinedButton(
                onClick = { showInvestmentPlanner = true },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Buka investasi, portofolio, dan simulasi")
            }
        }
        item {
            SectionTitle("Aset, liabilitas, dan Net Worth")
            MasterDataRow(
                title = "Net Worth tracker",
                subtitle = "Gabungkan aset manual dengan tabungan, investasi, dan sisa utang secara otomatis.",
            )
            OutlinedButton(
                onClick = { showNetWorthPlanner = true },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Buka aset, liabilitas, dan Net Worth")
            }
        }
        item {
            SectionTitle("Rekening")
            OutlinedButton(
                onClick = { showAccountDialog = true },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Tambah rekening")
            }
        }
        items(state.accounts, key = { "account-${it.id}" }) { account ->
            MasterDataRow(account.name, account.type.label)
        }
        item {
            SectionTitle("Kategori")
            OutlinedButton(
                onClick = { showCategoryDialog = true },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Tambah kategori")
            }
        }
        items(state.categories, key = { "category-${it.id}" }) { category ->
            MasterDataRow(category.name, category.type.label)
        }
        item {
            SectionTitle("Data lokal")
            Button(
                onClick = onDemo,
                enabled = state.recentTransactions.isEmpty(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    if (state.recentTransactions.isEmpty()) {
                        "Isi data contoh"
                    } else {
                        "Data contoh hanya untuk database kosong"
                    },
                )
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
        item { Text("Versi 0.8.0 • id.djawadwipa.kalkulatorkeuangan") }
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
    if (showCategoryDialog) {
        AddCategoryDialog(
            onDismiss = { showCategoryDialog = false },
            onSave = { name, type ->
                onAddCategory(name, type)
                showCategoryDialog = false
            },
        )
    }
    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("Hapus semua transaksi?") },
            text = {
                Text("Profil, rekening, kategori, budget, target, setoran, utang, investasi, item Net Worth, dan riwayat terkait tetap tersimpan. Tindakan ini tidak dapat dibatalkan.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        onClear()
                        confirmClear = false
                    },
                ) {
                    Text("Hapus")
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }) {
                    Text("Batal")
                }
            },
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
    onDismiss: () -> Unit,
    onSave: (String, AccountType) -> Unit,
) {
    var name by rememberSaveable { mutableStateOf("") }
    var type by rememberSaveable { mutableStateOf(AccountType.CASH) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tambah rekening") },
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
            Button(
                enabled = name.isNotBlank(),
                onClick = { onSave(name.trim(), type) },
            ) { Text("Simpan") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } },
    )
}

@Composable
private fun AddCategoryDialog(
    onDismiss: () -> Unit,
    onSave: (String, TransactionType) -> Unit,
) {
    var name by rememberSaveable { mutableStateOf("") }
    var type by rememberSaveable { mutableStateOf(TransactionType.EXPENSE) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tambah kategori") },
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
            Button(
                enabled = name.isNotBlank(),
                onClick = { onSave(name.trim(), type) },
            ) { Text("Simpan") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } },
    )
}
