package com.humoyun.musicapp.ui.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import com.humoyun.musicapp.R
import kotlin.math.abs

class WaveformSeekBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // Colors
    private val playedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.humo_primary)
        style = Paint.Style.FILL
    }
    private val remainingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.getThemeColor(R.attr.humoTextSecondary)
//        color = ContextCompat.getColor(context, R.color.text_secondary)
        alpha = 80 // Dimmer for unplayed part
        style = Paint.Style.FILL
    }

    private fun Context.getThemeColor(attrId: Int): Int {
        val typedValue = TypedValue()
        theme.resolveAttribute(attrId, typedValue, true)
        return typedValue.data
    }

    private val scrubberPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.white)
        strokeWidth = 4f
        style = Paint.Style.STROKE
    }

    // Data
    private var rawAmplitudes: List<Float> = emptyList()
    private var filteredAmplitudes: List<Float> = emptyList()

    // Progress (0.0 to 1.0)
    private var progress: Float = 0f

    // Interaction
    private var onSeekListener: ((Long) -> Unit)? = null
    private var onSeekingListener: ((Long) -> Unit)? = null
    private var isDragging = false
    private var dragX = 0f
    private var duration: Long = 0

    // Dimensions
    private val barWidth = 8f // Width of each bar
    private val barGap = 4f   // Gap between bars
    private val barRadius = 4f
    private val minBarHeight = 10f


    fun setWaveform(amplitudes: List<Float>, duration: Long) {
        this.rawAmplitudes = amplitudes
        this.duration = duration

        requestLayout()
        invalidate()
    }

    fun setProgress(currentPosition: Long) {
        if (!isDragging && duration > 0) {
            this.progress = (currentPosition.toFloat() / duration).coerceIn(0f, 1f)
            invalidate()
        }
    }

    fun setOnSeekListener(listener: (Long) -> Unit) {
        this.onSeekListener = listener
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (rawAmplitudes.isNotEmpty()) {
            resampleAmplitudes(w)
        }
    }

    private fun resampleAmplitudes(width: Int) {
        if (width == 0) return

        // Calculate how many bars fit in the width
        val totalBars = (width / (barWidth + barGap)).toInt()
        if (totalBars == 0 || rawAmplitudes.isEmpty()) return

        // Simple downsampling: mapped raw indices to visual bars
        val sampled = FloatArray(totalBars)
        val step = rawAmplitudes.size.toFloat() / totalBars

        for (i in 0 until totalBars) {
            val index = (i * step).toInt().coerceIn(rawAmplitudes.indices)
            sampled[i] = rawAmplitudes[index]
        }

        filteredAmplitudes = sampled.toList()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (filteredAmplitudes.isEmpty() && rawAmplitudes.isNotEmpty()) {
            resampleAmplitudes(width)
        }

        if (filteredAmplitudes.isEmpty()) return

        val centerY = height / 2f
        val maxBarHeight = height * 0.9f

        filteredAmplitudes.forEachIndexed { index, amplitude ->
            // Calculate bar coordinates
            val startX = index * (barWidth + barGap)
            if (startX + barWidth > width) return@forEachIndexed

            // Height scaling: amplitude (0..1) * maxHeight, but keep a minimum height
            val barHeight = (maxBarHeight * amplitude).coerceAtLeast(minBarHeight)
            val top = centerY - (barHeight / 2)
            val bottom = centerY + (barHeight / 2)

            val barCenterRatio = startX / width.toFloat()
            val currentProgressRatio = if (isDragging) (dragX / width) else progress

            val paint = if (barCenterRatio <= currentProgressRatio) playedPaint else remainingPaint

            canvas.drawRoundRect(
                RectF(startX, top, startX + barWidth, bottom),
                barRadius, barRadius,
                paint
            )
        }

        if (isDragging) {
            canvas.drawLine(dragX, 0f, dragX, height.toFloat(), scrubberPaint)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                isDragging = true
                updateDragPosition(event.x)
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                updateDragPosition(event.x)
                return true
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                isDragging = false
                val finalProgress = (event.x / width).coerceIn(0f, 1f)
                val seekPos = (finalProgress * duration).toLong()
                onSeekListener?.invoke(seekPos)
                invalidate()
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun updateDragPosition(x: Float) {
        dragX = x.coerceIn(0f, width.toFloat())
        invalidate()
    }
}