package com.inc.codemy

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class LessonsAdapter(
    private var lessons: List<Lesson> = emptyList()
) : RecyclerView.Adapter<LessonsAdapter.LessonViewHolder>() {

    val currentLessons: List<Lesson>
        get() = lessons


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

        holder.itemView.setOnClickListener {
            val intent = Intent(holder.itemView.context, LessonActivity::class.java)
            intent.putExtra("LESSON_ID", lesson.id)
            intent.putExtra("LESSON_TITLE", lesson.title)
            holder.itemView.context.startActivity(intent)
        }
    }
    fun updateLessons(newLessons: List<Lesson>) {
        // Простой способ — если список сильно меняется
        // В реальном проекте лучше использовать DiffUtil
        lessons = newLessons
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = lessons.size
}
