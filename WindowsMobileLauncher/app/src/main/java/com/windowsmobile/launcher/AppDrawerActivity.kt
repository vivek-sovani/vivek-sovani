package com.windowsmobile.launcher

import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.SectionIndexer
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.windowsmobile.launcher.databinding.ActivityAppDrawerBinding

class AppDrawerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAppDrawerBinding
    private lateinit var appAdapter: AppListAdapter
    private val allApps = mutableListOf<AppInfo>()
    private val filteredApps = mutableListOf<AppInfo>()

    data class AppInfo(
        val label: String,
        val packageName: String,
        val activityName: String,
        val icon: android.graphics.drawable.Drawable?,
        val isHeader: Boolean = false,
        val headerLetter: String = ""
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        )
        window.statusBarColor = Color.TRANSPARENT

        binding = ActivityAppDrawerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        setupSearch()
        loadApps()

        // Swipe down to close
        binding.root.setOnClickListener {
            // do nothing, prevent close on background tap
        }
    }

    private fun setupRecyclerView() {
        appAdapter = AppListAdapter(filteredApps) { appInfo ->
            launchApp(appInfo)
        }
        binding.appListRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@AppDrawerActivity)
            adapter = appAdapter
        }
    }

    private fun setupSearch() {
        binding.searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                filterApps(s?.toString() ?: "")
            }
        })
    }

    private fun loadApps() {
        Thread {
            val pm = packageManager
            val intent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            val resolveInfos = pm.queryIntentActivities(intent, PackageManager.MATCH_ALL)
            val apps = resolveInfos
                .filter { it.activityInfo.packageName != packageName }
                .map { ri ->
                    AppInfo(
                        label = ri.loadLabel(pm).toString(),
                        packageName = ri.activityInfo.packageName,
                        activityName = ri.activityInfo.name,
                        icon = ri.loadIcon(pm)
                    )
                }
                .sortedBy { it.label.lowercase() }

            // Build grouped list with alphabet headers
            val grouped = mutableListOf<AppInfo>()
            var lastLetter = ""
            apps.forEach { app ->
                val firstLetter = app.label.first().uppercaseChar().toString()
                if (firstLetter != lastLetter && firstLetter.matches(Regex("[A-Z]"))) {
                    grouped.add(AppInfo("", "", "", null, isHeader = true, headerLetter = firstLetter))
                    lastLetter = firstLetter
                } else if (lastLetter.isEmpty() || (firstLetter != lastLetter && !firstLetter.matches(Regex("[A-Z]")))) {
                    if (lastLetter != "#") {
                        grouped.add(AppInfo("", "", "", null, isHeader = true, headerLetter = "#"))
                        lastLetter = "#"
                    }
                }
                grouped.add(app)
            }

            allApps.clear()
            allApps.addAll(grouped)

            runOnUiThread {
                filteredApps.clear()
                filteredApps.addAll(allApps)
                appAdapter.notifyDataSetChanged()
            }
        }.start()
    }

    private fun filterApps(query: String) {
        filteredApps.clear()
        if (query.isEmpty()) {
            filteredApps.addAll(allApps)
        } else {
            val lowerQuery = query.lowercase()
            // Filter and rebuild without headers when searching
            allApps.filter { !it.isHeader && it.label.lowercase().contains(lowerQuery) }
                .also { filteredApps.addAll(it) }
        }
        appAdapter.notifyDataSetChanged()
    }

    private fun launchApp(appInfo: AppInfo) {
        try {
            val intent = packageManager.getLaunchIntentForPackage(appInfo.packageName)
                ?: Intent().apply {
                    setClassName(appInfo.packageName, appInfo.activityName)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            startActivity(intent)
            finish()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.no_anim, R.anim.slide_down)
    }

    override fun onBackPressed() {
        finish()
    }

    class AppListAdapter(
        private val apps: List<AppInfo>,
        private val onAppClick: (AppInfo) -> Unit
    ) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        companion object {
            const val TYPE_HEADER = 0
            const val TYPE_APP = 1
        }

        override fun getItemViewType(position: Int): Int {
            return if (apps[position].isHeader) TYPE_HEADER else TYPE_APP
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            return if (viewType == TYPE_HEADER) {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_app_header, parent, false)
                HeaderViewHolder(view)
            } else {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_app_list, parent, false)
                AppViewHolder(view)
            }
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            val app = apps[position]
            if (holder is HeaderViewHolder) {
                holder.letter.text = app.headerLetter
            } else if (holder is AppViewHolder) {
                holder.icon.setImageDrawable(app.icon)
                holder.label.text = app.label
                holder.itemView.setOnClickListener { onAppClick(app) }
            }
        }

        override fun getItemCount() = apps.size

        inner class HeaderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val letter: TextView = view.findViewById(R.id.headerLetter)
        }

        inner class AppViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val icon: ImageView = view.findViewById(R.id.appIcon)
            val label: TextView = view.findViewById(R.id.appLabel)
        }
    }
}
