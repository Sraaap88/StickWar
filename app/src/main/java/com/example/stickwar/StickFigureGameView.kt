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
        stickY = h * 0.75f  // Plus bas pour qu'il marche sur le sol
        baseY = h * 0.75f   // Sol à 75% de l'écran
        
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
        
        // Fond blanc en haut, noir en bas (sol)
        val groundY = height * 0.6f  // Sol à 60% de l'écran
        canvas.drawRect(0f, 0f, width.toFloat(), groundY, whitePaint)
        canvas.drawRect(0f, groundY, width.toFloat(), height.toFloat(), blackPaint)
        
        // Ligne de sol
        val linePaint = Paint().apply {
            color = Color.GRAY
            strokeWidth = 6f
        }
        canvas.drawLine(0f, groundY, width.toFloat(), groundY, linePaint)
        
        drawStickFigure(canvas, stickX, stickY)
        
        // Debug toujours visible
        debugPaint.color = Color.RED
        canvas.drawText(debugMessage, 20f, 60f, debugPaint)
        canvas.drawText("Action: $lastAction", 20f, height - 60f, debugPaint)
    }
    
    private fun drawStickFigure(canvas: Canvas, x: Float, y: Float) {
        val size = 120f * scale
        
        // Bonhomme TOUJOURS en rouge pour être visible partout
        stickPaint.color = Color.RED
        stickPaint.strokeWidth = 10f  // Plus épais pour mieux voir
        
        // Attitude - balancement du corps
        val bodySwagger = sin(walkCycle * 0.3f) * 3f
        
        canvas.save()
        canvas.translate(bodySwagger, 0f)
        
        // === TÊTE AVEC ATTITUDE ===
        val headRadius = size/8
        val headY = y - size * 0.8f  // Plus haut
        canvas.drawCircle(x, headY, headRadius, stickPaint)
        
        // === CORPS ===
        val bodyTop = y - size * 0.7f
        val bodyBottom = y - size * 0.2f
        canvas.drawLine(x, bodyTop, x, bodyBottom, stickPaint)
        
        // === BRAS AVEC COUDES ===
        val shoulderY = bodyTop + size * 0.15f
        val armLength = size * 0.3f
        
        val armSwing = when (actionAnimation) {
            "SHOOT" -> 45f
            "EXPLODE" -> sin(animationTime * 25) * 60f
            "JUMP" -> -30f
            else -> sin(walkCycle) * 20f + 10f
        }
        
        // BRAS GAUCHE
        val leftShoulderAngle = 30f + armSwing
        val leftElbowX = x - armLength * 0.6f * cos(Math.toRadians(leftShoulderAngle.toDouble())).toFloat()
        val leftElbowY = shoulderY + armLength * 0.6f * sin(Math.toRadians(leftShoulderAngle.toDouble())).toFloat()
        
        val leftForearmAngle = leftShoulderAngle + 45f
        val leftHandX = leftElbowX - armLength * 0.5f * cos(Math.toRadians(leftForearmAngle.toDouble())).toFloat()
        val leftHandY = leftElbowY + armLength * 0.5f * sin(Math.toRadians(leftForearmAngle.toDouble())).toFloat()
        
        canvas.drawLine(x, shoulderY, leftElbowX, leftElbowY, stickPaint)
        canvas.drawLine(leftElbowX, leftElbowY, leftHandX, leftHandY, stickPaint)
        
        // BRAS DROIT
        val rightShoulderAngle = 150f - armSwing
        val rightElbowX = x + armLength * 0.6f * cos(Math.toRadians(rightShoulderAngle.toDouble())).toFloat()
        val rightElbowY = shoulderY + armLength * 0.6f * sin(Math.toRadians(rightShoulderAngle.toDouble())).toFloat()
        
        val rightForearmAngle = rightShoulderAngle - 45f
        val rightHandX = rightElbowX + armLength * 0.5f * cos(Math.toRadians(rightForearmAngle.toDouble())).toFloat()
        val rightHandY = rightElbowY + armLength * 0.5f * sin(Math.toRadians(rightForearmAngle.toDouble())).toFloat()
        
        canvas.drawLine(x, shoulderY, rightElbowX, rightElbowY, stickPaint)
        canvas.drawLine(rightElbowX, rightElbowY, rightHandX, rightHandY, stickPaint)
        
        // === JAMBES AVEC GENOUX - ANGLES CORRIGÉS ===
        val hipY = bodyBottom
        val legLength = size * 0.4f
        
        // Démarche réaliste vers l'AVANT/ARRIÈRE
        val leftLegCycle = walkCycle
        val rightLegCycle = walkCycle + PI.toFloat()
        
        val leftLegForward = sin(leftLegCycle) * 25f  // Avant/arrière
        val rightLegForward = sin(rightLegCycle) * 25f
        
        // JAMBE GAUCHE - marche vers l'avant
        val leftKneeX = x + leftLegForward * 0.3f  // Genou suit le mouvement
        val leftKneeY = hipY + legLength * 0.5f
        val leftFootX = x + leftLegForward  // Pied va plus loin
        val leftFootY = y  // Sur le sol
        
        canvas.drawLine(x, hipY, leftKneeX, leftKneeY, stickPaint)
        canvas.drawLine(leftKneeX, leftKneeY, leftFootX, leftFootY, stickPaint)
        
        // JAMBE DROITE - marche vers l'avant
        val rightKneeX = x + rightLegForward * 0.3f
        val rightKneeY = hipY + legLength * 0.5f
        val rightFootX = x + rightLegForward
        val rightFootY = y  // Sur le sol
        
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
