package com.inc.codemy

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class ProfileActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        val navHome = findViewById<TextView>(R.id.navHome)
        val navProfile = findViewById<TextView>(R.id.navProfile)
        val btnLogout = findViewById<TextView>(R.id.btnLogout)

        // Переход на главный экран
        navHome.setOnClickListener {
            val intent = Intent(this, MainScreenActivity::class.java)
            startActivity(intent)
            finish()
        }

        // Можно сделать активной текущую вкладку профиля
        navProfile.setOnClickListener {
            // Здесь можно просто ничего не делать или прокрутку вверх
        }
        btnLogout.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }

        val btnSandbox = findViewById<TextView>(R.id.navSandbox)

        btnSandbox.setOnClickListener {
            val intent = Intent(this, SandboxActivity::class.java)
            startActivity(intent)
        }

        val btnTrophy = findViewById<TextView>(R.id.navTrophy)
        btnTrophy.setOnClickListener {
            val intent = Intent(this, LeagueActivity::class.java)
            startActivity(intent)
        }

    }
}
