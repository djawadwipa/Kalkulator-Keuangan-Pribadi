package id.djawadwipa.kalkulatorkeuangan.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.djawadwipa.kalkulatorkeuangan.data.AppThemeMode
import id.djawadwipa.kalkulatorkeuangan.model.DashboardSummary
import id.djawadwipa.kalkulatorkeuangan.model.FinanceAccount
import id.djawadwipa.kalkulatorkeuangan.model.FinanceCategory
import id.djawadwipa.kalkulatorkeuangan.model.FinanceTransaction
import id.djawadwipa.kalkulatorkeuangan.model.FinancialProfile
import id.djawadwipa.kalkulatorkeuangan.model.TransactionDraft
import id.djawadwipa.kalkulatorkeuangan.model.TransactionType
import id.djawadwipa.kalkulatorkeuangan.ui.theme.Emerald
import id.djawadwipa.kalkulatorkeuangan.ui.theme.Navy
import java.text.DateFormat
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class AppTab(val label: String, val symbol: String) {
    HOME("Beranda", "⌂"),
    TRANSACTIONS("Transaksi", "↕"),
    BUDGET("Budget", "◎"),
    REPORTS("Laporan", "▥"),
    MORE("Lainnya", "⋯"),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinanceApp(
    viewModel: MainViewModel,
    themeMode: AppThemeMode,
    onThemeModeChange: (AppThemeMode) -> Unit,
    hasPin: Boolean,
    biometricEnabled: Boolean,
    biometricAvailable: Boolean,
    onSetPin: (String) -> Unit,
    onClearPin: () -> Unit,
    onVerifyPin: (String) -> Boolean,
    onBiometricEnabledChange: (Boolean) -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var showEditor by rememberSaveable { mutableStateOf(false) }
    var editingTransaction by remember { mutableStateOf<FinanceTransaction?>(null) }
    var deletingTransaction by remember { mutableStateOf<FinanceTransaction?>(null) }

    LaunchedEffect(state.message) {
        state.message?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Kalkulator Keuangan Pribadi",
                        fontWeight = FontWeight.Bold,
                    )
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar {
                AppTab.entries.forEachIndexed { index, item ->
                    NavigationBarItem(
                        selected = tab == index,
                        onClick = { tab = index },
                        icon = { Text(item.symbol, fontSize = 19.sp) },
                        label = { Text(item.label) },
                    )
                }
            }
        },
        floatingActionButton = {
            if (tab <= AppTab.TRANSACTIONS.ordinal) {
                FloatingActionButton(
                    onClick = {
                        editingTransaction = null
                        showEditor = true
                    },
                ) {
                    Text("+", fontSize = 28.sp)
                }
            }
        },
    ) { padding ->
        when (AppTab.entries[tab]) {
            AppTab.HOME -> DashboardScreen(
                summary = state.summary,
                profile = state.profile,
                transactions = state.recentTransactions,
                modifier = Modifier.padding(padding),
            )

            AppTab.TRANSACTIONS -> TransactionsScreen(
                state = state,
                onSearchChange = viewModel::setSearchQuery,
                onFilterChange = viewModel::setTypeFilter,
                onEdit = {
                    editingTransaction = it
                    showEditor = true
                },
                onDelete = { deletingTransaction = it },
                modifier = Modifier.padding(padding),
            )

            AppTab.BUDGET -> BudgetScreen(
                state = state,
                onPreviousMonth = viewModel::previousMonth,
                onNextMonth = viewModel::nextMonth,
                onCurrentMonth = viewModel::selectCurrentMonth,
                onSaveBudget = viewModel::saveBudget,
                onDeleteBudget = viewModel::deleteBudget,
                modifier = Modifier.padding(padding),
            )

            AppTab.REPORTS -> ReportsScreen(
                modifier = Modifier.padding(padding),
            )

            AppTab.MORE -> MoreScreen(
                state = state,
                themeMode = themeMode,
                onThemeModeChange = onThemeModeChange,
                hasPin = hasPin,
                biometricEnabled = biometricEnabled,
                biometricAvailable = biometricAvailable,
                onSetPin = onSetPin,
                onClearPin = onClearPin,
                onVerifyPin = onVerifyPin,
                onBiometricEnabledChange = onBiometricEnabledChange,
                onSaveProfile = viewModel::saveProfile,
                onAddAccount = viewModel::addAccount,
                onUpdateAccount = viewModel::updateAccount,
                onAddCategory = viewModel::addCategory,
                onUpdateCategory = viewModel::updateCategory,
                onDemo = viewModel::addDemoData,
                onClear = viewModel::clearAllData,
                modifier = Modifier.padding(padding),
            )
        }
    }

    if (showEditor) {
        TransactionEditorDialog(
            transaction = editingTransaction,
            accounts = state.accounts,
            categories = state.categories,
            onDismiss = { showEditor = false },
            onSave = { draft ->
                editingTransaction?.let { viewModel.updateTransaction(it.id, draft) }
                    ?: viewModel.addTransaction(draft)
                showEditor = false
            },
        )
    }

    deletingTransaction?.let { transaction ->
        AlertDialog(
            onDismissRequest = { deletingTransaction = null },
            title = { Text("Hapus transaksi?") },
            text = {
                Text("${transaction.categoryName} sebesar ${rupiah(transaction.amount)} akan dihapus permanen.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteTransaction(transaction.id)
                        deletingTransaction = null
                    },
                ) {
                    Text("Hapus")
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingTransaction = null }) {
                    Text("Batal")
                }
            },
        )
    }
}

@Composable
private fun DashboardScreen(
    summary: DashboardSummary,
    profile: FinancialProfile,
    transactions: List<FinanceTransaction>,
    modifier: Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                if (profile.displayName.isBlank()) "Ringkasan bulan ini" else "Halo, ${profile.displayName}",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                "Data keuangan tersimpan lokal di perangkat.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Navy),
            ) {
                Column(
                    modifier = Modifier.padding(22.dp),
                    verticalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    Text("Arus kas bersih", color = Emerald, fontWeight = FontWeight.SemiBold)
                    Text(
                        rupiah(summary.balance),
                        color = androidx.compose.ui.graphics.Color.White,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        "${summary.transactionCount} transaksi bulan ini",
                        color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.75f),
                    )
                }
            }
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                MetricCard("Pemasukan", rupiah(summary.income), Modifier.weight(1f))
                MetricCard("Pengeluaran", rupiah(summary.expense), Modifier.weight(1f))
            }
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                MetricCard("Saving rate", percent(summary.savingsRate), Modifier.weight(1f))
                MetricCard("Health score", "${summary.healthScore}/100", Modifier.weight(1f))
            }
        }
        if (profile.monthlyIncomeTarget > 0L) {
            item {
                Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text("Target pemasukan", fontWeight = FontWeight.Bold)
                        Text("${rupiah(summary.income)} dari ${rupiah(profile.monthlyIncomeTarget)}")
                        LinearProgressIndicator(
                            progress = { (summary.incomeTargetProgress / 100.0).coerceIn(0.0, 1.0).toFloat() },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Text(
                            "Tercapai ${percent(summary.incomeTargetProgress)} • target tabungan ${profile.savingsTargetPercent}%",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        }
        item {
            Text(
                "Transaksi terbaru",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
        }
        if (transactions.isEmpty()) {
            item { EmptyState("Belum ada transaksi. Tekan tombol + untuk mulai.") }
        } else {
            items(transactions.take(5), key = { it.id }) { TransactionSummaryRow(it) }
        }
    }
}

@Composable
private fun TransactionsScreen(
    state: FinanceUiState,
    onSearchChange: (String) -> Unit,
    onFilterChange: (TransactionType?) -> Unit,
    onEdit: (FinanceTransaction) -> Unit,
    onDelete: (FinanceTransaction) -> Unit,
    modifier: Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Text(
                "Semua transaksi",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
        }
        item {
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = onSearchChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Cari kategori, rekening, atau catatan") },
                singleLine = true,
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = state.typeFilter == null,
                    onClick = { onFilterChange(null) },
                    label = { Text("Semua") },
                )
                TransactionType.entries.forEach { type ->
                    FilterChip(
                        selected = state.typeFilter == type,
                        onClick = { onFilterChange(type) },
                        label = { Text(type.label) },
                    )
                }
            }
        }
        if (state.transactions.isEmpty()) {
            item {
                EmptyState(
                    if (state.searchQuery.isBlank() && state.typeFilter == null) {
                        "Belum ada transaksi."
                    } else {
                        "Tidak ada transaksi yang cocok dengan filter."
                    },
                )
            }
        } else {
            items(state.transactions, key = { it.id }) { transaction ->
                TransactionManageRow(
                    transaction = transaction,
                    onEdit = { onEdit(transaction) },
                    onDelete = { onDelete(transaction) },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TransactionEditorDialog(
    transaction: FinanceTransaction?,
    accounts: List<FinanceAccount>,
    categories: List<FinanceCategory>,
    onDismiss: () -> Unit,
    onSave: (TransactionDraft) -> Unit,
) {
    var type by rememberSaveable(transaction?.id) { mutableStateOf(transaction?.type ?: TransactionType.EXPENSE) }
    var amount by rememberSaveable(transaction?.id) { mutableStateOf(transaction?.amount?.toString().orEmpty()) }
    var accountId by rememberSaveable(transaction?.id) { mutableStateOf(transaction?.accountId ?: accounts.firstOrNull()?.id ?: 0L) }
    var categoryId by rememberSaveable(transaction?.id) { mutableStateOf(transaction?.categoryId ?: 0L) }
    var description by rememberSaveable(transaction?.id) { mutableStateOf(transaction?.description.orEmpty()) }
    var occurredAt by rememberSaveable(transaction?.id) { mutableStateOf(transaction?.occurredAt ?: System.currentTimeMillis()) }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }

    val matchingCategories = categories.filter { it.type == type }
    LaunchedEffect(type, matchingCategories) {
        if (matchingCategories.none { it.id == categoryId }) categoryId = matchingCategories.firstOrNull()?.id ?: 0L
    }
    LaunchedEffect(accounts) {
        if (accounts.none { it.id == accountId }) accountId = accounts.firstOrNull()?.id ?: 0L
    }

    val parsedAmount = amount.toLongOrNull()
    val valid = parsedAmount != null && parsedAmount > 0 && accountId > 0 && categoryId > 0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (transaction == null) "Tambah transaksi" else "Edit transaksi") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TransactionType.entries.forEach { option ->
                            FilterChip(selected = type == option, onClick = { type = option }, label = { Text(option.label) })
                        }
                    }
                }
                item {
                    OutlinedTextField(
                        value = amount,
                        onValueChange = { amount = it.filter(Char::isDigit).take(15) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Nominal (Rp)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                    )
                }
                item {
                    SelectionMenu(
                        label = "Rekening",
                        selectedLabel = accounts.firstOrNull { it.id == accountId }?.name ?: "Pilih rekening",
                        options = accounts.map { it.id to "${it.name} • ${it.type.label}" },
                        onSelected = { accountId = it },
                    )
                }
                item {
                    SelectionMenu(
                        label = "Kategori",
                        selectedLabel = matchingCategories.firstOrNull { it.id == categoryId }?.name ?: "Pilih kategori",
                        options = matchingCategories.map { it.id to it.name },
                        onSelected = { categoryId = it },
                    )
                }
                item {
                    OutlinedButton(onClick = { showDatePicker = true }, modifier = Modifier.fillMaxWidth()) {
                        Text("Tanggal: ${formatDate(occurredAt)}")
                    }
                }
                item {
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it.take(120) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Catatan opsional") },
                    )
                }
            }
        },
        confirmButton = {
            Button(
                enabled = valid,
                onClick = {
                    onSave(
                        TransactionDraft(
                            type = type,
                            amount = requireNotNull(parsedAmount),
                            accountId = accountId,
                            categoryId = categoryId,
                            description = description.trim(),
                            occurredAt = occurredAt,
                        ),
                    )
                },
            ) { Text("Simpan") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } },
    )

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = occurredAt)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    occurredAt = datePickerState.selectedDateMillis ?: occurredAt
                    showDatePicker = false
                }) { Text("Pilih") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Batal") } },
        ) { DatePicker(state = datePickerState) }
    }
}

@Composable
internal fun SelectionMenu(
    label: String,
    selectedLabel: String,
    options: List<Pair<Long, String>>,
    onSelected: (Long) -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium)
        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth(), enabled = options.isNotEmpty()) {
                Text(selectedLabel)
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEach { (id, text) ->
                    DropdownMenuItem(text = { Text(text) }, onClick = {
                        onSelected(id)
                        expanded = false
                    })
                }
            }
        }
    }
}

@Composable
internal fun MetricCard(label: String, value: String, modifier: Modifier) {
    Card(modifier = modifier, shape = RoundedCornerShape(16.dp)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun TransactionSummaryRow(transaction: FinanceTransaction) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
        TransactionContent(transaction = transaction, modifier = Modifier.padding(16.dp))
    }
}

@Composable
private fun TransactionManageRow(transaction: FinanceTransaction, onEdit: () -> Unit, onDelete: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onEdit), shape = RoundedCornerShape(16.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            TransactionContent(transaction = transaction)
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onEdit) { Text("Edit") }
                TextButton(onClick = onDelete) { Text("Hapus") }
            }
        }
    }
}

@Composable
private fun TransactionContent(transaction: FinanceTransaction, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Column(Modifier.weight(1f)) {
            Text(transaction.categoryName, fontWeight = FontWeight.Bold)
            Text(transaction.description.ifBlank { "Tanpa catatan" }, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("${transaction.accountName} • ${formatDate(transaction.occurredAt)}", style = MaterialTheme.typography.labelSmall)
        }
        Text(
            (if (transaction.type == TransactionType.INCOME) "+" else "−") + rupiah(transaction.amount),
            color = if (transaction.type == TransactionType.INCOME) Emerald else MaterialTheme.colorScheme.error,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
internal fun MasterDataRow(
    title: String,
    subtitle: String,
    onClick: (() -> Unit)? = null,
) {
    val cardModifier = if (onClick == null) {
        Modifier.fillMaxWidth()
    } else {
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    }
    Card(cardModifier, shape = RoundedCornerShape(14.dp)) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
internal fun EmptyState(text: String) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
        Text(text, modifier = Modifier.padding(20.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
internal fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 6.dp))
}

internal fun rupiah(value: Long): String = NumberFormat.getCurrencyInstance(Locale("id", "ID")).apply {
    minimumFractionDigits = 0
    maximumFractionDigits = 0
}.format(value)

internal fun percent(value: Double): String = NumberFormat.getPercentInstance(Locale("id", "ID")).apply {
    maximumFractionDigits = 1
}.format(value / 100.0)

internal fun formatDate(value: Long): String = DateFormat.getDateInstance(DateFormat.MEDIUM, Locale("id", "ID")).format(Date(value))

internal fun formatMonth(value: Long): String = SimpleDateFormat("MMMM yyyy", Locale("id", "ID")).format(
    Date(value.takeIf { it > 0L } ?: System.currentTimeMillis()),
)
