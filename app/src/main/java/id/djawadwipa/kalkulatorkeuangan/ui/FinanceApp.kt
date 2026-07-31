package id.djawadwipa.kalkulatorkeuangan.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.djawadwipa.kalkulatorkeuangan.model.DashboardSummary
import id.djawadwipa.kalkulatorkeuangan.model.FinanceTransaction
import id.djawadwipa.kalkulatorkeuangan.model.TransactionType
import id.djawadwipa.kalkulatorkeuangan.ui.theme.Emerald
import id.djawadwipa.kalkulatorkeuangan.ui.theme.Navy
import java.text.DateFormat
import java.text.NumberFormat
import java.util.Date
import java.util.Locale

private enum class AppTab(val label: String, val symbol: String) {
    HOME("Beranda", "⌂"), TRANSACTIONS("Transaksi", "↕"),
    ANALYTICS("Analisis", "▥"), MORE("Lainnya", "⋯"),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinanceApp(viewModel: MainViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var showAdd by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Kalkulator Keuangan Pribadi", fontWeight = FontWeight.Bold) }) },
        bottomBar = {
            NavigationBar {
                AppTab.entries.forEachIndexed { index, item ->
                    NavigationBarItem(
                        selected = tab == index,
                        onClick = { tab = index },
                        icon = { Text(item.symbol, fontSize = 20.sp) },
                        label = { Text(item.label) },
                    )
                }
            }
        },
        floatingActionButton = {
            if (tab <= AppTab.TRANSACTIONS.ordinal) {
                FloatingActionButton(onClick = { showAdd = true }) { Text("+", fontSize = 28.sp) }
            }
        },
    ) { padding ->
        when (AppTab.entries[tab]) {
            AppTab.HOME -> DashboardScreen(state.summary, state.transactions, Modifier.padding(padding))
            AppTab.TRANSACTIONS -> TransactionsScreen(state.transactions, Modifier.padding(padding))
            AppTab.ANALYTICS -> AnalyticsScreen(state.summary, Modifier.padding(padding))
            AppTab.MORE -> MoreScreen(
                hasData = state.transactions.isNotEmpty(),
                onDemo = viewModel::addDemoData,
                onClear = viewModel::clearAllData,
                modifier = Modifier.padding(padding),
            )
        }
    }

    if (showAdd) {
        AddTransactionDialog(
            onDismiss = { showAdd = false },
            onSave = { type, amount, category, note ->
                viewModel.addTransaction(type, amount, category, note)
                showAdd = false
            },
        )
    }
}

@Composable
private fun DashboardScreen(
    summary: DashboardSummary,
    transactions: List<FinanceTransaction>,
    modifier: Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text("Ringkasan bulan ini", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("Data keuangan tersimpan lokal di perangkat.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Navy),
            ) {
                Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Arus kas bersih", color = Emerald, fontWeight = FontWeight.SemiBold)
                    Text(rupiah(summary.balance), color = androidx.compose.ui.graphics.Color.White,
                        style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Text("${summary.transactionCount} transaksi bulan ini",
                        color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.75f))
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricCard("Pemasukan", rupiah(summary.income), Modifier.weight(1f))
                MetricCard("Pengeluaran", rupiah(summary.expense), Modifier.weight(1f))
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricCard("Saving rate", percent(summary.savingsRate), Modifier.weight(1f))
                MetricCard("Health score", "${summary.healthScore}/100", Modifier.weight(1f))
            }
        }
        item { Text("Transaksi terbaru", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
        if (transactions.isEmpty()) item { Text("Belum ada transaksi. Tekan tombol + untuk mulai.") }
        else items(transactions.take(5), key = { it.id }) { TransactionRow(it) }
    }
}

@Composable
private fun TransactionsScreen(items: List<FinanceTransaction>, modifier: Modifier) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item { Text("Semua transaksi", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) }
        if (items.isEmpty()) item { Text("Belum ada transaksi.") }
        else items(items, key = { it.id }) { TransactionRow(it) }
    }
}

@Composable
private fun AnalyticsScreen(summary: DashboardSummary, modifier: Modifier) {
    LazyColumn(
        modifier = modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item { Text("Analisis", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) }
        item { MetricCard("Arus kas", rupiah(summary.balance), Modifier.fillMaxWidth()) }
        item { MetricCard("Saving rate", percent(summary.savingsRate), Modifier.fillMaxWidth()) }
        item { MetricCard("Expense ratio", percent(summary.expenseRatio), Modifier.fillMaxWidth()) }
        item { MetricCard("Financial Health Score", "${summary.healthScore}/100", Modifier.fillMaxWidth()) }
    }
}

@Composable
private fun MoreScreen(
    hasData: Boolean,
    onDemo: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier,
) {
    var confirmClear by rememberSaveable { mutableStateOf(false) }
    LazyColumn(
        modifier = modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { Text("Lainnya", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) }
        item { Text("Offline-first • tanpa iklan • tanpa analytics • tanpa permission sensitif") }
        item {
            Button(onClick = onDemo, enabled = !hasData, modifier = Modifier.fillMaxWidth()) {
                Text(if (hasData) "Data contoh hanya untuk database kosong" else "Isi data contoh")
            }
        }
        item {
            OutlinedButton(onClick = { confirmClear = true }, enabled = hasData, modifier = Modifier.fillMaxWidth()) {
                Text("Hapus seluruh data lokal")
            }
        }
        item { Text("Versi 0.1.0 • id.djawadwipa.kalkulatorkeuangan") }
    }
    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("Hapus semua data?") },
            text = { Text("Tindakan ini tidak dapat dibatalkan.") },
            confirmButton = {
                Button(onClick = { onClear(); confirmClear = false }) { Text("Hapus") }
            },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("Batal") } },
        )
    }
}

@Composable
private fun MetricCard(label: String, value: String, modifier: Modifier) {
    Card(modifier = modifier, shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun TransactionRow(transaction: FinanceTransaction) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(Modifier.weight(1f)) {
                Text(transaction.category, fontWeight = FontWeight.Bold)
                Text(transaction.description.ifBlank { "Tanpa catatan" },
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(DateFormat.getDateInstance(DateFormat.MEDIUM, Locale("id", "ID"))
                    .format(Date(transaction.occurredAt)), style = MaterialTheme.typography.labelSmall)
            }
            Text(
                (if (transaction.type == TransactionType.INCOME) "+" else "−") + rupiah(transaction.amount),
                color = if (transaction.type == TransactionType.INCOME) Emerald else MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun AddTransactionDialog(
    onDismiss: () -> Unit,
    onSave: (TransactionType, Long, String, String) -> Unit,
) {
    var type by rememberSaveable { mutableStateOf(TransactionType.EXPENSE) }
    var amount by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf("") }
    var note by rememberSaveable { mutableStateOf("") }
    val parsedAmount = amount.filter(Char::isDigit).toLongOrNull()
    val valid = parsedAmount != null && parsedAmount > 0 && category.trim().isNotEmpty()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tambah transaksi") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = type == TransactionType.EXPENSE,
                        onClick = { type = TransactionType.EXPENSE },
                        label = { Text("Pengeluaran") },
                    )
                    FilterChip(
                        selected = type == TransactionType.INCOME,
                        onClick = { type = TransactionType.INCOME },
                        label = { Text("Pemasukan") },
                    )
                }
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it.filter(Char::isDigit).take(15) },
                    label = { Text("Nominal (Rp)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                )
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it.take(40) },
                    label = { Text("Kategori") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it.take(120) },
                    label = { Text("Catatan opsional") },
                )
            }
        },
        confirmButton = {
            Button(
                enabled = valid,
                onClick = { onSave(type, requireNotNull(parsedAmount), category.trim(), note.trim()) },
            ) { Text("Simpan") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } },
    )
}

private fun rupiah(value: Long): String = NumberFormat.getCurrencyInstance(Locale("id", "ID")).format(value)
private fun percent(value: Double): String = NumberFormat.getPercentInstance(Locale("id", "ID")).apply {
    maximumFractionDigits = 1
}.format(value / 100.0)
