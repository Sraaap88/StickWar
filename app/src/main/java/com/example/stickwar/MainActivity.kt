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
    
    companion object {
        private const val PERMISSION_REQUEST_CODE = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        gameView = findViewById(R.id.gameView)
        
        if (checkPermissions()) {
            tryAlternativeVoiceRecognition()
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
                tryAlternativeVoiceRecognition()
            } else {
                fallbackToTouch()
            }
        }
    }
    
    private fun tryAlternativeVoiceRecognition() {
        try {
            // Essayer de créer le recognizer avec un package spécifique
            val recognizer = SpeechRecognizer.createSpeechRecognizer(this, 
                android.content.ComponentName("com.huawei.vassistant", 
                "com.huawei.vassistant.service.VoiceRecognitionService"))
            
            if (recognizer != null) {
                setupVoiceRecognition(recognizer)
                gameView?.setDebugMode("🎤 Reconnaissance Huawei - Parlez continuellement!")
            } else {
                tryDefaultRecognition()
            }
        } catch (e: Exception) {
            tryDefaultRecognition()
        }
    }
    
    private fun tryDefaultRecognition() {
        try {
            val recognizer = SpeechRecognizer.createSpeechRecognizer(this)
            if (recognizer != null) {
                setupVoiceRecognition(recognizer)
                gameView?.setDebugMode("🎤 Reconnaissance Android - Parlez continuellement!")
            } else {
                fallbackToTouch()
            }
        } catch (e: Exception) {
            gameView?.setDebugMode("❌ Erreur: ${e.message}")
            fallbackToTouch()
        }
    }
    
    private fun setupVoiceRecognition(recognizer: SpeechRecognizer) {
        speechRecognizer = recognizer
        
        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                gameView?.setDebugMode("🎤 Écoute continue - Dites: hop, paf, boum!")
            }
            
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            
            override fun onError(error: Int) {
                when (error) {
                    SpeechRecognizer.ERROR_NO_MATCH -> {
                        // Pas grave, on continue
                        restartListening()
                    }
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT,
                    SpeechRecognizer.ERROR_NETWORK -> {
                        gameView?.setDebugMode("⚠️ Erreur réseau - Redémarrage...")
                        restartListening()
                    }
                    SpeechRecognizer.ERROR_CLIENT -> {
                        gameView?.setDebugMode("❌ Service bloqué - Mode tactile")
                        fallbackToTouch()
                    }
                    else -> {
                        gameView?.setDebugMode("⚠️ Erreur $error - Redémarrage...")
                        restartListening()
                    }
                }
            }
            
            override fun onResults(results: Bundle?) {
                results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.let { matches ->
                    for (result in matches) {
                        val command = result.lowercase(Locale.getDefault()).trim()
                        if (handleVoiceCommand(command)) {
                            break // Commande trouvée, arrêter de chercher
                        }
                    }
                }
                // Redémarrer immédiatement pour écoute continue
                restartListening()
            }
            
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        
        startListening()
    }
    
    private fun startListening() {
        try {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "fr-FR")
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            }
            speechRecognizer?.startListening(intent)
            isListening = true
        } catch (e: Exception) {
            gameView?.setDebugMode("❌ Erreur démarrage: ${e.message}")
            fallbackToTouch()
        }
    }
    
    private fun restartListening() {
        if (isListening) {
            restartHandler.postDelayed({
                startListening()
            }, 100) // Redémarrage rapide
        }
    }
    
    private fun handleVoiceCommand(command: String): Boolean {
        return when {
            command.contains("hop") || command.contains("saute") || command.contains("saut") -> {
                gameView?.jump()
                gameView?.setDebugMode("🎤 HOP! Continuez à parler...")
                true
            }
            command.contains("paf") || command.contains("pan") || command.contains("tir") -> {
                gameView?.shoot()
                gameView?.setDebugMode("🎤 PAF! Continuez à parler...")
                true
            }
            command.contains("boum") || command.contains("boom") || command.contains("explose") -> {
                gameView?.explode()
                gameView?.setDebugMode("🎤 BOUM! Continuez à parler...")
                true
            }
            else -> false
        }
    }
    
    private fun fallbackToTouch() {
        isListening = false
        speechRecognizer?.destroy()
        gameView?.setDebugMode("🎮 Mode tactile: Gauche=HOP, Centre=PAF, Droite=BOUM")
    }
    
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (event?.action == MotionEvent.ACTION_DOWN && !isListening) {
            val screenWidth = resources.displayMetrics.widthPixels
            val x = event.x
            
            when {
                x < screenWidth / 3 -> {
                    gameView?.jump()
                    gameView?.setDebugMode("🎮 HOP! Gauche=HOP, Centre=PAF, Droite=BOUM")
                }
                x < screenWidth * 2 / 3 -> {
                    gameView?.shoot()
                    gameView?.setDebugMode("🎮 PAF! Gauche=HOP, Centre=PAF, Droite=BOUM")
                }
                else -> {
                    gameView?.explode()
                    gameView?.setDebugMode("🎮 BOUM! Gauche=HOP, Centre=PAF, Droite=BOUM")
                }
            }
            return true
        }
        return super.onTouchEvent(event)
    }
    
    override fun onDestroy() {
        super.onDestroy()
        isListening = false
        speechRecognizer?.destroy()
        restartHandler.removeCallbacksAndMessages(null)
    }
}
