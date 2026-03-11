package com.inc.codemy

import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.inc.codemy.models.LeaderboardUserResponse
import com.inc.codemy.network.ApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LeagueActivity : AppCompatActivity() {

    private lateinit var listView: ListView
    private lateinit var textDaysLeft: TextView
    private lateinit var textLeagueName: TextView
    private var userId: Long = -1L

    // Кэш для leaderboard (ключ: userId)
    private data class LeaderboardCache(
        val data: List<LeaderboardUserResponse>,
        val daysRemaining: Int,
        val userRank: LeaderboardUserResponse?,
        val timestamp: Long
    )
    private var leaderboardCache: LeaderboardCache? = null
    private val CACHE_TTL_MS = 30_000L // 30 секунд

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_league)

        // Получаем userId из SharedPreferences
        val sharedPref = getSharedPreferences("user_data", MODE_PRIVATE)
        userId = sharedPref.getLong("user_id", -1L)
        if (userId == -1L) {
            Toast.makeText(this, "Пользователь не авторизован", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        listView = findViewById(R.id.leagueListView)
        textDaysLeft = findViewById(R.id.textDaysLeft)
        textLeagueName = findViewById(R.id.textLeagueName)

        // Загружаем данные из API
        loadLeaderboard()

        // Нижняя навигация
        setupBottomNavigation()
    }

    private fun setupBottomNavigation() {
        findViewById<LinearLayout>(R.id.navHome).setOnClickListener {
            startActivity(Intent(this, MainScreenActivity::class.java))
        }
        findViewById<LinearLayout>(R.id.navProfile).setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }
        findViewById<LinearLayout>(R.id.navSandbox).setOnClickListener {
            startActivity(Intent(this, SandboxActivity::class.java))
        }
    }

    private fun loadLeaderboard() {
        // Проверяем кэш
        val cache = leaderboardCache
        if (cache != null && System.currentTimeMillis() - cache.timestamp < CACHE_TTL_MS) {
            updateLeaderboardUI(cache.data, cache.daysRemaining, cache.userRank)
            return
        }

        lifecycleScope.launch {
            try {
                val leaderboard = withContext(Dispatchers.IO) {
                    ApiClient.apiService.getWeeklyLeaderboard(userId)
                }

                // Кэшируем результат
                leaderboardCache = LeaderboardCache(
                    data = leaderboard.users,
                    daysRemaining = leaderboard.daysRemaining,
                    userRank = leaderboard.userRank,
                    timestamp = System.currentTimeMillis()
                )

                updateLeaderboardUI(leaderboard.users, leaderboard.daysRemaining, leaderboard.userRank)

            } catch (e: Exception) {
                Toast.makeText(
                    this@LeagueActivity,
                    "Ошибка загрузки рейтинга: ${e.localizedMessage}",
                    Toast.LENGTH_LONG
                ).show()

                // Fallback на заглушку
                textDaysLeft.text = "Ошибка загрузки"
            }
        }
    }

    private fun updateLeaderboardUI(
        users: List<LeaderboardUserResponse>,
        daysRemaining: Int,
        userRank: LeaderboardUserResponse?
    ) {
        // Обновляем UI
        textDaysLeft.text = "$daysRemaining дн. до завершения недели"
        textLeagueName.text = "Еженедельный рейтинг"

        // Создаём список с выделением пользователя
        val allUsers = users.toMutableList()

        // Если пользователя нет в топе, добавляем его в конец
        if (userRank != null && allUsers.none { it.userId == userRank.userId }) {
            allUsers.add(userRank)
        }

        // Преобразуем в LeagueUser
        val leagueUsers = allUsers.map { response ->
            LeagueUser(
                name = response.fullName ?: response.username,
                weeklyXP = response.weeklyXp,
                rank = response.rank,
                isCurrentUser = response.userId == userId
            )
        }

        listView.adapter = LeagueAdapter(leagueUsers)
    }
}
