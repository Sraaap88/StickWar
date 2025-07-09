package com.example.stickwar

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.Manifest
import android.content.pm.PackageManager
import android.speech.SpeechRecognizer
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {
    
    private var gameView: StickFigureGameView? = null
    
    companion object {
        private const val PERMISSION_REQUEST_CODE = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        gameView = findViewById(R.id.gameView)
        
        if (checkPermissions()) {
            // Just check if speech recognition is available
            if (SpeechRecognizer.isRecognitionAvailable(this)) {
                gameView?.setDebugMode("✅ Reconnaissance vocale disponible")
            } else {
                gameView?.setDebugMode("❌ Reconnaissance vocale non disponible")
            }
        } else {
            requestPermissions()
        }
    }
    
    private fun checkPermissions(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    }
    
    private fun requestPermissions() {
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), PERMISSION_REQUEST_CODE)
    }
    
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (SpeechRecognizer.isRecognitionAvailable(this)) {
                    gameView?.setDebugMode("✅ Permission accordée + Reconnaissance dispo")
                } else {
                    gameView?.setDebugMode("❌ Permission OK mais reconnaissance indispo")
                }
            } else {
                gameView?.setDebugMode("❌ Permission refusée")
            }
        }
    }
}
