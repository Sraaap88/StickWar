package com.example.stickwar

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MotionEvent
import android.Manifest
import android.content.pm.PackageManager
import android.content.Intent
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.os.Handler
import android.os.Looper
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.util.*

class MainActivity : AppCompatActivity() {
    
    private var gameView: StickFigureGameView? = null
    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false
    private var restartHandler = Handler(Looper.getMainLooper())
    private var restartAttempts = 0
    private var maxRestartAttempts = 5
    
    companion object {
        private const val PERMISSION_REQUEST_CODE = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        gameView = findViewById(R.id.gameView)
        
        // DIAGNOSTIC COMPLET - ÉTAPE PAR ÉTAPE
        gameView?.setDebugMode("🔍 Starting diagnostic...")
        
        // Étape 1: Vérifier disponibilité
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            gameView?.setDebugMode("❌ Speech recognition NOT available on this device")
            fallbackToTouch()
            return
        }
        gameView?.setDebugMode("✅ Speech recognition available - checking permissions...")
        
        // Étape 2: Vérifier permissions
        if (checkPermissions()) {
            gameView?.setDebugMode("✅ Audio permission granted - setting up voice...")
            setupVoiceRecognition()
        } else {
            gameView?.setDebugMode("⚠️ Requesting audio permission...")
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
                gameView?.setDebugMode("✅ Permission granted - setting up voice...")
                setupVoiceRecognition()
            } else {
                gameView?.setDebugMode("❌ Audio permission DENIED - touch mode only")
                fallbackToTouch()
            }
        }
    }
    
    private fun setupVoiceRecognition() {
        gameView?.setDebugMode("🔧 Creating speech recognizer...")
        
        try {
            // Utiliser le recognizer par défaut (plus fiable)
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
            
            if (speechRecognizer == null) {
                gameView?.setDebugMode("❌ FAILED to create speech recognizer")
                fallbackToTouch()
                return
            }
            
            gameView?.setDebugMode("✅ Speech recognizer created - setting up listener...")
            
            speechRecognizer?.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    gameView?.setDebugMode("🎤 Listening - Say: GO, FIRE, BOOM!")
                    restartAttempts = 0 // Reset compteur en cas de succès
                }
                
                override fun onBeginningOfSpeech() {
                    gameView?.setDebugMode("🎤 Speech detected...")
                }
                
                override fun onRmsChanged(rmsdB: Float) {
                    // Indicateur visuel du niveau sonore
                    gameView?.updateSoundLevel(rmsdB)
                }
                
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {}
                
                override fun onError(error: Int) {
                    val errorMessage = when (error) {
                        SpeechRecognizer.ERROR_NO_MATCH -> "No words recognized"
                        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                        SpeechRecognizer.ERROR_NETWORK -> "Network error"
                        SpeechRecognizer.ERROR_AUDIO -> "Audio error"
                        SpeechRecognizer.ERROR_SERVER -> "Server error"
                        SpeechRecognizer.ERROR_CLIENT -> "Client error"
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Speech timeout"
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
                        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognizer busy"
                        SpeechRecognizer.ERROR_TOO_MANY_REQUESTS -> "Too many requests"
                        else -> "Unknown error ($error)"
                    }
                    
                    gameView?.setDebugMode("⚠️ $errorMessage")
                    
                    // Gestion intelligente des erreurs
                    when (error) {
                        SpeechRecognizer.ERROR_NO_MATCH -> {
                            // Pas grave, on continue
                            scheduleRestart(500) // Délai plus long
                        }
                        SpeechRecognizer.ERROR_CLIENT,
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> {
                            // Fatal errors
                            gameView?.setDebugMode("❌ Fatal error - Touch mode")
                            fallbackToTouch()
                        }
                        else -> {
                            // Other errors - try to restart
                            if (restartAttempts < maxRestartAttempts) {
                                scheduleRestart(1000) // Even longer delay
                            } else {
                                gameView?.setDebugMode("❌ Too many errors - Touch mode")
                                fallbackToTouch()
                            }
                        }
                    }
                }
                
                override fun onResults(results: Bundle?) {
                    results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.let { matches ->
                        gameView?.setDebugMode("🎤 Results: ${matches.joinToString(", ")}")
                        
                        for (result in matches) {
                            val command = result.lowercase(Locale.getDefault()).trim()
                            if (handleVoiceCommand(command)) {
                                break // Command found
                            }
                        }
                    }
                    
                    // Restart for continuous listening
                    scheduleRestart(300) // Reasonable delay
                }
                
                override fun onPartialResults(partialResults: Bundle?) {
                    // Show partial results for debugging
                    partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.let { matches ->
                        if (matches.isNotEmpty()) {
                            gameView?.setDebugMode("🎤 Partial: ${matches[0]}")
                        }
                    }
                }
                
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
            
            gameView?.setDebugMode("✅ Listener set - starting first listen...")
            startListening()
            
        } catch (e: Exception) {
            gameView?.setDebugMode("❌ Setup error: ${e.message}")
            fallbackToTouch()
        }
    }
    
    private fun startListening() {
        if (!isListening) {
            gameView?.setDebugMode("🎤 Attempting to start listening...")
            
            try {
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1000)
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1000)
                }
                
                speechRecognizer?.startListening(intent)
                isListening = true
                gameView?.setDebugMode("🎤 startListening() called - waiting for onReadyForSpeech...")
                
            } catch (e: Exception) {
                gameView?.setDebugMode("❌ Start error: ${e.message}")
                fallbackToTouch()
            }
        } else {
            gameView?.setDebugMode("⚠️ Already listening - skipping start")
        }
    }
    
    private fun scheduleRestart(delayMs: Long) {
        restartAttempts++
        restartHandler.postDelayed({
            if (isListening) {
                isListening = false
                startListening()
            }
        }, delayMs)
    }
    
    private fun handleVoiceCommand(command: String): Boolean {
        return when {
            // Simple English words that all phones understand
            command.contains("go") || command.contains("jump") || command.contains("up") -> {
                gameView?.jump()
                gameView?.setDebugMode("🎤 JUMP detected! Say: GO, FIRE, BOOM")
                true
            }
            command.contains("fire") || command.contains("shoot") || command.contains("bang") -> {
                gameView?.shoot()
                gameView?.setDebugMode("🎤 FIRE detected! Say: GO, FIRE, BOOM")
                true
            }
            command.contains("boom") || command.contains("bomb") || command.contains("explode") -> {
                gameView?.explode()
                gameView?.setDebugMode("🎤 BOOM detected! Say: GO, FIRE, BOOM")
                true
            }
            else -> false
        }
    }
    
    private fun fallbackToTouch() {
        isListening = false
        speechRecognizer?.destroy()
        speechRecognizer = null
        restartHandler.removeCallbacksAndMessages(null)
        gameView?.setDebugMode("🎮 Touch mode: Left=JUMP, Center=FIRE, Right=BOOM")
    }
    
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (event?.action == MotionEvent.ACTION_DOWN) {
            val screenWidth = resources.displayMetrics.widthPixels
            val x = event.x
            
            when {
                x < screenWidth / 3 -> {
                    gameView?.jump()
                    gameView?.setDebugMode("🎮 JUMP! Touch to continue...")
                }
                x < screenWidth * 2 / 3 -> {
                    gameView?.shoot()
                    gameView?.setDebugMode("🎮 FIRE! Touch to continue...")
                }
                else -> {
                    gameView?.explode()
                    gameView?.setDebugMode("🎮 BOOM! Touch to continue...")
                }
            }
            return true
        }
        return super.onTouchEvent(event)
    }
    
    override fun onPause() {
        super.onPause()
        if (isListening) {
            speechRecognizer?.cancel()
            isListening = false
        }
    }
    
    override fun onResume() {
        super.onResume()
        if (speechRecognizer != null && !isListening) {
            startListening()
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        isListening = false
        speechRecognizer?.destroy()
        restartHandler.removeCallbacksAndMessages(null)
    }
}
