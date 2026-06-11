package com.wp10.launcher.util

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.wp10.launcher.model.TileData
import com.wp10.launcher.model.TileSize

class TileManager(private val context: Context) {

    private val prefs = context.getSharedPreferences("wp10_tiles", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val TILES_KEY = "tiles_json"

    fun loadTiles(): MutableList<TileData> {
        val json = prefs.getString(TILES_KEY, null)
        return if (json != null) {
            val type = object : TypeToken<MutableList<TileData>>() {}.type
            try {
                gson.fromJson(json, type) ?: getDefaultTiles()
            } catch (e: Exception) {
                getDefaultTiles()
            }
        } else {
            getDefaultTiles()
        }
    }

    fun saveTiles(tiles: List<TileData>) {
        prefs.edit().putString(TILES_KEY, gson.toJson(tiles)).apply()
    }

    fun pinTile(tile: TileData): MutableList<TileData> {
        val tiles = loadTiles()
        if (tiles.none { it.packageName == tile.packageName && it.activityName == tile.activityName }) {
            tiles.add(tile)
            saveTiles(tiles)
        }
        return tiles
    }

    fun unpinTile(tileId: String): MutableList<TileData> {
        val tiles = loadTiles()
        tiles.removeAll { it.id == tileId }
        saveTiles(tiles)
        return tiles
    }

    fun updateTile(updated: TileData) {
        val tiles = loadTiles()
        val idx = tiles.indexOfFirst { it.id == updated.id }
        if (idx >= 0) {
            tiles[idx] = updated
            saveTiles(tiles)
        }
    }

    private fun getDefaultTiles(): MutableList<TileData> {
        val tiles = mutableListOf<TileData>()
        val pm = context.packageManager

        // Phone dialer
        addIntentTile(tiles, pm, Intent.ACTION_DIAL, "Phone", TileSize.MEDIUM)

        // Contacts
        addPackageTile(tiles, pm, "com.android.contacts", "People", TileSize.MEDIUM)
            ?: addPackageTile(tiles, pm, "com.google.android.contacts", "People", TileSize.MEDIUM)
            ?: addPackageTile(tiles, pm, "com.samsung.android.contacts", "People", TileSize.MEDIUM)

        // SMS/Messaging
        val smsIntent = Intent(Intent.ACTION_SENDTO).apply { data = android.net.Uri.parse("smsto:") }
        addIntentTile(tiles, pm, smsIntent, "Messaging", TileSize.MEDIUM)

        // Email
        val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
            data = android.net.Uri.parse("mailto:")
        }
        addIntentTile(tiles, pm, emailIntent, "Email", TileSize.WIDE)

        // Calendar
        addPackageTile(tiles, pm, "com.google.android.calendar", "Calendar", TileSize.MEDIUM)
            ?: addPackageTile(tiles, pm, "com.android.calendar", "Calendar", TileSize.MEDIUM)

        // Browser
        val browserIntent = Intent(Intent.ACTION_VIEW).apply {
            data = android.net.Uri.parse("http://www.google.com")
        }
        addIntentTile(tiles, pm, browserIntent, "Internet", TileSize.MEDIUM)

        // Camera
        addIntentTile(tiles, pm, "android.media.action.STILL_IMAGE_CAMERA", "Camera", TileSize.SMALL)

        // Store (Play Store)
        addPackageTile(tiles, pm, "com.android.vending", "Store", TileSize.MEDIUM)

        // Settings
        addPackageTile(tiles, pm, "com.android.settings", "Settings", TileSize.SMALL)

        // Maps
        addPackageTile(tiles, pm, "com.google.android.apps.maps", "Maps", TileSize.MEDIUM)

        // Photos/Gallery
        addPackageTile(tiles, pm, "com.google.android.apps.photos", "Photos", TileSize.MEDIUM)

        return tiles
    }

    private fun addIntentTile(
        tiles: MutableList<TileData>,
        pm: PackageManager,
        action: String,
        fallbackLabel: String,
        size: TileSize
    ) {
        val intent = Intent(action)
        val ri = pm.resolveActivity(intent, 0)
        if (ri != null) {
            val label = ri.loadLabel(pm).toString().ifBlank { fallbackLabel }
            tiles.add(
                TileData(
                    packageName = ri.activityInfo.packageName,
                    activityName = ri.activityInfo.name,
                    label = label,
                    tileSize = size
                )
            )
        }
    }

    private fun addIntentTile(
        tiles: MutableList<TileData>,
        pm: PackageManager,
        intent: Intent,
        fallbackLabel: String,
        size: TileSize
    ) {
        val ri = pm.resolveActivity(intent, 0)
        if (ri != null) {
            val label = ri.loadLabel(pm).toString().ifBlank { fallbackLabel }
            tiles.add(
                TileData(
                    packageName = ri.activityInfo.packageName,
                    activityName = ri.activityInfo.name,
                    label = label,
                    tileSize = size
                )
            )
        }
    }

    private fun addPackageTile(
        tiles: MutableList<TileData>,
        pm: PackageManager,
        packageName: String,
        fallbackLabel: String,
        size: TileSize
    ): TileData? {
        return try {
            val appInfo = pm.getApplicationInfo(packageName, 0)
            val label = pm.getApplicationLabel(appInfo).toString().ifBlank { fallbackLabel }
            val launchIntent = pm.getLaunchIntentForPackage(packageName)
            val activityName = launchIntent?.component?.className ?: ""
            val tile = TileData(
                packageName = packageName,
                activityName = activityName,
                label = label,
                tileSize = size
            )
            tiles.add(tile)
            tile
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }
    }
}
