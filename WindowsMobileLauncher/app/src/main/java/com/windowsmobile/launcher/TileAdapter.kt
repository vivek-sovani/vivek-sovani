package com.windowsmobile.launcher

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

class TileAdapter(
    private val context: Context,
    private var tiles: MutableList<AppTile>,
    private val onTileClick: (AppTile, Int) -> Unit,
    private val onTileLongClick: (AppTile, Int) -> Unit,
    private val onTileRemove: (Int) -> Unit
) : RecyclerView.Adapter<TileAdapter.TileViewHolder>() {

    companion object {
        const val COLUMNS = 4
        const val VIEW_TYPE_CLOCK = 1
        const val VIEW_TYPE_NORMAL = 0
    }

    private var isEditMode = false
    private val timeFormat = SimpleDateFormat("h:mm", Locale.getDefault())
    private val dateFormat = SimpleDateFormat("EEEE\nMMMM d", Locale.getDefault())

    override fun getItemViewType(position: Int): Int {
        return if (tiles[position].isSystemTile && tiles[position].id == "clock") {
            VIEW_TYPE_CLOCK
        } else {
            VIEW_TYPE_NORMAL
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TileViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_tile, parent, false)
        return TileViewHolder(view)
    }

    override fun onBindViewHolder(holder: TileViewHolder, position: Int) {
        holder.bind(tiles[position])
    }

    override fun getItemCount() = tiles.size

    fun setEditMode(editMode: Boolean) {
        isEditMode = editMode
        notifyDataSetChanged()
    }

    fun updateTiles(newTiles: List<AppTile>) {
        tiles.clear()
        tiles.addAll(newTiles)
        notifyDataSetChanged()
    }

    fun getTiles(): List<AppTile> = tiles.toList()

    inner class TileViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tileCard: CardView = itemView.findViewById(R.id.tileCard)
        private val tileIcon: ImageView = itemView.findViewById(R.id.tileIcon)
        private val tileLabel: TextView = itemView.findViewById(R.id.tileLabel)
        private val tileTime: TextView? = itemView.findViewById(R.id.tileTime)
        private val tileDate: TextView? = itemView.findViewById(R.id.tileDate)
        private val editBadge: View? = itemView.findViewById(R.id.editBadge)

        fun bind(tile: AppTile) {
            val color = if (tile.tileColor != 0) tile.tileColor else TileManager.DEFAULT_ACCENT
            tileCard.setCardBackgroundColor(color)
            tileCard.radius = 0f
            tileCard.cardElevation = 0f

            if (tile.icon != null) {
                tileIcon.setImageDrawable(tile.icon)
                tileIcon.visibility = View.VISIBLE
            } else {
                tileIcon.visibility = View.GONE
            }

            tileLabel.text = tile.label
            tileLabel.setTextColor(Color.WHITE)

            if (tile.id == "clock") {
                tileTime?.text = timeFormat.format(Date())
                tileDate?.text = dateFormat.format(Date())
                tileTime?.visibility = View.VISIBLE
                tileDate?.visibility = View.VISIBLE
                tileIcon.visibility = View.GONE
            } else {
                tileTime?.visibility = View.GONE
                tileDate?.visibility = View.GONE
            }

            // X badge visible in edit mode for non-system tiles; tapping it removes the tile
            val showBadge = isEditMode && !tile.isSystemTile
            editBadge?.visibility = if (showBadge) View.VISIBLE else View.GONE
            editBadge?.setOnClickListener {
                val pos = bindingAdapterPosition
                if (pos != RecyclerView.NO_POSITION) onTileRemove(pos)
            }

            tileCard.setOnClickListener {
                val pos = bindingAdapterPosition
                if (pos == RecyclerView.NO_POSITION) return@setOnClickListener
                if (!isEditMode) {
                    val anim = AnimationUtils.loadAnimation(context, R.anim.tile_press)
                    itemView.startAnimation(anim)
                }
                onTileClick(tile, pos)
            }

            tileCard.setOnLongClickListener {
                val pos = bindingAdapterPosition
                if (pos != RecyclerView.NO_POSITION) onTileLongClick(tile, pos)
                true
            }

            val slideIn = AnimationUtils.loadAnimation(context, R.anim.tile_slide_in)
            slideIn.startOffset = (bindingAdapterPosition * 50L).coerceAtMost(400L)
            itemView.startAnimation(slideIn)
        }
    }
}
