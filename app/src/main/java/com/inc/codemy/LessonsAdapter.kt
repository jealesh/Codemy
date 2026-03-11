package com.inc.codemy

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

class LessonsAdapter : ListAdapter<Lesson, LessonsAdapter.LessonViewHolder>(LessonDiffCallback()) {

    val currentLessons: List<Lesson>
        get() = currentList


    class LessonViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.textLessonTitle)
        val progress: TextView = view.findViewById(R.id.textLessonProgress)
        val progressCircle: ProgressBar = view.findViewById(R.id.progressLessonCircle)
        val completionBadge: LinearLayout = view.findViewById(R.id.completionBadge)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LessonViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_lesson, parent, false)
        return LessonViewHolder(view)
    }

    override fun onBindViewHolder(holder: LessonViewHolder, position: Int) {
        val lesson = getItem(position)
        holder.title.text = lesson.title
        holder.progress.text = "${lesson.progress}%"
        holder.progressCircle.progress = lesson.progress

        android.util.Log.d("LessonsAdapter", "Урок: ${lesson.title}, Прогресс: ${lesson.progress}")

        // Показываем бейдж успеха только для завершённых уроков (100%)
        if (lesson.progress >= 100) {
            holder.completionBadge.visibility = View.VISIBLE
            holder.progress.visibility = View.GONE
            holder.progressCircle.visibility = View.GONE
        } else {
            holder.completionBadge.visibility = View.GONE
            holder.progress.visibility = View.VISIBLE
            holder.progressCircle.visibility = View.VISIBLE
        }

        holder.itemView.setOnClickListener {
            val intent = Intent(holder.itemView.context, LessonActivity::class.java)
            intent.putExtra("LESSON_ID", lesson.id)
            intent.putExtra("LESSON_TITLE", lesson.title)
            holder.itemView.context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int = currentList.size

    fun updateLessons(newLessons: List<Lesson>) {
        // Полностью заменяем список и уведомляем адаптер
        submitList(newLessons.toList()) {
            // Гарантируем обновление после завершения submitList
            notifyDataSetChanged()
        }
    }

    private class LessonDiffCallback : DiffUtil.ItemCallback<Lesson>() {
        override fun areItemsTheSame(oldItem: Lesson, newItem: Lesson): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Lesson, newItem: Lesson): Boolean {
            return oldItem.title == newItem.title &&
                    oldItem.progress == newItem.progress
        }
    }
}
