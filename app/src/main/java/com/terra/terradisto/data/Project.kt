package com.terra.terradisto.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "projects")
data class Project(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val projectName: String,
    val location: String,
    val description: String,
    val createdAt: String
)
