package com.inc.codemy

import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.inc.codemy.models.LessonResponse
import com.inc.codemy.network.ApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LessonActivity : AppCompatActivity() {

    private lateinit var recycler: RecyclerView
    private lateinit var tvTitle: TextView
    private var lessonId: Long = -1L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lesson)

        recycler = findViewById(R.id.recyclerSections)
        tvTitle = findViewById(R.id.tvLessonTitle)

        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = LessonSectionsAdapter(emptyList())

        lessonId = intent.getLongExtra("LESSON_ID", -1L)
        val title = intent.getStringExtra("LESSON_TITLE") ?: "Урок без названия"

        tvTitle.text = title

        if (lessonId == -1L) {
            Toast.makeText(this, "Урок не найден (ID = -1)", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        loadLesson()
    }

    private fun loadLesson() {
        lifecycleScope.launch {
            try {
                val lesson = withContext(Dispatchers.IO) {
                    ApiClient.apiService.getLessonContent(lessonId, 1L) // userId = 1 пока
                }

                val count = lesson.content.size
                Toast.makeText(this@LessonActivity, "Загружено карточек: $count", Toast.LENGTH_LONG).show()

                tvTitle.text = "${lesson.title} ($count карточек)"

                (recycler.adapter as LessonSectionsAdapter).update(lesson.content)

                if (count == 0) {
                    Toast.makeText(this@LessonActivity, "В уроке пока нет заданий", Toast.LENGTH_LONG).show()
                }

            } catch (e: Exception) {
                Toast.makeText(this@LessonActivity, "Ошибка загрузки урока: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}