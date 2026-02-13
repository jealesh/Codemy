package com.inc.codemy

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.inc.codemy.models.UserProfileResponse
import com.inc.codemy.network.ApiClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ProfileActivity : AppCompatActivity() {

    private lateinit var profileAvatar: ImageView
    private lateinit var profileName: TextView
    private lateinit var profileLogin: TextView
    private lateinit var profileStreak: TextView
    private lateinit var profileXP: TextView
    private lateinit var profileMaxStreak: TextView
    private lateinit var btnLogout: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        profileAvatar = findViewById(R.id.profileAvatar)
        profileName = findViewById(R.id.profileName)
        profileLogin = findViewById(R.id.profileLogin)
        profileStreak = findViewById(R.id.profileStreak)
        profileXP = findViewById(R.id.profileXP)
        profileMaxStreak = findViewById(R.id.profileMaxStreak)
        btnLogout = findViewById(R.id.btnLogout)

        // Загружаем данные
        CoroutineScope(Dispatchers.Main).launch {
            loadUserProfile()
        }

        // Навигация
        findViewById<TextView>(R.id.navHome).setOnClickListener {
            startActivity(Intent(this, MainScreenActivity::class.java))
            finish()
        }

        findViewById<TextView>(R.id.navSandbox).setOnClickListener {
            startActivity(Intent(this, SandboxActivity::class.java))
        }

        findViewById<TextView>(R.id.navTrophy).setOnClickListener {
            startActivity(Intent(this, LeagueActivity::class.java))
        }

        // Выход
        btnLogout.setOnClickListener {
            getSharedPreferences("user_data", MODE_PRIVATE).edit().clear().apply()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    private suspend fun loadUserProfile() {
        val sharedPref = getSharedPreferences("user_data", MODE_PRIVATE)
        val userId = sharedPref.getLong("user_id", -1L)
        Log.d("ProfileActivity", "Загрузка профиля для userId = $userId")

        if (userId == -1L) {
            profileName.text = "Не авторизован"
            profileLogin.text = "Войдите в аккаунт"
            profileXP.text = "⚡ — XP"
            return
        }

        try {
            Log.d("ProfileActivity", "Запрос к серверу для userId = $userId")
            val profile = withContext(Dispatchers.IO) {
                ApiClient.apiService.getUserProfile(userId)
            }
            Log.d("ProfileActivity", "Получен ответ: $profile")

            // Имя и username из users
            profileName.text = profile.full_name ?: "Имя Пользователя"
            profileLogin.text = profile.username ?: "username"

            // Статистика из user_stats
            profileXP.text = "⚡ ${profile.total_xp} XP"
            profileStreak.text = "🔥 Стрик: 0 дней" // заглушка, потом из базы
            profileMaxStreak.text = "🏆 Лучший стрик: 0 дней" // заглушка

        } catch (e: Exception) {
            Log.e("ProfileActivity", "Ошибка при загрузке профиля: ${e.message}", e)
            profileName.text = "Ошибка загрузки"
            profileLogin.text = e.localizedMessage ?: "Неизвестная ошибка"
            profileXP.text = "⚡ — XP"
        }
    }
}