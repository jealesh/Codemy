package com.inc.codemy

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.inc.codemy.models.LessonSection

class LessonSectionsAdapter(
    private var sections: List<LessonSection> = emptyList()
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        const val TYPE_THEORY = 0
        const val TYPE_ORAL = 1
        const val TYPE_PROGRAMMING = 2
        const val TYPE_MATCHING = 3
    }

    override fun getItemViewType(position: Int): Int {
        return when (sections[position].type) {
            "theory" -> TYPE_THEORY
            "oral_code" -> TYPE_ORAL
            "programming" -> TYPE_PROGRAMMING
            "matching" -> TYPE_MATCHING
            else -> TYPE_THEORY
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_THEORY -> TheoryHolder(inflater.inflate(R.layout.item_theory, parent, false))
            TYPE_ORAL -> OralHolder(inflater.inflate(R.layout.item_oral_code, parent, false))
            TYPE_PROGRAMMING -> ProgrammingHolder(inflater.inflate(R.layout.item_programming, parent, false))
            TYPE_MATCHING -> MatchingHolder(inflater.inflate(R.layout.item_matching, parent, false))
            else -> TheoryHolder(inflater.inflate(R.layout.item_theory, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val section = sections[position]
        when (holder) {
            is TheoryHolder -> holder.text.text = section.text
            is OralHolder -> {
                holder.question.text = section.text
                holder.btnCheck.setOnClickListener {
                    val answer = holder.input.text.toString().trim()
                    if (answer == section.correctAnswer?.trim()) {
                        Toast.makeText(holder.itemView.context, "Правильно!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(holder.itemView.context, "Неправильно. Ответ: ${section.correctAnswer}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            is ProgrammingHolder -> {
                holder.question.text = section.text
                holder.btnCheck.setOnClickListener {
                    val code = holder.input.text.toString().trim()
                    if (code == section.correctAnswer?.trim()) {
                        Toast.makeText(holder.itemView.context, "Правильно!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(holder.itemView.context, "Неправильно. Ответ: ${section.correctAnswer}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            is MatchingHolder -> {
                holder.question.text = section.text
                holder.radioGroup.removeAllViews()
                section.options?.forEachIndexed { index, option ->
                    val radio = RadioButton(holder.itemView.context)
                    radio.text = option
                    radio.id = index
                    holder.radioGroup.addView(radio)
                }
                holder.btnCheck.setOnClickListener {
                    val selectedId = holder.radioGroup.checkedRadioButtonId
                    val selected = section.options?.getOrNull(selectedId)
                    if (selected == section.correctAnswer) {
                        Toast.makeText(holder.itemView.context, "Правильно!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(holder.itemView.context, "Неправильно. Ответ: ${section.correctAnswer}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    override fun getItemCount() = sections.size

    fun update(newSections: List<LessonSection>) {
        sections = newSections
        notifyDataSetChanged()
    }

    class TheoryHolder(view: View) : RecyclerView.ViewHolder(view) {
        val text: TextView = view.findViewById(R.id.textTheory)
    }

    class OralHolder(view: View) : RecyclerView.ViewHolder(view) {
        val question: TextView = view.findViewById(R.id.textQuestion)
        val input: EditText = view.findViewById(R.id.inputAnswer)
        val btnCheck: Button = view.findViewById(R.id.btnCheck)
    }

    class ProgrammingHolder(view: View) : RecyclerView.ViewHolder(view) {
        val question: TextView = view.findViewById(R.id.textQuestion)
        val input: EditText = view.findViewById(R.id.inputCode)
        val btnCheck: Button = view.findViewById(R.id.btnCheck)
    }

    class MatchingHolder(view: View) : RecyclerView.ViewHolder(view) {
        val question: TextView = view.findViewById(R.id.textQuestion)
        val radioGroup: RadioGroup = view.findViewById(R.id.radioGroupOptions)
        val btnCheck: Button = view.findViewById(R.id.btnCheck)
    }
}