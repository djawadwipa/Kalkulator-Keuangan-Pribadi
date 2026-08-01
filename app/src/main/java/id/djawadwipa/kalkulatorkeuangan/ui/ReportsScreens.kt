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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import id.djawadwipa.kalkulatorkeuangan.model.MonthlyCashFlow
import id.djawadwipa.kalkulatorkeuangan.model.MonthlyReportSnapshot
import id.djawadwipa.kalkulatorkeuangan.ui.theme.Emerald
import id.djawadwipa.kalkulatorkeuangan.ui.theme.Navy
import kotlin.math.abs

@Composable
internal fun ReportsScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val factory = remember(context) { ReportsViewModel.Factory(context) }
    val viewModel: ReportsViewModel = viewModel(factory = factory)
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showReviewEditor by rememberSaveable { mutableStateOf(false) }
    val report = state.report
    val cashFlow = report.cashFlow

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                "Laporan bulanan",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                "Pantau arus kas, perubahan, tren, kategori, rekening, dan evaluasi setiap bulan.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        item {
            ReportMonthNavigator(
                monthStart = state.selectedMonthStart,
                onPrevious = viewModel::previousMonth,
                onNext = viewModel::nextMonth,
                onCurrent = viewModel::selectCurrentMonth,
            )
        }
        state.message?.let { message ->
            item {
                Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(message)
                        TextButton(onClick = viewModel::clearMessage) { Text("Tutup") }
                    }
                }
            }
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = Navy),
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    Text("Arus kas bersih", color = Emerald, fontWeight = FontWeight.SemiBold)
                    Text(
                        rupiah(cashFlow.netCashFlow),
                        color = androidx.compose.ui.graphics.Color.White,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        "${cashFlow.transactionCount} transaksi • saving rate ${percent(cashFlow.savingsRate)}",
                        color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.78f),
                    )
                }
            }
        }
        item { MetricCard("Pemasukan", rupiah(cashFlow.income), Modifier.fillMaxWidth()) }
        item { MetricCard("Pengeluaran", rupiah(cashFlow.expense), Modifier.fillMaxWidth()) }
        item { MetricCard("Financial Health Score", "${report.healthScore}/100", Modifier.fillMaxWidth()) }
        item {
            MetricCard(
                "Cakupan dana darurat",
                "${reportDecimal(report.emergencyFundMonths)} bulan",
                Modifier.fillMaxWidth(),
            )
        }
        item {
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text("Perbandingan dengan bulan sebelumnya", fontWeight = FontWeight.Bold)
                    Text("Pemasukan ${changeText(report.change.incomePercent)}")
                    Text("Pengeluaran ${changeText(report.change.expensePercent)}")
                    Text(
                        "Perubahan arus kas ${signedRupiah(report.change.netAmount)}",
                        color = if (report.change.netAmount >= 0L) Emerald else MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
        item {
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    Text("Kedisiplinan budget", fontWeight = FontWeight.Bold)
                    Text("Kepatuhan ${percent(report.budget.adherencePercent)}")
                    LinearProgressIndicator(
                        progress = { (report.budget.adherencePercent / 100.0).coerceIn(0.0, 1.0).toFloat() },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        "Terpakai ${rupiah(report.budget.totalSpent)} dari ${rupiah(report.budget.totalBudget)}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        item { SectionTitle("Tren 12 bulan") }
        if (report.trend.isEmpty()) {
            item { EmptyState("Belum ada data tren.") }
        } else {
            val maxAmount = report.trend.maxOfOrNull { maxOf(it.income, it.expense) }?.coerceAtLeast(1L) ?: 1L
            items(report.trend, key = { it.monthStart }) { point ->
                TrendRow(point = point, maxAmount = maxAmount)
            }
        }
        item { SectionTitle("Kategori pengeluaran") }
        if (report.expenseCategories.isEmpty()) {
            item { EmptyState("Belum ada pengeluaran pada ${formatMonth(report.monthStart)}.") }
        } else {
            items(report.expenseCategories.take(8), key = { it.categoryName }) { category ->
                Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(category.categoryName, fontWeight = FontWeight.SemiBold)
                            Text(rupiah(category.amount))
                        }
                        LinearProgressIndicator(
                            progress = { (category.sharePercent / 100.0).coerceIn(0.0, 1.0).toFloat() },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Text(
                            "${percent(category.sharePercent)} dari pengeluaran",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
        item { SectionTitle("Arus kas per rekening") }
        if (report.accounts.isEmpty()) {
            item { EmptyState("Belum ada aktivitas rekening pada bulan ini.") }
        } else {
            items(report.accounts, key = { it.accountId }) { account ->
                MasterDataRow(
                    title = account.accountName,
                    subtitle = "Masuk ${rupiah(account.income)} • keluar ${rupiah(account.expense)} • bersih ${signedRupiah(account.netCashFlow)}",
                )
            }
        }
        item {
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    Text("Evaluasi pribadi", fontWeight = FontWeight.Bold)
                    val review = state.review
                    if (review == null) {
                        Text("Belum ada evaluasi untuk bulan ini.")
                    } else {
                        Text("Nilai ${review.score}/100")
                        Text("Sorotan: ${review.highlight.ifBlank { "—" }}")
                        Text("Perbaikan: ${review.improvement.ifBlank { "—" }}")
                    }
                    OutlinedButton(
                        onClick = { showReviewEditor = true },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(if (review == null) "Buat evaluasi" else "Ubah evaluasi")
                    }
                }
            }
        }
        item {
            Button(onClick = viewModel::saveSnapshot, modifier = Modifier.fillMaxWidth()) {
                Text("Simpan snapshot laporan bulan ini")
            }
        }
        item { SectionTitle("Snapshot tersimpan") }
        if (state.snapshots.isEmpty()) {
            item { EmptyState("Belum ada snapshot laporan.") }
        } else {
            items(state.snapshots, key = { it.id }) { snapshot ->
                SnapshotCard(snapshot = snapshot, onDelete = { viewModel.deleteSnapshot(snapshot.id) })
            }
        }
    }

    if (showReviewEditor) {
        ReviewEditorDialog(
            currentScore = state.review?.score ?: report.healthScore,
            currentHighlight = state.review?.highlight.orEmpty(),
            currentImprovement = state.review?.improvement.orEmpty(),
            onDismiss = { showReviewEditor = false },
            onSave = { score, highlight, improvement ->
                viewModel.saveReview(score, highlight, improvement)
                showReviewEditor = false
            },
        )
    }
}

@Composable
private fun ReportMonthNavigator(
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
            Text(formatMonth(monthStart), fontWeight = FontWeight.Bold)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                OutlinedButton(onClick = onPrevious) { Text("‹ Sebelumnya") }
                OutlinedButton(onClick = onNext) { Text("Berikutnya ›") }
            }
            TextButton(onClick = onCurrent, modifier = Modifier.fillMaxWidth()) {
                Text("Kembali ke bulan ini")
            }
        }
    }
}

@Composable
private fun TrendRow(point: MonthlyCashFlow, maxAmount: Long) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(formatMonth(point.monthStart), fontWeight = FontWeight.SemiBold)
                Text(signedRupiah(point.netCashFlow))
            }
            Text("Pemasukan ${rupiah(point.income)}")
            LinearProgressIndicator(
                progress = { (point.income.toDouble() / maxAmount).coerceIn(0.0, 1.0).toFloat() },
                modifier = Modifier.fillMaxWidth(),
            )
            Text("Pengeluaran ${rupiah(point.expense)}")
            LinearProgressIndicator(
                progress = { (point.expense.toDouble() / maxAmount).coerceIn(0.0, 1.0).toFloat() },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun SnapshotCard(snapshot: MonthlyReportSnapshot, onDelete: () -> Unit) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Text(formatMonth(snapshot.monthStart), fontWeight = FontWeight.Bold)
            Text("Arus kas ${signedRupiah(snapshot.netCashFlow)} • skor ${snapshot.healthScore}/100")
            Text(
                "Masuk ${rupiah(snapshot.income)} • keluar ${rupiah(snapshot.expense)} • saving rate ${percent(snapshot.savingsRate)}",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text("Disimpan ${formatDate(snapshot.generatedAt)}", style = MaterialTheme.typography.labelSmall)
            TextButton(onClick = onDelete) { Text("Hapus snapshot") }
        }
    }
}

@Composable
private fun ReviewEditorDialog(
    currentScore: Int,
    currentHighlight: String,
    currentImprovement: String,
    onDismiss: () -> Unit,
    onSave: (Int, String, String) -> Unit,
) {
    var score by rememberSaveable { mutableStateOf(currentScore.toString()) }
    var highlight by rememberSaveable { mutableStateOf(currentHighlight) }
    var improvement by rememberSaveable { mutableStateOf(currentImprovement) }
    val parsedScore = score.toIntOrNull()
    val valid = parsedScore != null && parsedScore in 0..100

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Evaluasi bulanan") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = score,
                    onValueChange = { score = it.filter(Char::isDigit).take(3) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Nilai 0–100") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                )
                OutlinedTextField(
                    value = highlight,
                    onValueChange = { highlight = it.take(240) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Sorotan bulan ini") },
                    minLines = 2,
                )
                OutlinedTextField(
                    value = improvement,
                    onValueChange = { improvement = it.take(240) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Rencana perbaikan") },
                    minLines = 2,
                )
            }
        },
        confirmButton = {
            Button(
                enabled = valid,
                onClick = { onSave(requireNotNull(parsedScore), highlight.trim(), improvement.trim()) },
            ) { Text("Simpan") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } },
    )
}

private fun changeText(value: Double?): String = when {
    value == null -> "baru muncul dibanding bulan tanpa data"
    value > 0.05 -> "naik ${percent(value)}"
    value < -0.05 -> "turun ${percent(abs(value))}"
    else -> "tetap"
}

private fun signedRupiah(value: Long): String = (if (value >= 0L) "+" else "−") + rupiah(abs(value))
