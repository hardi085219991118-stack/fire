package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface HotspotDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(hotspots: List<HotspotEntity>)

    @Query("DELETE FROM hotspots")
    suspend fun deleteAll()

    @Query("SELECT * FROM hotspots ORDER BY acquisitionTimestamp DESC")
    fun getAllHotspotsFlow(): Flow<List<HotspotEntity>>

    @Query("SELECT * FROM hotspots ORDER BY acquisitionTimestamp DESC")
    suspend fun getAllHotspots(): List<HotspotEntity>

    @Query("SELECT MAX(dataReceivedTimestamp) FROM hotspots")
    suspend fun getLatestReceivedTimestamp(): Long?

    @Query("SELECT MAX(acquisitionTimestamp) FROM hotspots")
    suspend fun getLatestAcquisitionTimestamp(): Long?
}
