package id.djawadwipa.kalkulatorkeuangan.domain

import id.djawadwipa.kalkulatorkeuangan.model.InvestmentAsset
import id.djawadwipa.kalkulatorkeuangan.model.InvestmentProjection
import id.djawadwipa.kalkulatorkeuangan.model.InvestmentProjectionPoint
import id.djawadwipa.kalkulatorkeuangan.model.InvestmentTransaction
import id.djawadwipa.kalkulatorkeuangan.model.InvestmentTransactionType
import id.djawadwipa.kalkulatorkeuangan.model.PortfolioOverview
import id.djawadwipa.kalkulatorkeuangan.model.PositionUpdate
import kotlin.math.pow
import kotlin.math.roundToLong

object InvestmentCalculator {
    fun positionAfter(
        currentUnits: Double,
        currentAverageCost: Long,
        type: InvestmentTransactionType,
        units: Double,
        unitPrice: Long,
    ): PositionUpdate {
        require(currentUnits.isFinite() && currentUnits >= 0.0) { "Jumlah unit saat ini tidak valid" }
        require(currentAverageCost >= 0L) { "Harga rata-rata saat ini tidak valid" }

        return when (type) {
            InvestmentTransactionType.BUY -> {
                require(units.isFinite() && units > 0.0) { "Jumlah unit pembelian harus lebih dari nol" }
                require(unitPrice > 0L) { "Harga per unit harus lebih dari nol" }
                val newUnits = currentUnits + units
                val oldCost = currentUnits * currentAverageCost.toDouble()
                val additionalCost = units * unitPrice.toDouble()
                PositionUpdate(
                    units = newUnits,
                    averageCost = ((oldCost + additionalCost) / newUnits).roundToLong(),
                )
            }

            InvestmentTransactionType.SELL -> {
                require(units.isFinite() && units > 0.0) { "Jumlah unit penjualan harus lebih dari nol" }
                require(units <= currentUnits + EPSILON) { "Unit yang dijual melebihi kepemilikan" }
                require(unitPrice > 0L) { "Harga per unit harus lebih dari nol" }
                val remaining = (currentUnits - units).coerceAtLeast(0.0)
                PositionUpdate(
                    units = remaining,
                    averageCost = if (remaining <= EPSILON) 0L else currentAverageCost,
                )
            }

            InvestmentTransactionType.DIVIDEND,
            InvestmentTransactionType.FEE,
            -> PositionUpdate(currentUnits, currentAverageCost)
        }
    }

    fun enrichAllocation(assets: List<InvestmentAsset>): List<InvestmentAsset> {
        val total = assets.sumOf { it.marketValue }.coerceAtLeast(0L)
        return assets.map { asset ->
            asset.copy(
                allocationPercent = if (total > 0L) {
                    asset.marketValue.toDouble() / total.toDouble() * 100.0
                } else {
                    0.0
                },
            )
        }
    }

    fun overview(
        assets: List<InvestmentAsset>,
        transactions: List<InvestmentTransaction> = emptyList(),
    ): PortfolioOverview {
        val totalCost = assets.sumOf { it.costBasis }
        val totalValue = assets.sumOf { it.marketValue }
        val income = transactions
            .filter { it.type == InvestmentTransactionType.DIVIDEND }
            .sumOf { it.amount }
        val fees = transactions.sumOf { transaction ->
            transaction.fee + if (transaction.type == InvestmentTransactionType.FEE) transaction.amount else 0L
        }
        return PortfolioOverview(
            assetCount = assets.count { it.units > EPSILON || it.marketValue > 0L },
            totalCostBasis = totalCost,
            totalMarketValue = totalValue,
            totalGainLoss = totalValue - totalCost,
            returnPercent = if (totalCost > 0L) {
                (totalValue - totalCost).toDouble() / totalCost.toDouble() * 100.0
            } else {
                0.0
            },
            totalIncome = income,
            totalFees = fees,
        )
    }

    fun project(
        initialInvestment: Long,
        monthlyContribution: Long,
        annualReturnPercent: Double,
        years: Int,
        annualInflationPercent: Double,
    ): InvestmentProjection {
        require(initialInvestment >= 0L) { "Modal awal tidak boleh negatif" }
        require(monthlyContribution >= 0L) { "Setoran bulanan tidak boleh negatif" }
        require(annualReturnPercent.isFinite() && annualReturnPercent in -100.0..1000.0) {
            "Imbal hasil tahunan tidak valid"
        }
        require(years in 1..60) { "Durasi simulasi harus 1 sampai 60 tahun" }
        require(annualInflationPercent.isFinite() && annualInflationPercent in 0.0..100.0) {
            "Inflasi tahunan tidak valid"
        }

        val months = years * 12
        val monthlyRate = annualReturnPercent / 100.0 / 12.0
        var value = initialInvestment.toDouble()
        val points = mutableListOf<InvestmentProjectionPoint>()

        for (month in 1..months) {
            value = value * (1.0 + monthlyRate) + monthlyContribution.toDouble()
            if (month % 12 == 0) {
                val year = month / 12
                points += InvestmentProjectionPoint(
                    year = year,
                    projectedValue = value.coerceAtLeast(0.0).roundToLong(),
                    totalContributions = initialInvestment + monthlyContribution * month.toLong(),
                )
            }
        }

        val futureValue = value.coerceAtLeast(0.0).roundToLong()
        val totalContributions = initialInvestment + monthlyContribution * months.toLong()
        val inflationFactor = (1.0 + annualInflationPercent / 100.0).pow(years.toDouble())
        val realValue = (futureValue.toDouble() / inflationFactor).coerceAtLeast(0.0).roundToLong()

        return InvestmentProjection(
            initialInvestment = initialInvestment,
            monthlyContribution = monthlyContribution,
            annualReturnPercent = annualReturnPercent,
            years = years,
            annualInflationPercent = annualInflationPercent,
            futureValue = futureValue,
            totalContributions = totalContributions,
            estimatedGain = futureValue - totalContributions,
            inflationAdjustedValue = realValue,
            points = points,
        )
    }

    const val EPSILON: Double = 0.0000001
}
