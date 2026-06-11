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
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.CallLog
import android.provider.Telephony
import android.telephony.TelephonyManager
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.wp10.launcher.databinding.ActivityLauncherBinding
import com.wp10.launcher.util.PrefsHelper

class LauncherActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLauncherBinding
    private val badgeHandler = Handler(Looper.getMainLooper())

    private val PERMISSION_REQUEST_CODE = 1001
    private val SETTINGS_REQUEST = 2002

    private val smsReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context, intent: Intent) {
            updateSmsBadge()
        }
    }

    private val phoneStateReceiver = object : BroadcastReceiver() {
        private var wasRinging = false
        override fun onReceive(ctx: Context, intent: Intent) {
            val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
            when (state) {
                TelephonyManager.EXTRA_STATE_RINGING -> wasRinging = true
                TelephonyManager.EXTRA_STATE_IDLE -> {
                    if (wasRinging) {
                        // Call ended from ringing state = missed call
                        badgeHandler.postDelayed({ updateCallBadge() }, 1000)
                    }
                    wasRinging = false
                }
                TelephonyManager.EXTRA_STATE_OFFHOOK -> wasRinging = false
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLauncherBinding.inflate(layoutInflater)
        setContentView(binding.root)

        window.apply {
            addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            statusBarColor = Color.TRANSPARENT
            navigationBarColor = Color.TRANSPARENT
        }
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        )

        setupWallpaper()
        setupViewPager()
        setupBottomBar()
        requestNeededPermissions()
    }

    override fun onResume() {
        super.onResume()
        setupWallpaper()
        startBadgeUpdates()
        try {
            ContextCompat.registerReceiver(
                this, smsReceiver,
                IntentFilter("android.provider.Telephony.SMS_RECEIVED"),
                ContextCompat.RECEIVER_EXPORTED
            )
        } catch (e: Exception) { }
        try {
            ContextCompat.registerReceiver(
                this, phoneStateReceiver,
                IntentFilter(TelephonyManager.ACTION_PHONE_STATE_CHANGED),
                ContextCompat.RECEIVER_EXPORTED
            )
        } catch (e: Exception) { }
    }

    override fun onPause() {
        super.onPause()
        badgeHandler.removeCallbacksAndMessages(null)
        try { unregisterReceiver(smsReceiver) } catch (e: Exception) { }
        try { unregisterReceiver(phoneStateReceiver) } catch (e: Exception) { }
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
        try {
            val wm = WallpaperManager.getInstance(this)
            binding.wallpaperImage.setImageDrawable(wm.drawable)
            binding.wallpaperImage.scaleType = ImageView.ScaleType.CENTER_CROP
        } catch (e: Exception) {
            binding.wallpaperImage.setImageDrawable(null)
            binding.wallpaperImage.setBackgroundColor(Color.BLACK)
        }
    }

    private fun setupViewPager() {
        binding.viewPager.adapter = LauncherPagerAdapter(this)
        binding.viewPager.offscreenPageLimit = 1
        binding.viewPager.isUserInputEnabled = true
    }

    fun setViewPagerEnabled(enabled: Boolean) {
        binding.viewPager.isUserInputEnabled = enabled
    }

    private fun setupBottomBar() {
        binding.btnSearch.setOnClickListener {
            try {
                val intent = Intent(Intent.ACTION_WEB_SEARCH)
                intent.putExtra("query", "")
                startActivity(intent)
            } catch (e: Exception) { }
        }
        binding.btnApps.setOnClickListener {
            binding.viewPager.setCurrentItem(1, true)
        }
        binding.btnSettings.setOnClickListener {
            startActivityForResult(Intent(this, SettingsActivity::class.java), SETTINGS_REQUEST)
        }
    }

    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == SETTINGS_REQUEST) {
            setupWallpaper()
            getHomeFragment()?.refreshAccentColors()
        }
    }

    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        when {
            getHomeFragment()?.onBackPressedInActivity() == true -> { /* handled by fragment */ }
            binding.viewPager.currentItem != 0 -> binding.viewPager.setCurrentItem(0, true)
            // else: launcher root — do nothing
        }
    }

    // ─── Fragment helpers ────────────────────────────────────────

    private fun getHomeFragment(): HomeFragment? =
        supportFragmentManager.findFragmentByTag("f0") as? HomeFragment

    // ─── Badge updates ───────────────────────────────────────────

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

    private fun updateSmsBadge() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS)
            != PackageManager.PERMISSION_GRANTED) return
        try {
            val cursor = contentResolver.query(
                Telephony.Sms.Inbox.CONTENT_URI, arrayOf("read"), "read=0", null, null)
            val unread = cursor?.count ?: 0
            cursor?.close()
            val smsPackages = setOf("com.android.mms", "com.google.android.apps.messaging",
                "com.samsung.android.messaging", "com.oneplus.mms")
            smsPackages.forEach { getHomeFragment()?.updateBadge(it, unread) }
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
                null, null)
            val missed = cursor?.count ?: 0
            cursor?.close()
            val phonePackages = setOf("com.android.dialer", "com.google.android.dialer",
                "com.samsung.android.incallui")
            phonePackages.forEach { getHomeFragment()?.updateBadge(it, missed) }
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
        if (requestCode == PERMISSION_REQUEST_CODE) startBadgeUpdates()
    }
}

private class LauncherPagerAdapter(activity: AppCompatActivity) : FragmentStateAdapter(activity) {
    override fun getItemCount() = 2
    override fun createFragment(position: Int): Fragment = when (position) {
        0 -> HomeFragment()
        else -> AppsFragment()
    }
}
