package com.terra.terradisto.data

import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST


data class LicenseRegisterRequest(
    val email: String,
    @SerializedName("password")
    val password: String,
    val licenseKey: String
) {

}

data class LicenseRegisterResponse(
    val success: Boolean,
    val hasLicense: Boolean,
    val message: String
)

interface ApiService {
    @POST("/api/apps/disto/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    //
    @POST("/api/apps/disto/license")
    suspend fun registerLicense(@Body request: LicenseRegisterRequest): Response<LicenseRegisterResponse>
}