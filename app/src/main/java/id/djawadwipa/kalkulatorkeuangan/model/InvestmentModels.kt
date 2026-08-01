package id.djawadwipa.kalkulatorkeuangan.model

enum class InvestmentType(val label: String) {
    DEPOSIT("Deposito"),
    BOND("Obligasi / SBN"),
    MUTUAL_FUND("Reksa dana"),
    STOCK("Saham"),
    GOLD("Emas"),
    CRYPTO("Aset kripto"),
    OTHER("Lainnya"),
}

enum class InvestmentTransactionType(val label: String) {
    BUY("Beli / setoran"),
    SELL("Jual / penarikan"),
    DIVIDEND("Dividen / hasil"),
    FEE("Biaya"),
}

data class InvestmentAsset(
    val id: Long,
    val name: String,
    val symbol: String,
    val provider: String,
    val type: InvestmentType,
    val units: Double,
    val averageCost: Long,
    val currentPrice: Long,
    val targetAllocationPercent: Double,
    val costBasis: Long,
    val marketValue: Long,
    val gainLoss: Long,
    val returnPercent: Double,
    val allocationPercent: Double,
    val createdAt: Long,
    val updatedAt: Long,
)

data class InvestmentAssetDraft(
    val name: String,
    val symbol: String,
    val provider: String,
    val type: InvestmentType,
    val currentPrice: Long,
    val targetAllocationPercent: Double,
)

data class InvestmentTransaction(
    val id: Long,
    val assetId: Long,
    val assetName: String,
    val type: InvestmentTransactionType,
    val units: Double,
    val unitPrice: Long,
    val amount: Long,
    val fee: Long,
    val transactedAt: Long,
    val note: String,
)

data class InvestmentTransactionDraft(
    val assetId: Long,
    val type: InvestmentTransactionType,
    val units: Double = 0.0,
    val unitPrice: Long = 0,
    val amount: Long = 0,
    val fee: Long = 0,
    val transactedAt: Long,
    val note: String = "",
)

data class PortfolioOverview(
    val assetCount: Int = 0,
    val totalCostBasis: Long = 0,
    val totalMarketValue: Long = 0,
    val totalGainLoss: Long = 0,
    val returnPercent: Double = 0.0,
    val totalIncome: Long = 0,
    val totalFees: Long = 0,
)

data class PositionUpdate(
    val units: Double,
    val averageCost: Long,
)

data class InvestmentProjectionPoint(
    val year: Int,
    val projectedValue: Long,
    val totalContributions: Long,
)

data class InvestmentProjection(
    val initialInvestment: Long,
    val monthlyContribution: Long,
    val annualReturnPercent: Double,
    val years: Int,
    val annualInflationPercent: Double,
    val futureValue: Long,
    val totalContributions: Long,
    val estimatedGain: Long,
    val inflationAdjustedValue: Long,
    val points: List<InvestmentProjectionPoint>,
)
