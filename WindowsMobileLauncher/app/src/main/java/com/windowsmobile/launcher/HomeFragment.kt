package com.windowsmobile.launcher

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.windowsmobile.launcher.databinding.FragmentHomeBinding

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var tileAdapter: TileAdapter
    private lateinit var tileManager: TileManager
    private val tiles = mutableListOf<AppTile>()
    private val handler = Handler(Looper.getMainLooper())
    private var clockUpdateRunnable: Runnable? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        tileManager = TileManager(requireContext())
        setupTileGrid()
        loadTiles()
        startClockUpdate()
    }

    private fun setupTileGrid() {
        val gridLayoutManager = GridLayoutManager(requireContext(), TileAdapter.COLUMNS)
        gridLayoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                return if (position < tiles.size) {
                    tiles[position].tileSize.columnSpan
                } else {
                    1
                }
            }
        }

        tileAdapter = TileAdapter(
            context = requireContext(),
            tiles = tiles,
            onTileClick = { tile -> launchTile(tile) },
            onTileLongClick = { tile, position -> enterEditMode(tile, position) }
        )

        binding.tileRecyclerView.apply {
            layoutManager = gridLayoutManager
            adapter = tileAdapter
            setHasFixedSize(false)
        }
    }

    private fun loadTiles() {
        val savedTiles = tileManager.loadTiles()
        val tilesToLoad = savedTiles ?: tileManager.getDefaultTiles()

        tiles.clear()
        tiles.addAll(tilesToLoad)

        // Load icons asynchronously
        Thread {
            tilesToLoad.forEach { tile ->
                if (!tile.isSystemTile) {
                    try {
                        val pm = requireContext().packageManager
                        val icon = pm.getApplicationIcon(tile.packageName)
                        tile.icon = icon
                    } catch (e: PackageManager.NameNotFoundException) {
                        // Use default icon
                    }
                }
            }
            activity?.runOnUiThread {
                tileAdapter.notifyDataSetChanged()
            }
        }.start()
    }

    private fun launchTile(tile: AppTile) {
        if (tile.isSystemTile) return

        try {
            val pm = requireContext().packageManager
            val launchIntent = pm.getLaunchIntentForPackage(tile.packageName)
            if (launchIntent != null) {
                startActivity(launchIntent)
            } else {
                // Try direct activity launch
                val intent = Intent().apply {
                    setClassName(tile.packageName, tile.activityName)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                startActivity(intent)
            }
        } catch (e: Exception) {
            Toast.makeText(
                requireContext(),
                "App not installed: ${tile.label}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun enterEditMode(tile: AppTile, position: Int) {
        tileAdapter.setEditMode(true)
        Toast.makeText(requireContext(), "Edit mode: Long press to move tiles", Toast.LENGTH_SHORT).show()
    }

    fun exitEditMode() {
        tileAdapter.setEditMode(false)
        tileManager.saveTiles(tileAdapter.getTiles())
    }

    private fun startClockUpdate() {
        clockUpdateRunnable = object : Runnable {
            override fun run() {
                tileAdapter.notifyItemChanged(0) // Clock is always first tile
                handler.postDelayed(this, 60000) // Update every minute
            }
        }
        handler.postDelayed(clockUpdateRunnable!!, 60000)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        clockUpdateRunnable?.let { handler.removeCallbacks(it) }
        _binding = null
    }

    companion object {
        fun newInstance() = HomeFragment()
    }
}
