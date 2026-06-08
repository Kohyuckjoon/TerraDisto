package com.terra.terradisto.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.terra.terradisto.data.LoginRequest
import com.terra.terradisto.data.RetrofitClient
import kotlinx.coroutines.launch

class LoginViewModel : ViewModel() {
    // 상태 관리 : UI에서 로그인 결과를 기다리는지, 성공 / 실패 확인 여부
    var errorMessage by mutableStateOf<String?>(null)
    var isLoading by mutableStateOf(false)

    // 로그인 함수
    fun login(email: String, password: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            isLoading = true
            try {
                // RetrofitClient를 통해서 API 호출 (백그라운드에서 실행)
                val response = RetrofitClient.apiService.login(LoginRequest(email, password))

                if (response.isSuccessful) {
                    // 200 (ok일때)
                    onResult(true)
                } else {
                    // 403, 429등 실패
                    errorMessage = "로그인 실패 : {${response.code()} 에러가 발생했어요. / ${response.message()} }"
                    onResult(false)
                }
            } catch (e: Exception) {
                errorMessage = "서버 연결에 실패했어요.."
                onResult(false)
            } finally {
                isLoading = false
            }
        }
    }
}