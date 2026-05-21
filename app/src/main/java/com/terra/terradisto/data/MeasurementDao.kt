package com.terra.terradisto.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MeasurementDao {
    // 측정 데이터 저장
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMeasurement(measurement: MeasurementEntity)

    // 측정 데이터 불러오기
    @Query("SELECT * FROM measurements WHERE projectId = :projectId ORDER BY timestamp DESC")
    fun getMesurementByProject(projectId: Long) : Flow<List<MeasurementEntity>>

    // 측정 데이터 삭제
    @Delete
    suspend fun deleteMeasurement(measurement: MeasurementEntity)
}