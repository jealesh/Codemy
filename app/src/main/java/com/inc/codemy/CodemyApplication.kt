package com.inc.codemy

import android.app.Application
import com.inc.codemy.network.ApiClient

class CodemyApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        // Инициализируем кэш для сетевых запросов
        ApiClient.initCache(applicationContext)
    }
}
