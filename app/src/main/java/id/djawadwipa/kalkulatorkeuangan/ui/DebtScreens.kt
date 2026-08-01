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
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import id.djawadwipa.kalkulatorkeuangan.model.Debt
import id.djawadwipa.kalkulatorkeuangan.model.DebtDraft
import id.djawadwipa.kalkulatorkeuangan.model.DebtPayment
import id.djawadwipa.kalkulatorkeuangan.model.DebtPaymentDraft
import id.djawadwipa.kalkulatorkeuangan.model.DebtPayoffPlan
import id.djawadwipa.kalkulatorkeuangan.model.DebtType
import id.djawadwipa.kalkulatorkeuangan.model.PayoffStrategy
import id.djawadwipa.kalkulatorkeuangan.ui.theme.Emerald
import id.djawadwipa.kalkulatorkeuangan.ui.theme.Navy

@Composable
internal fun DebtScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val factory = remember(context) { DebtViewModel.Factory(context) }
    val viewModel: DebtViewModel = viewModel(factory = factory)
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var budgetText by rememberSaveable { mutableStateOf("") }
    var editingDebt by remember { mutableStateOf<Debt?>(null) }
    var paymentDebt by remember { mutableStateOf<Debt?>(null) }
    var deletingDebt by remember { mutableStateOf<Debt?>(null) }
    var deletingPayment by remember { mutableStateOf<DebtPayment?>(null) }
    var showDebtEditor by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(state.overview.minimumMonthlyPayment) {
        if (budgetText.isBlank() && state.overview.minimumMonthlyPayment > 0L) {
            budgetText = state.overview.minimumMonthlyPayment.toString()
            viewModel.setMonthlyBudget(state.overview.minimumMonthlyPayment)
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text("Cicilan dan utang", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(
                "Catat saldo dan pembayaran, lalu bandingkan strategi Snowball dan Avalanche.",
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
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text("Sisa seluruh utang", color = Emerald, fontWeight = FontWeight.SemiBold)
                    Text(
                        rupiah(state.overview.totalBalance),
                        color = androidx.compose.ui.graphics.Color.White,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        "${state.overview.activeCount} utang aktif • minimum ${rupiah(state.overview.minimumMonthlyPayment)}/bulan",
                        color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.78f),
                    )
                }
            }
        }
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricCard("Sudah dibayar", rupiah(state.overview.totalPaid), Modifier.weight(1f))
                MetricCard("Progres", percent(state.overview.progressPercent), Modifier.weight(1f))
            }
        }
        item {
            Button(
                onClick = {
                    editingDebt = null
                    showDebtEditor = true
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Tambah utang atau cicilan") }
        }
        item { SectionTitle("Strategi pelunasan") }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PayoffStrategy.entries.forEach { strategy ->
                    FilterChip(
                        selected = state.strategy == strategy,
                        onClick = { viewModel.setStrategy(strategy) },
                        label = { Text(strategy.label) },
                    )
                }
            }
            Text(state.strategy.description, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item {
            OutlinedTextField(
                value = budgetText,
                onValueChange = { value ->
                    budgetText = value.filter(Char::isDigit).take(15)
                    viewModel.setMonthlyBudget(budgetText.toLongOrNull() ?: 0L)
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Total anggaran pembayaran per bulan (Rp)") },
                supportingText = {
                    Text("Minimum seluruh cicilan: ${rupiah(state.overview.minimumMonthlyPayment)}")
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
            )
        }
        item { PayoffPlanCard(state.plan) }
        item { SectionTitle("Daftar utang") }
        if (state.debts.isEmpty()) {
            item { EmptyState("Belum ada utang. Tambahkan data untuk membuat simulasi pelunasan.") }
        } else {
            items(state.debts, key = { "debt-${it.id}" }) { debt ->
                DebtCard(
                    debt = debt,
                    onPay = { paymentDebt = debt },
                    onEdit = {
                        editingDebt = debt
                        showDebtEditor = true
                    },
                    onDelete = { deletingDebt = debt },
                )
            }
        }
        item { SectionTitle("Riwayat pembayaran") }
        if (state.payments.isEmpty()) {
            item { EmptyState("Belum ada pembayaran utang yang dicatat.") }
        } else {
            items(state.payments.take(30), key = { "debt-payment-${it.id}" }) { payment ->
                PaymentHistoryCard(payment = payment, onDelete = { deletingPayment = payment })
            }
        }
    }

    if (showDebtEditor) {
        DebtEditorDialog(
            debt = editingDebt,
            onDismiss = { showDebtEditor = false },
            onSave = { draft ->
                viewModel.saveDebt(editingDebt?.id, draft)
                showDebtEditor = false
            },
        )
    }

    paymentDebt?.let { debt ->
        PaymentEditorDialog(
            debt = debt,
            onDismiss = { paymentDebt = null },
            onSave = { draft ->
                viewModel.addPayment(draft)
                paymentDebt = null
            },
        )
    }

    deletingDebt?.let { debt ->
        AlertDialog(
            onDismissRequest = { deletingDebt = null },
            title = { Text("Hapus utang?") },
            text = { Text("${debt.name} dan seluruh riwayat pembayarannya akan dihapus permanen.") },
            confirmButton = {
                Button(onClick = {
                    viewModel.deleteDebt(debt.id)
                    deletingDebt = null
                }) { Text("Hapus") }
            },
            dismissButton = { TextButton(onClick = { deletingDebt = null }) { Text("Batal") } },
        )
    }

    deletingPayment?.let { payment ->
        AlertDialog(
            onDismissRequest = { deletingPayment = null },
            title = { Text("Hapus pembayaran?") },
            text = { Text("Pembayaran ${rupiah(payment.amount)} untuk ${payment.debtName} akan dihapus.") },
            confirmButton = {
                Button(onClick = {
                    viewModel.deletePayment(payment.id)
                    deletingPayment = null
                }) { Text("Hapus") }
            },
            dismissButton = { TextButton(onClick = { deletingPayment = null }) { Text("Batal") } },
        )
    }
}

@Composable
private fun PayoffPlanCard(plan: DebtPayoffPlan) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Text("Proyeksi ${plan.strategy.label}", fontWeight = FontWeight.Bold)
            if (plan.totalBalance <= 0L) {
                Text("Tidak ada saldo utang aktif.")
            } else if (!plan.feasible) {
                Text(plan.message, color = MaterialTheme.colorScheme.error)
                Text("Anggaran minimum ${rupiah(plan.minimumRequired)} per bulan")
            } else {
                Text("Bebas utang dalam ${plan.monthsToDebtFree} bulan")
                plan.debtFreeAt?.let { Text("Perkiraan selesai ${formatMonth(it)}") }
                Text("Estimasi total bunga ${rupiah(plan.totalInterest)}")
                Text(plan.message, color = MaterialTheme.colorScheme.onSurfaceVariant)
                plan.steps.forEachIndexed { index, step ->
                    Text(
                        "${index + 1}. ${step.debtName} • lunas ${formatMonth(step.payoffDate)} • bunga ${rupiah(step.interestPaid)}",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}

@Composable
private fun DebtCard(
    debt: Debt,
    onPay: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f)) {
                    Text(debt.name, fontWeight = FontWeight.Bold)
                    Text(
                        listOf(debt.creditor, debt.type.label).filter(String::isNotBlank).joinToString(" • "),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(if (debt.isPaidOff) "Lunas" else rupiah(debt.currentBalance), fontWeight = FontWeight.Bold)
            }
            LinearProgressIndicator(
                progress = { (debt.progressPercent / 100.0).coerceIn(0.0, 1.0).toFloat() },
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                "Terbayar ${rupiah(debt.paidAmount)} dari ${rupiah(debt.startingBalance)} • ${percent(debt.progressPercent)}",
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                "Bunga ${reportDecimal(debt.annualInterestRate)}%/tahun • minimum ${rupiah(debt.minimumPayment)} • jatuh tempo tanggal ${debt.dueDay}",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                TextButton(onClick = onPay, enabled = !debt.isPaidOff) { Text("Bayar") }
                TextButton(onClick = onEdit) { Text("Edit") }
                TextButton(onClick = onDelete) { Text("Hapus") }
            }
        }
    }
}

@Composable
private fun PaymentHistoryCard(payment: DebtPayment, onDelete: () -> Unit) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(Modifier.weight(1f)) {
                Text(payment.debtName, fontWeight = FontWeight.SemiBold)
                Text(payment.note.ifBlank { "Pembayaran utang" }, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(formatDate(payment.paidAt), style = MaterialTheme.typography.labelSmall)
            }
            Column {
                Text(rupiah(payment.amount), color = Emerald, fontWeight = FontWeight.Bold)
                TextButton(onClick = onDelete) { Text("Hapus") }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DebtEditorDialog(
    debt: Debt?,
    onDismiss: () -> Unit,
    onSave: (DebtDraft) -> Unit,
) {
    var name by rememberSaveable(debt?.id) { mutableStateOf(debt?.name.orEmpty()) }
    var creditor by rememberSaveable(debt?.id) { mutableStateOf(debt?.creditor.orEmpty()) }
    var type by rememberSaveable(debt?.id) { mutableStateOf(debt?.type ?: DebtType.PERSONAL_LOAN) }
    var balance by rememberSaveable(debt?.id) { mutableStateOf(debt?.startingBalance?.toString().orEmpty()) }
    var interest by rememberSaveable(debt?.id) { mutableStateOf(debt?.annualInterestRate?.toString() ?: "0") }
    var minimum by rememberSaveable(debt?.id) { mutableStateOf(debt?.minimumPayment?.toString().orEmpty()) }
    var dueDay by rememberSaveable(debt?.id) { mutableStateOf(debt?.dueDay?.toString() ?: "1") }
    var startDate by rememberSaveable(debt?.id) { mutableStateOf(debt?.startDate ?: System.currentTimeMillis()) }
    var targetDate by rememberSaveable(debt?.id) { mutableStateOf(debt?.targetPayoffDate) }
    var pickStartDate by rememberSaveable { mutableStateOf(false) }
    var pickTargetDate by rememberSaveable { mutableStateOf(false) }

    val parsedBalance = balance.toLongOrNull()
    val parsedInterest = interest.replace(',', '.').toDoubleOrNull()
    val parsedMinimum = minimum.toLongOrNull()
    val parsedDueDay = dueDay.toIntOrNull()
    val valid = name.isNotBlank() && parsedBalance != null && parsedBalance > 0L &&
        parsedInterest != null && parsedInterest in 0.0..100.0 && parsedMinimum != null &&
        parsedMinimum > 0L && parsedDueDay != null && parsedDueDay in 1..31

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (debt == null) "Tambah utang" else "Edit utang") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it.take(60) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Nama utang") },
                        singleLine = true,
                    )
                }
                item {
                    OutlinedTextField(
                        value = creditor,
                        onValueChange = { creditor = it.take(60) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Kreditur atau penyedia") },
                        singleLine = true,
                    )
                }
                item {
                    Text("Jenis utang", style = MaterialTheme.typography.labelMedium)
                    DebtType.entries.chunked(2).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            row.forEach { option ->
                                FilterChip(
                                    selected = type == option,
                                    onClick = { type = option },
                                    label = { Text(option.label) },
                                )
                            }
                        }
                    }
                }
                item { MoneyField(balance, { balance = it }, "Saldo awal (Rp)") }
                item {
                    OutlinedTextField(
                        value = interest,
                        onValueChange = { value ->
                            interest = value.filter { it.isDigit() || it == '.' || it == ',' }.take(6)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Bunga per tahun (%)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                    )
                }
                item { MoneyField(minimum, { minimum = it }, "Pembayaran minimum/bulan (Rp)") }
                item {
                    OutlinedTextField(
                        value = dueDay,
                        onValueChange = { dueDay = it.filter(Char::isDigit).take(2) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Tanggal jatuh tempo (1–31)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                    )
                }
                item {
                    OutlinedButton(onClick = { pickStartDate = true }, modifier = Modifier.fillMaxWidth()) {
                        Text("Mulai: ${formatDate(startDate)}")
                    }
                }
                item {
                    OutlinedButton(onClick = { pickTargetDate = true }, modifier = Modifier.fillMaxWidth()) {
                        Text(targetDate?.let { "Target lunas: ${formatDate(it)}" } ?: "Tambahkan target pelunasan")
                    }
                    targetDate?.let {
                        TextButton(onClick = { targetDate = null }) { Text("Hapus target tanggal") }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                enabled = valid,
                onClick = {
                    onSave(
                        DebtDraft(
                            name = name.trim(),
                            creditor = creditor.trim(),
                            type = type,
                            startingBalance = requireNotNull(parsedBalance),
                            annualInterestRate = requireNotNull(parsedInterest),
                            minimumPayment = requireNotNull(parsedMinimum),
                            dueDay = requireNotNull(parsedDueDay),
                            startDate = startDate,
                            targetPayoffDate = targetDate,
                        ),
                    )
                },
            ) { Text("Simpan") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } },
    )

    if (pickStartDate) {
        val picker = rememberDatePickerState(initialSelectedDateMillis = startDate)
        DatePickerDialog(
            onDismissRequest = { pickStartDate = false },
            confirmButton = {
                TextButton(onClick = {
                    startDate = picker.selectedDateMillis ?: startDate
                    pickStartDate = false
                }) { Text("Pilih") }
            },
            dismissButton = { TextButton(onClick = { pickStartDate = false }) { Text("Batal") } },
        ) { DatePicker(state = picker) }
    }

    if (pickTargetDate) {
        val picker = rememberDatePickerState(initialSelectedDateMillis = targetDate ?: startDate)
        DatePickerDialog(
            onDismissRequest = { pickTargetDate = false },
            confirmButton = {
                TextButton(onClick = {
                    targetDate = picker.selectedDateMillis
                    pickTargetDate = false
                }) { Text("Pilih") }
            },
            dismissButton = { TextButton(onClick = { pickTargetDate = false }) { Text("Batal") } },
        ) { DatePicker(state = picker) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PaymentEditorDialog(
    debt: Debt,
    onDismiss: () -> Unit,
    onSave: (DebtPaymentDraft) -> Unit,
) {
    var amount by rememberSaveable(debt.id) { mutableStateOf("") }
    var paidAt by rememberSaveable(debt.id) { mutableStateOf(System.currentTimeMillis()) }
    var note by rememberSaveable(debt.id) { mutableStateOf("") }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    val parsedAmount = amount.toLongOrNull()
    val valid = parsedAmount != null && parsedAmount > 0L && parsedAmount <= debt.currentBalance

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Catat pembayaran ${debt.name}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Sisa utang ${rupiah(debt.currentBalance)}")
                MoneyField(amount, { amount = it }, "Nominal pembayaran (Rp)")
                OutlinedButton(onClick = { showDatePicker = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("Tanggal: ${formatDate(paidAt)}")
                }
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it.take(120) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Catatan opsional") },
                )
            }
        },
        confirmButton = {
            Button(
                enabled = valid,
                onClick = {
                    onSave(
                        DebtPaymentDraft(
                            debtId = debt.id,
                            amount = requireNotNull(parsedAmount),
                            paidAt = paidAt,
                            note = note.trim(),
                        ),
                    )
                },
            ) { Text("Simpan") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } },
    )

    if (showDatePicker) {
        val picker = rememberDatePickerState(initialSelectedDateMillis = paidAt)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    paidAt = picker.selectedDateMillis ?: paidAt
                    showDatePicker = false
                }) { Text("Pilih") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Batal") } },
        ) { DatePicker(state = picker) }
    }
}

@Composable
private fun MoneyField(value: String, onValueChange: (String) -> Unit, label: String) {
    OutlinedTextField(
        value = value,
        onValueChange = { onValueChange(it.filter(Char::isDigit).take(15)) },
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
    )
}
