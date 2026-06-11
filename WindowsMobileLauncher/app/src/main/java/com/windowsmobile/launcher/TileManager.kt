package com.windowsmobile.launcher

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import org.json.JSONArray
import org.json.JSONObject

class TileManager(private val context: Context) {

    companion object {
        private const val PREFS_NAME = "tile_layout"
        private const val KEY_TILES = "tiles"

        // Windows Phone accent colors
        val ACCENT_COLORS = listOf(
            Color.parseColor("#0078D7"), // Blue (Windows Blue)
            Color.parseColor("#00B4FF"), // Cyan
            Color.parseColor("#00ABA9"), // Teal
            Color.parseColor("#339933"), // Green
            Color.parseColor("#8CBF26"), // Lime
            Color.parseColor("#E3A21A"), // Amber
            Color.parseColor("#F0A30A"), // Yellow
            Color.parseColor("#DA532C"), // Orange
            Color.parseColor("#E51400"), // Red
            Color.parseColor("#FF0097"), // Magenta
            Color.parseColor("#9F00A7"), // Purple
            Color.parseColor("#6D8764"), // Olive
            Color.parseColor("#647687"), // Steel
            Color.parseColor("#76608A"), // Mauve
            Color.parseColor("#87794E"), // Taupe
        )

        val DEFAULT_ACCENT = Color.parseColor("#0078D7")
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun saveTiles(tiles: List<AppTile>) {
        val jsonArray = JSONArray()
        tiles.forEach { tile ->
            val obj = JSONObject().apply {
                put("id", tile.id)
                put("packageName", tile.packageName)
                put("activityName", tile.activityName)
                put("label", tile.label)
                put("tileSize", tile.tileSize.name)
                put("tileColor", tile.tileColor)
                put("gridColumn", tile.gridColumn)
                put("gridRow", tile.gridRow)
                put("isSystemTile", tile.isSystemTile)
            }
            jsonArray.put(obj)
        }
        prefs.edit().putString(KEY_TILES, jsonArray.toString()).apply()
    }

    fun loadTiles(): List<AppTile>? {
        val json = prefs.getString(KEY_TILES, null) ?: return null
        return try {
            val jsonArray = JSONArray(json)
            val tiles = mutableListOf<AppTile>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                tiles.add(
                    AppTile(
                        id = obj.getString("id"),
                        packageName = obj.getString("packageName"),
                        activityName = obj.getString("activityName"),
                        label = obj.getString("label"),
                        tileSize = TileSize.valueOf(obj.getString("tileSize")),
                        tileColor = obj.getInt("tileColor"),
                        gridColumn = obj.getInt("gridColumn"),
                        gridRow = obj.getInt("gridRow"),
                        isSystemTile = obj.getBoolean("isSystemTile")
                    )
                )
            }
            tiles
        } catch (e: Exception) {
            null
        }
    }

    fun getDefaultTiles(): List<AppTile> {
        return listOf(
            AppTile(
                id = "clock",
                packageName = context.packageName,
                activityName = "ClockTile",
                label = "Clock",
                tileSize = TileSize.WIDE,
                tileColor = ACCENT_COLORS[0],
                gridColumn = 0,
                gridRow = 0,
                isSystemTile = true
            ),
            AppTile(
                id = "phone",
                packageName = "com.android.dialer",
                activityName = "com.android.dialer.DialtactsActivity",
                label = "Phone",
                tileSize = TileSize.MEDIUM,
                tileColor = ACCENT_COLORS[2],
                gridColumn = 0,
                gridRow = 2,
                isSystemTile = false
            ),
            AppTile(
                id = "messages",
                packageName = "com.android.mms",
                activityName = "com.android.mms.ui.ConversationList",
                label = "Messaging",
                tileSize = TileSize.MEDIUM,
                tileColor = ACCENT_COLORS[3],
                gridColumn = 2,
                gridRow = 2,
                isSystemTile = false
            ),
            AppTile(
                id = "people",
                packageName = "com.android.contacts",
                activityName = "com.android.contacts.activities.PeopleActivity",
                label = "People",
                tileSize = TileSize.MEDIUM,
                tileColor = ACCENT_COLORS[9],
                gridColumn = 0,
                gridRow = 4,
                isSystemTile = false
            ),
            AppTile(
                id = "camera",
                packageName = "com.android.camera2",
                activityName = "com.android.camera.CameraLauncher",
                label = "Camera",
                tileSize = TileSize.MEDIUM,
                tileColor = ACCENT_COLORS[6],
                gridColumn = 2,
                gridRow = 4,
                isSystemTile = false
            ),
            AppTile(
                id = "settings",
                packageName = "com.android.settings",
                activityName = "com.android.settings.Settings",
                label = "Settings",
                tileSize = TileSize.SMALL,
                tileColor = ACCENT_COLORS[13],
                gridColumn = 0,
                gridRow = 6,
                isSystemTile = false
            ),
            AppTile(
                id = "maps",
                packageName = "com.google.android.apps.maps",
                activityName = "com.google.android.maps.MapsActivity",
                label = "Maps",
                tileSize = TileSize.SMALL,
                tileColor = ACCENT_COLORS[3],
                gridColumn = 1,
                gridRow = 6,
                isSystemTile = false
            ),
            AppTile(
                id = "calendar",
                packageName = "com.android.calendar",
                activityName = "com.android.calendar.AllInOneActivity",
                label = "Calendar",
                tileSize = TileSize.SMALL,
                tileColor = ACCENT_COLORS[4],
                gridColumn = 2,
                gridRow = 6,
                isSystemTile = false
            ),
            AppTile(
                id = "email",
                packageName = "com.android.email",
                activityName = "com.android.email.activity.Welcome",
                label = "Email",
                tileSize = TileSize.SMALL,
                tileColor = ACCENT_COLORS[8],
                gridColumn = 3,
                gridRow = 6,
                isSystemTile = false
            )
        )
    }

    fun getAccentColor(index: Int): Int {
        return ACCENT_COLORS.getOrElse(index) { DEFAULT_ACCENT }
    }

    fun hasLayoutSaved(): Boolean {
        return prefs.contains(KEY_TILES)
    }
}
