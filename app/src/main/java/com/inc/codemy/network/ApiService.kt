package com.inc.codemy.network

import com.inc.codemy.models.CourseResponse
import com.inc.codemy.models.LessonResponse
import com.inc.codemy.models.RegisterRequest
import com.inc.codemy.models.RegisterResponse
import com.inc.codemy.models.LoginRequest
import com.inc.codemy.models.LoginResponse
import com.inc.codemy.models.UserProfileResponse
import com.inc.codemy.models.UserStatsResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {
    @POST("register")
    suspend fun register(@Body request: RegisterRequest): RegisterResponse

    @POST("login")
    suspend fun login(@Body request: LoginRequest): LoginResponse

    @GET("user/profile/{userId}")
    suspend fun getUserProfile(@Path("userId") userId: Long): UserProfileResponse

    @GET("courses")
    suspend fun getCourses(): List<CourseResponse>

    @GET("courses/{courseId}/lessons")
    suspend fun getLessons(
        @Path("courseId") courseId: Long,
        @Query("userId") userId: Long
    ): List<LessonResponse>
}