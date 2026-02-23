package com.inc.codemy

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment

class TheoryFragment : Fragment() {

    companion object {
        private const val ARG_TEXT = "text"

        fun newInstance(text: String) = TheoryFragment().apply {
            arguments = Bundle().apply { putString(ARG_TEXT, text) }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_theory, container, false)
        view.findViewById<TextView>(R.id.textTheory).text = arguments?.getString(ARG_TEXT) ?: ""
        return view
    }
}