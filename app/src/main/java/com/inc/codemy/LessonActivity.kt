package com.inc.codemy

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
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
    lateinit var adapter: LessonSectionsPagerAdapter
    private var lessonId: Long = -1L
    private var userId: Long = 1L // TODO: получить из сессии
    
    // Храним состояние цветов для каждого таба
    val tabColors = mutableMapOf<Int, Boolean>() // position -> isCompleted
    private var currentTabPosition: Int = 0 // Текущий выбранный таб

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lesson)

        viewPager = findViewById(R.id.viewPager)
        tabLayout = findViewById(R.id.tabLayout)

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

                // Загружаем прогресс упражнений для этого урока
                val exercisesProgress = withContext(Dispatchers.IO) {
                    try {
                        ApiClient.apiService.getExercisesProgress(1L, lessonId)
                    } catch (e: Exception) {
                        null
                    }
                }

                // Создаём множество ID выполненных упражнений для быстрого поиска
                val completedExerciseIds: Set<Long> = exercisesProgress?.completedExerciseIds?.toSet() ?: emptySet()

                // Настраиваем ViewPager
                adapter = LessonSectionsPagerAdapter(this@LessonActivity, lesson.content, userId, lessonId, completedExerciseIds)
                adapter.setCompletionListener(this@LessonActivity)
                viewPager.adapter = adapter

                // Инициализируем все табы
                for (i in 0 until lesson.content.size) {
                    // Таб выполнен, только если упражнение есть в completedExerciseIds (теория НЕ отмечается сразу)
                    val section = lesson.content[i]
                    val isCompleted = section.id != null && completedExerciseIds.contains(section.id)
                    tabColors[i] = isCompleted
                }
                println("[LessonActivity] Initial tabColors: $tabColors")

                // Связываем TabLayout с ViewPager (номера карточек)
                TabLayoutMediator(tabLayout, viewPager) { tab, position ->
                    tab.text = "${position + 1}"
                    // Устанавливаем цвет таба на основе сохранённого состояния
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
                            val color = tabColors[position] ?: false
                            println("[TabListener] onTabSelected: position=$position, color=$color, tabColors=$tabColors")
                            
                            // Обновляем ВСЕ табы - только текущий должен иметь подчёркивание
                            refreshAllTabs()
                        }
                    }
                    override fun onTabUnselected(tab: TabLayout.Tab?) {}
                    override fun onTabReselected(tab: TabLayout.Tab?) {
                        tab?.let {
                            val position = it.position
                            val color = tabColors[position] ?: false
                            println("[TabListener] onTabReselected: position=$position, color=$color, tabColors=$tabColors")
                            
                            // Обновляем ВСЕ табы
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
        // Сохраняем состояние
        tabColors[position] = isCompleted
        println("[LessonActivity] onExerciseCompleted: position=$position, isCompleted=$isCompleted, tabColors=$tabColors")

        // Обновляем ВСЕ табы
        refreshAllTabs()
    }

    /**
     * Обновляет все табы - только текущий имеет подчёркивание
     */
    private fun refreshAllTabs() {
        for (i in 0 until tabLayout.tabCount) {
            val t = tabLayout.getTabAt(i)
            val color = tabColors[i] ?: false
            val isCurrentTab = i == currentTabPosition
            println("[refreshAllTabs] Tab $i: color=$color, isCurrent=$isCurrentTab")
            t?.let { updateTabColor(it, i, color, isCurrentTab) }
        }
    }

    private fun updateTabColor(tab: TabLayout.Tab, position: Int, isCompleted: Boolean, isCurrent: Boolean) {
        val color = if (isCompleted) {
            ContextCompat.getColor(this, R.color.colorSuccess) // зелёный
        } else {
            ContextCompat.getColor(this, R.color.colorTextPrimary) // белый
        }

        println("[updateTabColor] position=$position, isCompleted=$isCompleted, isCurrent=$isCurrent, colorName=${if (isCompleted) "GREEN" else "WHITE"}")

        // Устанавливаем цвет и подчёркивание через custom view таба
        val customView = layoutInflater.inflate(R.layout.tab_custom, null)
        val textView = customView.findViewById<TextView>(R.id.tabText)
        val underline = customView.findViewById<View>(R.id.tabUnderline)
        
        textView.text = "${position + 1}"
        textView.setTextColor(color)
        
        // Показываем подчёркивание только для текущего таба
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
            // Это последняя карточка - завершаем урок
            Toast.makeText(this, "Урок завершён!", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}