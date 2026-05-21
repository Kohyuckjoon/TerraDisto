package com.terra.terradisto.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectDao {
    // 실시간으로 프로젝트 리스트를 관찰하기 위해 Flow를 사용합니다.
    @Query("SELECT * FROM projects ORDER BY id DESC")
    fun getAllProjects(): Flow<List<Project>>

    // 새로운 프로젝트를 삽입합니다. (중복 시 덮어쓰기)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: Project)

    // 프로젝트를 삭제합니다.
    @Delete
    suspend fun deleteProject(project: Project)
}