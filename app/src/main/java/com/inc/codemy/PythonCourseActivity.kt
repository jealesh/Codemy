package com.inc.codemy

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class PythonCourseActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_python_course)

        val recycler = findViewById<RecyclerView>(R.id.lessonsRecycler)

        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = LessonsAdapter(
            listOf(
                Lesson("Ввод и вывод данных", 0),
                Lesson("Типы данных", 0),
                Lesson("Условный оператор", 0),
                Lesson("Циклы for и while", 0),
                Lesson("Строки", 0),
                Lesson("Списки", 0),
                Lesson("Функции", 0)
            )
        )
    }
}
