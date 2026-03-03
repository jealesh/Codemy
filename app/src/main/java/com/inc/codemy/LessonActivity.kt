package com.inc.codemy

import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.inc.codemy.models.LessonResponse
import com.inc.codemy.network.ApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LessonActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var tabLayout: TabLayout
    private var lessonId: Long = -1L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lesson)

        viewPager = findViewById(R.id.viewPager)
        tabLayout = findViewById(R.id.tabLayout)

        lessonId = intent.getLongExtra("LESSON_ID", -1L)
        val title = intent.getStringExtra("LESSON_TITLE") ?: "Урок"

        findViewById<TextView>(R.id.tvLessonTitle)?.text = title

        if (lessonId == -1L) {
            Toast.makeText(this, "Урок не найден", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        loadLesson()
    }

    private fun loadLesson() {
        lifecycleScope.launch {
            try {
                val lesson = withContext(Dispatchers.IO) {
                    ApiClient.apiService.getLessonContent(lessonId, 1L) // userId = 1
                }

                val count = lesson.content.size
                Toast.makeText(this@LessonActivity, "Загружено карточек: $count", Toast.LENGTH_LONG).show()

                findViewById<TextView>(R.id.tvLessonTitle)?.text = "${lesson.title} ($count карточек)"

                // Настраиваем ViewPager
                viewPager.adapter = LessonSectionsPagerAdapter(this@LessonActivity, lesson.content)

                // Связываем TabLayout с ViewPager (номера карточек)
                TabLayoutMediator(tabLayout, viewPager) { tab, position ->
                    tab.text = "${position + 1}"
                }.attach()

            } catch (e: Exception) {
                Toast.makeText(this@LessonActivity, "Ошибка: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    /**
     * Переход на следующую карточку
     */
    fun goToNextCard() {
        val currentItem = viewPager.currentItem
        val adapter = viewPager.adapter
        if (adapter != null && currentItem < adapter.itemCount - 1) {
            viewPager.setCurrentItem(currentItem + 1, true)
        } else {
            // Это последняя карточка - завершаем урок
            Toast.makeText(this, "Урок завершён!", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}