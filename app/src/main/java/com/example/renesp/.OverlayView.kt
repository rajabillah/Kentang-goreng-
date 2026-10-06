package com.example.renesp

import android.content.Context
import android.graphics.*
import android.view.View
import kotlin.math.max

data class Det(val box: RectF, val label: String, val score: Float, val meters: Float)

class OverlayView(c: Context) : View(c) {
    private var dets: List<Det> = emptyList()
    private var imgW = 1; private var imgH = 1
    private val boxP = Paint().apply { style = Paint.Style.STROKE; strokeWidth = 4f }
    private val bgP = Paint().apply { color = Color.argb(200, 0, 0, 0) }
    private val txtP = Paint().apply { textSize = 38f; typeface = Typeface.MONOSPACE; isAntiAlias = true }
    private val linP = Paint().apply { strokeWidth = 3f; color = Color.RED }

    fun update(d: List<Det>, w: Int, h: Int) { dets = d; imgW = w; imgH = h; postInvalidate() }

    override fun onDraw(cv: Canvas) {
        val s = max(width / imgW.toFloat(), height / imgH.toFloat())
        val ox = (width - imgW * s) / 2f; val oy = (height - imgH * s) / 2f
        for (d in dets) {
            val col = if (d.meters < 20f) Color.RED else Color.GREEN
            boxP.color = col; txtP.color = col
            val r = RectF(d.box.left * s + ox, d.box.top * s + oy, d.box.right * s + ox, d.box.bottom * s + oy)
            cv.drawRect(r, boxP)
            val t = "${d.label} ${(d.score * 100).toInt()}% ${d.meters.toInt()}m"
            val tw = txtP.measureText(t)
            cv.drawRect(r.left, r.top - 46f, r.left + tw + 12f, r.top, bgP)
            cv.drawText(t, r.left + 6f, r.top - 10f, txtP)
            cv.drawLine(width / 2f, height.toFloat(), r.centerX(), r.bottom, linP)
        }
    }
}
