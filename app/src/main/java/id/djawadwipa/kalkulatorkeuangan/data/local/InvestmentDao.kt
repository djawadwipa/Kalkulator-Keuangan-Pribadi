package id.djawadwipa.kalkulatorkeuangan.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface InvestmentDao {
    @Query(
        """
        SELECT id, name, symbol, provider, type, units, average_cost,
            current_price, target_allocation_percent, created_at, updated_at
        FROM investment_assets
        WHERE is_archived = 0
        ORDER BY current_price * units DESC, updated_at DESC, name COLLATE NOCASE
        """,
    )
    fun observeAssets(): Flow<List<InvestmentAssetRecord>>

    @Query(
        """
        SELECT t.id, t.asset_id, a.name AS asset_name, t.type, t.units,
            t.unit_price, t.amount, t.fee, t.transacted_at, t.note
        FROM investment_transactions t
        INNER JOIN investment_assets a ON a.id = t.asset_id
        ORDER BY t.transacted_at DESC, t.id DESC
        """,
    )
    fun observeTransactions(): Flow<List<InvestmentTransactionRecord>>

    @Query("SELECT * FROM investment_assets WHERE id = :id AND is_archived = 0 LIMIT 1")
    suspend fun findAssetById(id: Long): InvestmentAssetEntity?

    @Query("SELECT * FROM investment_transactions WHERE id = :id LIMIT 1")
    suspend fun findTransactionById(id: Long): InvestmentTransactionEntity?

    @Query(
        """
        SELECT * FROM investment_transactions
        WHERE asset_id = :assetId
        ORDER BY transacted_at ASC, id ASC
        """,
    )
    suspend fun getTransactionsForAsset(assetId: Long): List<InvestmentTransactionEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAsset(asset: InvestmentAssetEntity): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertTransaction(transaction: InvestmentTransactionEntity): Long

    @Update
    suspend fun updateAsset(asset: InvestmentAssetEntity)

    @Query("DELETE FROM investment_transactions WHERE id = :id")
    suspend fun deleteTransactionById(id: Long)

    @Query("DELETE FROM investment_assets WHERE id = :id")
    suspend fun deleteAssetById(id: Long)
}
