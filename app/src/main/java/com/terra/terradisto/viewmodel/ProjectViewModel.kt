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
//    fun deleteProject(project: Project) {
//        viewModelScope.launch {
//            repository.delete(project)
//
//            // 삭제하려는 프로젝트가 현재 선택된 프로젝트라면 선택 해제
//            if (_selectedProject.value?.id == project.id) {
//                _selectedProject.value = null
//            }
//        }
//    }
    fun deleteProject(project: Project) {
        viewModelScope.launch {
            try {
                repository.delete(project)
                // 삭제하려는 프로젝트가 현재 선택된 프로젝트라면 선택 해제
                if (_selectedProject.value?.id == project.id) {
                    _selectedProject.value = null
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // 프로젝트 생성 로직 (기존 유지)
    suspend fun insertProject(name: String, location: String, desc: String, date: String): Boolean {
        return try {
            val newProject = Project(
                projectName = name,
                location = location,
                description = desc,
                createdAt = date
            )
            // 저장이 완료될 때까지 여기서 기다립니다.
            val generatedId = repository.insert(newProject)

            val projectWithId = newProject.copy(id = generatedId)
            _selectedProject.value = projectWithId
            true // 성공 반환
        } catch (e: Exception) {
            e.printStackTrace()
            false // 실패 시 크래시 대신 false 반환
        }
    }
}
