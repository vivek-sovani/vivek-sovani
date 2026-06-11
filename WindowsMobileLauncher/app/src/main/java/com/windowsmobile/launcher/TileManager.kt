package com.windowsmobile.launcher

import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Color
import org.json.JSONArray
import org.json.JSONObject

class TileManager(private val context: Context) {

    companion object {
        private const val PREFS_NAME = "tile_layout"
        private const val KEY_TILES = "tiles"

        val ACCENT_COLORS = listOf(
            Color.parseColor("#0078D7"),
            Color.parseColor("#00B4FF"),
            Color.parseColor("#00ABA9"),
            Color.parseColor("#339933"),
            Color.parseColor("#8CBF26"),
            Color.parseColor("#E3A21A"),
            Color.parseColor("#F0A30A"),
            Color.parseColor("#DA532C"),
            Color.parseColor("#E51400"),
            Color.parseColor("#FF0097"),
            Color.parseColor("#9F00A7"),
            Color.parseColor("#6D8764"),
            Color.parseColor("#647687"),
            Color.parseColor("#76608A"),
            Color.parseColor("#87794E"),
        )

        val DEFAULT_ACCENT = Color.parseColor("#0078D7")
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun findInstalledPackage(vararg candidates: String): String {
        val pm = context.packageManager
        for (pkg in candidates) {
            try {
                pm.getApplicationInfo(pkg, PackageManager.GET_META_DATA)
                return pkg
            } catch (e: PackageManager.NameNotFoundException) {
                // try next candidate
            }
        }
        return candidates[0]
    }

    private fun isPackageInstalled(packageName: String): Boolean {
        return try {
            context.packageManager.getApplicationInfo(packageName, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    private fun migrateTileIfNeeded(tile: AppTile): AppTile {
        if (tile.isSystemTile || isPackageInstalled(tile.packageName)) return tile
        val replacement = when (tile.id) {
            "phone" -> findInstalledPackage(
                "com.samsung.android.dialer", "com.android.dialer", "com.google.android.dialer"
            )
            "messages" -> findInstalledPackage(
                "com.samsung.android.messaging", "com.google.android.apps.messaging", "com.android.mms"
            )
            "people" -> findInstalledPackage(
                "com.samsung.android.contacts", "com.google.android.contacts", "com.android.contacts"
            )
            "camera" -> findInstalledPackage(
                "com.sec.android.app.camera", "com.google.android.GoogleCamera", "com.android.camera2"
            )
            "calendar" -> findInstalledPackage(
                "com.samsung.android.calendar", "com.google.android.calendar", "com.android.calendar"
            )
            "email" -> findInstalledPackage(
                "com.google.android.gm", "com.samsung.android.email.provider", "com.android.email"
            )
            "maps" -> findInstalledPackage(
                "com.google.android.apps.maps", "com.samsung.android.maps"
            )
            else -> tile.packageName
        }
        return tile.copy(packageName = replacement, activityName = "")
    }

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
                val tile = AppTile(
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
                tiles.add(migrateTileIfNeeded(tile))
            }
            tiles
        } catch (e: Exception) {
            null
        }
    }

    fun getDefaultTiles(): List<AppTile> {
        val phonePackage = findInstalledPackage(
            "com.samsung.android.dialer", "com.android.dialer", "com.google.android.dialer"
        )
        val smsPackage = findInstalledPackage(
            "com.samsung.android.messaging", "com.google.android.apps.messaging", "com.android.mms"
        )
        val contactsPackage = findInstalledPackage(
            "com.samsung.android.contacts", "com.google.android.contacts", "com.android.contacts"
        )
        val cameraPackage = findInstalledPackage(
            "com.sec.android.app.camera", "com.google.android.GoogleCamera", "com.android.camera2"
        )
        val calendarPackage = findInstalledPackage(
            "com.samsung.android.calendar", "com.google.android.calendar", "com.android.calendar"
        )
        val emailPackage = findInstalledPackage(
            "com.google.android.gm", "com.samsung.android.email.provider", "com.android.email"
        )
        val mapsPackage = findInstalledPackage(
            "com.google.android.apps.maps", "com.samsung.android.maps"
        )

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
                packageName = phonePackage,
                activityName = "",
                label = "Phone",
                tileSize = TileSize.MEDIUM,
                tileColor = ACCENT_COLORS[2],
                gridColumn = 0,
                gridRow = 2,
                isSystemTile = false
            ),
            AppTile(
                id = "messages",
                packageName = smsPackage,
                activityName = "",
                label = "Messaging",
                tileSize = TileSize.MEDIUM,
                tileColor = ACCENT_COLORS[3],
                gridColumn = 2,
                gridRow = 2,
                isSystemTile = false
            ),
            AppTile(
                id = "people",
                packageName = contactsPackage,
                activityName = "",
                label = "People",
                tileSize = TileSize.MEDIUM,
                tileColor = ACCENT_COLORS[9],
                gridColumn = 0,
                gridRow = 4,
                isSystemTile = false
            ),
            AppTile(
                id = "camera",
                packageName = cameraPackage,
                activityName = "",
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
                activityName = "",
                label = "Settings",
                tileSize = TileSize.SMALL,
                tileColor = ACCENT_COLORS[13],
                gridColumn = 0,
                gridRow = 6,
                isSystemTile = false
            ),
            AppTile(
                id = "maps",
                packageName = mapsPackage,
                activityName = "",
                label = "Maps",
                tileSize = TileSize.SMALL,
                tileColor = ACCENT_COLORS[3],
                gridColumn = 1,
                gridRow = 6,
                isSystemTile = false
            ),
            AppTile(
                id = "calendar",
                packageName = calendarPackage,
                activityName = "",
                label = "Calendar",
                tileSize = TileSize.SMALL,
                tileColor = ACCENT_COLORS[4],
                gridColumn = 2,
                gridRow = 6,
                isSystemTile = false
            ),
            AppTile(
                id = "email",
                packageName = emailPackage,
                activityName = "",
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
