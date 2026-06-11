package com.wp10.launcher.model

enum class TileSize(val spanX: Int, val spanY: Int, val label: String) {
    SMALL(1, 1, "Small"),
    MEDIUM(2, 2, "Medium"),
    WIDE(4, 2, "Wide"),
    LARGE(4, 4, "Large");

    companion object {
        fun fromLabel(label: String) = values().find { it.label == label } ?: MEDIUM
    }
}
