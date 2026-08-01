package id.djawadwipa.kalkulatorkeuangan.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "investment_assets",
    indices = [
        Index(value = ["type"]),
        Index(value = ["is_archived"]),
        Index(value = ["name"]),
    ],
)
data class InvestmentAssetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val symbol: String,
    val provider: String,
    val type: String,
    val units: Double = 0.0,
    @ColumnInfo(name = "average_cost") val averageCost: Long = 0,
    @ColumnInfo(name = "current_price") val currentPrice: Long = 0,
    @ColumnInfo(name = "target_allocation_percent") val targetAllocationPercent: Double = 0.0,
    @ColumnInfo(name = "is_archived") val isArchived: Boolean = false,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
)

@Entity(
    tableName = "investment_transactions",
    foreignKeys = [
        ForeignKey(
            entity = InvestmentAssetEntity::class,
            parentColumns = ["id"],
            childColumns = ["asset_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["asset_id"]),
        Index(value = ["type"]),
        Index(value = ["transacted_at"]),
    ],
)
data class InvestmentTransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "asset_id") val assetId: Long,
    val type: String,
    val units: Double = 0.0,
    @ColumnInfo(name = "unit_price") val unitPrice: Long = 0,
    val amount: Long = 0,
    val fee: Long = 0,
    @ColumnInfo(name = "transacted_at") val transactedAt: Long,
    val note: String = "",
)

data class InvestmentAssetRecord(
    val id: Long,
    val name: String,
    val symbol: String,
    val provider: String,
    val type: String,
    val units: Double,
    @ColumnInfo(name = "average_cost") val averageCost: Long,
    @ColumnInfo(name = "current_price") val currentPrice: Long,
    @ColumnInfo(name = "target_allocation_percent") val targetAllocationPercent: Double,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
)

data class InvestmentTransactionRecord(
    val id: Long,
    @ColumnInfo(name = "asset_id") val assetId: Long,
    @ColumnInfo(name = "asset_name") val assetName: String,
    val type: String,
    val units: Double,
    @ColumnInfo(name = "unit_price") val unitPrice: Long,
    val amount: Long,
    val fee: Long,
    @ColumnInfo(name = "transacted_at") val transactedAt: Long,
    val note: String,
)
