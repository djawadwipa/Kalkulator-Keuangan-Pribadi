package id.djawadwipa.kalkulatorkeuangan.data

import id.djawadwipa.kalkulatorkeuangan.data.local.DebtRecord
import id.djawadwipa.kalkulatorkeuangan.data.local.FinanceDao
import id.djawadwipa.kalkulatorkeuangan.data.local.FinanceDatabase
import id.djawadwipa.kalkulatorkeuangan.data.local.InvestmentAssetRecord
import id.djawadwipa.kalkulatorkeuangan.data.local.InvestmentDao
import id.djawadwipa.kalkulatorkeuangan.data.local.NetWorthDao
import id.djawadwipa.kalkulatorkeuangan.data.local.NetWorthItemEntity
import id.djawadwipa.kalkulatorkeuangan.data.local.NetWorthSnapshotEntity
import id.djawadwipa.kalkulatorkeuangan.data.local.SavingsGoalRecord
import id.djawadwipa.kalkulatorkeuangan.domain.NetWorthCalculator
import id.djawadwipa.kalkulatorkeuangan.model.BalanceSheetSide
import id.djawadwipa.kalkulatorkeuangan.model.NetWorthItem
import id.djawadwipa.kalkulatorkeuangan.model.NetWorthItemDraft
import id.djawadwipa.kalkulatorkeuangan.model.NetWorthItemType
import id.djawadwipa.kalkulatorkeuangan.model.NetWorthOverview
import id.djawadwipa.kalkulatorkeuangan.model.NetWorthSnapshot
import java.util.Calendar
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlin.math.roundToLong

class NetWorthRepository(
    database: FinanceDatabase,
    private val netWorthDao: NetWorthDao = database.netWorthDao(),
    financeDao: FinanceDao = database.financeDao(),
    investmentDao: InvestmentDao = database.investmentDao(),
) {
    val items: Flow<List<NetWorthItem>> = netWorthDao.observeItems().map { entities ->
        entities.map(NetWorthItemEntity::toModel)
    }

    val snapshots: Flow<List<NetWorthSnapshot>> = netWorthDao.observeSnapshots().map { entities ->
        entities.map(NetWorthSnapshotEntity::toModel)
    }

    val overview: Flow<NetWorthOverview> = combine(
        items,
        financeDao.observeSavingsGoals(),
        financeDao.observeDebts(),
        investmentDao.observeAssets(),
    ) { manualItems, savingsGoals, debts, investments ->
        NetWorthCalculator.calculate(
            items = manualItems,
            savingsValue = savingsGoals.totalSavingsValue(),
            investmentValue = investments.totalMarketValue(),
            debtValue = debts.totalDebtValue(),
        )
    }

    suspend fun saveItem(id: Long?, draft: NetWorthItemDraft) {
        validateDraft(draft)
        val now = System.currentTimeMillis()
        if (id == null) {
            netWorthDao.insertItem(
                NetWorthItemEntity(
                    name = draft.name.trim(),
                    side = draft.type.side.name,
                    type = draft.type.name,
                    value = draft.value,
                    note = draft.note.trim(),
                    updatedAt = now,
                ),
            )
        } else {
            require(id > 0L) { "ID item neraca tidak valid" }
            val existing = requireNotNull(netWorthDao.findItemById(id)) { "Item aset atau liabilitas tidak ditemukan" }
            netWorthDao.updateItem(
                existing.copy(
                    name = draft.name.trim(),
                    side = draft.type.side.name,
                    type = draft.type.name,
                    value = draft.value,
                    note = draft.note.trim(),
                    updatedAt = now,
                ),
            )
        }
    }

    suspend fun deleteItem(id: Long) {
        require(id > 0L) { "ID item neraca tidak valid" }
        netWorthDao.deleteItemById(id)
    }

    suspend fun saveCurrentMonthSnapshot(overview: NetWorthOverview) {
        netWorthDao.saveSnapshot(
            NetWorthSnapshotEntity(
                monthStart = currentMonthStart(),
                manualAssets = overview.manualAssets,
                savingsValue = overview.savingsValue,
                investmentValue = overview.investmentValue,
                manualLiabilities = overview.manualLiabilities,
                debtValue = overview.debtValue,
                totalAssets = overview.totalAssets,
                totalLiabilities = overview.totalLiabilities,
                netWorth = overview.netWorth,
                generatedAt = System.currentTimeMillis(),
            ),
        )
    }

    suspend fun deleteSnapshot(id: Long) {
        require(id > 0L) { "ID snapshot tidak valid" }
        netWorthDao.deleteSnapshotById(id)
    }

    private fun validateDraft(draft: NetWorthItemDraft) {
        require(draft.name.isNotBlank()) { "Nama aset atau liabilitas wajib diisi" }
        require(draft.name.length <= 60) { "Nama maksimal 60 karakter" }
        require(draft.value > 0L) { "Nilai harus lebih dari nol" }
        require(draft.note.length <= 120) { "Catatan maksimal 120 karakter" }
        require(draft.type.side == BalanceSheetSide.ASSET || draft.type.side == BalanceSheetSide.LIABILITY)
    }

    private fun currentMonthStart(): Long = Calendar.getInstance().apply {
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}

private fun List<SavingsGoalRecord>.totalSavingsValue(): Long = sumOf { it.currentAmount.coerceAtLeast(0L) }

private fun List<DebtRecord>.totalDebtValue(): Long = sumOf { it.currentBalance.coerceAtLeast(0L) }

private fun List<InvestmentAssetRecord>.totalMarketValue(): Long = sumOf { record ->
    (record.units.coerceAtLeast(0.0) * record.currentPrice.coerceAtLeast(0L).toDouble()).roundToLong()
}

private fun NetWorthItemEntity.toModel(): NetWorthItem = NetWorthItem(
    id = id,
    name = name,
    type = NetWorthItemType.valueOf(type),
    value = value,
    note = note,
    updatedAt = updatedAt,
)

private fun NetWorthSnapshotEntity.toModel(): NetWorthSnapshot = NetWorthSnapshot(
    id = id,
    monthStart = monthStart,
    manualAssets = manualAssets,
    savingsValue = savingsValue,
    investmentValue = investmentValue,
    manualLiabilities = manualLiabilities,
    debtValue = debtValue,
    totalAssets = totalAssets,
    totalLiabilities = totalLiabilities,
    netWorth = netWorth,
    generatedAt = generatedAt,
)
