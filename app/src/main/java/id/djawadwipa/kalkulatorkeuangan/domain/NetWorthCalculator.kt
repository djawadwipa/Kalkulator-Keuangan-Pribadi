package id.djawadwipa.kalkulatorkeuangan.domain

import id.djawadwipa.kalkulatorkeuangan.model.BalanceSheetSide
import id.djawadwipa.kalkulatorkeuangan.model.NetWorthItem
import id.djawadwipa.kalkulatorkeuangan.model.NetWorthOverview

object NetWorthCalculator {
    fun calculate(
        items: List<NetWorthItem>,
        savingsValue: Long,
        investmentValue: Long,
        debtValue: Long,
    ): NetWorthOverview {
        require(savingsValue >= 0L) { "Nilai tabungan tidak boleh negatif" }
        require(investmentValue >= 0L) { "Nilai investasi tidak boleh negatif" }
        require(debtValue >= 0L) { "Nilai utang tidak boleh negatif" }
        require(items.all { it.value >= 0L }) { "Nilai aset dan liabilitas tidak boleh negatif" }

        val manualAssets = items
            .filter { it.type.side == BalanceSheetSide.ASSET }
            .sumOf { it.value }
        val manualLiabilities = items
            .filter { it.type.side == BalanceSheetSide.LIABILITY }
            .sumOf { it.value }
        val totalAssets = safeAdd(manualAssets, savingsValue, investmentValue)
        val totalLiabilities = safeAdd(manualLiabilities, debtValue)
        val netWorth = totalAssets - totalLiabilities

        return NetWorthOverview(
            manualAssets = manualAssets,
            savingsValue = savingsValue,
            investmentValue = investmentValue,
            manualLiabilities = manualLiabilities,
            debtValue = debtValue,
            totalAssets = totalAssets,
            totalLiabilities = totalLiabilities,
            netWorth = netWorth,
            debtToAssetPercent = if (totalAssets > 0L) {
                totalLiabilities.toDouble() / totalAssets.toDouble() * 100.0
            } else if (totalLiabilities > 0L) {
                100.0
            } else {
                0.0
            },
            assetItems = items.count { it.type.side == BalanceSheetSide.ASSET },
            liabilityItems = items.count { it.type.side == BalanceSheetSide.LIABILITY },
        )
    }

    private fun safeAdd(vararg values: Long): Long = values.fold(0L) { total, value ->
        Math.addExact(total, value)
    }
}
