package com.wp10.launcher

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.wp10.launcher.databinding.FragmentHomeBinding
import com.wp10.launcher.model.TileData
import com.wp10.launcher.model.TileSize
import com.wp10.launcher.util.TileManager
import com.wp10.launcher.view.TileGridView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var tileManager: TileManager
    lateinit var tileGrid: TileGridView

    private val clockHandler = Handler(Looper.getMainLooper())
    private val timeFormat = SimpleDateFormat("h:mm", Locale.getDefault())

    private val tileOptionsLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            if (data != null) {
                val action = data.getStringExtra("action")
                val tile = data.getParcelableExtra<TileData>("tile")
                when (action) {
                    "unpin" -> tile?.let {
                        tileGrid.removeTile(it.id)
                        tileManager.unpinTile(it.id)
                    }
                    "resize" -> tile?.let {
                        val sizeLabel = data.getStringExtra("size") ?: return@registerForActivityResult
                        val newSize = TileSize.fromLabel(sizeLabel)
                        tileGrid.updateTileSize(it.id, newSize)
                        it.tileSize = newSize
                        tileManager.updateTile(it)
                        saveTiles()
                    }
                }
            }
        }
        if (::tileGrid.isInitialized) {
            tileGrid.setEditMode(false)
            saveTiles()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        tileManager = TileManager(requireContext())
        try {
            setupTileGrid()
        } catch (e: Exception) {
            // Grid setup failed — show empty grid to avoid crash loop
        }
        startClock()
    }

    override fun onResume() {
        super.onResume()
        if (::tileGrid.isInitialized) {
            try {
                val freshTiles = tileManager.loadTiles()
                val currentIds = tileGrid.getTilesData().map { it.id }
                val freshIds = freshTiles.map { it.id }
                if (currentIds != freshIds) {
                    tileGrid.setTiles(freshTiles, tileManager)
                }
            } catch (e: Exception) { }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        clockHandler.removeCallbacksAndMessages(null)
        _binding = null
    }

    private fun setupTileGrid() {
        tileGrid = TileGridView(requireContext())
        binding.tileContainer.addView(tileGrid)
        val savedTiles = tileManager.loadTiles()
        tileGrid.setTiles(savedTiles, tileManager)

        tileGrid.onTileClick = { tile -> launchTile(tile) }
        tileGrid.onTileOptions = { tile, _ ->
            val intent = Intent(requireContext(), TileOptionsActivity::class.java).apply {
                putExtra("tile", tile)
            }
            tileOptionsLauncher.launch(intent)
        }
        tileGrid.onTileUnpin = { tile ->
            tileManager.unpinTile(tile.id)
        }
        tileGrid.onEditModeChanged = { edit ->
            (activity as? LauncherActivity)?.setViewPagerEnabled(!edit)
        }
        tileGrid.onTileMoved = { saveTiles() }
    }

    private fun startClock() {
        val clockRunnable = object : Runnable {
            override fun run() {
                _binding?.tvTime?.text = timeFormat.format(Date())
                clockHandler.postDelayed(this, 1000)
            }
        }
        clockHandler.post(clockRunnable)
    }

    private fun launchTile(tile: TileData) {
        try {
            val launchIntent = requireContext().packageManager
                .getLaunchIntentForPackage(tile.packageName)
                ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (launchIntent != null) {
                startActivity(launchIntent)
            } else if (tile.activityName.isNotEmpty()) {
                startActivity(Intent().apply {
                    component = ComponentName(tile.packageName, tile.activityName)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                })
            }
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Cannot open ${tile.label}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveTiles() {
        if (::tileGrid.isInitialized) {
            tileManager.saveTiles(tileGrid.getTilesData())
        }
    }

    fun updateBadge(packageName: String, count: Int) {
        updateLiveTile(packageName, count, "", "")
    }

    fun updateLiveTile(packageName: String, count: Int, sender: String = "", preview: String = "") {
        if (::tileGrid.isInitialized) tileGrid.updateLiveTile(packageName, count, sender, preview)
    }

    fun updatePhotoTile(packageName: String, uri: String?) {
        if (::tileGrid.isInitialized) tileGrid.updatePhotoTile(packageName, uri)
    }

    fun updateContactsTile(packageName: String, uris: List<String>) {
        if (::tileGrid.isInitialized) tileGrid.updateContactsTile(packageName, uris)
    }

    fun refreshAccentColors() {
        if (::tileGrid.isInitialized) tileGrid.refreshAccentColors()
    }

    fun onBackPressedInActivity(): Boolean {
        if (::tileGrid.isInitialized && tileGrid.isEditMode) {
            tileGrid.setEditMode(false)
            saveTiles()
            return true
        }
        return false
    }
}
