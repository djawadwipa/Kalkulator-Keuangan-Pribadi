package id.djawadwipa.kalkulatorkeuangan.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "net_worth_items",
    indices = [
        Index(value = ["side"]),
        Index(value = ["type"]),
        Index(value = ["is_archived"]),
    ],
)
data class NetWorthItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val side: String,
    val type: String,
    val value: Long,
    val note: String = "",
    @ColumnInfo(name = "is_archived") val isArchived: Boolean = false,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
)

@Entity(
    tableName = "net_worth_snapshots",
    indices = [Index(value = ["month_start"], unique = true)],
)
data class NetWorthSnapshotEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "month_start") val monthStart: Long,
    @ColumnInfo(name = "manual_assets") val manualAssets: Long,
    @ColumnInfo(name = "savings_value") val savingsValue: Long,
    @ColumnInfo(name = "investment_value") val investmentValue: Long,
    @ColumnInfo(name = "manual_liabilities") val manualLiabilities: Long,
    @ColumnInfo(name = "debt_value") val debtValue: Long,
    @ColumnInfo(name = "total_assets") val totalAssets: Long,
    @ColumnInfo(name = "total_liabilities") val totalLiabilities: Long,
    @ColumnInfo(name = "net_worth") val netWorth: Long,
    @ColumnInfo(name = "generated_at") val generatedAt: Long,
)
