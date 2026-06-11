package com.wp10.launcher

import android.app.WallpaperManager
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.wp10.launcher.databinding.ActivityWallpaperBinding
import com.wp10.launcher.util.PrefsHelper
import java.io.IOException

class WallpaperActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWallpaperBinding

    private val imagePicker = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { setWallpaper(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWallpaperBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnPickImage.setOnClickListener {
            imagePicker.launch(arrayOf("image/*"))
        }

        binding.btnSystemWallpaper.setOnClickListener {
            try {
                val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER)
                startActivity(intent)
            } catch (e: Exception) {
                val intent = Intent(Intent.ACTION_SET_WALLPAPER)
                try { startActivity(intent) } catch (e2: Exception) { }
            }
        }

        binding.btnClear.setOnClickListener {
            PrefsHelper.setWallpaperUri(this, null)
            setResult(RESULT_OK)
            finish()
        }

        binding.btnBack.setOnClickListener { finish() }
    }

    private fun setWallpaper(uri: Uri) {
        try {
            contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } catch (e: Exception) { }
        try {
            PrefsHelper.setWallpaperUri(this, uri.toString())
            binding.ivPreview.setImageURI(uri)
            setResult(RESULT_OK)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
