package com.wp10.launcher.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.util.UUID

@Parcelize
data class TileData(
    val id: String = UUID.randomUUID().toString(),
    val packageName: String,
    val activityName: String = "",
    var label: String,
    var tileSize: TileSize = TileSize.MEDIUM,
    var gridX: Int = -1,
    var gridY: Int = -1,
    var isTransparent: Boolean = false,
    var customColor: Int = 0,   // 0 = use accent color
    var liveTileEnabled: Boolean = true,
    var badgeCount: Int = 0
) : Parcelable
