package id.djawadwipa.kalkulatorkeuangan.domain

import id.djawadwipa.kalkulatorkeuangan.model.InvestmentAsset
import id.djawadwipa.kalkulatorkeuangan.model.InvestmentTransactionType
import id.djawadwipa.kalkulatorkeuangan.model.InvestmentType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class InvestmentCalculatorTest {
    @Test
    fun buyUsesWeightedAverageCost() {
        val update = InvestmentCalculator.positionAfter(
            currentUnits = 10.0,
            currentAverageCost = 1_000L,
            type = InvestmentTransactionType.BUY,
            units = 5.0,
            unitPrice = 1_600L,
        )

        assertEquals(15.0, update.units, 0.0001)
        assertEquals(1_200L, update.averageCost)
    }

    @Test
    fun sellKeepsAverageCostUntilPositionIsClosed() {
        val partial = InvestmentCalculator.positionAfter(
            currentUnits = 10.0,
            currentAverageCost = 2_000L,
            type = InvestmentTransactionType.SELL,
            units = 4.0,
            unitPrice = 2_500L,
        )
        val closed = InvestmentCalculator.positionAfter(
            currentUnits = 6.0,
            currentAverageCost = 2_000L,
            type = InvestmentTransactionType.SELL,
            units = 6.0,
            unitPrice = 2_500L,
        )

        assertEquals(6.0, partial.units, 0.0001)
        assertEquals(2_000L, partial.averageCost)
        assertEquals(0.0, closed.units, 0.0001)
        assertEquals(0L, closed.averageCost)
    }

    @Test
    fun portfolioAllocationAndReturnAreCalculated() {
        val assets = InvestmentCalculator.enrichAllocation(
            listOf(
                asset(id = 1, marketValue = 6_000_000L, costBasis = 5_000_000L),
                asset(id = 2, marketValue = 4_000_000L, costBasis = 4_000_000L),
            ),
        )
        val overview = InvestmentCalculator.overview(assets)

        assertEquals(60.0, assets.first().allocationPercent, 0.001)
        assertEquals(40.0, assets.last().allocationPercent, 0.001)
        assertEquals(10_000_000L, overview.totalMarketValue)
        assertEquals(1_000_000L, overview.totalGainLoss)
        assertTrue(overview.returnPercent > 11.0)
    }

    @Test
    fun projectionIncludesMonthlyCompoundingAndInflationAdjustment() {
        val projection = InvestmentCalculator.project(
            initialInvestment = 10_000_000L,
            monthlyContribution = 1_000_000L,
            annualReturnPercent = 8.0,
            years = 10,
            annualInflationPercent = 3.0,
        )

        assertEquals(130_000_000L, projection.totalContributions)
        assertTrue(projection.futureValue > projection.totalContributions)
        assertTrue(projection.inflationAdjustedValue < projection.futureValue)
        assertEquals(10, projection.points.size)
        assertEquals(10, projection.points.last().year)
    }

    private fun asset(id: Long, marketValue: Long, costBasis: Long): InvestmentAsset = InvestmentAsset(
        id = id,
        name = "Aset $id",
        symbol = "A$id",
        provider = "Provider",
        type = InvestmentType.STOCK,
        units = 1.0,
        averageCost = costBasis,
        currentPrice = marketValue,
        targetAllocationPercent = 50.0,
        costBasis = costBasis,
        marketValue = marketValue,
        gainLoss = marketValue - costBasis,
        returnPercent = if (costBasis > 0) (marketValue - costBasis).toDouble() / costBasis * 100.0 else 0.0,
        allocationPercent = 0.0,
        createdAt = 1L,
        updatedAt = 1L,
    )
}
