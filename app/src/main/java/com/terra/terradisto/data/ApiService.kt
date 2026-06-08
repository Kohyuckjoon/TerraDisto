package com.terra.terradisto.data

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface ApiService {
    @POST("/api/v1/apps/disto/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>
}