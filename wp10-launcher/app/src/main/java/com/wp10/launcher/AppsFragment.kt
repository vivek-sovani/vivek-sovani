package com.wp10.launcher

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.wp10.launcher.adapter.AppListAdapter
import com.wp10.launcher.databinding.FragmentAppsBinding
import com.wp10.launcher.model.AppInfo
import com.wp10.launcher.model.TileData
import com.wp10.launcher.model.TileSize
import com.wp10.launcher.util.AppLoader
import com.wp10.launcher.util.TileManager

class AppsFragment : Fragment() {

    private var _binding: FragmentAppsBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: AppListAdapter
    private lateinit var tileManager: TileManager
    private var allApps: List<AppInfo> = emptyList()
    private var appsLoaded = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAppsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        tileManager = TileManager(requireContext())
        try {
            setupRecyclerView()
            setupSearch()
            setupLetterIndex()
        } catch (e: Exception) { }
        if (!appsLoaded) loadApps()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupRecyclerView() {
        adapter = AppListAdapter(
            onAppClick = { app -> launchApp(app) },
            onAppLongClick = { app -> pinToStart(app) }
        )
        binding.recyclerApps.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerApps.adapter = adapter
    }

    private fun setupSearch() {
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                filterApps(s?.toString()?.trim() ?: "")
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    private fun setupLetterIndex() {
        binding.letterIndex.layoutManager = LinearLayoutManager(requireContext())
        val letters = ('A'..'Z').map { it.toString() }
        binding.letterIndex.adapter = LetterIndexAdapter(letters) { letter ->
            val pos = adapter.getFirstPositionForLetter(letter)
            if (pos >= 0) {
                (binding.recyclerApps.layoutManager as? LinearLayoutManager)
                    ?.scrollToPositionWithOffset(pos, 0)
            }
        }
    }

    private fun loadApps() {
        if (_binding == null) return
        binding.progressBar.visibility = View.VISIBLE
        binding.recyclerApps.visibility = View.GONE
        val ctx = requireContext().applicationContext
        Thread {
            val pinned = tileManager.loadTiles().map { it.packageName }.toSet()
            val apps = AppLoader.loadApps(ctx, pinned)
            activity?.runOnUiThread {
                if (_binding != null) {
                    allApps = apps
                    appsLoaded = true
                    adapter.setApps(apps)
                    binding.progressBar.visibility = View.GONE
                    binding.recyclerApps.visibility = View.VISIBLE
                }
            }
        }.start()
    }

    private fun filterApps(query: String) {
        val filtered = if (query.isBlank()) allApps
        else allApps.filter { it.label.contains(query, ignoreCase = true) }
        adapter.setApps(filtered)
    }

    private fun launchApp(app: AppInfo) {
        try {
            val intent = Intent().apply {
                component = android.content.ComponentName(app.packageName, app.activityName)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
        } catch (e: Exception) {
            val intent = requireContext().packageManager.getLaunchIntentForPackage(app.packageName)
            if (intent != null) startActivity(intent)
        }
    }

    private fun pinToStart(app: AppInfo) {
        if (app.isPinned) {
            android.widget.Toast.makeText(requireContext(), "${app.label} is already on Start", android.widget.Toast.LENGTH_SHORT).show()
            return
        }
        val tile = TileData(
            packageName = app.packageName,
            activityName = app.activityName,
            label = app.label,
            tileSize = TileSize.MEDIUM
        )
        tileManager.pinTile(tile)
        adapter.markAsPinned(app.packageName)
        android.widget.Toast.makeText(requireContext(), "${app.label} pinned to Start", android.widget.Toast.LENGTH_SHORT).show()
    }

    inner class LetterIndexAdapter(
        private val letters: List<String>,
        private val onClick: (String) -> Unit
    ) : androidx.recyclerview.widget.RecyclerView.Adapter<LetterIndexAdapter.VH>() {

        inner class VH(view: View) : androidx.recyclerview.widget.RecyclerView.ViewHolder(view) {
            val tv: TextView = view.findViewById(android.R.id.text1)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val tv = TextView(parent.context).apply {
                textSize = 11f
                setTextColor(0xFFFFFFFF.toInt())
                setPadding(8, 4, 8, 4)
                id = android.R.id.text1
            }
            return VH(tv)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            holder.tv.text = letters[position]
            holder.tv.setOnClickListener { onClick(letters[position]) }
        }

        override fun getItemCount() = letters.size
    }
}
