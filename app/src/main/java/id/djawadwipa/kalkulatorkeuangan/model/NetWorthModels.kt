package id.djawadwipa.kalkulatorkeuangan.model

enum class BalanceSheetSide(val label: String) {
    ASSET("Aset"),
    LIABILITY("Liabilitas"),
}

enum class NetWorthItemType(
    val label: String,
    val side: BalanceSheetSide,
) {
    CASH_EQUIVALENT("Kas di luar rekening aplikasi", BalanceSheetSide.ASSET),
    PROPERTY("Properti", BalanceSheetSide.ASSET),
    VEHICLE("Kendaraan", BalanceSheetSide.ASSET),
    RECEIVABLE("Piutang", BalanceSheetSide.ASSET),
    BUSINESS("Nilai usaha", BalanceSheetSide.ASSET),
    PERSONAL_ASSET("Aset pribadi", BalanceSheetSide.ASSET),
    OTHER_ASSET("Aset lainnya", BalanceSheetSide.ASSET),
    MORTGAGE("KPR / pinjaman properti di luar modul utang", BalanceSheetSide.LIABILITY),
    PERSONAL_LOAN("Pinjaman pribadi di luar modul utang", BalanceSheetSide.LIABILITY),
    VEHICLE_LOAN("Cicilan kendaraan di luar modul utang", BalanceSheetSide.LIABILITY),
    PAYABLE("Utang usaha / utang lain", BalanceSheetSide.LIABILITY),
    OTHER_LIABILITY("Liabilitas lainnya", BalanceSheetSide.LIABILITY),
}

data class NetWorthItem(
    val id: Long,
    val name: String,
    val type: NetWorthItemType,
    val value: Long,
    val note: String,
    val updatedAt: Long,
)

data class NetWorthItemDraft(
    val name: String,
    val type: NetWorthItemType,
    val value: Long,
    val note: String = "",
)

data class NetWorthOverview(
    val manualAssets: Long = 0,
    val savingsValue: Long = 0,
    val investmentValue: Long = 0,
    val manualLiabilities: Long = 0,
    val debtValue: Long = 0,
    val totalAssets: Long = 0,
    val totalLiabilities: Long = 0,
    val netWorth: Long = 0,
    val debtToAssetPercent: Double = 0.0,
    val assetItems: Int = 0,
    val liabilityItems: Int = 0,
)

data class NetWorthSnapshot(
    val id: Long,
    val monthStart: Long,
    val manualAssets: Long,
    val savingsValue: Long,
    val investmentValue: Long,
    val manualLiabilities: Long,
    val debtValue: Long,
    val totalAssets: Long,
    val totalLiabilities: Long,
    val netWorth: Long,
    val generatedAt: Long,
)
