package com.inc.codemy.network

import com.inc.codemy.models.CourseResponse
import com.inc.codemy.models.DailyActivityResponse
import com.inc.codemy.models.DailyGoalResponse
import com.inc.codemy.models.DailyGoalUpdateRequest
import com.inc.codemy.models.ExerciseCompletionRequest
import com.inc.codemy.models.ExerciseCompletionResponse
import com.inc.codemy.models.ExerciseProgressResponse
import com.inc.codemy.models.LeaderboardResponse
import com.inc.codemy.models.LessonProgressResponse
import com.inc.codemy.models.LessonProgressUpdateRequest
import com.inc.codemy.models.LessonResponse
import com.inc.codemy.models.UserProgressResponse
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

    @GET("lessons/{lessonId}/content")
    suspend fun getLessonContent(
        @Path("lessonId") lessonId: Long,
        @Query("userId") userId: Long
    ): LessonResponse

    @GET("leaderboard/weekly")
    suspend fun getWeeklyLeaderboard(
        @Query("userId") userId: Long
    ): LeaderboardResponse

    @POST("user-stats/add-xp")
    suspend fun addXp(
        @Query("userId") userId: Long,
        @Query("xpAmount") xpAmount: Int
    ): Map<String, String>

    // ────────────────────────────────────────────────
    // Синхронизация прогресса
    // ────────────────────────────────────────────────

    @POST("exercise/complete")
    suspend fun completeExercise(
        @Body request: ExerciseCompletionRequest
    ): ExerciseCompletionResponse

    @POST("theory/complete")
    suspend fun completeTheory(
        @Body request: ExerciseCompletionRequest
    ): ExerciseCompletionResponse

    @POST("lesson/progress")
    suspend fun updateLessonProgress(
        @Body request: LessonProgressUpdateRequest
    ): Map<String, String>

    @GET("lesson/progress")
    suspend fun getLessonProgress(
        @Query("userId") userId: Long,
        @Query("lessonId") lessonId: Long
    ): LessonProgressResponse

    @GET("lesson/exercises-progress")
    suspend fun getExercisesProgress(
        @Query("userId") userId: Long,
        @Query("lessonId") lessonId: Long
    ): ExerciseProgressResponse

    @GET("user/progress")
    suspend fun getUserProgress(
        @Query("userId") userId: Long
    ): UserProgressResponse

    // ────────────────────────────────────────────────
    // Daily Goal API
    // ────────────────────────────────────────────────

    @GET("daily-goal/{userId}")
    suspend fun getDailyGoal(
        @Path("userId") userId: Long
    ): DailyGoalResponse

    @POST("daily-goal/update")
    suspend fun updateDailyGoal(
        @Body request: DailyGoalUpdateRequest
    ): DailyGoalResponse

    @GET("daily-activity/{userId}")
    suspend fun getDailyActivity(
        @Path("userId") userId: Long
    ): DailyActivityResponse
}