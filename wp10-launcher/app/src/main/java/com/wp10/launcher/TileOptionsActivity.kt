package com.wp10.launcher

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.Window
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import com.wp10.launcher.databinding.ActivityTileOptionsBinding
import com.wp10.launcher.model.TileData
import com.wp10.launcher.model.TileSize
import com.wp10.launcher.util.PrefsHelper

class TileOptionsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTileOptionsBinding
    private var tile: TileData? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
        )
        binding = ActivityTileOptionsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        tile = intent.getParcelableExtra("tile")
        setupUI()
    }

    private fun setupUI() {
        val t = tile ?: return
        binding.tvTileName.text = t.label

        // Transparency toggle
        binding.switchTransparent.isChecked = t.isTransparent
        binding.switchTransparent.setOnCheckedChangeListener { _, checked ->
            val result = Intent().apply {
                putExtra("action", "transparent")
                putExtra("tile", t)
                putExtra("transparent", checked)
            }
            setResult(RESULT_OK, result)
            finish()
        }

        // Size options
        val sizeButtons = mapOf(
            TileSize.SMALL to binding.btnSizeSmall,
            TileSize.MEDIUM to binding.btnSizeMedium,
            TileSize.WIDE to binding.btnSizeWide,
            TileSize.LARGE to binding.btnSizeLarge
        )

        sizeButtons.forEach { (size, btn) ->
            if (size == t.tileSize) {
                btn.isSelected = true
                btn.alpha = 1f
            }
            btn.setOnClickListener {
                val result = Intent().apply {
                    putExtra("action", "resize")
                    putExtra("tile", t)
                    putExtra("size", size.label)
                }
                setResult(RESULT_OK, result)
                finish()
            }
        }

        // Unpin button
        binding.btnUnpin.setOnClickListener {
            val result = Intent().apply {
                putExtra("action", "unpin")
                putExtra("tile", t)
            }
            setResult(RESULT_OK, result)
            finish()
        }

        // Cancel
        binding.btnCancel.setOnClickListener {
            setResult(RESULT_CANCELED)
            finish()
        }

        // Dismiss on outside touch
        binding.optionsRoot.setOnClickListener {
            setResult(RESULT_CANCELED)
            finish()
        }
        binding.optionsCard.setOnClickListener { /* consume - don't close */ }
    }
}
