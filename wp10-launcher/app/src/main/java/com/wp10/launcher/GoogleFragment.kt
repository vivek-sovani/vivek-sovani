package com.wp10.launcher

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment

class GoogleFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val root = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.BLACK)
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        val logoText = TextView(requireContext()).apply {
            text = "Google"
            textSize = 52f
            setTextColor(Color.WHITE)
            typeface = Typeface.create("sans-serif-light", Typeface.NORMAL)
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(32) }
        }

        val searchBox = EditText(requireContext()).apply {
            hint = "Search"
            setHintTextColor(Color.argb(128, 255, 255, 255))
            setTextColor(Color.WHITE)
            textSize = 16f
            imeOptions = EditorInfo.IME_ACTION_SEARCH
            inputType = InputType.TYPE_CLASS_TEXT
            background = GradientDrawable().apply {
                setColor(Color.argb(55, 255, 255, 255))
                cornerRadius = dp(24).toFloat()
            }
            val pH = dp(20); val pV = dp(12)
            setPadding(pH, pV, pH, pV)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { marginStart = dp(24); marginEnd = dp(24) }
            setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    launchSearch(text.toString().trim()); true
                } else false
            }
        }

        root.addView(logoText)
        root.addView(searchBox)
        return root
    }

    private fun launchSearch(query: String) {
        try {
            val intent = if (query.isNotEmpty()) {
                Intent(Intent.ACTION_WEB_SEARCH).apply { putExtra("query", query) }
            } else {
                requireContext().packageManager
                    .getLaunchIntentForPackage("com.google.android.googlequicksearchbox")
                    ?: Intent(Intent.ACTION_WEB_SEARCH).apply { putExtra("query", "") }
            }
            startActivity(intent)
        } catch (e: Exception) { }
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()
}
