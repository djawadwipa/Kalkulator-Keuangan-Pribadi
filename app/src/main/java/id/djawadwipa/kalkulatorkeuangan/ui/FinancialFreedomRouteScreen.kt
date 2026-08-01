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
import androidx.compose.material3.Switch
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
import id.djawadwipa.kalkulatorkeuangan.model.FinancialFreedomPlan
import id.djawadwipa.kalkulatorkeuangan.ui.theme.Emerald
import id.djawadwipa.kalkulatorkeuangan.ui.theme.Navy
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
internal fun FinancialFreedomRouteScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val factory = remember(context) { FinancialFreedomViewModel.Factory(context) }
    val viewModel: FinancialFreedomViewModel = viewModel(factory = factory)
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showEditor by rememberSaveable { mutableStateOf(false) }
    val projection = state.projection

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            TextButton(onClick = onBack) { Text("‹ Kembali ke Lainnya") }
            Text(
                "Financial Freedom",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                "Proyeksi menggunakan pengeluaran, aset investasi lokal, asumsi return, inflasi, dan safe withdrawal rate.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        state.message?.let { message ->
            item {
                Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(message, modifier = Modifier.weight(1f))
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
                    Text("Target Financial Freedom", color = Emerald, fontWeight = FontWeight.SemiBold)
                    Text(
                        rupiah(projection.targetAmount),
                        color = androidx.compose.ui.graphics.Color.White,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        "Aset terhitung ${rupiah(projection.currentInvestableAssets)} • progres ${freedomPercent(projection.progressPercent)}",
                        color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.80f),
                    )
                    LinearProgressIndicator(
                        progress = { (projection.progressPercent / 100.0).coerceIn(0.0, 1.0).toFloat() },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
        item { MetricCard("Pengeluaran bulanan", rupiah(projection.monthlyExpenses), Modifier.fillMaxWidth()) }
        item { MetricCard("Pendapatan pasif saat ini", rupiah(projection.passiveIncomePerMonth), Modifier.fillMaxWidth()) }
        item { MetricCard("Sisa menuju target", rupiah(projection.remainingAmount), Modifier.fillMaxWidth()) }
        item {
            MetricCard(
                "Estimasi tercapai",
                projection.estimatedCompletionAt?.let(::formatFreedomMonth)
                    ?: if (projection.currentInvestableAssets >= projection.targetAmount && projection.targetAmount > 0L) {
                        "Sudah tercapai"
                    } else {
                        "Belum dapat dihitung"
                    },
                Modifier.fillMaxWidth(),
            )
        }
        item {
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text("Asumsi aktif", fontWeight = FontWeight.Bold)
                    Text("Safe withdrawal rate ${freedomPercent(state.plan.withdrawalRatePercent)}")
                    Text("Return nominal ${freedomPercent(state.plan.expectedReturnPercent)}")
                    Text("Inflasi ${freedomPercent(state.plan.inflationPercent)}")
                    Text("Return riil ${freedomPercent(projection.realAnnualReturnPercent)}")
                    Text("Setoran rutin ${rupiah(state.plan.monthlyContribution)} per bulan")
                    Text(
                        if (state.plan.monthlyExpenses == 0L) {
                            "Pengeluaran memakai rata-rata transaksi 90 hari terakhir."
                        } else {
                            "Pengeluaran memakai asumsi manual."
                        },
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        if (state.plan.includeSavings) {
                            "Saldo target tabungan ikut dihitung bersama nilai pasar investasi."
                        } else {
                            "Hanya nilai pasar investasi yang dihitung."
                        },
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    OutlinedButton(onClick = { showEditor = true }, modifier = Modifier.fillMaxWidth()) {
                        Text("Ubah asumsi")
                    }
                }
            }
        }
        item { SectionTitle("Milestone") }
        items(projection.milestones, key = { it.percent }) { milestone ->
            MasterDataRow(
                title = "${milestone.percent}% • ${rupiah(milestone.targetAmount)}",
                subtitle = if (milestone.reached) "Tercapai" else "Belum tercapai",
            )
        }
        item {
            Text(
                "Proyeksi adalah alat perencanaan berdasarkan asumsi. Return investasi, inflasi, pengeluaran, dan safe withdrawal rate dapat berubah.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }

    if (showEditor) {
        FinancialFreedomPlanDialog(
            plan = state.plan,
            onDismiss = { showEditor = false },
            onSave = { plan ->
                viewModel.savePlan(plan)
                showEditor = false
            },
        )
    }
}

@Composable
private fun FinancialFreedomPlanDialog(
    plan: FinancialFreedomPlan,
    onDismiss: () -> Unit,
    onSave: (FinancialFreedomPlan) -> Unit,
) {
    var monthlyExpenses by rememberSaveable {
        mutableStateOf(plan.monthlyExpenses.takeIf { it > 0L }?.toString().orEmpty())
    }
    var withdrawalRate by rememberSaveable { mutableStateOf(plan.withdrawalRatePercent.toString()) }
    var expectedReturn by rememberSaveable { mutableStateOf(plan.expectedReturnPercent.toString()) }
    var inflation by rememberSaveable { mutableStateOf(plan.inflationPercent.toString()) }
    var contribution by rememberSaveable {
        mutableStateOf(plan.monthlyContribution.takeIf { it > 0L }?.toString().orEmpty())
    }
    var includeSavings by rememberSaveable { mutableStateOf(plan.includeSavings) }

    val parsedExpenses = monthlyExpenses.toLongOrNull() ?: 0L
    val parsedWithdrawal = withdrawalRate.decimalOrNull()
    val parsedReturn = expectedReturn.decimalOrNull()
    val parsedInflation = inflation.decimalOrNull()
    val parsedContribution = contribution.toLongOrNull() ?: 0L
    val valid = parsedExpenses >= 0L && parsedContribution >= 0L &&
        parsedWithdrawal != null && parsedWithdrawal in 0.1..20.0 &&
        parsedReturn != null && parsedReturn in -50.0..100.0 &&
        parsedInflation != null && parsedInflation in -20.0..100.0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Asumsi Financial Freedom") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                item {
                    OutlinedTextField(
                        value = monthlyExpenses,
                        onValueChange = { monthlyExpenses = it.filter(Char::isDigit).take(15) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Pengeluaran bulanan (Rp)") },
                        supportingText = { Text("Kosongkan untuk rata-rata 90 hari") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                    )
                }
                item { DecimalField(withdrawalRate, { withdrawalRate = it }, "Safe withdrawal rate (%)") }
                item { DecimalField(expectedReturn, { expectedReturn = it }, "Estimasi return tahunan (%)") }
                item { DecimalField(inflation, { inflation = it }, "Estimasi inflasi tahunan (%)") }
                item {
                    OutlinedTextField(
                        value = contribution,
                        onValueChange = { contribution = it.filter(Char::isDigit).take(15) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Setoran investasi per bulan (Rp)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                    )
                }
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("Hitung saldo tabungan")
                            Text(
                                "Selain nilai pasar investasi",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(checked = includeSavings, onCheckedChange = { includeSavings = it })
                    }
                }
            }
        },
        confirmButton = {
            Button(
                enabled = valid,
                onClick = {
                    onSave(
                        FinancialFreedomPlan(
                            monthlyExpenses = parsedExpenses,
                            withdrawalRatePercent = requireNotNull(parsedWithdrawal),
                            expectedReturnPercent = requireNotNull(parsedReturn),
                            inflationPercent = requireNotNull(parsedInflation),
                            monthlyContribution = parsedContribution,
                            includeSavings = includeSavings,
                        ),
                    )
                },
            ) { Text("Simpan") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } },
    )
}

@Composable
private fun DecimalField(value: String, onValueChange: (String) -> Unit, label: String) {
    OutlinedTextField(
        value = value,
        onValueChange = { input ->
            onValueChange(input.filter { it.isDigit() || it == '.' || it == ',' || it == '-' }.take(8))
        },
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true,
    )
}

private fun String.decimalOrNull(): Double? = replace(',', '.').toDoubleOrNull()

private fun freedomPercent(value: Double): String = String.format(Locale.US, "%.1f%%", value)

private fun formatFreedomMonth(value: Long): String = SimpleDateFormat(
    "MMMM yyyy",
    Locale("id", "ID"),
).format(Date(value))
