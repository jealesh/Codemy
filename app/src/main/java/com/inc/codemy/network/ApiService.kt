package com.inc.codemy.network

import com.inc.codemy.models.RegisterRequest
import com.inc.codemy.models.RegisterResponse
import com.inc.codemy.models.LoginRequest
import com.inc.codemy.models.LoginResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface ApiService {
    @POST("register")
    suspend fun register(@Body request: RegisterRequest): RegisterResponse

    @POST("login")
    suspend fun login(@Body request: LoginRequest): LoginResponse
}