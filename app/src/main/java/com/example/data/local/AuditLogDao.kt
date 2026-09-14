package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AuditLogDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: AuditLogEntity)

    @Query("SELECT * FROM data_audit_logs ORDER BY requestTime DESC")
    fun getAllLogsFlow(): Flow<List<AuditLogEntity>>

    @Query("SELECT * FROM data_audit_logs ORDER BY requestTime DESC LIMIT :limit")
    suspend fun getRecentLogs(limit: Int = 30): List<AuditLogEntity>

    @Query("DELETE FROM data_audit_logs")
    suspend fun deleteAll()
}
