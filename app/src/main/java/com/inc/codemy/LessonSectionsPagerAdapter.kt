package com.inc.codemy

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.inc.codemy.models.LessonSection

class LessonSectionsPagerAdapter(
    fa: FragmentActivity,
    private val sections: List<LessonSection>
) : FragmentStateAdapter(fa) {

    override fun getItemCount(): Int = sections.size

    override fun createFragment(position: Int): Fragment {
        val section = sections[position]
        return when (section.type) {
            "theory" -> TheoryFragment.newInstance(section.text)
            "oral_code" -> OralCodeFragment.newInstance(section.text, section.correctAnswer)
            "programming" -> ProgrammingFragment.newInstance(section.text, section.correctAnswer)
            "matching" -> MatchingFragment.newInstance(section.text, section.options, section.correctAnswer)
            else -> TheoryFragment.newInstance("Неизвестный тип: ${section.type}")
        }
    }
}