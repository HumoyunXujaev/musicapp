package com.humoyun.musicapp.ui.view

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import android.view.animation.LinearInterpolator
import androidx.core.content.ContextCompat
import com.humoyun.musicapp.R
import kotlin.random.Random

class LiveVisualizerView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.getThemeColor(R.attr.humoPrimary)
//        color = ContextCompat.getColor(context, R.color.humo_accent)
        style = Paint.Style.FILL
        strokeCap = Paint.Cap.ROUND
        strokeWidth = 8f
    }

    private fun Context.getThemeColor(attrId: Int): Int {
        val typedValue = TypedValue()
        theme.resolveAttribute(attrId, typedValue, true)
        return typedValue.data
    }

    private val barCount = 5
    private val heights = FloatArray(barCount)
    private val targetHeights = FloatArray(barCount)
    private var isPlaying = false
    private var animator: ValueAnimator? = null

    init {
        startAnimation()
    }

    fun setPlaying(playing: Boolean) {
        if (this.isPlaying == playing) return
        this.isPlaying = playing
        if (playing) {
            animator?.resume()
        } else {
            animator?.pause()
            // Flatten bars when paused
            for (i in heights.indices) heights[i] = 10f
            invalidate()
        }
    }

    private fun startAnimation() {
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 150
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener {
                if (isPlaying) {
                    updateHeights()
                    invalidate()
                }
            }
            start()
        }
    }

    private fun updateHeights() {
        for (i in heights.indices) {
            if (kotlin.math.abs(heights[i] - targetHeights[i]) < 5f) {
                targetHeights[i] = Random.nextFloat() * (height * 0.8f) + 10f
            }
            heights[i] += (targetHeights[i] - heights[i]) * 0.2f
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val totalWidth = width.toFloat()
        // Center the visualization
        val gap = 12f
        val contentWidth = (barCount * barPaint.strokeWidth) + ((barCount - 1) * gap)
        var startX = (totalWidth - contentWidth) / 2

        val centerY = height / 2f

        for (i in 0 until barCount) {
            val h = heights[i]
            canvas.drawLine(startX, centerY - (h / 2), startX, centerY + (h / 2), barPaint)
            startX += barPaint.strokeWidth + gap
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        animator?.cancel()
    }
}