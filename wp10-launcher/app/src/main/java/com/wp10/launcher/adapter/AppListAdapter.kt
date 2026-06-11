package com.wp10.launcher.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.wp10.launcher.R
import com.wp10.launcher.model.AppInfo

class AppListAdapter(
    private val onAppClick: (AppInfo) -> Unit,
    private val onAppLongClick: (AppInfo) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_HEADER = 0
        private const val VIEW_TYPE_APP = 1
    }

    private val items = mutableListOf<ListItem>()

    sealed class ListItem {
        data class Header(val letter: String) : ListItem()
        data class App(val info: AppInfo) : ListItem()
    }

    fun setApps(apps: List<AppInfo>) {
        items.clear()
        var lastLetter = ""
        apps.sortedBy { it.label.uppercase() }.forEach { app ->
            val letter = app.label.firstOrNull()?.uppercaseChar()?.toString() ?: "#"
            val group = if (letter[0].isLetter()) letter else "#"
            if (group != lastLetter) {
                items.add(ListItem.Header(group))
                lastLetter = group
            }
            items.add(ListItem.App(app))
        }
        notifyDataSetChanged()
    }

    fun getFirstPositionForLetter(letter: String): Int {
        return items.indexOfFirst { it is ListItem.Header && it.letter == letter }
    }

    override fun getItemViewType(position: Int) = when (items[position]) {
        is ListItem.Header -> VIEW_TYPE_HEADER
        is ListItem.App -> VIEW_TYPE_APP
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_HEADER -> HeaderVH(
                LayoutInflater.from(parent.context).inflate(R.layout.item_app_header, parent, false)
            )
            else -> AppVH(
                LayoutInflater.from(parent.context).inflate(R.layout.item_app, parent, false)
            )
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is ListItem.Header -> (holder as HeaderVH).bind(item.letter)
            is ListItem.App -> (holder as AppVH).bind(item.info)
        }
    }

    override fun getItemCount() = items.size

    inner class HeaderVH(view: View) : RecyclerView.ViewHolder(view) {
        private val tvLetter: TextView = view.findViewById(R.id.tvLetter)
        fun bind(letter: String) { tvLetter.text = letter }
    }

    inner class AppVH(view: View) : RecyclerView.ViewHolder(view) {
        private val ivIcon: ImageView = view.findViewById(R.id.ivAppIcon)
        private val tvName: TextView = view.findViewById(R.id.tvAppName)

        fun bind(app: AppInfo) {
            ivIcon.setImageDrawable(app.icon)
            tvName.text = app.label
            itemView.setOnClickListener { onAppClick(app) }
            itemView.setOnLongClickListener {
                onAppLongClick(app)
                true
            }
        }
    }
}
