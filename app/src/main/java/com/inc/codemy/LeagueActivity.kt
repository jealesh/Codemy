package com.inc.codemy

import android.content.Intent
import android.os.Bundle
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class LeagueActivity : AppCompatActivity() {

    private val users = listOf(
        LeagueUser("Anna", 4321),
        LeagueUser("Ирина", 1870),
        LeagueUser("BurcuSahra", 1548),
        LeagueUser("Andrew", 1491),
        LeagueUser("Елизавета", 1420),
        LeagueUser("Marina", 1388),
        LeagueUser("Pablo", 1350),
        LeagueUser("Ярослав", 1203),
        LeagueUser("Сергей", 980),
        LeagueUser("Julia", 900),
        LeagueUser("Max", 850),
        LeagueUser("Dima", 700),
        LeagueUser("Nika", 600),
        LeagueUser("Ashot", 420),
        LeagueUser("Lena", 390)
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_league)

        val listView = findViewById<ListView>(R.id.leagueListView)
        listView.adapter = LeagueAdapter(this, users)

        // Нижняя навигация
        findViewById<TextView>(R.id.navHome).setOnClickListener {
            startActivity(Intent(this, MainScreenActivity::class.java))
        }
        findViewById<TextView>(R.id.navProfile).setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }
        findViewById<TextView>(R.id.navSandbox).setOnClickListener {
            startActivity(Intent(this, SandboxActivity::class.java))
        }
    }
}
