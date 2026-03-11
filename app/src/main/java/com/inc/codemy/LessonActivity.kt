package com.inc.codemy

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.inc.codemy.models.LessonResponse
import com.inc.codemy.network.ApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LessonActivity : AppCompatActivity(), CompletionListener {

    private lateinit var viewPager: ViewPager2
    private lateinit var tabLayout: TabLayout
    private lateinit var progressLesson: ProgressBar
    private lateinit var tvLessonProgress: TextView
    lateinit var adapter: LessonSectionsPagerAdapter
    private var lessonId: Long = -1L
    private var userId: Long = -1L
    private var totalSections: Int = 0

    // Храним состояние цветов для каждого таба
    val tabColors = mutableMapOf<Int, Boolean>() // position -> isCompleted
    private var currentTabPosition: Int = 0 // Текущий выбранный таб

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lesson)

        viewPager = findViewById(R.id.viewPager)
        tabLayout = findViewById(R.id.tabLayout)
        progressLesson = findViewById(R.id.progressLesson)
        tvLessonProgress = findViewById(R.id.tvLessonProgress)

        lessonId = intent.getLongExtra("LESSON_ID", -1L)
        val title = intent.getStringExtra("LESSON_TITLE") ?: "Урок"

        val titleTextView = findViewById<TextView>(R.id.tvLessonTitle)
        titleTextView.text = title

        // Добавляем клик на заголовок для возврата на главную
        findViewById<LinearLayout>(R.id.headerPanel).setOnClickListener {
            finish()
        }

        if (lessonId == -1L) {
            Toast.makeText(this, "Урок не найден", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Получаем userId из SharedPreferences
        val sharedPref = getSharedPreferences("user_data", MODE_PRIVATE)
        userId = sharedPref.getLong("user_id", -1L)
        if (userId == -1L) {
            Toast.makeText(this, "Пользователь не авторизован", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Оптимизация: ограничиваем количество предварительно загружаемых страниц
        viewPager.offscreenPageLimit = 1

        loadLesson()
    }

    private fun loadLesson() {
        lifecycleScope.launch {
            try {
                val lesson = withContext(Dispatchers.IO) {
                    ApiClient.apiService.getLessonContent(lessonId, userId)
                }

                val count = lesson.content.size
                totalSections = count

                findViewById<TextView>(R.id.tvLessonTitle)?.text = lesson.title

                // Загружаем прогресс упражнений для этого урока
                val exercisesProgress = withContext(Dispatchers.IO) {
                    try {
                        ApiClient.apiService.getExercisesProgress(userId, lessonId)
                    } catch (e: Exception) {
                        null
                    }
                }

                // Создаём множество ID выполненных упражнений для быстрого поиска
                val completedExerciseIds: Set<Long> = exercisesProgress?.completedExerciseIds?.toSet() ?: emptySet()

                // Настраиваем ViewPager
                adapter = LessonSectionsPagerAdapter(
                    this@LessonActivity,
                    lesson.content,
                    userId,
                    lessonId,
                    completedExerciseIds
                )
                adapter.setCompletionListener(this@LessonActivity)
                viewPager.adapter = adapter

                // Инициализируем все табы
                var completedCount = 0
                for (i in 0 until lesson.content.size) {
                    val section = lesson.content[i]
                    val isCompleted = section.id != null && completedExerciseIds.contains(section.id)
                    tabColors[i] = isCompleted
                    if (isCompleted) completedCount++
                }

                // Обновляем прогресс урока
                updateLessonProgress(completedCount)

                // Связываем TabLayout с ViewPager (номера карточек)
                TabLayoutMediator(tabLayout, viewPager) { tab, position ->
                    tab.text = "${position + 1}"
                    val isCompleted = tabColors[position] ?: false
                    val isCurrent = position == currentTabPosition
                    updateTabColor(tab, position, isCompleted, isCurrent)
                }.attach()

                // Добавляем listener для восстановления цвета при переключении
                tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
                    override fun onTabSelected(tab: TabLayout.Tab?) {
                        tab?.let {
                            val position = it.position
                            currentTabPosition = position
                            refreshAllTabs()
                        }
                    }
                    override fun onTabUnselected(tab: TabLayout.Tab?) {}
                    override fun onTabReselected(tab: TabLayout.Tab?) {
                        tab?.let {
                            val position = it.position
                            currentTabPosition = position
                            refreshAllTabs()
                        }
                    }
                })

            } catch (e: Exception) {
                Toast.makeText(this@LessonActivity, "Ошибка: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onExerciseCompleted(position: Int, isCompleted: Boolean) {
        tabColors[position] = isCompleted
        refreshAllTabs()
        
        // Считаем количество выполненных
        val completedCount = tabColors.count { it.value }
        updateLessonProgress(completedCount)
    }

    /**
     * Обновляет прогресс урока (процент и прогресс бар)
     */
    private fun updateLessonProgress(completedCount: Int) {
        if (totalSections <= 0) return
        
        val percentage = (completedCount * 100) / totalSections
        tvLessonProgress.text = "$percentage%"
        progressLesson.progress = percentage
    }

    /**
     * Обновляет все табы - только текущий имеет подчёркивание
     */
    private fun refreshAllTabs() {
        for (i in 0 until tabLayout.tabCount) {
            val t = tabLayout.getTabAt(i)
            val color = tabColors[i] ?: false
            val isCurrentTab = i == currentTabPosition
            t?.let { updateTabColor(it, i, color, isCurrentTab) }
        }
    }

    private fun updateTabColor(tab: TabLayout.Tab, position: Int, isCompleted: Boolean, isCurrent: Boolean) {
        val color = if (isCompleted) {
            ContextCompat.getColor(this, R.color.colorSuccess)
        } else {
            ContextCompat.getColor(this, R.color.colorTextPrimary)
        }

        // Устанавливаем цвет и подчёркивание через custom view таба
        val customView = layoutInflater.inflate(R.layout.tab_custom, null)
        val textView = customView.findViewById<TextView>(R.id.tabText)
        val underline = customView.findViewById<View>(R.id.tabUnderline)

        textView.text = "${position + 1}"
        textView.setTextColor(color)
        underline.visibility = if (isCurrent) View.VISIBLE else View.GONE

        tab.customView = customView
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
            Toast.makeText(this, "Урок завершён!", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}
