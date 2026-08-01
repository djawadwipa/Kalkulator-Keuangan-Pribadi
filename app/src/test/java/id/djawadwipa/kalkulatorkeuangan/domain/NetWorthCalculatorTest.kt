package id.djawadwipa.kalkulatorkeuangan.domain

import id.djawadwipa.kalkulatorkeuangan.model.NetWorthItem
import id.djawadwipa.kalkulatorkeuangan.model.NetWorthItemType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NetWorthCalculatorTest {
    @Test
    fun combinesManualItemsSavingsInvestmentsAndDebt() {
        val overview = NetWorthCalculator.calculate(
            items = listOf(
                item(1, NetWorthItemType.PROPERTY, 500_000_000L),
                item(2, NetWorthItemType.VEHICLE, 120_000_000L),
                item(3, NetWorthItemType.OTHER_LIABILITY, 20_000_000L),
            ),
            savingsValue = 30_000_000L,
            investmentValue = 80_000_000L,
            debtValue = 150_000_000L,
        )

        assertEquals(730_000_000L, overview.totalAssets)
        assertEquals(170_000_000L, overview.totalLiabilities)
        assertEquals(560_000_000L, overview.netWorth)
        assertEquals(2, overview.assetItems)
        assertEquals(1, overview.liabilityItems)
        assertTrue(overview.debtToAssetPercent in 23.2..23.4)
    }

    @Test
    fun liabilitiesWithoutAssetsProduceFullDebtRatio() {
        val overview = NetWorthCalculator.calculate(
            items = listOf(item(1, NetWorthItemType.PAYABLE, 5_000_000L)),
            savingsValue = 0L,
            investmentValue = 0L,
            debtValue = 5_000_000L,
        )

        assertEquals(-10_000_000L, overview.netWorth)
        assertEquals(100.0, overview.debtToAssetPercent, 0.001)
    }

    private fun item(id: Long, type: NetWorthItemType, value: Long) = NetWorthItem(
        id = id,
        name = type.label,
        type = type,
        value = value,
        note = "",
        updatedAt = 1L,
    )
}
