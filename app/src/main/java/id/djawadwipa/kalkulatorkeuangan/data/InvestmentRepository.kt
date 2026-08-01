package id.djawadwipa.kalkulatorkeuangan.data

import androidx.room.withTransaction
import id.djawadwipa.kalkulatorkeuangan.data.local.FinanceDatabase
import id.djawadwipa.kalkulatorkeuangan.data.local.InvestmentAssetEntity
import id.djawadwipa.kalkulatorkeuangan.data.local.InvestmentAssetRecord
import id.djawadwipa.kalkulatorkeuangan.data.local.InvestmentDao
import id.djawadwipa.kalkulatorkeuangan.data.local.InvestmentTransactionEntity
import id.djawadwipa.kalkulatorkeuangan.data.local.InvestmentTransactionRecord
import id.djawadwipa.kalkulatorkeuangan.domain.InvestmentCalculator
import id.djawadwipa.kalkulatorkeuangan.model.InvestmentAsset
import id.djawadwipa.kalkulatorkeuangan.model.InvestmentAssetDraft
import id.djawadwipa.kalkulatorkeuangan.model.InvestmentTransaction
import id.djawadwipa.kalkulatorkeuangan.model.InvestmentTransactionDraft
import id.djawadwipa.kalkulatorkeuangan.model.InvestmentTransactionType
import id.djawadwipa.kalkulatorkeuangan.model.InvestmentType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.math.roundToLong

class InvestmentRepository(
    private val database: FinanceDatabase,
    private val dao: InvestmentDao = database.investmentDao(),
) {
    val assets: Flow<List<InvestmentAsset>> = dao.observeAssets().map { records ->
        InvestmentCalculator.enrichAllocation(records.map(InvestmentAssetRecord::toModel))
    }

    val transactions: Flow<List<InvestmentTransaction>> = dao.observeTransactions().map { records ->
        records.map(InvestmentTransactionRecord::toModel)
    }

    suspend fun saveAsset(id: Long?, draft: InvestmentAssetDraft) {
        validateAsset(draft)
        val now = System.currentTimeMillis()
        if (id == null) {
            dao.insertAsset(
                InvestmentAssetEntity(
                    name = draft.name.trim(),
                    symbol = draft.symbol.trim().uppercase(),
                    provider = draft.provider.trim(),
                    type = draft.type.name,
                    currentPrice = draft.currentPrice,
                    targetAllocationPercent = draft.targetAllocationPercent,
                    createdAt = now,
                    updatedAt = now,
                ),
            )
        } else {
            require(id > 0L) { "ID aset tidak valid" }
            val existing = requireNotNull(dao.findAssetById(id)) { "Aset investasi tidak ditemukan" }
            dao.updateAsset(
                existing.copy(
                    name = draft.name.trim(),
                    symbol = draft.symbol.trim().uppercase(),
                    provider = draft.provider.trim(),
                    type = draft.type.name,
                    currentPrice = draft.currentPrice,
                    targetAllocationPercent = draft.targetAllocationPercent,
                    updatedAt = now,
                ),
            )
        }
    }

    suspend fun updateMarketPrice(assetId: Long, currentPrice: Long) {
        require(assetId > 0L) { "ID aset tidak valid" }
        require(currentPrice >= 0L) { "Harga pasar tidak boleh negatif" }
        val asset = requireNotNull(dao.findAssetById(assetId)) { "Aset investasi tidak ditemukan" }
        dao.updateAsset(asset.copy(currentPrice = currentPrice, updatedAt = System.currentTimeMillis()))
    }

    suspend fun deleteAsset(id: Long) {
        require(id > 0L) { "ID aset tidak valid" }
        dao.deleteAssetById(id)
    }

    suspend fun addTransaction(draft: InvestmentTransactionDraft) {
        validateTransaction(draft)
        database.withTransaction {
            val asset = requireNotNull(dao.findAssetById(draft.assetId)) { "Aset investasi tidak ditemukan" }
            val update = InvestmentCalculator.positionAfter(
                currentUnits = asset.units,
                currentAverageCost = asset.averageCost,
                type = draft.type,
                units = draft.units,
                unitPrice = draft.unitPrice,
            )
            val computedAmount = when (draft.type) {
                InvestmentTransactionType.BUY,
                InvestmentTransactionType.SELL,
                -> (draft.units * draft.unitPrice.toDouble()).roundToLong()

                InvestmentTransactionType.DIVIDEND,
                InvestmentTransactionType.FEE,
                -> draft.amount
            }
            dao.insertTransaction(
                InvestmentTransactionEntity(
                    assetId = draft.assetId,
                    type = draft.type.name,
                    units = if (draft.type == InvestmentTransactionType.BUY || draft.type == InvestmentTransactionType.SELL) draft.units else 0.0,
                    unitPrice = if (draft.type == InvestmentTransactionType.BUY || draft.type == InvestmentTransactionType.SELL) draft.unitPrice else 0L,
                    amount = computedAmount,
                    fee = draft.fee,
                    transactedAt = draft.transactedAt,
                    note = draft.note.trim(),
                ),
            )
            val latestMarketPrice = if (
                (draft.type == InvestmentTransactionType.BUY || draft.type == InvestmentTransactionType.SELL) &&
                draft.unitPrice > 0L
            ) {
                draft.unitPrice
            } else {
                asset.currentPrice
            }
            dao.updateAsset(
                asset.copy(
                    units = update.units,
                    averageCost = update.averageCost,
                    currentPrice = latestMarketPrice,
                    updatedAt = System.currentTimeMillis(),
                ),
            )
        }
    }

    suspend fun deleteTransaction(id: Long) {
        require(id > 0L) { "ID transaksi investasi tidak valid" }
        database.withTransaction {
            val transaction = requireNotNull(dao.findTransactionById(id)) { "Transaksi investasi tidak ditemukan" }
            val asset = requireNotNull(dao.findAssetById(transaction.assetId)) { "Aset investasi tidak ditemukan" }
            dao.deleteTransactionById(id)

            var units = 0.0
            var averageCost = 0L
            dao.getTransactionsForAsset(asset.id).forEach { item ->
                val update = InvestmentCalculator.positionAfter(
                    currentUnits = units,
                    currentAverageCost = averageCost,
                    type = InvestmentTransactionType.valueOf(item.type),
                    units = item.units,
                    unitPrice = item.unitPrice,
                )
                units = update.units
                averageCost = update.averageCost
            }
            dao.updateAsset(
                asset.copy(
                    units = units,
                    averageCost = averageCost,
                    updatedAt = System.currentTimeMillis(),
                ),
            )
        }
    }

    private fun validateAsset(draft: InvestmentAssetDraft) {
        require(draft.name.isNotBlank()) { "Nama aset wajib diisi" }
        require(draft.name.length <= 60) { "Nama aset maksimal 60 karakter" }
        require(draft.symbol.length <= 16) { "Kode aset maksimal 16 karakter" }
        require(draft.provider.length <= 60) { "Nama platform maksimal 60 karakter" }
        require(draft.currentPrice >= 0L) { "Harga pasar tidak boleh negatif" }
        require(draft.targetAllocationPercent.isFinite() && draft.targetAllocationPercent in 0.0..100.0) {
            "Target alokasi harus antara 0 dan 100 persen"
        }
    }

    private fun validateTransaction(draft: InvestmentTransactionDraft) {
        require(draft.assetId > 0L) { "Aset investasi belum dipilih" }
        require(draft.transactedAt > 0L) { "Tanggal transaksi tidak valid" }
        require(draft.note.length <= 120) { "Catatan maksimal 120 karakter" }
        require(draft.fee >= 0L) { "Biaya tidak boleh negatif" }
        when (draft.type) {
            InvestmentTransactionType.BUY,
            InvestmentTransactionType.SELL,
            -> {
                require(draft.units.isFinite() && draft.units > 0.0) { "Jumlah unit harus lebih dari nol" }
                require(draft.unitPrice > 0L) { "Harga per unit harus lebih dari nol" }
            }

            InvestmentTransactionType.DIVIDEND,
            InvestmentTransactionType.FEE,
            -> require(draft.amount > 0L) { "Nominal harus lebih dari nol" }
        }
    }
}

private fun InvestmentAssetRecord.toModel(): InvestmentAsset {
    val costBasis = (units * averageCost.toDouble()).roundToLong().coerceAtLeast(0L)
    val marketValue = (units * currentPrice.toDouble()).roundToLong().coerceAtLeast(0L)
    val gainLoss = marketValue - costBasis
    return InvestmentAsset(
        id = id,
        name = name,
        symbol = symbol,
        provider = provider,
        type = InvestmentType.valueOf(type),
        units = units,
        averageCost = averageCost,
        currentPrice = currentPrice,
        targetAllocationPercent = targetAllocationPercent,
        costBasis = costBasis,
        marketValue = marketValue,
        gainLoss = gainLoss,
        returnPercent = if (costBasis > 0L) gainLoss.toDouble() / costBasis.toDouble() * 100.0 else 0.0,
        allocationPercent = 0.0,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
}

private fun InvestmentTransactionRecord.toModel(): InvestmentTransaction = InvestmentTransaction(
    id = id,
    assetId = assetId,
    assetName = assetName,
    type = InvestmentTransactionType.valueOf(type),
    units = units,
    unitPrice = unitPrice,
    amount = amount,
    fee = fee,
    transactedAt = transactedAt,
    note = note,
)
