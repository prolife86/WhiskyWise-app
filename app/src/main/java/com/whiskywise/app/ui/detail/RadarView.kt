package com.whiskywise.app.ui.detail

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * A heptagonal (7-axis) radar / spider chart matching the WhiskyWise web flavour profile chart.
 * Axes: woody, smoky, cereal, floral, fruity, medicinal, fiery  (0–5 scale)
 */
class RadarView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0,
) : View(context, attrs, defStyle) {

    private val labels  = listOf("Woody", "Smoky", "Cereal", "Floral", "Fruity", "Medicinal", "Fiery")
    private val values  = IntArray(7) { 0 }
    private val levels  = 5

    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color  = Color.parseColor("#c8a96e")
        style  = Paint.Style.STROKE
        strokeWidth = 1f
        alpha  = 100
    }
    private val dataPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color  = Color.parseColor("#c8a96e")
        style  = Paint.Style.FILL
        alpha  = 60
    }
    private val dataStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color  = Color.parseColor("#c8a96e")
        style  = Paint.Style.STROKE
        strokeWidth = 2.5f
    }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color    = Color.parseColor("#c8a96e")
        textAlign = Paint.Align.CENTER
        // textSize is set dynamically in onDraw based on the view radius
        // so it scales correctly on all screen sizes and densities.
    }

    fun setValues(woody: Int, smoky: Int, cereal: Int, floral: Int, fruity: Int, medicinal: Int, fiery: Int) {
        values[0] = woody; values[1] = smoky; values[2] = cereal
        values[3] = floral; values[4] = fruity; values[5] = medicinal; values[6] = fiery
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cx = width / 2f
        val cy = height / 2f
        val r  = min(cx, cy) * 0.55f
        val n  = labels.size

        // Scale text size and label offset proportionally to the chart radius
        // so labels never clip on large screens (tablets) or small ones (phones).
        labelPaint.textSize = r * 0.18f
        val labelOffset = r * 0.28f

        fun point(idx: Int, radius: Float): Pair<Float, Float> {
            val angle = (idx.toDouble() / n * 2 * Math.PI) - (Math.PI / 2)
            return Pair(cx + radius * cos(angle).toFloat(), cy + radius * sin(angle).toFloat())
        }

        // Draw grid rings
        for (lvl in 1..levels) {
            val path = Path()
            val ringR = r * lvl / levels
            for (i in 0 until n) {
                val (x, y) = point(i, ringR)
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            path.close()
            canvas.drawPath(path, gridPaint)
        }

        // Draw spokes
        for (i in 0 until n) {
            val (x, y) = point(i, r)
            canvas.drawLine(cx, cy, x, y, gridPaint)
        }

        // Draw data polygon.
        // Use a minimum ratio of 0.04 so that even a value of 0 places the vertex
        // slightly off-centre — this makes the polygon visible and avoids the
        // degenerate case where all-zero values collapse to a single invisible point.
        val minRatio = 0.04f
        val dataPath = Path()
        for (i in 0 until n) {
            val ratio  = maxOf(values[i].coerceIn(0, levels).toFloat() / levels, minRatio)
            val (x, y) = point(i, r * ratio)
            if (i == 0) dataPath.moveTo(x, y) else dataPath.lineTo(x, y)
        }
        dataPath.close()
        canvas.drawPath(dataPath, dataPaint)
        canvas.drawPath(dataPath, dataStrokePaint)

        // Draw labels
        for (i in 0 until n) {
            val (x, y) = point(i, r + labelOffset)
            canvas.drawText(labels[i], x, y + labelPaint.textSize / 3, labelPaint)
        }
    }
}
