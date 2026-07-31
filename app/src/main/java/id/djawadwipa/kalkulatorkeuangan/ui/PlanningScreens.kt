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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import id.djawadwipa.kalkulatorkeuangan.model.BudgetItem
import id.djawadwipa.kalkulatorkeuangan.model.BudgetStatus
import id.djawadwipa.kalkulatorkeuangan.model.FinanceCategory
import id.djawadwipa.kalkulatorkeuangan.model.TransactionType
import id.djawadwipa.kalkulatorkeuangan.ui.theme.Emerald
import id.djawadwipa.kalkulatorkeuangan.ui.theme.Navy

@Composable
internal fun BudgetScreen(
    state: FinanceUiState,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onCurrentMonth: () -> Unit,
    onSaveBudget: (Long, Long) -> Unit,
    onDeleteBudget: (Long) -> Unit,
    modifier: Modifier,
) {
    var showEditor by rememberSaveable { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<BudgetItem?>(null) }
    var deletingItem by remember { mutableStateOf<BudgetItem?>(null) }
    val expenseCategories = state.categories.filter { it.type == TransactionType.EXPENSE }
    val summary = state.budgetSummary

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                "Budget bulanan",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                "Tetapkan batas pengeluaran per kategori dan pantau realisasinya.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        item {
            MonthNavigator(
                monthStart = state.selectedMonthStart,
                onPrevious = onPreviousMonth,
                onNext = onNextMonth,
                onCurrent = onCurrentMonth,
            )
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = Navy),
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("Sisa budget", color = Emerald, fontWeight = FontWeight.SemiBold)
                    Text(
                        rupiah(summary.remaining),
                        color = androidx.compose.ui.graphics.Color.White,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        "Terpakai ${rupiah(summary.totalSpent)} dari ${rupiah(summary.totalBudget)}",
                        color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.78f),
                    )
                    LinearProgressIndicator(
                        progress = { (summary.utilizationPercent / 100.0).coerceIn(0.0, 1.0).toFloat() },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        "Kepatuhan budget ${percent(summary.adherencePercent)}",
                        color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.78f),
                    )
                }
            }
        }
        item {
            Button(
                onClick = {
                    editingItem = null
                    showEditor = true
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = expenseCategories.isNotEmpty(),
            ) {
                Text("Tambah budget kategori")
            }
        }
        item { SectionTitle("Rincian kategori") }
        if (summary.items.isEmpty()) {
            item { EmptyState("Belum ada budget untuk ${formatMonth(state.selectedMonthStart)}.") }
        } else {
            items(summary.items, key = { it.id }) { item ->
                BudgetItemCard(
                    item = item,
                    onEdit = {
                        editingItem = item
                        showEditor = true
                    },
                    onDelete = { deletingItem = item },
                )
            }
        }
    }

    if (showEditor) {
        BudgetEditorDialog(
            item = editingItem,
            categories = expenseCategories,
            onDismiss = { showEditor = false },
            onSave = { categoryId, amount ->
                onSaveBudget(categoryId, amount)
                showEditor = false
            },
        )
    }

    deletingItem?.let { item ->
        AlertDialog(
            onDismissRequest = { deletingItem = null },
            title = { Text("Hapus budget?") },
            text = { Text("Budget ${item.categoryName} untuk bulan ini akan dihapus.") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteBudget(item.id)
                        deletingItem = null
                    },
                ) { Text("Hapus") }
            },
            dismissButton = {
                TextButton(onClick = { deletingItem = null }) { Text("Batal") }
            },
        )
    }
}

@Composable
private fun BudgetItemCard(
    item: BudgetItem,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(item.categoryName, fontWeight = FontWeight.Bold)
                    Text(
                        item.status.label,
                        color = when (item.status) {
                            BudgetStatus.SAFE -> Emerald
                            BudgetStatus.WARNING -> MaterialTheme.colorScheme.tertiary
                            BudgetStatus.EXCEEDED -> MaterialTheme.colorScheme.error
                        },
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
                Text(
                    percent(item.utilizationPercent),
                    fontWeight = FontWeight.Bold,
                )
            }
            LinearProgressIndicator(
                progress = { (item.utilizationPercent / 100.0).coerceIn(0.0, 1.0).toFloat() },
                modifier = Modifier.fillMaxWidth(),
            )
            Text("${rupiah(item.spentAmount)} dari ${rupiah(item.limitAmount)}")
            Text(
                if (item.remainingAmount >= 0L) {
                    "Sisa ${rupiah(item.remainingAmount)}"
                } else {
                    "Melebihi ${rupiah(-item.remainingAmount)}"
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onEdit) { Text("Edit") }
                TextButton(onClick = onDelete) { Text("Hapus") }
            }
        }
    }
}

@Composable
private fun BudgetEditorDialog(
    item: BudgetItem?,
    categories: List<FinanceCategory>,
    onDismiss: () -> Unit,
    onSave: (Long, Long) -> Unit,
) {
    var categoryId by rememberSaveable(item?.id) {
        mutableStateOf(item?.categoryId ?: categories.firstOrNull()?.id ?: 0L)
    }
    var amount by rememberSaveable(item?.id) {
        mutableStateOf(item?.limitAmount?.toString().orEmpty())
    }
    val parsedAmount = amount.toLongOrNull()
    val valid = categoryId > 0L && parsedAmount != null && parsedAmount > 0L

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (item == null) "Tambah budget" else "Edit budget") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SelectionMenu(
                    label = "Kategori pengeluaran",
                    selectedLabel = categories.firstOrNull { it.id == categoryId }?.name ?: "Pilih kategori",
                    options = if (item == null) {
                        categories.map { it.id to it.name }
                    } else {
                        categories.filter { it.id == item.categoryId }.map { it.id to it.name }
                    },
                    onSelected = { categoryId = it },
                )
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it.filter(Char::isDigit).take(15) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Batas budget (Rp)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                )
            }
        },
        confirmButton = {
            Button(
                enabled = valid,
                onClick = { onSave(categoryId, requireNotNull(parsedAmount)) },
            ) { Text("Simpan") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } },
    )
}

@Composable
internal fun AnalyticsScreen(
    state: FinanceUiState,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onCurrentMonth: () -> Unit,
    modifier: Modifier,
) {
    val analysis = state.monthlyAnalysis
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                "Analisis pengeluaran",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                "Pahami kategori terbesar dan kedisiplinan budget setiap bulan.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        item {
            MonthNavigator(
                monthStart = state.selectedMonthStart,
                onPrevious = onPreviousMonth,
                onNext = onNextMonth,
                onCurrent = onCurrentMonth,
            )
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                MetricCard("Total pengeluaran", rupiah(analysis.totalExpense), Modifier.weight(1f))
                MetricCard("Rata-rata harian", rupiah(analysis.averageDailyExpense), Modifier.weight(1f))
            }
        }
        item {
            MetricCard(
                "Kategori terbesar",
                "${analysis.topCategoryName} • ${rupiah(analysis.topCategoryAmount)}",
                Modifier.fillMaxWidth(),
            )
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                MetricCard(
                    "Pemakaian budget",
                    percent(analysis.budget.utilizationPercent),
                    Modifier.weight(1f),
                )
                MetricCard(
                    "Kepatuhan",
                    percent(analysis.budget.adherencePercent),
                    Modifier.weight(1f),
                )
            }
        }
        item { SectionTitle("Komposisi pengeluaran") }
        if (analysis.categories.isEmpty()) {
            item { EmptyState("Belum ada pengeluaran pada ${formatMonth(state.selectedMonthStart)}.") }
        } else {
            items(analysis.categories, key = { it.categoryId }) { category ->
                Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(7.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(category.categoryName, fontWeight = FontWeight.Bold)
                            Text(rupiah(category.amount), fontWeight = FontWeight.SemiBold)
                        }
                        LinearProgressIndicator(
                            progress = { (category.sharePercent / 100.0).coerceIn(0.0, 1.0).toFloat() },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Text(
                            "${percent(category.sharePercent)} dari pengeluaran bulan ini",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthNavigator(
    monthStart: Long,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onCurrent: () -> Unit,
) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                formatMonth(monthStart),
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(onClick = onPrevious, modifier = Modifier.weight(1f)) {
                    Text("‹ Sebelumnya")
                }
                OutlinedButton(onClick = onNext, modifier = Modifier.weight(1f)) {
                    Text("Berikutnya ›")
                }
            }
            TextButton(onClick = onCurrent, modifier = Modifier.fillMaxWidth()) {
                Text("Kembali ke bulan ini")
            }
        }
    }
}
