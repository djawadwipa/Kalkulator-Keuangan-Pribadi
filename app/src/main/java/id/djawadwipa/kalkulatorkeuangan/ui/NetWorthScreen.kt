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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import id.djawadwipa.kalkulatorkeuangan.model.BalanceSheetSide
import id.djawadwipa.kalkulatorkeuangan.model.NetWorthItem
import id.djawadwipa.kalkulatorkeuangan.model.NetWorthItemDraft
import id.djawadwipa.kalkulatorkeuangan.model.NetWorthItemType
import id.djawadwipa.kalkulatorkeuangan.model.NetWorthOverview
import id.djawadwipa.kalkulatorkeuangan.model.NetWorthSnapshot
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

@Composable
internal fun NetWorthScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val viewModel: NetWorthViewModel = viewModel(factory = NetWorthViewModel.Factory(context))
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showEditor by rememberSaveable { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<NetWorthItem?>(null) }
    var deletingItem by remember { mutableStateOf<NetWorthItem?>(null) }
    var deletingSnapshot by remember { mutableStateOf<NetWorthSnapshot?>(null) }

    LaunchedEffect(state.message) {
        state.message?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingItem = null
                    showEditor = true
                },
            ) {
                Text("+")
            }
        },
    ) { padding ->
        val assets = state.items.filter { it.type.side == BalanceSheetSide.ASSET }
        val liabilities = state.items.filter { it.type.side == BalanceSheetSide.LIABILITY }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text(
                    "Aset, Liabilitas, dan Net Worth",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    "Tabungan, investasi, dan sisa utang diambil otomatis. Tambahkan hanya nilai yang belum tercatat di modul lain.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            item { NetWorthOverviewCard(state.overview) }
            item { NetWorthCompositionCard(state.overview) }
            item {
                Button(
                    onClick = viewModel::saveSnapshot,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Simpan snapshot Net Worth bulan ini")
                }
            }
            item {
                SectionTitle("Aset manual")
                Text("Properti, kendaraan, piutang, usaha, dan aset lain yang belum tercatat sebagai tabungan atau investasi.")
            }
            if (assets.isEmpty()) {
                item { EmptyState("Belum ada aset manual.") }
            } else {
                items(assets, key = { "net-worth-asset-${it.id}" }) { item ->
                    NetWorthItemCard(
                        item = item,
                        onEdit = {
                            editingItem = item
                            showEditor = true
                        },
                        onDelete = { deletingItem = item },
                    )
                }
            }
            item {
                SectionTitle("Liabilitas manual")
                Text("Gunakan bagian ini hanya untuk kewajiban yang belum dicatat pada modul Cicilan dan Utang.")
            }
            if (liabilities.isEmpty()) {
                item { EmptyState("Belum ada liabilitas manual.") }
            } else {
                items(liabilities, key = { "net-worth-liability-${it.id}" }) { item ->
                    NetWorthItemCard(
                        item = item,
                        onEdit = {
                            editingItem = item
                            showEditor = true
                        },
                        onDelete = { deletingItem = item },
                    )
                }
            }
            item { SectionTitle("Riwayat snapshot") }
            if (state.snapshots.isEmpty()) {
                item { EmptyState("Belum ada snapshot Net Worth bulanan.") }
            } else {
                itemsIndexed(
                    items = state.snapshots.take(24),
                    key = { _, snapshot -> "net-worth-snapshot-${snapshot.id}" },
                ) { index, snapshot ->
                    val previous = state.snapshots.getOrNull(index + 1)
                    NetWorthSnapshotCard(
                        snapshot = snapshot,
                        change = previous?.let { snapshot.netWorth - it.netWorth },
                        onDelete = { deletingSnapshot = snapshot },
                    )
                }
            }
        }
    }

    if (showEditor) {
        NetWorthItemDialog(
            item = editingItem,
            onDismiss = { showEditor = false },
            onSave = { draft ->
                viewModel.saveItem(editingItem?.id, draft)
                showEditor = false
            },
        )
    }
    deletingItem?.let { item ->
        AlertDialog(
            onDismissRequest = { deletingItem = null },
            title = { Text("Hapus item neraca?") },
            text = { Text("${item.name} senilai ${rupiah(item.value)} akan dihapus permanen.") },
            confirmButton = {
                Button(onClick = {
                    viewModel.deleteItem(item.id)
                    deletingItem = null
                }) { Text("Hapus") }
            },
            dismissButton = { TextButton(onClick = { deletingItem = null }) { Text("Batal") } },
        )
    }
    deletingSnapshot?.let { snapshot ->
        AlertDialog(
            onDismissRequest = { deletingSnapshot = null },
            title = { Text("Hapus snapshot?") },
            text = { Text("Snapshot ${netWorthMonth(snapshot.monthStart)} akan dihapus.") },
            confirmButton = {
                Button(onClick = {
                    viewModel.deleteSnapshot(snapshot.id)
                    deletingSnapshot = null
                }) { Text("Hapus") }
            },
            dismissButton = { TextButton(onClick = { deletingSnapshot = null }) { Text("Batal") } },
        )
    }
}

@Composable
private fun NetWorthOverviewCard(overview: NetWorthOverview) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("Kekayaan bersih", fontWeight = FontWeight.Bold)
            Text(
                signedNetWorth(overview.netWorth),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = if (overview.netWorth >= 0L) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text("Total aset", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(rupiah(overview.totalAssets), fontWeight = FontWeight.SemiBold)
                }
                Column {
                    Text("Total liabilitas", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(rupiah(overview.totalLiabilities), fontWeight = FontWeight.SemiBold)
                }
            }
            LinearProgressIndicator(
                progress = {
                    if (overview.totalAssets > 0L) {
                        (1.0 - overview.totalLiabilities.toDouble() / overview.totalAssets.toDouble())
                            .coerceIn(0.0, 1.0)
                            .toFloat()
                    } else {
                        0f
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )
            Text("Rasio liabilitas terhadap aset ${netWorthPercent(overview.debtToAssetPercent)}")
        }
    }
}

@Composable
private fun NetWorthCompositionCard(overview: NetWorthOverview) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text("Komposisi", fontWeight = FontWeight.Bold)
            NetWorthValueRow("Aset manual", overview.manualAssets)
            NetWorthValueRow("Tabungan dan target", overview.savingsValue)
            NetWorthValueRow("Nilai pasar investasi", overview.investmentValue)
            NetWorthValueRow("Liabilitas manual", -overview.manualLiabilities)
            NetWorthValueRow("Sisa cicilan dan utang", -overview.debtValue)
            Text("${overview.assetItems} item aset manual • ${overview.liabilityItems} item liabilitas manual")
        }
    }
}

@Composable
private fun NetWorthValueRow(label: String, value: Long) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label)
        Text(
            signedNetWorth(value),
            fontWeight = FontWeight.SemiBold,
            color = if (value >= 0L) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
        )
    }
}

@Composable
private fun NetWorthItemCard(
    item: NetWorthItem,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Text(item.name, fontWeight = FontWeight.Bold)
            Text(item.type.label, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(rupiah(item.value), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            if (item.note.isNotBlank()) Text(item.note)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onEdit) { Text("Edit") }
                TextButton(onClick = onDelete) { Text("Hapus") }
            }
        }
    }
}

@Composable
private fun NetWorthSnapshotCard(
    snapshot: NetWorthSnapshot,
    change: Long?,
    onDelete: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Text(netWorthMonth(snapshot.monthStart), fontWeight = FontWeight.Bold)
            Text(
                signedNetWorth(snapshot.netWorth),
                style = MaterialTheme.typography.titleLarge,
                color = if (snapshot.netWorth >= 0L) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
            )
            Text("Aset ${rupiah(snapshot.totalAssets)} • liabilitas ${rupiah(snapshot.totalLiabilities)}")
            change?.let {
                Text(
                    "Perubahan dari snapshot sebelumnya ${signedNetWorth(it)}",
                    color = if (it >= 0L) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                )
            }
            TextButton(onClick = onDelete) { Text("Hapus snapshot") }
        }
    }
}

@Composable
private fun NetWorthItemDialog(
    item: NetWorthItem?,
    onDismiss: () -> Unit,
    onSave: (NetWorthItemDraft) -> Unit,
) {
    var name by rememberSaveable(item?.id) { mutableStateOf(item?.name.orEmpty()) }
    var value by rememberSaveable(item?.id) { mutableStateOf(item?.value?.toString().orEmpty()) }
    var note by rememberSaveable(item?.id) { mutableStateOf(item?.note.orEmpty()) }
    var side by remember(item?.id) { mutableStateOf(item?.type?.side ?: BalanceSheetSide.ASSET) }
    var type by remember(item?.id) {
        mutableStateOf(item?.type ?: NetWorthItemType.PROPERTY)
    }
    val parsedValue = value.toLongOrNull()
    val valid = name.isNotBlank() && parsedValue != null && parsedValue > 0L && type.side == side
    val availableTypes = NetWorthItemType.entries.filter { it.side == side }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (item == null) "Tambah item neraca" else "Edit item neraca") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        BalanceSheetSide.entries.forEach { option ->
                            FilterChip(
                                selected = side == option,
                                onClick = {
                                    side = option
                                    type = NetWorthItemType.entries.first { it.side == option }
                                },
                                label = { Text(option.label) },
                            )
                        }
                    }
                }
                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it.take(60) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Nama") },
                        singleLine = true,
                    )
                }
                item {
                    availableTypes.forEach { option ->
                        FilterChip(
                            selected = type == option,
                            onClick = { type = option },
                            label = { Text(option.label) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
                item {
                    OutlinedTextField(
                        value = value,
                        onValueChange = { value = it.filter(Char::isDigit).take(16) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Nilai saat ini (Rp)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                    )
                }
                item {
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it.take(120) },
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
                        NetWorthItemDraft(
                            name = name.trim(),
                            type = type,
                            value = requireNotNull(parsedValue),
                            note = note.trim(),
                        ),
                    )
                },
            ) { Text("Simpan") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } },
    )
}

private fun signedNetWorth(value: Long): String = when {
    value > 0L -> "+${rupiah(value)}"
    value < 0L -> "−${rupiah(abs(value))}"
    else -> rupiah(0L)
}

private fun netWorthPercent(value: Double): String = String.format(Locale("id", "ID"), "%.1f%%", value)

private fun netWorthMonth(timestamp: Long): String = SimpleDateFormat("MMMM yyyy", Locale("id", "ID")).format(Date(timestamp))
