package com.wp10.launcher.view

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import com.wp10.launcher.R
import com.wp10.launcher.model.TileData
import com.wp10.launcher.model.TileSize
import com.wp10.launcher.util.PrefsHelper
import kotlin.math.min
import kotlin.random.Random

class TileItemView(context: Context, var tileData: TileData) : FrameLayout(context) {

    private val frontFace = FrameLayout(context)
    private val backFace = FrameLayout(context)

    private val iconView = ImageView(context)
    private val labelView = TextView(context)
    private val badgeView = TextView(context)
    private val backLabelView = TextView(context)
    private val backCountView = TextView(context)

    private val liveHandler = Handler(Looper.getMainLooper())
    private var isShowingBack = false
    private var isEditMode = false

    private val wobbleAnimator: ObjectAnimator by lazy {
        ObjectAnimator.ofFloat(this, "rotation", -2f, 2f).apply {
            duration = 150
            repeatCount = ObjectAnimator.INFINITE
            repeatMode = ObjectAnimator.REVERSE
        }
    }

    init {
        clipChildren = false
        clipToPadding = false
        buildFrontFace()
        buildBackFace()
        addView(backFace)
        addView(frontFace)
        backFace.rotationY = 180f
        refreshVisuals()
        scheduleLiveTileFlip()
    }

    private fun buildFrontFace() {
        frontFace.layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        val pad = dp(8)

        // App icon
        iconView.layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
            gravity = Gravity.CENTER
        }
        iconView.scaleType = ImageView.ScaleType.FIT_CENTER

        // Label at bottom-left
        labelView.apply {
            setTextColor(Color.WHITE)
            textSize = when (tileData.tileSize) {
                TileSize.SMALL -> 10f
                TileSize.MEDIUM -> 12f
                TileSize.WIDE -> 13f
                TileSize.LARGE -> 14f
            }
            setShadowLayer(2f, 0f, 1f, Color.BLACK)
            maxLines = 2
            ellipsize = android.text.TextUtils.TruncateAt.END
            text = tileData.label
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
                gravity = Gravity.BOTTOM or Gravity.START
                leftMargin = pad
                rightMargin = pad
                bottomMargin = pad
            }
        }

        // Badge count top-right
        badgeView.apply {
            setTextColor(Color.WHITE)
            textSize = 22f
            gravity = Gravity.TOP or Gravity.END
            layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                gravity = Gravity.TOP or Gravity.END
                topMargin = pad
                rightMargin = pad
            }
            visibility = View.GONE
        }

        frontFace.addView(iconView)
        frontFace.addView(labelView)
        frontFace.addView(badgeView)
    }

    private fun buildBackFace() {
        backFace.layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        val pad = dp(8)

        // Back label (app name)
        backLabelView.apply {
            setTextColor(Color.WHITE)
            textSize = 11f
            text = tileData.label
            maxLines = 1
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
                gravity = Gravity.TOP or Gravity.START
                topMargin = pad
                leftMargin = pad
                rightMargin = pad
            }
        }

        // Back count (large number)
        backCountView.apply {
            setTextColor(Color.WHITE)
            textSize = 36f
            gravity = Gravity.CENTER
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
                gravity = Gravity.CENTER
            }
        }

        backFace.addView(backLabelView)
        backFace.addView(backCountView)
    }

    fun refreshVisuals() {
        val accentColor = PrefsHelper.getAccentColor(context)
        val bgColor = when {
            tileData.isTransparent -> Color.argb(120, Color.red(accentColor), Color.green(accentColor), Color.blue(accentColor))
            tileData.customColor != 0 -> tileData.customColor
            else -> accentColor
        }
        frontFace.setBackgroundColor(bgColor)
        backFace.setBackgroundColor(bgColor)

        // Icon size proportional to tile
        val iconSize = when (tileData.tileSize) {
            TileSize.SMALL -> dp(28)
            TileSize.MEDIUM -> dp(52)
            TileSize.WIDE -> dp(44)
            TileSize.LARGE -> dp(72)
        }
        iconView.layoutParams = (iconView.layoutParams as LayoutParams).also {
            it.width = iconSize
            it.height = iconSize
            // Shift icon upward on medium/large to leave room for label
            it.gravity = when (tileData.tileSize) {
                TileSize.SMALL -> Gravity.CENTER
                else -> Gravity.CENTER_HORIZONTAL or Gravity.TOP
            }
            it.topMargin = when (tileData.tileSize) {
                TileSize.SMALL -> 0
                TileSize.MEDIUM -> dp(20)
                TileSize.WIDE -> dp(14)
                TileSize.LARGE -> dp(32)
            }
        }

        loadIcon()
        updateBadge()
    }

    private fun loadIcon() {
        val pm = context.packageManager
        val icon: Drawable? = try {
            pm.getApplicationIcon(tileData.packageName)
        } catch (e: PackageManager.NameNotFoundException) {
            try {
                pm.getActivityIcon(android.content.ComponentName(tileData.packageName, tileData.activityName))
            } catch (e2: Exception) {
                null
            }
        }
        iconView.setImageDrawable(icon)
    }

    fun updateBadge(count: Int = tileData.badgeCount) {
        tileData.badgeCount = count
        if (count > 0 && tileData.tileSize != TileSize.SMALL) {
            badgeView.text = if (count > 99) "99+" else count.toString()
            badgeView.visibility = View.VISIBLE
            backCountView.text = count.toString()
        } else {
            badgeView.visibility = View.GONE
            backCountView.text = ""
        }
    }

    private fun scheduleLiveTileFlip() {
        if (!tileData.liveTileEnabled || tileData.tileSize == TileSize.SMALL) return
        val delay = (5000L + Random.nextLong(5000L))
        liveHandler.postDelayed({
            if (isAttachedToWindow && !isEditMode) {
                if (tileData.badgeCount > 0) {
                    flipTile()
                }
            }
            scheduleLiveTileFlip()
        }, delay)
    }

    fun flipTile() {
        val flipOut = ObjectAnimator.ofFloat(frontFace, "rotationY", 0f, 90f).apply { duration = 200 }
        val flipIn = ObjectAnimator.ofFloat(backFace, "rotationY", -90f, 0f).apply { duration = 200 }
        flipOut.addListener(object : android.animation.AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: android.animation.Animator) {
                frontFace.visibility = View.INVISIBLE
                backFace.visibility = View.VISIBLE
                flipIn.start()
                isShowingBack = true
                // Flip back after 3 seconds
                liveHandler.postDelayed({ flipBack() }, 3000)
            }
        })
        backFace.visibility = View.INVISIBLE
        flipOut.start()
    }

    private fun flipBack() {
        val flipOut = ObjectAnimator.ofFloat(backFace, "rotationY", 0f, 90f).apply { duration = 200 }
        val flipIn = ObjectAnimator.ofFloat(frontFace, "rotationY", -90f, 0f).apply { duration = 200 }
        flipOut.addListener(object : android.animation.AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: android.animation.Animator) {
                backFace.visibility = View.INVISIBLE
                frontFace.visibility = View.VISIBLE
                flipIn.start()
                isShowingBack = false
            }
        })
        flipOut.start()
    }

    fun setEditMode(edit: Boolean) {
        isEditMode = edit
        if (edit) {
            wobbleAnimator.start()
        } else {
            wobbleAnimator.cancel()
            rotation = 0f
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        liveHandler.removeCallbacksAndMessages(null)
        wobbleAnimator.cancel()
    }

    private fun dp(value: Int): Int =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value.toFloat(), resources.displayMetrics).toInt()
}
