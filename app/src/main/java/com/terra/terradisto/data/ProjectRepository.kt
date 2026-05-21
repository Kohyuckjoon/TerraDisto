package com.terra.terradisto.data

import kotlinx.coroutines.flow.Flow

class ProjectRepository(private val projectDao: ProjectDao) {
    val allProjects: Flow<List<Project>> = projectDao.getAllProjects()

    suspend fun insert(project: Project) {
        projectDao.insertProject(project)
    }

    suspend fun delete(project: Project) {
        projectDao.deleteProject(project)
    }
}