package com.example.stickwar

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MotionEvent

class MainActivity : AppCompatActivity() {
    
    private var gameView: StickFigureGameView? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        gameView = findViewById(R.id.gameView)
        gameView?.setDebugMode("🎮 Tapez l'écran: Gauche=HOP, Centre=PAF, Droite=BOUM")
    }
    
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (event?.action == MotionEvent.ACTION_DOWN) {
            val screenWidth = resources.displayMetrics.widthPixels
            val x = event.x
            
            when {
                x < screenWidth / 3 -> {
                    // Zone gauche = HOP
                    gameView?.jump()
                    gameView?.setDebugMode("🎮 HOP! Tapez: Gauche=HOP, Centre=PAF, Droite=BOUM")
                }
                x < screenWidth * 2 / 3 -> {
                    // Zone centre = PAF
                    gameView?.shoot()
                    gameView?.setDebugMode("🎮 PAF! Tapez: Gauche=HOP, Centre=PAF, Droite=BOUM")
                }
                else -> {
                    // Zone droite = BOUM
                    gameView?.explode()
                    gameView?.setDebugMode("🎮 BOUM! Tapez: Gauche=HOP, Centre=PAF, Droite=BOUM")
                }
            }
            return true
        }
        return super.onTouchEvent(event)
    }
}
