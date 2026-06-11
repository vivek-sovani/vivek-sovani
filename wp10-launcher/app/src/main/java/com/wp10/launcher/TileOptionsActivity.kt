package com.wp10.launcher

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.wp10.launcher.databinding.ActivityTileOptionsBinding
import com.wp10.launcher.model.TileData
import com.wp10.launcher.model.TileSize

class TileOptionsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTileOptionsBinding
    private var tile: TileData? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTileOptionsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        tile = intent.getParcelableExtra("tile")
        setupUI()
    }

    private fun setupUI() {
        val t = tile ?: return
        binding.tvTileName.text = t.label

        // Size swatch buttons — highlight the current size, others dimmed
        val sizeButtons = mapOf(
            TileSize.SMALL to binding.btnSizeSmall as View,
            TileSize.MEDIUM to binding.btnSizeMedium as View,
            TileSize.WIDE to binding.btnSizeWide as View,
            TileSize.LARGE to binding.btnSizeLarge as View
        )

        sizeButtons.forEach { (size, btn) ->
            btn.alpha = if (size == t.tileSize) 1f else 0.45f
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
