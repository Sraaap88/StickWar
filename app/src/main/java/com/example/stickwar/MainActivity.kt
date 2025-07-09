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
        
        // Vérifier si la reconnaissance vocale est disponible
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            gameView?.setDebugMode("❌ Reconnaissance vocale non disponible")
            fallbackToTouch()
            return
        }
        
        if (checkPermissions()) {
            setupVoiceRecognition()
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
                setupVoiceRecognition()
            } else {
                gameView?.setDebugMode("❌ Permission audio refusée")
                fallbackToTouch()
            }
        }
    }
    
    private fun setupVoiceRecognition() {
        try {
            // Utiliser le recognizer par défaut (plus fiable)
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
            
            if (speechRecognizer == null) {
                gameView?.setDebugMode("❌ Impossible de créer le recognizer")
                fallbackToTouch()
                return
            }
            
            speechRecognizer?.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    gameView?.setDebugMode("🎤 Écoute active - Dites: HOP, PAF, BOUM!")
                    restartAttempts = 0 // Reset compteur en cas de succès
                }
                
                override fun onBeginningOfSpeech() {
                    gameView?.setDebugMode("🎤 Parole détectée...")
                }
                
                override fun onRmsChanged(rmsdB: Float) {
                    // Indicateur visuel du niveau sonore
                    gameView?.updateSoundLevel(rmsdB)
                }
                
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {}
                
                override fun onError(error: Int) {
                    val errorMessage = when (error) {
                        SpeechRecognizer.ERROR_NO_MATCH -> "Aucun mot reconnu"
                        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Timeout réseau"
                        SpeechRecognizer.ERROR_NETWORK -> "Erreur réseau"
                        SpeechRecognizer.ERROR_AUDIO -> "Erreur audio"
                        SpeechRecognizer.ERROR_SERVER -> "Erreur serveur"
                        SpeechRecognizer.ERROR_CLIENT -> "Erreur client"
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Timeout parole"
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Permissions insuffisantes"
                        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognizer occupé"
                        SpeechRecognizer.ERROR_TOO_MANY_REQUESTS -> "Trop de requêtes"
                        else -> "Erreur inconnue ($error)"
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
                            // Erreurs fatales
                            gameView?.setDebugMode("❌ Erreur fatale - Mode tactile")
                            fallbackToTouch()
                        }
                        else -> {
                            // Autres erreurs - on essaie de redémarrer
                            if (restartAttempts < maxRestartAttempts) {
                                scheduleRestart(1000) // Délai encore plus long
                            } else {
                                gameView?.setDebugMode("❌ Trop d'erreurs - Mode tactile")
                                fallbackToTouch()
                            }
                        }
                    }
                }
                
                override fun onResults(results: Bundle?) {
                    results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.let { matches ->
                        gameView?.setDebugMode("🎤 Résultats: ${matches.joinToString(", ")}")
                        
                        for (result in matches) {
                            val command = result.lowercase(Locale.getDefault()).trim()
                            if (handleVoiceCommand(command)) {
                                break // Commande trouvée
                            }
                        }
                    }
                    
                    // Redémarrer pour écoute continue
                    scheduleRestart(300) // Délai raisonnable
                }
                
                override fun onPartialResults(partialResults: Bundle?) {
                    // Afficher les résultats partiels pour le debug
                    partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.let { matches ->
                        if (matches.isNotEmpty()) {
                            gameView?.setDebugMode("🎤 Partiel: ${matches[0]}")
                        }
                    }
                }
                
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
            
            startListening()
            
        } catch (e: Exception) {
            gameView?.setDebugMode("❌ Erreur setup: ${e.message}")
            fallbackToTouch()
        }
    }
    
    private fun startListening() {
        if (!isListening) {
            try {
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, "fr-FR")
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1000)
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1000)
                }
                
                speechRecognizer?.startListening(intent)
                isListening = true
                
            } catch (e: Exception) {
                gameView?.setDebugMode("❌ Erreur démarrage: ${e.message}")
                fallbackToTouch()
            }
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
            command.contains("hop") || command.contains("saute") || command.contains("saut") || command.contains("jump") -> {
                gameView?.jump()
                gameView?.setDebugMode("🎤 HOP détecté! Continuez...")
                true
            }
            command.contains("paf") || command.contains("pan") || command.contains("tir") || command.contains("shoot") -> {
                gameView?.shoot()
                gameView?.setDebugMode("🎤 PAF détecté! Continuez...")
                true
            }
            command.contains("boum") || command.contains("boom") || command.contains("explose") || command.contains("explode") -> {
                gameView?.explode()
                gameView?.setDebugMode("🎤 BOUM détecté! Continuez...")
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
        gameView?.setDebugMode("🎮 Mode tactile: Gauche=HOP, Centre=PAF, Droite=BOUM")
    }
    
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (event?.action == MotionEvent.ACTION_DOWN) {
            val screenWidth = resources.displayMetrics.widthPixels
            val x = event.x
            
            when {
                x < screenWidth / 3 -> {
                    gameView?.jump()
                    gameView?.setDebugMode("🎮 HOP! Touchez pour continuer...")
                }
                x < screenWidth * 2 / 3 -> {
                    gameView?.shoot()
                    gameView?.setDebugMode("🎮 PAF! Touchez pour continuer...")
                }
                else -> {
                    gameView?.explode()
                    gameView?.setDebugMode("🎮 BOUM! Touchez pour continuer...")
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
