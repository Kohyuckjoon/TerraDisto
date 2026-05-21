package com.terra.terradisto.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.terra.terradisto.data.AppDatabase
import com.terra.terradisto.data.Project
import com.terra.terradisto.data.ProjectRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ProjectViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: ProjectRepository
    val allProjects: kotlinx.coroutines.flow.Flow<List<Project>>

    // 현재 선택된 프로젝트를 관리하는 상태
    private val _selectedProject = MutableStateFlow<Project?>(null)
    val selectedProject: StateFlow<Project?> = _selectedProject

    init {
        val projectDao = AppDatabase.getDatabase(application).projectDao()
        repository = ProjectRepository(projectDao)
        allProjects = repository.allProjects
    }

    // 프로젝트 선택 함수
    fun selectProject(project: Project) {
        _selectedProject.value = project
    }

    // 프로젝트 삭제 함수
    fun deleteProject(project: Project) {
        viewModelScope.launch {
            repository.delete(project)

            // 삭제하려는 프로젝트가 현재 선택된 프로젝트라면 선택 해제
            if (_selectedProject.value?.id == project.id) {
                _selectedProject.value = null
            }
        }
    }

    // 프로젝트 생성 로직 (기존 유지)
    fun insertProject(name: String, location: String, desc: String, date: String) {
        viewModelScope.launch {
            val newProject = Project(
                projectName = name,
                location = location,
                description = desc,
                createdAt = date
            )
            repository.insert(newProject)
        }
    }
}