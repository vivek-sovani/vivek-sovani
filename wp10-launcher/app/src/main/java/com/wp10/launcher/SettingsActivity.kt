package com.wp10.launcher

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.wp10.launcher.databinding.ActivitySettingsBinding
import com.wp10.launcher.util.PrefsHelper

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding

    // OpenDocument (not GetContent) so the URI grant can be persisted across reboots
    private val wallpaperPicker = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            try {
                contentResolver.takePersistableUriPermission(
                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (e: Exception) { }
            PrefsHelper.setWallpaperUri(this, uri.toString())
            Glide.with(this).load(uri).centerCrop().into(binding.ivWallpaperPreview)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupWallpaperSection()
        setupAccentColors()
        setupThemeToggle()
        setupTransparencyToggle()
        setupToolbar()
    }

    private fun setupToolbar() {
        binding.btnBack.setOnClickListener {
            setResult(RESULT_OK)
            finish()
        }
    }

    private fun setupWallpaperSection() {
        // Preview current wallpaper
        val uri = PrefsHelper.getWallpaperUri(this)
        if (uri != null) {
            try {
                Glide.with(this).load(Uri.parse(uri)).centerCrop().into(binding.ivWallpaperPreview)
            } catch (e: Exception) {
                showSystemWallpaperPreview()
            }
        } else {
            showSystemWallpaperPreview()
        }

        binding.btnChooseWallpaper.setOnClickListener {
            wallpaperPicker.launch(arrayOf("image/*"))
        }

        binding.btnClearWallpaper.setOnClickListener {
            PrefsHelper.setWallpaperUri(this, null)
            binding.ivWallpaperPreview.setImageURI(null)
            showSystemWallpaperPreview()
        }
    }

    private fun showSystemWallpaperPreview() {
        // Throws SecurityException on Android 13+; fall back to plain black
        try {
            val wm = android.app.WallpaperManager.getInstance(this)
            binding.ivWallpaperPreview.setImageDrawable(wm.drawable)
        } catch (e: Exception) {
            binding.ivWallpaperPreview.setImageDrawable(null)
        }
    }

    private fun setupAccentColors() {
        val currentColor = PrefsHelper.getAccentColor(this)
        val adapter = AccentColorAdapter(PrefsHelper.ACCENT_COLORS, currentColor) { color ->
            PrefsHelper.setAccentColor(this, color)
            setResult(RESULT_OK)
        }
        binding.rvAccentColors.apply {
            layoutManager = GridLayoutManager(this@SettingsActivity, 5)
            this.adapter = adapter
        }
    }

    private fun setupThemeToggle() {
        binding.switchDarkTheme.isChecked = PrefsHelper.isDarkTheme(this)
        binding.switchDarkTheme.setOnCheckedChangeListener { _, checked ->
            PrefsHelper.setDarkTheme(this, checked)
        }
    }

    private fun setupTransparencyToggle() {
        binding.switchStartTransparent.isChecked = PrefsHelper.isStartTransparent(this)
        binding.switchStartTransparent.setOnCheckedChangeListener { _, checked ->
            PrefsHelper.setStartTransparent(this, checked)
            setResult(RESULT_OK)
        }
    }

    // Color swatch adapter
    inner class AccentColorAdapter(
        private val colors: List<Int>,
        private var selected: Int,
        private val onColorPicked: (Int) -> Unit
    ) : RecyclerView.Adapter<AccentColorAdapter.VH>() {

        inner class VH(val frame: FrameLayout) : RecyclerView.ViewHolder(frame)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val size = resources.getDimensionPixelSize(R.dimen.color_swatch_size)
            val frame = FrameLayout(parent.context).apply {
                layoutParams = ViewGroup.MarginLayoutParams(size, size).apply {
                    setMargins(8, 8, 8, 8)
                }
            }
            return VH(frame)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val color = colors[position]
            val circle = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(color)
                if (color == selected) {
                    setStroke(6, Color.WHITE)
                } else {
                    setStroke(0, Color.TRANSPARENT)
                }
            }
            holder.frame.background = circle
            holder.frame.setOnClickListener {
                selected = color
                onColorPicked(color)
                notifyDataSetChanged()
            }
        }

        override fun getItemCount() = colors.size
    }
}
