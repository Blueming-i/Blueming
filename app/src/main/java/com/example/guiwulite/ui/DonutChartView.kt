package com.example.guiwulite.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.example.guiwulite.R
import kotlin.math.min

class DonutChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeWidth = 28f
        color = ContextCompat.getColor(context, R.color.chartTrack)
    }

    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeWidth = 28f
        color = ContextCompat.getColor(context, R.color.chartAccent)
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        color = ContextCompat.getColor(context, R.color.textPrimary)
    }

    private var ratio = 1f
    private var centerTop = ""
    private var centerBottom = ""

    fun setContent(ratio: Float, top: String, bottom: String) {
        this.ratio = ratio.coerceIn(0f, 1f)
        this.centerTop = top
        this.centerBottom = bottom
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val size = min(width, height).toFloat()
        val rect = RectF(
            (width - size) / 2f + 26f,
            (height - size) / 2f + 26f,
            (width + size) / 2f - 26f,
            (height + size) / 2f - 26f
        )
        canvas.drawArc(rect, 0f, 360f, false, trackPaint)
        canvas.drawArc(rect, -90f, 360f * ratio, false, progressPaint)

        textPaint.textSize = 28f
        canvas.drawText(centerTop, width / 2f, height / 2f - 8f, textPaint)
        textPaint.textSize = 40f
        canvas.drawText(centerBottom, width / 2f, height / 2f + 44f, textPaint)
    }
}
