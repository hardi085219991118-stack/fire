package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.IncidentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface IncidentDao {

    @Query("SELECT * FROM incidents ORDER BY createdAt DESC")
    suspend fun getAllIncidents(): List<IncidentEntity>

    @Query("SELECT * FROM incidents ORDER BY createdAt DESC")
    fun observeAllIncidents(): Flow<List<IncidentEntity>>

    @Query("SELECT * FROM incidents WHERE id = :id LIMIT 1")
    suspend fun getIncidentById(id: String): IncidentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIncident(incident: IncidentEntity)

    @Update
    suspend fun updateIncident(incident: IncidentEntity)

    @Delete
    suspend fun deleteIncident(incident: IncidentEntity)

    @Query("DELETE FROM incidents")
    suspend fun deleteAllIncidents()
}
