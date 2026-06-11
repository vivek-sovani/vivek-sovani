package com.windowsmobile.launcher

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.windowsmobile.launcher.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var homeFragment: HomeFragment
    private var isEditMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Make status bar dark/transparent
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        )
        window.statusBarColor = android.graphics.Color.TRANSPARENT

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupViewPager()
        setupBottomBar()
    }

    private fun setupViewPager() {
        homeFragment = HomeFragment.newInstance()

        val adapter = object : FragmentStateAdapter(this) {
            override fun getItemCount() = 1
            override fun createFragment(position: Int): Fragment = homeFragment
        }

        binding.viewPager.adapter = adapter
        binding.viewPager.isUserInputEnabled = false // Disable swipe between pages
    }

    private fun setupBottomBar() {
        // App drawer button (swipe up or button press)
        binding.appDrawerButton.setOnClickListener {
            openAppDrawer()
        }

        binding.btnSearch.setOnClickListener {
            openAppDrawer()
        }
    }

    private fun openAppDrawer() {
        val intent = Intent(this, AppDrawerActivity::class.java)
        startActivity(intent)
        overridePendingTransition(R.anim.slide_up, R.anim.no_anim)
    }

    fun setEditMode(editing: Boolean) {
        isEditMode = editing
    }

    override fun onBackPressed() {
        if (isEditMode) {
            homeFragment.exitEditMode()
        } else {
            // On home screen, back does nothing (standard launcher behavior)
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        if (isEditMode) {
            homeFragment.exitEditMode()
        }
    }
}
