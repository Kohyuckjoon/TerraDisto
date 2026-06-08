package com.terra.terradisto.data

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    // 실제 서버 기본 주소(BASE_URL) 입력
    // 마지막은 "/"로 마무리
//    private const val BASE_URL = "https://api.terra-survey.com/"
    private const val BASE_URL = "https://terra-survey.com/"

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create()) // JSON을 객체로 변경
            .build()
    }

    // 상기 코드에서 만든 이용해서 ApiService를 생성
    val apiService: ApiService by lazy {
        retrofit.create(ApiService::class.java)
    }
}