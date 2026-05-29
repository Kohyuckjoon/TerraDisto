package com.terra.terradisto.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.terra.terradisto.data.PipeUiItem

@Entity(tableName = "measurements")
data class MeasurementEntity (
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long,
    val manholeType: String,
    val lidMaterial: String,
    val lidSize: String,
    val topieValue: String,
    val chamberMaterial: String,
    val chamberSize: String,
    val selectedChamberShape: String,
    val hasLadder: Boolean,
    val hasInverter: Boolean,
    val anomalyMemo: String,
//    val pipeListJson: String,
    val timestamp: Long = System.currentTimeMillis(), // 저장된 시간 기록
    val pipeList: List<PipeUiItem>
)

class MeasurementConverters {
    private val gson = Gson()


//    fun fromPipeList(value: List<PipeUiItem>): String {
//        return gson.toJson(value)
//    }
    @TypeConverter
    fun fromPipeList(value: List<PipeUiItem>?): String { // [수정] null 허용이 안전합니다
        return gson.toJson(value)
    }

    @TypeConverter
    fun toPipeList(value: String?): List<PipeUiItem> {
        if (value.isNullOrEmpty()) return emptyList()
        val listType = object : TypeToken<List<PipeUiItem>>() {}.type
        return gson.fromJson(value, listType)
    }
}