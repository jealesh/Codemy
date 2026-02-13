package com.inc.codemy

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class LessonsAdapter(
    private var lessons: List<Lesson>
) : RecyclerView.Adapter<LessonsAdapter.LessonViewHolder>() {

    class LessonViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.textLessonTitle)
        val progress: TextView = view.findViewById(R.id.textLessonProgress)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LessonViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_lesson, parent, false)
        return LessonViewHolder(view)
    }

    override fun onBindViewHolder(holder: LessonViewHolder, position: Int) {
        val lesson = lessons[position]
        holder.title.text = lesson.title
        holder.progress.text = "${lesson.progress}%"
    }
    fun updateLessons(newLessons: List<Lesson>) {
        // Простой способ — если список сильно меняется
        // В реальном проекте лучше использовать DiffUtil
        lessons = newLessons
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = lessons.size
}
