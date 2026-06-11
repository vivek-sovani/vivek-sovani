package com.windowsmobile.launcher

import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import com.windowsmobile.launcher.databinding.FragmentHomeBinding

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var tileAdapter: TileAdapter
    private lateinit var tileManager: TileManager
    private val tiles = mutableListOf<AppTile>()
    private val handler = Handler(Looper.getMainLooper())
    private var clockUpdateRunnable: Runnable? = null
    private var isEditMode = false

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
                return if (position < tiles.size) tiles[position].tileSize.columnSpan else 1
            }
        }

        tileAdapter = TileAdapter(
            context = requireContext(),
            tiles = tiles,
            onTileClick = { tile, position ->
                if (isEditMode) showTileOptionsDialog(tile, position)
                else launchTile(tile)
            },
            onTileLongClick = { tile, position -> enterEditMode(tile, position) },
            onTileRemove = { position -> removeTile(position) }
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

        Thread {
            tilesToLoad.forEach { tile ->
                if (!tile.isSystemTile) {
                    try {
                        tile.icon = requireContext().packageManager.getApplicationIcon(tile.packageName)
                    } catch (e: PackageManager.NameNotFoundException) {
                        // no icon available
                    }
                }
            }
            activity?.runOnUiThread { tileAdapter.notifyDataSetChanged() }
        }.start()
    }

    private fun launchTile(tile: AppTile) {
        if (tile.isSystemTile) return
        val launchIntent = requireContext().packageManager.getLaunchIntentForPackage(tile.packageName)
        if (launchIntent != null) {
            startActivity(launchIntent)
        } else {
            Toast.makeText(requireContext(), "${tile.label} is not installed", Toast.LENGTH_SHORT).show()
        }
    }

    private fun enterEditMode(tile: AppTile, position: Int) {
        isEditMode = true
        tileAdapter.setEditMode(true)
        (activity as? MainActivity)?.setEditMode(true)
        showTileOptionsDialog(tile, position)
    }

    fun exitEditMode() {
        isEditMode = false
        tileAdapter.setEditMode(false)
        (activity as? MainActivity)?.setEditMode(false)
        tileManager.saveTiles(tileAdapter.getTiles())
    }

    private fun showTileOptionsDialog(tile: AppTile, position: Int) {
        if (position < 0 || position >= tiles.size) return
        val current = tiles[position].tileSize
        val check = "  ✓"
        val options = arrayOf(
            "Small  (1×1)${if (current == TileSize.SMALL) check else ""}",
            "Medium (2×2)${if (current == TileSize.MEDIUM) check else ""}",
            "Wide   (4×2)${if (current == TileSize.WIDE) check else ""}",
            "Large  (4×4)${if (current == TileSize.LARGE) check else ""}",
            "Remove tile"
        )
        AlertDialog.Builder(requireContext(), R.style.WPAlertDialog)
            .setTitle(tile.label)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> resizeTile(position, TileSize.SMALL)
                    1 -> resizeTile(position, TileSize.MEDIUM)
                    2 -> resizeTile(position, TileSize.WIDE)
                    3 -> resizeTile(position, TileSize.LARGE)
                    4 -> removeTile(position)
                }
            }
            .setNeutralButton("Done editing") { _, _ -> exitEditMode() }
            .show()
    }

    private fun resizeTile(position: Int, newSize: TileSize) {
        if (position < 0 || position >= tiles.size) return
        tiles[position].tileSize = newSize
        tileAdapter.notifyDataSetChanged()
        tileManager.saveTiles(tiles)
    }

    private fun removeTile(position: Int) {
        if (position < 0 || position >= tiles.size) return
        tiles.removeAt(position)
        tileAdapter.notifyDataSetChanged()
        tileManager.saveTiles(tiles)
    }

    private fun startClockUpdate() {
        clockUpdateRunnable = object : Runnable {
            override fun run() {
                tileAdapter.notifyItemChanged(0)
                handler.postDelayed(this, 60000)
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
