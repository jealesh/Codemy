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
    private var userId: Long = 1L // TODO: брать из SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_league)

        listView = findViewById(R.id.leagueListView)
        textDaysLeft = findViewById(R.id.textDaysLeft)
        textLeagueName = findViewById(R.id.textLeagueName)

        // Загружаем данные из API
        loadLeaderboard()

        // Нижняя навигация
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
        lifecycleScope.launch {
            try {
                val leaderboard = withContext(Dispatchers.IO) {
                    ApiClient.apiService.getWeeklyLeaderboard(userId)
                }

                // Обновляем UI
                textDaysLeft.text = "${leaderboard.daysRemaining} дн. до завершения недели"
                textLeagueName.text = "Еженедельный рейтинг"

                // Создаём список с выделением пользователя
                val allUsers = leaderboard.users.toMutableList()
                
                // Если пользователя нет в топе, добавляем его в конец
                val userRank = leaderboard.userRank
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
}
