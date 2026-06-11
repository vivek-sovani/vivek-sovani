package com.wp10.launcher.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.TypedValue
import android.view.DragEvent
import android.view.View
import android.view.ViewGroup
import com.wp10.launcher.model.TileData
import com.wp10.launcher.model.TileSize
import com.wp10.launcher.util.TileManager
import kotlin.math.max

class TileGridView(context: Context) : ViewGroup(context) {

    companion object {
        const val SPAN_COUNT = 4
        const val GRID_ROWS = 100
    }

    private val gapPx = dp(3)
    private val paddingPx = dp(8)

    // cellSize computed in onMeasure
    var cellSize = 0
        private set

    private val tileViews = mutableListOf<TileItemView>()
    private val tiles = mutableListOf<TileData>()

    // 2-D grid: null = free, non-null = tile id occupying that cell
    private val grid = Array(GRID_ROWS) { arrayOfNulls<String>(SPAN_COUNT) }

    var isEditMode = false
        private set

    var onTileClick: ((TileData) -> Unit)? = null
    var onTileUnpin: ((TileData) -> Unit)? = null
    var onTileResize: ((TileData) -> Unit)? = null
    var onTileOptions: ((TileData, TileItemView) -> Unit)? = null

    // Drag state
    private var dragTileView: TileItemView? = null
    private var dragTileData: TileData? = null
    private var dragShadow: ImageView? = null
    private var dragOriginalX = 0
    private var dragOriginalY = 0

    private val dropIndicatorPaint = Paint().apply {
        color = Color.argb(80, 255, 255, 255)
        style = Paint.Style.FILL
    }
    private var dropHighlightRect: android.graphics.Rect? = null

    init {
        setWillNotDraw(false)
        clipChildren = false
        clipToPadding = false
        setOnDragListener { _, event -> handleDrag(event) }
    }

    // ─── Public API ──────────────────────────────────────────────

    fun setTiles(newTiles: List<TileData>, tileManager: TileManager) {
        tiles.clear()
        tileViews.forEach { removeView(it) }
        tileViews.clear()
        clearGrid()

        newTiles.forEach { addTileInternal(it) }
        requestLayout()
    }

    fun addTile(tile: TileData) {
        if (tile.gridX < 0 || tile.gridY < 0) {
            val pos = findNextAvailablePosition(tile.tileSize)
            tile.gridX = pos.first
            tile.gridY = pos.second
        }
        addTileInternal(tile)
        requestLayout()
    }

    fun removeTile(tileId: String) {
        val idx = tiles.indexOfFirst { it.id == tileId }
        if (idx >= 0) {
            val tile = tiles.removeAt(idx)
            clearGridFor(tile)
            val view = tileViews.removeAt(idx)
            removeView(view)
            requestLayout()
        }
    }

    fun updateTileSize(tileId: String, newSize: TileSize) {
        val idx = tiles.indexOfFirst { it.id == tileId }
        if (idx < 0) return
        val tile = tiles[idx]
        val view = tileViews[idx]

        clearGridFor(tile)
        tile.tileSize = newSize

        // Find new position that fits
        val pos = findNextAvailablePosition(newSize)
        tile.gridX = pos.first
        tile.gridY = pos.second
        markGridOccupied(tile)

        view.tileData = tile
        view.refreshVisuals()
        requestLayout()
    }

    fun updateTileTransparency(tileId: String, transparent: Boolean) {
        val idx = tiles.indexOfFirst { it.id == tileId }
        if (idx < 0) return
        tiles[idx].isTransparent = transparent
        tileViews[idx].tileData = tiles[idx]
        tileViews[idx].refreshVisuals()
    }

    fun updateTileColor(tileId: String, color: Int) {
        val idx = tiles.indexOfFirst { it.id == tileId }
        if (idx < 0) return
        tiles[idx].customColor = color
        tileViews[idx].tileData = tiles[idx]
        tileViews[idx].refreshVisuals()
    }

    fun setEditMode(edit: Boolean) {
        isEditMode = edit
        tileViews.forEach { it.setEditMode(edit) }
        invalidate()
    }

    fun updateBadge(packageName: String, count: Int) {
        tileViews.forEachIndexed { idx, view ->
            if (tiles[idx].packageName == packageName) {
                view.updateBadge(count)
            }
        }
    }

    fun refreshAccentColors() {
        tileViews.forEach { it.refreshVisuals() }
    }

    fun getTilesData() = tiles.toList()

    // ─── Measure / Layout ────────────────────────────────────────

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w = MeasureSpec.getSize(widthMeasureSpec)
        cellSize = (w - 2 * paddingPx - (SPAN_COUNT - 1) * gapPx) / SPAN_COUNT

        tiles.forEachIndexed { idx, tile ->
            val tw = tile.tileSize.spanX * cellSize + (tile.tileSize.spanX - 1) * gapPx
            val th = tile.tileSize.spanY * cellSize + (tile.tileSize.spanY - 1) * gapPx
            tileViews[idx].measure(
                MeasureSpec.makeMeasureSpec(tw, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(th, MeasureSpec.EXACTLY)
            )
        }

        val maxRow = tiles.maxOfOrNull { it.gridY + it.tileSize.spanY } ?: 0
        val totalH = paddingPx + maxRow * cellSize + max(0, maxRow - 1) * gapPx + paddingPx + dp(80)
        setMeasuredDimension(w, totalH)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        tiles.forEachIndexed { idx, tile ->
            val left = paddingPx + tile.gridX * (cellSize + gapPx)
            val top = paddingPx + tile.gridY * (cellSize + gapPx)
            val tw = tile.tileSize.spanX * cellSize + (tile.tileSize.spanX - 1) * gapPx
            val th = tile.tileSize.spanY * cellSize + (tile.tileSize.spanY - 1) * gapPx
            tileViews[idx].layout(left, top, left + tw, top + th)
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        dropHighlightRect?.let {
            canvas.drawRect(it, dropIndicatorPaint)
        }
    }

    // ─── Private helpers ─────────────────────────────────────────

    private fun addTileInternal(tile: TileData) {
        if (tile.gridX < 0 || tile.gridY < 0) {
            val pos = findNextAvailablePosition(tile.tileSize)
            tile.gridX = pos.first
            tile.gridY = pos.second
        }
        markGridOccupied(tile)

        val view = TileItemView(context, tile)
        view.setOnClickListener {
            if (isEditMode) {
                onTileOptions?.invoke(tile, view)
            } else {
                onTileClick?.invoke(tile)
            }
        }
        view.setOnLongClickListener {
            if (!isEditMode) {
                setEditMode(true)
                startTileDrag(view, tile)
            } else {
                onTileOptions?.invoke(tile, view)
            }
            true
        }

        tiles.add(tile)
        tileViews.add(view)
        addView(view)
    }

    private fun clearGrid() {
        for (r in grid.indices) grid[r].fill(null)
    }

    private fun clearGridFor(tile: TileData) {
        for (r in tile.gridY until (tile.gridY + tile.tileSize.spanY)) {
            for (c in tile.gridX until (tile.gridX + tile.tileSize.spanX)) {
                if (r < GRID_ROWS && c < SPAN_COUNT) grid[r][c] = null
            }
        }
    }

    private fun markGridOccupied(tile: TileData) {
        for (r in tile.gridY until (tile.gridY + tile.tileSize.spanY)) {
            for (c in tile.gridX until (tile.gridX + tile.tileSize.spanX)) {
                if (r < GRID_ROWS && c < SPAN_COUNT) grid[r][c] = tile.id
            }
        }
    }

    private fun findNextAvailablePosition(size: TileSize): Pair<Int, Int> {
        for (row in 0 until GRID_ROWS) {
            for (col in 0..(SPAN_COUNT - size.spanX)) {
                if (canFitFree(row, col, size)) return Pair(col, row)
            }
        }
        return Pair(0, 0)
    }

    private fun canFitFree(startRow: Int, startCol: Int, size: TileSize): Boolean {
        for (r in startRow until startRow + size.spanY) {
            for (c in startCol until startCol + size.spanX) {
                if (r >= GRID_ROWS || c >= SPAN_COUNT) return false
                if (grid[r][c] != null) return false
            }
        }
        return true
    }

    private fun pixelToGrid(px: Float, py: Float): Pair<Int, Int> {
        val col = ((px - paddingPx) / (cellSize + gapPx)).toInt().coerceIn(0, SPAN_COUNT - 1)
        val row = ((py - paddingPx) / (cellSize + gapPx)).toInt().coerceIn(0, GRID_ROWS - 1)
        return Pair(col, row)
    }

    private fun gridToPixelLeft(col: Int) = paddingPx + col * (cellSize + gapPx)
    private fun gridToPixelTop(row: Int) = paddingPx + row * (cellSize + gapPx)

    // ─── Drag & Drop ─────────────────────────────────────────────

    private fun startTileDrag(view: TileItemView, tile: TileData) {
        val shadow = View.DragShadowBuilder(view)
        view.startDragAndDrop(null, shadow, tile, 0)
        view.alpha = 0.3f
        dragTileView = view
        dragTileData = tile
    }

    private fun handleDrag(event: DragEvent): Boolean {
        when (event.action) {
            DragEvent.ACTION_DRAG_STARTED -> return event.localState is TileData
            DragEvent.ACTION_DRAG_LOCATION -> {
                val draggedTile = event.localState as? TileData ?: return false
                val (col, row) = pixelToGrid(event.x, event.y)
                if (canFit(row, col, draggedTile.tileSize, excludeId = draggedTile.id)) {
                    val left = gridToPixelLeft(col)
                    val top = gridToPixelTop(row)
                    val right = left + draggedTile.tileSize.spanX * cellSize + (draggedTile.tileSize.spanX - 1) * gapPx
                    val bottom = top + draggedTile.tileSize.spanY * cellSize + (draggedTile.tileSize.spanY - 1) * gapPx
                    dropHighlightRect = android.graphics.Rect(left, top, right, bottom)
                } else {
                    dropHighlightRect = null
                }
                invalidate()
                return true
            }
            DragEvent.ACTION_DROP -> {
                val draggedTile = event.localState as? TileData ?: return false
                val (col, row) = pixelToGrid(event.x, event.y)
                if (canFit(row, col, draggedTile.tileSize, excludeId = draggedTile.id)) {
                    moveTile(draggedTile, col, row)
                }
                dropHighlightRect = null
                invalidate()
                return true
            }
            DragEvent.ACTION_DRAG_ENDED -> {
                dragTileView?.alpha = 1f
                dragTileView = null
                dragTileData = null
                dropHighlightRect = null
                invalidate()
                return true
            }
        }
        return false
    }

    private fun canFit(startRow: Int, startCol: Int, size: TileSize, excludeId: String): Boolean {
        for (r in startRow until startRow + size.spanY) {
            for (c in startCol until startCol + size.spanX) {
                if (r >= GRID_ROWS || c >= SPAN_COUNT) return false
                val cellId = grid[r][c]
                if (cellId != null && cellId != excludeId) return false
            }
        }
        return true
    }

    private fun moveTile(tile: TileData, newCol: Int, newRow: Int) {
        clearGridFor(tile)
        tile.gridX = newCol
        tile.gridY = newRow
        markGridOccupied(tile)
        requestLayout()
    }

    private fun dp(value: Int): Int =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value.toFloat(), resources.displayMetrics).toInt()
}
