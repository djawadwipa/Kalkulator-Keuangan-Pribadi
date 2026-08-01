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
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
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
import id.djawadwipa.kalkulatorkeuangan.model.InvestmentAsset
import id.djawadwipa.kalkulatorkeuangan.model.InvestmentAssetDraft
import id.djawadwipa.kalkulatorkeuangan.model.InvestmentTransaction
import id.djawadwipa.kalkulatorkeuangan.model.InvestmentTransactionDraft
import id.djawadwipa.kalkulatorkeuangan.model.InvestmentTransactionType
import id.djawadwipa.kalkulatorkeuangan.model.InvestmentType
import java.text.NumberFormat
import java.util.Locale

@Composable
internal fun InvestmentScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val viewModel: InvestmentViewModel = viewModel(factory = InvestmentViewModel.Factory(context))
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var editingAsset by remember { mutableStateOf<InvestmentAsset?>(null) }
    var showAssetEditor by rememberSaveable { mutableStateOf(false) }
    var transactionAsset by remember { mutableStateOf<InvestmentAsset?>(null) }
    var priceAsset by remember { mutableStateOf<InvestmentAsset?>(null) }
    var deletingAsset by remember { mutableStateOf<InvestmentAsset?>(null) }
    var deletingTransaction by remember { mutableStateOf<InvestmentTransaction?>(null) }

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
                    editingAsset = null
                    showAssetEditor = true
                },
            ) {
                Text("+")
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text(
                    "Investasi dan Portofolio",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    "Catat posisi dan harga secara manual. Aplikasi tidak mengambil data pasar dari internet.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            item {
                PortfolioOverviewCard(state)
            }
            item {
                SectionTitle("Aset investasi")
                OutlinedButton(
                    onClick = {
                        editingAsset = null
                        showAssetEditor = true
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Tambah aset investasi")
                }
            }
            if (state.assets.isEmpty()) {
                item { EmptyState("Belum ada aset investasi. Tambahkan aset lalu catat pembelian atau setoran.") }
            } else {
                items(state.assets, key = { "investment-${it.id}" }) { asset ->
                    InvestmentAssetCard(
                        asset = asset,
                        onTransaction = { transactionAsset = asset },
                        onPrice = { priceAsset = asset },
                        onEdit = {
                            editingAsset = asset
                            showAssetEditor = true
                        },
                        onDelete = { deletingAsset = asset },
                    )
                }
            }
            item {
                InvestmentSimulationSection(
                    state = state,
                    onUpdate = viewModel::updateSimulation,
                )
            }
            item { SectionTitle("Riwayat investasi") }
            if (state.transactions.isEmpty()) {
                item { EmptyState("Belum ada pembelian, penjualan, dividen, atau biaya investasi.") }
            } else {
                items(state.transactions.take(50), key = { "investment-tx-${it.id}" }) { transaction ->
                    InvestmentTransactionCard(
                        transaction = transaction,
                        onDelete = { deletingTransaction = transaction },
                    )
                }
            }
        }
    }

    if (showAssetEditor) {
        InvestmentAssetDialog(
            asset = editingAsset,
            onDismiss = { showAssetEditor = false },
            onSave = { draft ->
                viewModel.saveAsset(editingAsset?.id, draft)
                showAssetEditor = false
            },
        )
    }
    transactionAsset?.let { asset ->
        InvestmentTransactionDialog(
            asset = asset,
            onDismiss = { transactionAsset = null },
            onSave = {
                viewModel.addTransaction(it)
                transactionAsset = null
            },
        )
    }
    priceAsset?.let { asset ->
        MarketPriceDialog(
            asset = asset,
            onDismiss = { priceAsset = null },
            onSave = { price ->
                viewModel.updateMarketPrice(asset.id, price)
                priceAsset = null
            },
        )
    }
    deletingAsset?.let { asset ->
        AlertDialog(
            onDismissRequest = { deletingAsset = null },
            title = { Text("Hapus aset investasi?") },
            text = { Text("${asset.name} dan seluruh riwayat transaksinya akan dihapus permanen.") },
            confirmButton = {
                Button(onClick = {
                    viewModel.deleteAsset(asset.id)
                    deletingAsset = null
                }) { Text("Hapus") }
            },
            dismissButton = { TextButton(onClick = { deletingAsset = null }) { Text("Batal") } },
        )
    }
    deletingTransaction?.let { transaction ->
        AlertDialog(
            onDismissRequest = { deletingTransaction = null },
            title = { Text("Hapus transaksi investasi?") },
            text = { Text("Posisi ${transaction.assetName} akan dihitung ulang dari riwayat yang tersisa.") },
            confirmButton = {
                Button(onClick = {
                    viewModel.deleteTransaction(transaction.id)
                    deletingTransaction = null
                }) { Text("Hapus") }
            },
            dismissButton = { TextButton(onClick = { deletingTransaction = null }) { Text("Batal") } },
        )
    }
}

@Composable
private fun PortfolioOverviewCard(state: InvestmentUiState) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("Ringkasan portofolio", fontWeight = FontWeight.Bold)
            Text(
                rupiah(state.overview.totalMarketValue),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
            Text("Modal tercatat ${rupiah(state.overview.totalCostBasis)}")
            Text(
                "Keuntungan/rugi ${signedRupiah(state.overview.totalGainLoss)} • ${percent(state.overview.returnPercent)}",
                color = if (state.overview.totalGainLoss >= 0L) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.error
                },
                fontWeight = FontWeight.SemiBold,
            )
            Text("${state.overview.assetCount} posisi aktif • hasil ${rupiah(state.overview.totalIncome)} • biaya ${rupiah(state.overview.totalFees)}")
        }
    }
}

@Composable
private fun InvestmentAssetCard(
    asset: InvestmentAsset,
    onTransaction: () -> Unit,
    onPrice: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Text(
                if (asset.symbol.isBlank()) asset.name else "${asset.name} • ${asset.symbol}",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                "${asset.type.label}${asset.provider.takeIf(String::isNotBlank)?.let { " • $it" }.orEmpty()}",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text("${formatUnits(asset.units)} unit • rata-rata ${rupiah(asset.averageCost)} • pasar ${rupiah(asset.currentPrice)}")
            Text("Nilai ${rupiah(asset.marketValue)} • modal ${rupiah(asset.costBasis)}")
            Text(
                "${signedRupiah(asset.gainLoss)} • ${percent(asset.returnPercent)}",
                color = if (asset.gainLoss >= 0L) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.SemiBold,
            )
            LinearProgressIndicator(
                progress = { (asset.allocationPercent / 100.0).coerceIn(0.0, 1.0).toFloat() },
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                "Alokasi aktual ${percent(asset.allocationPercent)} • target ${percent(asset.targetAllocationPercent)}",
                style = MaterialTheme.typography.bodySmall,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                TextButton(onClick = onTransaction) { Text("Transaksi") }
                TextButton(onClick = onPrice) { Text("Harga") }
                TextButton(onClick = onEdit) { Text("Edit") }
                TextButton(onClick = onDelete) { Text("Hapus") }
            }
        }
    }
}

@Composable
private fun InvestmentSimulationSection(
    state: InvestmentUiState,
    onUpdate: (InvestmentSimulationInput) -> Unit,
) {
    var initial by rememberSaveable { mutableStateOf(state.simulation.initialInvestment.toString()) }
    var monthly by rememberSaveable { mutableStateOf(state.simulation.monthlyContribution.toString()) }
    var annualReturn by rememberSaveable { mutableStateOf(state.simulation.annualReturnPercent.toString()) }
    var years by rememberSaveable { mutableStateOf(state.simulation.years.toString()) }
    var inflation by rememberSaveable { mutableStateOf(state.simulation.annualInflationPercent.toString()) }

    val parsedInitial = initial.toLongOrNull()
    val parsedMonthly = monthly.toLongOrNull()
    val parsedReturn = annualReturn.toDoubleOrNull()
    val parsedYears = years.toIntOrNull()
    val parsedInflation = inflation.toDoubleOrNull()
    val valid = parsedInitial != null && parsedInitial >= 0L &&
        parsedMonthly != null && parsedMonthly >= 0L &&
        parsedReturn != null && parsedReturn in -100.0..1000.0 &&
        parsedYears != null && parsedYears in 1..60 &&
        parsedInflation != null && parsedInflation in 0.0..100.0

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionTitle("Simulasi investasi")
        Text("Proyeksi menggunakan compounding bulanan dan bukan jaminan hasil investasi.")
        OutlinedTextField(
            value = initial,
            onValueChange = { initial = it.filter(Char::isDigit).take(16) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Modal awal (Rp)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
        )
        OutlinedTextField(
            value = monthly,
            onValueChange = { monthly = it.filter(Char::isDigit).take(16) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Setoran bulanan (Rp)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
        )
        OutlinedTextField(
            value = annualReturn,
            onValueChange = { annualReturn = numericDecimalInput(it, 8) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Estimasi imbal hasil tahunan (%)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
        )
        OutlinedTextField(
            value = years,
            onValueChange = { years = it.filter(Char::isDigit).take(2) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Durasi (tahun)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
        )
        OutlinedTextField(
            value = inflation,
            onValueChange = { inflation = numericDecimalInput(it, 6) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Estimasi inflasi tahunan (%)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
        )
        Button(
            enabled = valid,
            onClick = {
                onUpdate(
                    InvestmentSimulationInput(
                        initialInvestment = requireNotNull(parsedInitial),
                        monthlyContribution = requireNotNull(parsedMonthly),
                        annualReturnPercent = requireNotNull(parsedReturn),
                        years = requireNotNull(parsedYears),
                        annualInflationPercent = requireNotNull(parsedInflation),
                    ),
                )
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Hitung proyeksi")
        }
        state.projection?.let { projection ->
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text("Hasil proyeksi ${projection.years} tahun", fontWeight = FontWeight.Bold)
                    Text("Nilai masa depan ${rupiah(projection.futureValue)}", style = MaterialTheme.typography.titleLarge)
                    Text("Total kontribusi ${rupiah(projection.totalContributions)}")
                    Text("Estimasi pertumbuhan ${signedRupiah(projection.estimatedGain)}")
                    Text("Nilai setelah inflasi ${rupiah(projection.inflationAdjustedValue)}")
                    projection.points.takeLast(6).forEach { point ->
                        Text("Tahun ${point.year}: ${rupiah(point.projectedValue)}")
                    }
                }
            }
        }
    }
}

@Composable
private fun InvestmentTransactionCard(
    transaction: InvestmentTransaction,
    onDelete: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("${transaction.type.label} • ${transaction.assetName}", fontWeight = FontWeight.Bold)
            val detail = when (transaction.type) {
                InvestmentTransactionType.BUY,
                InvestmentTransactionType.SELL,
                -> "${formatUnits(transaction.units)} unit × ${rupiah(transaction.unitPrice)} = ${rupiah(transaction.amount)}"

                InvestmentTransactionType.DIVIDEND,
                InvestmentTransactionType.FEE,
                -> rupiah(transaction.amount)
            }
            Text(detail)
            if (transaction.fee > 0L) Text("Biaya ${rupiah(transaction.fee)}")
            Text("${formatDate(transaction.transactedAt)}${transaction.note.takeIf(String::isNotBlank)?.let { " • $it" }.orEmpty()}")
            TextButton(onClick = onDelete) { Text("Hapus transaksi") }
        }
    }
}

@Composable
private fun InvestmentAssetDialog(
    asset: InvestmentAsset?,
    onDismiss: () -> Unit,
    onSave: (InvestmentAssetDraft) -> Unit,
) {
    var name by rememberSaveable(asset?.id) { mutableStateOf(asset?.name.orEmpty()) }
    var symbol by rememberSaveable(asset?.id) { mutableStateOf(asset?.symbol.orEmpty()) }
    var provider by rememberSaveable(asset?.id) { mutableStateOf(asset?.provider.orEmpty()) }
    var type by rememberSaveable(asset?.id) { mutableStateOf(asset?.type ?: InvestmentType.MUTUAL_FUND) }
    var price by rememberSaveable(asset?.id) { mutableStateOf(asset?.currentPrice?.toString().orEmpty()) }
    var target by rememberSaveable(asset?.id) { mutableStateOf(asset?.targetAllocationPercent?.toString() ?: "0") }
    val parsedPrice = price.toLongOrNull()
    val parsedTarget = target.toDoubleOrNull()
    val valid = name.isNotBlank() && parsedPrice != null && parsedPrice >= 0L &&
        parsedTarget != null && parsedTarget in 0.0..100.0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (asset == null) "Tambah aset investasi" else "Edit aset investasi") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it.take(60) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Nama aset") },
                        singleLine = true,
                    )
                }
                item {
                    OutlinedTextField(
                        value = symbol,
                        onValueChange = { symbol = it.take(16) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Kode / ticker opsional") },
                        singleLine = true,
                    )
                }
                item {
                    OutlinedTextField(
                        value = provider,
                        onValueChange = { provider = it.take(60) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Platform / penerbit") },
                        singleLine = true,
                    )
                }
                item {
                    InvestmentType.entries.chunked(2).forEach { row ->
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
                item {
                    OutlinedTextField(
                        value = price,
                        onValueChange = { price = it.filter(Char::isDigit).take(16) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Harga pasar per unit (Rp)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                    )
                }
                item {
                    OutlinedTextField(
                        value = target,
                        onValueChange = { target = numericDecimalInput(it, 6) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Target alokasi (%)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                    )
                }
            }
        },
        confirmButton = {
            Button(
                enabled = valid,
                onClick = {
                    onSave(
                        InvestmentAssetDraft(
                            name = name.trim(),
                            symbol = symbol.trim(),
                            provider = provider.trim(),
                            type = type,
                            currentPrice = requireNotNull(parsedPrice),
                            targetAllocationPercent = requireNotNull(parsedTarget),
                        ),
                    )
                },
            ) { Text("Simpan") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InvestmentTransactionDialog(
    asset: InvestmentAsset,
    onDismiss: () -> Unit,
    onSave: (InvestmentTransactionDraft) -> Unit,
) {
    var type by rememberSaveable { mutableStateOf(InvestmentTransactionType.BUY) }
    var units by rememberSaveable { mutableStateOf("") }
    var unitPrice by rememberSaveable { mutableStateOf(asset.currentPrice.takeIf { it > 0L }?.toString().orEmpty()) }
    var amount by rememberSaveable { mutableStateOf("") }
    var fee by rememberSaveable { mutableStateOf("0") }
    var date by rememberSaveable { mutableStateOf(System.currentTimeMillis()) }
    var note by rememberSaveable { mutableStateOf("") }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    val parsedUnits = units.toDoubleOrNull()
    val parsedUnitPrice = unitPrice.toLongOrNull()
    val parsedAmount = amount.toLongOrNull()
    val parsedFee = fee.toLongOrNull()
    val isPositionTransaction = type == InvestmentTransactionType.BUY || type == InvestmentTransactionType.SELL
    val valid = parsedFee != null && parsedFee >= 0L && if (isPositionTransaction) {
        parsedUnits != null && parsedUnits > 0.0 && parsedUnitPrice != null && parsedUnitPrice > 0L &&
            (type != InvestmentTransactionType.SELL || parsedUnits <= asset.units + 0.0000001)
    } else {
        parsedAmount != null && parsedAmount > 0L
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Transaksi ${asset.name}") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                item {
                    InvestmentTransactionType.entries.chunked(2).forEach { row ->
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
                if (isPositionTransaction) {
                    item {
                        OutlinedTextField(
                            value = units,
                            onValueChange = { units = numericDecimalInput(it, 18) },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Jumlah unit") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            supportingText = if (type == InvestmentTransactionType.SELL) {
                                { Text("Tersedia ${formatUnits(asset.units)} unit") }
                            } else null,
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = unitPrice,
                            onValueChange = { unitPrice = it.filter(Char::isDigit).take(16) },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Harga per unit (Rp)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                        )
                    }
                } else {
                    item {
                        OutlinedTextField(
                            value = amount,
                            onValueChange = { amount = it.filter(Char::isDigit).take(16) },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Nominal (Rp)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                        )
                    }
                }
                item {
                    OutlinedTextField(
                        value = fee,
                        onValueChange = { fee = it.filter(Char::isDigit).take(16) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Biaya transaksi (Rp)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                    )
                }
                item {
                    OutlinedButton(onClick = { showDatePicker = true }, modifier = Modifier.fillMaxWidth()) {
                        Text("Tanggal: ${formatDate(date)}")
                    }
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
                        InvestmentTransactionDraft(
                            assetId = asset.id,
                            type = type,
                            units = parsedUnits ?: 0.0,
                            unitPrice = parsedUnitPrice ?: 0L,
                            amount = parsedAmount ?: 0L,
                            fee = requireNotNull(parsedFee),
                            transactedAt = date,
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
                TextButton(onClick = {
                    date = pickerState.selectedDateMillis ?: date
                    showDatePicker = false
                }) { Text("Pilih") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Batal") } },
        ) { DatePicker(state = pickerState) }
    }
}

@Composable
private fun MarketPriceDialog(
    asset: InvestmentAsset,
    onDismiss: () -> Unit,
    onSave: (Long) -> Unit,
) {
    var price by rememberSaveable(asset.id) { mutableStateOf(asset.currentPrice.toString()) }
    val parsed = price.toLongOrNull()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Perbarui harga ${asset.name}") },
        text = {
            OutlinedTextField(
                value = price,
                onValueChange = { price = it.filter(Char::isDigit).take(16) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Harga pasar per unit (Rp)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
            )
        },
        confirmButton = {
            Button(enabled = parsed != null && parsed >= 0L, onClick = { onSave(requireNotNull(parsed)) }) {
                Text("Simpan")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } },
    )
}

private fun signedRupiah(value: Long): String = (if (value >= 0L) "+" else "−") + rupiah(kotlin.math.abs(value))

private fun formatUnits(value: Double): String = NumberFormat.getNumberInstance(Locale("id", "ID")).apply {
    maximumFractionDigits = 6
    minimumFractionDigits = 0
}.format(value)

private fun numericDecimalInput(value: String, maxLength: Int): String {
    val normalized = value.replace(',', '.')
    val filtered = normalized.filter { it.isDigit() || it == '.' || it == '-' }
    val sign = if (filtered.startsWith('-')) "-" else ""
    val unsigned = filtered.removePrefix("-").replace("-", "")
    val parts = unsigned.split('.', limit = 2)
    return (sign + parts.firstOrNull().orEmpty() + if (unsigned.contains('.')) "." + parts.getOrElse(1) { "" } else "")
        .take(maxLength)
}
