package com.terra.terradisto.data

import kotlinx.coroutines.flow.Flow

class ProjectRepository(private val projectDao: ProjectDao) {
    val allProjects: Flow<List<Project>> = projectDao.getAllProjects()

    /**
     * 생성된 프로젝트 ID를 반환
     */
    suspend fun insert(project: Project): Long {
        return projectDao.insertProject(project)
    }

    suspend fun delete(project: Project) {
        projectDao.deleteProject(project)
    }
}