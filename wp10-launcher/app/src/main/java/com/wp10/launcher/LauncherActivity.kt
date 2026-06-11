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
import android.content.ContentUris
import android.provider.CalendarContract
import android.provider.CallLog
import android.provider.ContactsContract
import android.provider.MediaStore
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
import com.bumptech.glide.Glide
import com.wp10.launcher.databinding.ActivityLauncherBinding
import com.wp10.launcher.util.PrefsHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
                Glide.with(this)
                    .load(Uri.parse(customUri))
                    .centerCrop()
                    .into(binding.wallpaperImage)
                return
            } catch (e: Exception) { }
        }
        try {
            val wm = WallpaperManager.getInstance(this)
            Glide.with(this)
                .load(wm.drawable)
                .centerCrop()
                .into(binding.wallpaperImage)
        } catch (e: Exception) {
            binding.wallpaperImage.setImageDrawable(null)
            binding.wallpaperImage.setBackgroundColor(Color.BLACK)
        }
    }

    private fun setupViewPager() {
        binding.viewPager.adapter = LauncherPagerAdapter(this)
        binding.viewPager.offscreenPageLimit = 1
        binding.viewPager.isUserInputEnabled = true
        binding.viewPager.setCurrentItem(1, false)  // start on home (middle page)
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
            binding.viewPager.setCurrentItem(2, true)
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
            binding.viewPager.currentItem != 1 -> binding.viewPager.setCurrentItem(1, true)
            // page 1 (home) at root — do nothing (WP10 behavior)
        }
    }

    // ─── Fragment helpers ────────────────────────────────────────

    private fun getHomeFragment(): HomeFragment? =
        supportFragmentManager.findFragmentByTag("f1") as? HomeFragment

    // ─── Badge updates ───────────────────────────────────────────

    private fun startBadgeUpdates() {
        badgeHandler.removeCallbacksAndMessages(null)
        val updateRunnable = object : Runnable {
            override fun run() {
                updateSmsBadge()
                updateCallBadge()
                updateCalendarBadge()
                updatePhotosBadge()
                updateContactsBadge()
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
                Telephony.Sms.Inbox.CONTENT_URI,
                arrayOf("address", "body"),
                "read=0", null, "date DESC"
            )
            val unread = cursor?.count ?: 0
            var sender = ""
            var preview = ""
            if (cursor != null && cursor.moveToFirst()) {
                val addrIdx = cursor.getColumnIndex("address")
                val bodyIdx = cursor.getColumnIndex("body")
                val address = if (addrIdx >= 0) cursor.getString(addrIdx) ?: "" else ""
                sender = lookupContactName(address) ?: address
                preview = if (bodyIdx >= 0) (cursor.getString(bodyIdx) ?: "").take(80) else ""
            }
            cursor?.close()
            val smsPackages = setOf("com.android.mms", "com.google.android.apps.messaging",
                "com.samsung.android.messaging", "com.oneplus.mms")
            smsPackages.forEach { getHomeFragment()?.updateLiveTile(it, unread, sender, preview) }
        } catch (e: Exception) { }
    }

    private fun updateCallBadge() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CALL_LOG)
            != PackageManager.PERMISSION_GRANTED) return
        try {
            val cursor = contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                arrayOf(CallLog.Calls.CACHED_NAME, CallLog.Calls.NUMBER, CallLog.Calls.DATE, CallLog.Calls.TYPE),
                "${CallLog.Calls.TYPE}=${CallLog.Calls.MISSED_TYPE} AND ${CallLog.Calls.NEW}=1",
                null, "${CallLog.Calls.DATE} DESC"
            )
            val missed = cursor?.count ?: 0
            var caller = ""
            var callTime = ""
            if (cursor != null && cursor.moveToFirst()) {
                val nameIdx = cursor.getColumnIndex(CallLog.Calls.CACHED_NAME)
                val numIdx = cursor.getColumnIndex(CallLog.Calls.NUMBER)
                val dateIdx = cursor.getColumnIndex(CallLog.Calls.DATE)
                caller = (if (nameIdx >= 0) cursor.getString(nameIdx)?.takeIf { it.isNotBlank() } else null)
                    ?: (if (numIdx >= 0) cursor.getString(numIdx) else "") ?: ""
                if (dateIdx >= 0) callTime = formatRelativeTime(cursor.getLong(dateIdx))
            }
            cursor?.close()
            val phonePackages = setOf("com.android.dialer", "com.google.android.dialer",
                "com.samsung.android.incallui")
            phonePackages.forEach { getHomeFragment()?.updateLiveTile(it, missed, caller, callTime) }
        } catch (e: Exception) { }
    }

    private fun updateCalendarBadge() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CALENDAR)
            != PackageManager.PERMISSION_GRANTED) return
        try {
            val now = System.currentTimeMillis()
            val endOfDay = now + 24 * 60 * 60 * 1000L
            val cursor = contentResolver.query(
                CalendarContract.Events.CONTENT_URI,
                arrayOf(CalendarContract.Events.TITLE, CalendarContract.Events.DTSTART),
                "${CalendarContract.Events.DTSTART} >= ? AND ${CalendarContract.Events.DTSTART} <= ?" +
                    " AND ${CalendarContract.Events.DELETED} = 0",
                arrayOf(now.toString(), endOfDay.toString()),
                "${CalendarContract.Events.DTSTART} ASC"
            )
            val count = cursor?.count ?: 0
            var title = ""
            var time = ""
            if (cursor != null && cursor.moveToFirst()) {
                val titleIdx = cursor.getColumnIndex(CalendarContract.Events.TITLE)
                val startIdx = cursor.getColumnIndex(CalendarContract.Events.DTSTART)
                if (titleIdx >= 0) title = cursor.getString(titleIdx) ?: ""
                if (startIdx >= 0) time = SimpleDateFormat("h:mm a", Locale.getDefault())
                    .format(Date(cursor.getLong(startIdx)))
            }
            cursor?.close()
            val calPackages = setOf("com.google.android.calendar", "com.android.calendar",
                "com.samsung.android.calendar")
            calPackages.forEach { getHomeFragment()?.updateLiveTile(it, count, title, time) }
        } catch (e: Exception) { }
    }

    private fun updatePhotosBadge() {
        val hasMedia = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED
        val hasStorage = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        if (!hasMedia && !hasStorage) return
        try {
            val cursor = contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                arrayOf(MediaStore.Images.Media._ID),
                null, null,
                "${MediaStore.Images.Media.DATE_TAKEN} DESC"
            )
            val uri = if (cursor?.moveToFirst() == true) {
                ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, cursor.getLong(0)).toString()
            } else null
            cursor?.close()
            val photosPackages = setOf("com.google.android.apps.photos", "com.android.gallery3d",
                "com.samsung.android.gallery.app", "com.miui.gallery")
            photosPackages.forEach { getHomeFragment()?.updatePhotoTile(it, uri) }
        } catch (e: Exception) { }
    }

    private fun updateContactsBadge() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS)
            != PackageManager.PERMISSION_GRANTED) return
        try {
            val uris = mutableListOf<String>()
            val cursor = contentResolver.query(
                ContactsContract.Contacts.CONTENT_URI,
                arrayOf(ContactsContract.Contacts.PHOTO_THUMBNAIL_URI),
                "${ContactsContract.Contacts.HAS_PHONE_NUMBER}=1 AND ${ContactsContract.Contacts.PHOTO_THUMBNAIL_URI} IS NOT NULL",
                null, null
            )
            if (cursor != null) {
                val col = cursor.getColumnIndex(ContactsContract.Contacts.PHOTO_THUMBNAIL_URI)
                while (cursor.moveToNext()) {
                    val photoUri = if (col >= 0) cursor.getString(col) else null
                    if (!photoUri.isNullOrEmpty()) uris.add(photoUri)
                }
                cursor.close()
            }
            val selected = uris.shuffled().take(3)
            val contactPackages = setOf("com.android.contacts", "com.google.android.contacts",
                "com.samsung.android.contacts")
            contactPackages.forEach { getHomeFragment()?.updateContactsTile(it, selected) }
        } catch (e: Exception) { }
    }

    private fun lookupContactName(number: String): String? {
        if (number.isBlank()) return null
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS)
            != PackageManager.PERMISSION_GRANTED) return null
        return try {
            val uri = Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number)
            )
            val c = contentResolver.query(uri, arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME), null, null, null)
            val name = if (c?.moveToFirst() == true) c.getString(0) else null
            c?.close()
            name
        } catch (e: Exception) { null }
    }

    private fun formatRelativeTime(timeMs: Long): String {
        val diff = System.currentTimeMillis() - timeMs
        return when {
            diff < 60_000 -> "just now"
            diff < 3_600_000 -> "${diff / 60_000}m ago"
            diff < 86_400_000 -> "${diff / 3_600_000}h ago"
            else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(timeMs))
        }
    }

    // ─── Permissions ─────────────────────────────────────────────

    private fun requestNeededPermissions() {
        val needed = mutableListOf<String>()
        val perms = arrayOf(
            Manifest.permission.READ_SMS,
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.READ_CALENDAR
        )
        perms.forEach { perm ->
            if (ContextCompat.checkSelfPermission(this, perm) != PackageManager.PERMISSION_GRANTED)
                needed.add(perm)
        }
        val mediaPerm = if (android.os.Build.VERSION.SDK_INT >= 33)
            Manifest.permission.READ_MEDIA_IMAGES else Manifest.permission.READ_EXTERNAL_STORAGE
        if (ContextCompat.checkSelfPermission(this, mediaPerm) != PackageManager.PERMISSION_GRANTED)
            needed.add(mediaPerm)
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
    override fun getItemCount() = 3
    override fun createFragment(position: Int): Fragment = when (position) {
        0 -> GoogleFragment()
        1 -> HomeFragment()
        else -> AppsFragment()
    }
}
