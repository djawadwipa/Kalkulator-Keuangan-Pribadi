package id.djawadwipa.kalkulatorkeuangan.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import id.djawadwipa.kalkulatorkeuangan.model.SavingsContribution
import id.djawadwipa.kalkulatorkeuangan.model.SavingsContributionDraft
import id.djawadwipa.kalkulatorkeuangan.model.SavingsGoal
import id.djawadwipa.kalkulatorkeuangan.model.SavingsGoalDraft
import id.djawadwipa.kalkulatorkeuangan.model.SavingsGoalStatus
import id.djawadwipa.kalkulatorkeuangan.model.SavingsGoalType
import id.djawadwipa.kalkulatorkeuangan.ui.theme.Emerald
import id.djawadwipa.kalkulatorkeuangan.ui.theme.Navy

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SavingsPlannerSection(
    totalSavings: Long,
    emergencyFundMonths: Double,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val factory = remember(context) { SavingsViewModel.Factory(context) }
    val viewModel: SavingsViewModel = viewModel(factory = factory)
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var editingGoal by remember { mutableStateOf<SavingsGoal?>(null) }
    var showGoalEditor by rememberSaveable { mutableStateOf(false) }
    var contributionGoal by remember { mutableStateOf<SavingsGoal?>(null) }
    var deletingGoal by remember { mutableStateOf<SavingsGoal?>(null) }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = Navy),
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                Text("Total dana terkumpul", color = Emerald, fontWeight = FontWeight.SemiBold)
                Text(
                    rupiah(totalSavings),
                    color = androidx.compose.ui.graphics.Color.White,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    "Cakupan dana darurat ${decimal(emergencyFundMonths)} bulan pengeluaran saat ini",
                    color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.78f),
                )
            }
        }

        state.message?.let { message ->
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

        Button(
            onClick = {
                editingGoal = null
                showGoalEditor = true
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Tambah target tabungan")
        }

        if (state.goals.isEmpty()) {
            EmptyState("Belum ada target. Buat dana darurat, tabungan, atau target keuangan pertama Anda.")
        } else {
            state.goals.forEach { goal ->
                SavingsGoalCard(
                    goal = goal,
                    onContribution = { contributionGoal = goal },
                    onEdit = {
                        editingGoal = goal
                        showGoalEditor = true
                    },
                    onDelete = { deletingGoal = goal },
                )
            }
        }

        SectionTitle("Riwayat setoran terbaru")
        if (state.contributions.isEmpty()) {
            EmptyState("Belum ada setoran yang dicatat.")
        } else {
            state.contributions.take(5).forEach { contribution ->
                ContributionCard(
                    contribution = contribution,
                    onDelete = { viewModel.deleteContribution(contribution.id) },
                )
            }
        }
    }

    if (showGoalEditor) {
        SavingsGoalDialog(
            goal = editingGoal,
            onDismiss = { showGoalEditor = false },
            onSave = { draft ->
                viewModel.saveGoal(editingGoal?.id, draft)
                showGoalEditor = false
            },
        )
    }

    contributionGoal?.let { goal ->
        ContributionDialog(
            goal = goal,
            onDismiss = { contributionGoal = null },
            onSave = { draft ->
                viewModel.addContribution(draft)
                contributionGoal = null
            },
        )
    }

    deletingGoal?.let { goal ->
        AlertDialog(
            onDismissRequest = { deletingGoal = null },
            title = { Text("Hapus target?") },
            text = { Text("Target ${goal.name} dan seluruh riwayat setorannya akan dihapus permanen.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteGoal(goal.id)
                        deletingGoal = null
                    },
                ) { Text("Hapus") }
            },
            dismissButton = {
                TextButton(onClick = { deletingGoal = null }) { Text("Batal") }
            },
        )
    }
}

@Composable
private fun SavingsGoalCard(
    goal: SavingsGoal,
    onContribution: () -> Unit,
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
                    Text(goal.name, fontWeight = FontWeight.Bold)
                    Text(
                        "${goal.type.label} • ${goal.status.label}",
                        color = when (goal.status) {
                            SavingsGoalStatus.COMPLETED -> Emerald
                            SavingsGoalStatus.OVERDUE -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
                Text(percent(goal.progressPercent), fontWeight = FontWeight.Bold)
            }
            LinearProgressIndicator(
                progress = { (goal.progressPercent / 100.0).coerceIn(0.0, 1.0).toFloat() },
                modifier = Modifier.fillMaxWidth(),
            )
            Text("${rupiah(goal.currentAmount)} dari ${rupiah(goal.targetAmount)}")
            Text(
                if (goal.remainingAmount > 0L) "Sisa ${rupiah(goal.remainingAmount)}" else "Target telah tercapai",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (goal.monthlyContributionTarget > 0L) {
                Text("Rencana setoran ${rupiah(goal.monthlyContributionTarget)} per bulan")
            }
            goal.targetDate?.let { Text("Tenggat ${formatDate(it)}") }
            goal.estimatedCompletionDate?.let {
                Text(
                    "Estimasi tercapai ${formatDate(it)}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Button(onClick = onContribution) { Text("Setor") }
                TextButton(onClick = onEdit) { Text("Edit") }
                TextButton(onClick = onDelete) { Text("Hapus") }
            }
        }
    }
}

@Composable
private fun ContributionCard(
    contribution: SavingsContribution,
    onDelete: () -> Unit,
) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(Modifier.weight(1f)) {
                Text(contribution.goalName, fontWeight = FontWeight.SemiBold)
                Text(formatDate(contribution.contributedAt), style = MaterialTheme.typography.labelMedium)
                if (contribution.note.isNotBlank()) {
                    Text(contribution.note, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Column {
                Text(rupiah(contribution.amount), color = Emerald, fontWeight = FontWeight.Bold)
                TextButton(onClick = onDelete) { Text("Hapus") }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SavingsGoalDialog(
    goal: SavingsGoal?,
    onDismiss: () -> Unit,
    onSave: (SavingsGoalDraft) -> Unit,
) {
    var name by rememberSaveable(goal?.id) { mutableStateOf(goal?.name.orEmpty()) }
    var type by rememberSaveable(goal?.id) { mutableStateOf(goal?.type ?: SavingsGoalType.SAVINGS) }
    var targetAmount by rememberSaveable(goal?.id) {
        mutableStateOf(goal?.targetAmount?.toString().orEmpty())
    }
    var monthlyTarget by rememberSaveable(goal?.id) {
        mutableStateOf(goal?.monthlyContributionTarget?.takeIf { it > 0L }?.toString().orEmpty())
    }
    var targetDate by rememberSaveable(goal?.id) { mutableStateOf(goal?.targetDate) }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    val parsedTarget = targetAmount.toLongOrNull()
    val parsedMonthly = monthlyTarget.toLongOrNull() ?: 0L
    val valid = name.isNotBlank() && parsedTarget != null && parsedTarget > 0L && parsedMonthly >= 0L

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (goal == null) "Tambah target" else "Edit target") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(60) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Nama target") },
                    singleLine = true,
                )
                SavingsGoalType.entries.forEach { option ->
                    FilterChip(
                        selected = type == option,
                        onClick = { type = option },
                        label = { Text(option.label) },
                    )
                }
                OutlinedTextField(
                    value = targetAmount,
                    onValueChange = { targetAmount = it.filter(Char::isDigit).take(15) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Target nominal (Rp)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                )
                OutlinedTextField(
                    value = monthlyTarget,
                    onValueChange = { monthlyTarget = it.filter(Char::isDigit).take(15) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Rencana setoran bulanan (Rp)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                )
                OutlinedButton(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(targetDate?.let { "Tenggat: ${formatDate(it)}" } ?: "Tambahkan tenggat opsional")
                }
                if (targetDate != null) {
                    TextButton(onClick = { targetDate = null }, modifier = Modifier.fillMaxWidth()) {
                        Text("Hapus tenggat")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                enabled = valid,
                onClick = {
                    onSave(
                        SavingsGoalDraft(
                            name = name.trim(),
                            type = type,
                            targetAmount = requireNotNull(parsedTarget),
                            targetDate = targetDate,
                            monthlyContributionTarget = parsedMonthly,
                        ),
                    )
                },
            ) { Text("Simpan") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } },
    )

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = targetDate ?: System.currentTimeMillis(),
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        targetDate = pickerState.selectedDateMillis
                        showDatePicker = false
                    },
                ) { Text("Pilih") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Batal") }
            },
        ) {
            DatePicker(state = pickerState)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ContributionDialog(
    goal: SavingsGoal,
    onDismiss: () -> Unit,
    onSave: (SavingsContributionDraft) -> Unit,
) {
    var amount by rememberSaveable(goal.id) { mutableStateOf("") }
    var note by rememberSaveable(goal.id) { mutableStateOf("") }
    var date by rememberSaveable(goal.id) { mutableStateOf(System.currentTimeMillis()) }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    val parsedAmount = amount.toLongOrNull()
    val valid = parsedAmount != null && parsedAmount > 0L

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Setoran untuk ${goal.name}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it.filter(Char::isDigit).take(15) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Nominal setoran (Rp)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                )
                OutlinedButton(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Tanggal: ${formatDate(date)}") }
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
                        SavingsContributionDraft(
                            goalId = goal.id,
                            amount = requireNotNull(parsedAmount),
                            contributedAt = date,
                            note = note.trim(),
                        ),
                    )
                },
            ) { Text("Simpan") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } },
    )

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = date)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        date = pickerState.selectedDateMillis ?: date
                        showDatePicker = false
                    },
                ) { Text("Pilih") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Batal") }
            },
        ) {
            DatePicker(state = pickerState)
        }
    }
}

private fun decimal(value: Double): String = String.format(java.util.Locale("id", "ID"), "%.1f", value)
