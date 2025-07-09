ction = "EXPLOSION!"
    }
}
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
    private val debugPaint = Paint().apply {
        color = Color.YELLOW
        textSize = 40f
        typeface = Typeface.DEFAULT_BOLD
    }

    private var stickX = 0f
    private var stickY = 0f
    private var baseY = 0f
    private var centerY = 0f
    private var velocityY = 0f
    private var isJumping = false
    private var scale = 1f
    private var actionAnimation = ""
    private var actionTimer = 0f
    private var animationTime = 0f
    private var movementSpeed = 200f
    private var movementDirection = 1f
    private var lastAction = "En attente..."

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        centerY = h / 2f
        stickX = w / 2f
        stickY = centerY
        baseY = centerY
        
        // Démarrer l'animation
        startAnimation()
    }
    
    private fun startAnimation() {
        val runnable = object : Runnable {
            override fun run() {
                update()
                invalidate()
                postDelayed(this, 16) // 60 FPS
            }
        }
        post(runnable)
    }
    
    private fun update() {
        val deltaTime = 0.016f
        animationTime += deltaTime * 3f
        
        // Mouvement automatique
        stickX += movementSpeed * movementDirection * deltaTime
        if (stickX > width - 100) {
            movementDirection = -1f
            stickX = width - 100f
        } else if (stickX < 100) {
            movementDirection = 1f
            stickX = 100f
        }
        
        // Physique du saut
        if (isJumping) {
            stickY += velocityY * deltaTime
            velocityY += 800f * deltaTime // Gravité
            
            if (stickY >= baseY) {
                stickY = baseY
                isJumping = false
                velocityY = 0f
                scale = 1f
            }
        }
        
        // Timer d'action
        if (actionTimer > 0) {
            actionTimer -= deltaTime
            if (actionTimer <= 0) {
                actionAnimation = ""
            }
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        // Fond mi-noir mi-blanc horizontal
        canvas.drawRect(0f, 0f, width.toFloat(), height/2f, blackPaint)
        canvas.drawRect(0f, height/2f, width.toFloat(), height.toFloat(), whitePaint)
        
        // Ligne de séparation
        val linePaint = Paint().apply {
            color = Color.GRAY
            strokeWidth = 4f
        }
        canvas.drawLine(0f, height/2f, width.toFloat(), height/2f, linePaint)
        
        // Bonhomme allumette
        drawStickFigure(canvas, stickX, stickY)
        
        // Debug
        debugPaint.color = if (stickY < centerY) Color.WHITE else Color.BLACK
        canvas.drawText("🎮 StickWar - Dites: hop, paf, boum", 20f, 60f, debugPaint)
        canvas.drawText("Dernière action: $lastAction", 20f, height - 20f, debugPaint)
    }
    
    private fun drawStickFigure(canvas: Canvas, x: Float, y: Float) {
        val size = 120f * scale
        
        // Couleur selon position
        stickPaint.color = if (y < centerY) Color.WHITE else Color.BLACK
        
        // Animation selon l'action
        val wobble = when (actionAnimation) {
            "JUMP" -> sin(animationTime * 10) * 5f
            "SHOOT" -> sin(animationTime * 20) * 3f
            "EXPLODE" -> sin(animationTime * 30) * 8f
            else -> sin(animationTime) * 2f
        }
        
        canvas.save()
        canvas.translate(wobble, 0f)
        
        // Tête
        canvas.drawCircle(x, y - size/2, size/8, stickPaint)
        
        // Corps
        canvas.drawLine(x, y - size/3, x, y + size/3, stickPaint)
        
        // Bras avec animation
        val armOffset = when (actionAnimation) {
            "SHOOT" -> 20f
            "EXPLODE" -> 30f
            else -> 0f
        }
        canvas.drawLine(x, y - size/6, x - size/4, y + armOffset, stickPaint)
        canvas.drawLine(x, y - size/6, x + size/4, y - armOffset, stickPaint)
        
        // Jambes avec marche
        val legOffset = sin(animationTime * 4) * 10f
        canvas.drawLine(x, y + size/3, x - size/4 + legOffset, y + size/2, stickPaint)
        canvas.drawLine(x, y + size/3, x + size/4 - legOffset, y + size/2, stickPaint)
        
        canvas.restore()
    }
    
    fun jump() {
        if (!isJumping) {
            isJumping = true
            velocityY = -600f
            scale = 1.2f
            actionAnimation = "JUMP"
            actionTimer = 0.5f
            lastAction = "SAUT!"
        }
    }
    
    fun shoot() {
        actionAnimation = "SHOOT"
        actionTimer = 0.3f
        scale = 1.1f
        lastAction = "TIR!"
    }
    
    fun explode() {
        actionAnimation = "EXPLODE"
        actionTimer = 0.8f
        scale = 1.5f
        lastAction = "EXPLOSION!"
    }
}
