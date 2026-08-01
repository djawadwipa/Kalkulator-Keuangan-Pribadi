package id.djawadwipa.kalkulatorkeuangan.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface NetWorthDao {
    @Query(
        """
        SELECT * FROM net_worth_items
        WHERE is_archived = 0
        ORDER BY side, value DESC, name COLLATE NOCASE
        """,
    )
    fun observeItems(): Flow<List<NetWorthItemEntity>>

    @Query("SELECT * FROM net_worth_snapshots ORDER BY month_start DESC")
    fun observeSnapshots(): Flow<List<NetWorthSnapshotEntity>>

    @Query("SELECT * FROM net_worth_items WHERE id = :id AND is_archived = 0 LIMIT 1")
    suspend fun findItemById(id: Long): NetWorthItemEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertItem(item: NetWorthItemEntity): Long

    @Update
    suspend fun updateItem(item: NetWorthItemEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSnapshot(snapshot: NetWorthSnapshotEntity): Long

    @Query("DELETE FROM net_worth_items WHERE id = :id")
    suspend fun deleteItemById(id: Long)

    @Query("DELETE FROM net_worth_snapshots WHERE id = :id")
    suspend fun deleteSnapshotById(id: Long)
}
