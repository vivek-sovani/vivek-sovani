package com.windowsmobile.launcher

import android.graphics.drawable.Drawable

data class AppTile(
    val id: String,
    val packageName: String,
    val activityName: String,
    val label: String,
    var icon: Drawable? = null,
    var tileSize: TileSize = TileSize.MEDIUM,
    var tileColor: Int = 0,
    var gridColumn: Int = 0,
    var gridRow: Int = 0,
    var isSystemTile: Boolean = false
)

enum class TileSize(val columnSpan: Int, val rowSpan: Int) {
    SMALL(1, 1),    // 1x1 cell  (quarter tile)
    MEDIUM(2, 2),   // 2x2 cells (standard tile)
    WIDE(4, 2),     // 4x2 cells (wide tile)
    LARGE(4, 4)     // 4x4 cells (large tile)
}
