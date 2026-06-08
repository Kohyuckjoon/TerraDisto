package com.terra.terradisto.data

data class LoginResponse(
    val success: Boolean,
    val licenseKey: String?, // 성공 시 반환
    val message: String? // 실패 시 에러 메세지
)