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
    private var debugMessage = "🎮 StickWar - Reconnaissance vocale"
    private var soundLevel = 0f
    private var walkCycle = 0f

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        centerY = h / 2f
        stickX = w / 2f
        stickY = centerY
        baseY = centerY
        
        startAnimation()
    }
    
    private fun startAnimation() {
        val runnable = object : Runnable {
            override fun run() {
                update()
                invalidate()
                postDelayed(this, 16)
            }
        }
        post(runnable)
    }
    
    private fun update() {
        val deltaTime = 0.016f
        animationTime += deltaTime * 3f
        
        stickX += movementSpeed * movementDirection * deltaTime
        if (stickX > width - 100) {
            movementDirection = -1f
            stickX = width - 100f
        } else if (stickX < 100) {
            movementDirection = 1f
            stickX = 100f
        }
        
        walkCycle += deltaTime * 6f
        
        if (isJumping) {
            stickY += velocityY * deltaTime
            velocityY += 800f * deltaTime
            
            if (stickY >= baseY) {
                stickY = baseY
                isJumping = false
                velocityY = 0f
                scale = 1f
            }
        }
        
        if (actionTimer > 0) {
            actionTimer -= deltaTime
            if (actionTimer <= 0) {
                actionAnimation = ""
            }
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        canvas.drawRect(0f, 0f, width.toFloat(), height/2f, blackPaint)
        canvas.drawRect(0f, height/2f, width.toFloat(), height.toFloat(), whitePaint)
        
        val linePaint = Paint().apply {
            color = Color.GRAY
            strokeWidth = 4f
        }
        canvas.drawLine(0f, height/2f, width.toFloat(), height/2f, linePaint)
        
        drawStickFigure(canvas, stickX, stickY)
        
        debugPaint.color = if (stickY < centerY) Color.WHITE else Color.BLACK
        canvas.drawText(debugMessage, 20f, 60f, debugPaint)
        canvas.drawText("Action: $lastAction", 20f, height - 20f, debugPaint)
    }
    
    private fun drawStickFigure(canvas: Canvas, x: Float, y: Float) {
        val size = 120f * scale
        
        stickPaint.color = if (y < centerY) Color.WHITE else Color.BLACK
        
        val wobble = when (actionAnimation) {
            "JUMP" -> sin(animationTime * 10) * 5f
            "SHOOT" -> sin(animationTime * 20) * 3f
            "EXPLODE" -> sin(animationTime * 30) * 8f
            else -> sin(walkCycle * 0.5f) * 1f
        }
        
        canvas.save()
        canvas.translate(wobble, 0f)
        
        val headRadius = size/8
        canvas.drawCircle(x, y - size * 0.4f, headRadius, stickPaint)
        
        val bodyTop = y - size * 0.3f
        val bodyBottom = y + size * 0.1f
        canvas.drawLine(x, bodyTop, x, bodyBottom, stickPaint)
        
        val shoulderY = bodyTop + size * 0.1f
        val armLength = size * 0.25f
        
        val armSwing = when (actionAnimation) {
            "SHOOT" -> 0f
            "EXPLODE" -> sin(animationTime * 20) * 30f
            else -> sin(walkCycle) * 15f
        }
        
        val leftElbowX = x - armLength * cos(Math.toRadians((45f + armSwing).toDouble())).toFloat()
        val leftElbowY = shoulderY + armLength * sin(Math.toRadians((45f + armSwing).toDouble())).toFloat() * 0.5f
        val leftHandX = leftElbowX - armLength * 0.7f * cos(Math.toRadians((90f - armSwing).toDouble())).toFloat()
        val leftHandY = leftElbowY + armLength * 0.7f * sin(Math.toRadians((90f - armSwing).toDouble())).toFloat()
        
        canvas.drawLine(x, shoulderY, leftElbowX, leftElbowY, stickPaint)
        canvas.drawLine(leftElbowX, leftElbowY, leftHandX, leftHandY, stickPaint)
        
        val rightElbowX = x + armLength * cos(Math.toRadians((45f - armSwing).toDouble())).toFloat()
        val rightElbowY = shoulderY + armLength * sin(Math.toRadians((45f - armSwing).toDouble())).toFloat() * 0.5f
        val rightHandX = rightElbowX + armLength * 0.7f * cos(Math.toRadians((90f + armSwing).toDouble())).toFloat()
        val rightHandY = rightElbowY + armLength * 0.7f * sin(Math.toRadians((90f + armSwing).toDouble())).toFloat()
        
        canvas.drawLine(x, shoulderY, rightElbowX, rightElbowY, stickPaint)
        canvas.drawLine(rightElbowX, rightElbowY, rightHandX, rightHandY, stickPaint)
        
        val hipY = bodyBottom
        val legLength = size * 0.3f
        
        val leftLegSwing = sin(walkCycle) * 25f
        val rightLegSwing = sin(walkCycle + PI.toFloat()) * 25f
        
        val leftKneeX = x - legLength * 0.6f * sin(Math.toRadians(leftLegSwing.toDouble())).toFloat()
        val leftKneeY = hipY + legLength * 0.6f
        val leftFootX = leftKneeX - legLength * 0.8f * sin(Math.toRadians((leftLegSwing * 0.7f).toDouble())).toFloat()
        val leftFootY = hipY + legLength * 1.2f
        
        canvas.drawLine(x, hipY, leftKneeX, leftKneeY, stickPaint)
        canvas.drawLine(leftKneeX, leftKneeY, leftFootX, leftFootY, stickPaint)
        
        val rightKneeX = x + legLength * 0.6f * sin(Math.toRadians(rightLegSwing.toDouble())).toFloat()
        val rightKneeY = hipY + legLength * 0.6f
        val rightFootX = rightKneeX + legLength * 0.8f * sin(Math.toRadians((rightLegSwing * 0.7f).toDouble())).toFloat()
        val rightFootY = hipY + legLength * 1.2f
        
        canvas.drawLine(x, hipY, rightKneeX, rightKneeY, stickPaint)
        canvas.drawLine(rightKneeX, rightKneeY, rightFootX, rightFootY, stickPaint)
        
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
    
    fun setDebugMode(message: String) {
        debugMessage = message
    }
    
    fun updateSoundLevel(rmsdB: Float) {
        soundLevel = rmsdB
    }
}
