package com.wp10.launcher

import android.Manifest
import android.app.WallpaperManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.CallLog
import android.provider.Telephony
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.wp10.launcher.databinding.ActivityLauncherBinding
import com.wp10.launcher.model.TileData
import com.wp10.launcher.model.TileSize
import com.wp10.launcher.util.PrefsHelper
import com.wp10.launcher.util.TileManager
import com.wp10.launcher.view.TileGridView
import java.text.SimpleDateFormat
import java.util.*

class LauncherActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLauncherBinding
    private lateinit var tileManager: TileManager
    private lateinit var tileGrid: TileGridView
    private lateinit var gestureDetector: GestureDetector

    private val clockHandler = Handler(Looper.getMainLooper())
    private val badgeHandler = Handler(Looper.getMainLooper())

    private val PERMISSION_REQUEST_CODE = 1001
    private val TILE_OPTIONS_REQUEST = 2001
    private val SETTINGS_REQUEST = 2002
    private val WALLPAPER_REQUEST = 2003

    private val timeFormat = SimpleDateFormat("h:mm", Locale.getDefault())
    private val dateFormat = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault())

    private val smsReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context, intent: Intent) {
            updateSmsBadge()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLauncherBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Full-screen immersive
        window.apply {
            addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            statusBarColor = Color.TRANSPARENT
            navigationBarColor = Color.TRANSPARENT
        }
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        )

        tileManager = TileManager(this)

        setupWallpaper()
        setupTileGrid()
        setupGestures()
        setupBottomBar()
        startClock()
        requestNeededPermissions()
    }

    override fun onResume() {
        super.onResume()
        setupWallpaper()
        tileGrid.refreshAccentColors()
        startBadgeUpdates()
        // targetSdk 34 requires an export flag for runtime-registered receivers
        try {
            ContextCompat.registerReceiver(
                this, smsReceiver,
                IntentFilter("android.provider.Telephony.SMS_RECEIVED"),
                ContextCompat.RECEIVER_EXPORTED
            )
        } catch (e: Exception) { }
    }

    override fun onPause() {
        super.onPause()
        badgeHandler.removeCallbacksAndMessages(null)
        try { unregisterReceiver(smsReceiver) } catch (e: Exception) { }
    }

    override fun onDestroy() {
        super.onDestroy()
        clockHandler.removeCallbacksAndMessages(null)
    }

    // ─── Setup ───────────────────────────────────────────────────

    private fun setupWallpaper() {
        val customUri = PrefsHelper.getWallpaperUri(this)
        if (customUri != null) {
            try {
                binding.wallpaperImage.setImageURI(Uri.parse(customUri))
                binding.wallpaperImage.scaleType = ImageView.ScaleType.CENTER_CROP
                return
            } catch (e: Exception) { }
        }
        // Reading the system wallpaper throws SecurityException on Android 13+;
        // fall back to the authentic WP solid-black background.
        try {
            val wm = WallpaperManager.getInstance(this)
            binding.wallpaperImage.setImageDrawable(wm.drawable)
            binding.wallpaperImage.scaleType = ImageView.ScaleType.CENTER_CROP
        } catch (e: Exception) {
            binding.wallpaperImage.setImageDrawable(null)
            binding.wallpaperImage.setBackgroundColor(Color.BLACK)
        }
    }

    private fun setupTileGrid() {
        tileGrid = TileGridView(this)
        binding.tileScrollView.addView(tileGrid)

        val savedTiles = tileManager.loadTiles()
        tileGrid.setTiles(savedTiles, tileManager)

        tileGrid.onTileClick = { tile -> launchTile(tile) }

        tileGrid.onTileOptions = { tile, view ->
            val intent = Intent(this, TileOptionsActivity::class.java).apply {
                putExtra("tile", tile)
            }
            startActivityForResult(intent, TILE_OPTIONS_REQUEST)
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }

        tileGrid.onTileUnpin = { tile ->
            tileManager.unpinTile(tile.id)
        }
    }

    private fun setupGestures() {
        gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onFling(
                e1: MotionEvent?,
                e2: MotionEvent,
                velocityX: Float,
                velocityY: Float
            ): Boolean {
                val absX = Math.abs(velocityX)
                val absY = Math.abs(velocityY)
                if (absX > 800 && absX > absY * 2) {
                    // Horizontal swipe → app drawer
                    openAppDrawer()
                    return true
                }
                return false
            }

            override fun onSingleTapUp(e: MotionEvent): Boolean {
                if (tileGrid.isEditMode) {
                    tileGrid.setEditMode(false)
                    saveTiles()
                    return true
                }
                return false
            }
        })

        binding.root.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            false
        }
    }

    private fun setupBottomBar() {
        binding.btnSearch.setOnClickListener {
            val intent = Intent(Intent.ACTION_WEB_SEARCH)
            intent.putExtra("query", "")
            safeStart(intent)
        }

        binding.btnApps.setOnClickListener {
            openAppDrawer()
        }

        binding.btnSettings.setOnClickListener {
            startActivityForResult(Intent(this, SettingsActivity::class.java), SETTINGS_REQUEST)
        }
    }

    private fun startClock() {
        val clockRunnable = object : Runnable {
            override fun run() {
                val now = Date()
                binding.tvTime.text = timeFormat.format(now)
                binding.tvDate.text = dateFormat.format(now)
                clockHandler.postDelayed(this, 1000)
            }
        }
        clockHandler.post(clockRunnable)
    }

    private fun startBadgeUpdates() {
        badgeHandler.removeCallbacksAndMessages(null)
        val updateRunnable = object : Runnable {
            override fun run() {
                updateSmsBadge()
                updateCallBadge()
                badgeHandler.postDelayed(this, 30_000)
            }
        }
        badgeHandler.post(updateRunnable)
    }

    // ─── Badge updates ───────────────────────────────────────────

    private fun updateSmsBadge() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS)
            != PackageManager.PERMISSION_GRANTED) return

        try {
            val cursor = contentResolver.query(
                Telephony.Sms.Inbox.CONTENT_URI,
                arrayOf("read"),
                "read=0",
                null, null
            )
            val unread = cursor?.count ?: 0
            cursor?.close()

            // Update SMS tile badge
            val smsPackages = setOf("com.android.mms", "com.google.android.apps.messaging",
                "com.samsung.android.messaging", "com.oneplus.mms")
            smsPackages.forEach { tileGrid.updateBadge(it, unread) }
        } catch (e: Exception) { }
    }

    private fun updateCallBadge() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CALL_LOG)
            != PackageManager.PERMISSION_GRANTED) return

        try {
            val cursor = contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                arrayOf(CallLog.Calls.TYPE),
                "${CallLog.Calls.TYPE}=${CallLog.Calls.MISSED_TYPE} AND ${CallLog.Calls.NEW}=1",
                null, null
            )
            val missed = cursor?.count ?: 0
            cursor?.close()

            val phonePackages = setOf("com.android.dialer", "com.google.android.dialer",
                "com.samsung.android.incallui")
            phonePackages.forEach { tileGrid.updateBadge(it, missed) }
        } catch (e: Exception) { }
    }

    // ─── Navigation ──────────────────────────────────────────────

    private fun openAppDrawer() {
        val intent = Intent(this, AppDrawerActivity::class.java)
        startActivity(intent)
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
    }

    private fun launchTile(tile: TileData) {
        try {
            if (tile.activityName.isNotEmpty()) {
                val intent = Intent().apply {
                    component = android.content.ComponentName(tile.packageName, tile.activityName)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                startActivity(intent)
            } else {
                val intent = packageManager.getLaunchIntentForPackage(tile.packageName)
                    ?: return
                startActivity(intent)
            }
        } catch (e: Exception) {
            android.widget.Toast.makeText(this, "Cannot open ${tile.label}", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveTiles() {
        tileManager.saveTiles(tileGrid.getTilesData())
    }

    private fun safeStart(intent: Intent) {
        try {
            startActivity(intent)
        } catch (e: Exception) { }
    }

    // ─── Permissions ─────────────────────────────────────────────

    private fun requestNeededPermissions() {
        val needed = mutableListOf<String>()
        val perms = arrayOf(
            Manifest.permission.READ_SMS,
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.READ_PHONE_STATE
        )
        perms.forEach { perm ->
            if (ContextCompat.checkSelfPermission(this, perm) != PackageManager.PERMISSION_GRANTED)
                needed.add(perm)
        }
        if (needed.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, needed.toTypedArray(), PERMISSION_REQUEST_CODE)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            startBadgeUpdates()
        }
    }

    // ─── Activity results ────────────────────────────────────────

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            TILE_OPTIONS_REQUEST -> {
                if (resultCode == RESULT_OK && data != null) {
                    val action = data.getStringExtra("action")
                    val tile = data.getParcelableExtra<TileData>("tile")
                    when (action) {
                        "unpin" -> {
                            tile?.let {
                                tileGrid.removeTile(it.id)
                                tileManager.unpinTile(it.id)
                            }
                        }
                        "resize" -> {
                            tile?.let {
                                val sizeLabel = data.getStringExtra("size") ?: return
                                val newSize = TileSize.fromLabel(sizeLabel)
                                tileGrid.updateTileSize(it.id, newSize)
                                it.tileSize = newSize
                                tileManager.updateTile(it)
                                saveTiles()
                            }
                        }
                        "transparent" -> {
                            tile?.let {
                                val isTransparent = data.getBooleanExtra("transparent", false)
                                tileGrid.updateTileTransparency(it.id, isTransparent)
                                it.isTransparent = isTransparent
                                tileManager.updateTile(it)
                                saveTiles()
                            }
                        }
                    }
                }
                tileGrid.setEditMode(false)
                saveTiles()
            }
            SETTINGS_REQUEST -> {
                setupWallpaper()
                tileGrid.refreshAccentColors()
            }
            WALLPAPER_REQUEST -> {
                setupWallpaper()
            }
        }
    }

    override fun onBackPressed() {
        if (tileGrid.isEditMode) {
            tileGrid.setEditMode(false)
            saveTiles()
        }
        // Launcher doesn't go back further
    }
}
