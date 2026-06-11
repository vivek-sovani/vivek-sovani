package com.wp10.launcher

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.wp10.launcher.adapter.AppListAdapter
import com.wp10.launcher.databinding.ActivityAppDrawerBinding
import com.wp10.launcher.model.AppInfo
import com.wp10.launcher.model.TileData
import com.wp10.launcher.model.TileSize
import com.wp10.launcher.util.AppLoader
import com.wp10.launcher.util.TileManager

class AppDrawerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAppDrawerBinding
    private lateinit var adapter: AppListAdapter
    private lateinit var tileManager: TileManager
    private var allApps: List<AppInfo> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAppDrawerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        tileManager = TileManager(this)

        setupRecyclerView()
        setupSearch()
        setupLetterIndex()
        loadApps()
    }

    private fun setupRecyclerView() {
        adapter = AppListAdapter(
            onAppClick = { app -> launchApp(app) },
            onAppLongClick = { app -> pinToStart(app) }
        )
        binding.recyclerApps.apply {
            layoutManager = LinearLayoutManager(this@AppDrawerActivity)
            adapter = this@AppDrawerActivity.adapter
        }
    }

    private fun setupSearch() {
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val query = s?.toString()?.trim() ?: ""
                filterApps(query)
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    private fun setupLetterIndex() {
        val letters = ('A'..'Z').map { it.toString() }
        val lm = binding.letterIndex.layoutManager as? LinearLayoutManager
            ?: LinearLayoutManager(this).also { binding.letterIndex.layoutManager = it }

        val letterAdapter = LetterIndexAdapter(letters) { letter ->
            scrollToLetter(letter)
        }
        binding.letterIndex.adapter = letterAdapter
    }

    private fun loadApps() {
        val pinned = tileManager.loadTiles().map { it.packageName }.toSet()
        Thread {
            allApps = AppLoader.loadApps(this, pinned)
            runOnUiThread {
                displayApps(allApps)
            }
        }.start()
    }

    private fun filterApps(query: String) {
        val filtered = if (query.isBlank()) {
            allApps
        } else {
            allApps.filter { it.label.contains(query, ignoreCase = true) }
        }
        displayApps(filtered)
    }

    private fun displayApps(apps: List<AppInfo>) {
        adapter.setApps(apps)
    }

    private fun scrollToLetter(letter: String) {
        val pos = adapter.getFirstPositionForLetter(letter)
        if (pos >= 0) {
            (binding.recyclerApps.layoutManager as LinearLayoutManager)
                .scrollToPositionWithOffset(pos, 0)
        }
    }

    private fun launchApp(app: AppInfo) {
        try {
            val intent = Intent().apply {
                component = android.content.ComponentName(app.packageName, app.activityName)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
        } catch (e: Exception) {
            val intent = packageManager.getLaunchIntentForPackage(app.packageName)
            if (intent != null) startActivity(intent)
        }
    }

    private fun pinToStart(app: AppInfo) {
        val tile = TileData(
            packageName = app.packageName,
            activityName = app.activityName,
            label = app.label,
            tileSize = TileSize.MEDIUM
        )
        tileManager.pinTile(tile)
        android.widget.Toast.makeText(this, "${app.label} pinned to Start", android.widget.Toast.LENGTH_SHORT).show()
        loadApps()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }

    // Simple letter index adapter
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
