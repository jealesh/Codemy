package com.inc.codemy.network

import android.content.Context
import kotlinx.serialization.json.Json
import okhttp3.Cache
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.io.File
import java.util.concurrent.TimeUnit

object ApiClient {
    private const val BASE_URL = "http://10.0.2.2:8080/"  // для эмулятора Android (localhost сервера)
    private const val CACHE_SIZE = 10 * 1024 * 1024L // 10 MB кэш

    private val json = Json { ignoreUnknownKeys = true }

    // Логгер для отладки сетевых запросов (отключаем в release для производительности)
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.NONE // Отключаем логгер для production
    }

    // Кэш для ответов сервера
    private var cache: Cache? = null

    fun initCache(context: Context) {
        val cacheDir = File(context.cacheDir, "http_cache")
        cache = Cache(cacheDir, CACHE_SIZE)
    }

    // Оптимизированный OkHttpClient с кэшем, таймаутами и connection pooling
    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS) // Уменьшили таймаут для быстрой ошибки
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .connectionPool(
                connectionPool = okhttp3.ConnectionPool(
                    maxIdleConnections = 10, // Держим до 10 соединений
                    keepAliveDuration = 5, TimeUnit.MINUTES // 5 минут простоя
                )
            )
            .retryOnConnectionFailure(true)
            .addInterceptor(loggingInterceptor)
            .addInterceptor { chain ->
                val request = chain.request()

                // Добавляем заголовки для кэширования только GET-запросам
                val response = chain.proceed(request)

                if (request.method == "GET") {
                    val maxAge = 60 // 1 минута для свежих данных
                    response.newBuilder()
                        .removeHeader("Pragma")
                        .header("Cache-Control", "public, max-age=$maxAge")
                        .build()
                } else {
                    response
                }
            }
            .apply {
                cache?.let { cache(it) }
            }
            .build()
    }

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
    }

    val apiService: ApiService by lazy {
        retrofit.create(ApiService::class.java)
    }
}
