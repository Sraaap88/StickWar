package com.example.stickwar

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import kotlin.math.*

class StickFigureGameView(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {

    private val blackPaint = Paint().apply { color = Color.BLACK }
    private val whitePaint = Paint().apply { color = Color.WHITE }
    private val stickPaint = Paint().apply {
        color = Color.BLACK
        strokeWidth = 8f
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    private var stickX = 0f
    private var stickY = 0f
    private var centerY = 0f

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        centerY = h / 2f
        stickX = w / 2f
        stickY = centerY
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        // Fond mi-noir mi-blanc horizontal
        canvas.drawRect(0f, 0f, width.toFloat(), height/2f, blackPaint)
        canvas.drawRect(0f, height/2f, width.toFloat(), height.toFloat(), whitePaint)
        
        // Bonhomme allumette
        drawStickFigure(canvas, stickX, stickY)
    }
    
    private fun drawStickFigure(canvas: Canvas, x: Float, y: Float) {
        val size = 100f
        
        // Couleur selon position
        stickPaint.color = if (y < centerY) Color.WHITE else Color.BLACK
        
        // Tête
        canvas.drawCircle(x, y - size/2, size/8, stickPaint)
        
        // Corps
        canvas.drawLine(x, y - size/3, x, y + size/3, stickPaint)
        
        // Bras
        canvas.drawLine(x, y - size/6, x - size/4, y, stickPaint)
        canvas.drawLine(x, y - size/6, x + size/4, y, stickPaint)
        
        // Jambes
        canvas.drawLine(x, y + size/3, x - size/4, y + size/2, stickPaint)
        canvas.drawLine(x, y + size/3, x + size/4, y + size/2, stickPaint)
    }
}
