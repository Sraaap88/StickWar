package com.example.stickwar

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.Manifest
import android.content.pm.PackageManager
import android.content.Intent
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.util.*

class MainActivity : AppCompatActivity() {
    
    private var gameView: StickFigureGameView? = null
    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false
    
    companion object {
        private const val PERMISSION_REQUEST_CODE = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        gameView = findViewById(R.id.gameView)
        
        if (checkPermissions()) {
            initSpeechRecognition()
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
                initSpeechRecognition()
            } else {
                gameView?.setDebugMode("❌ Permission micro refusée")
            }
        }
    }
    
    private fun initSpeechRecognition() {
        try {
            if (!SpeechRecognizer.isRecognitionAvailable(this)) {
                gameView?.setDebugMode("❌ Reconnaissance vocale non disponible")
                return
            }
            
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
            speechRecognizer?.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    gameView?.setDebugMode("🎤 Écoute... Dites: hop, paf, boum")
                }
                override fun onBeginningOfSpeech() {
                    gameView?.setDebugMode("🗣️ Parole détectée...")
                }
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {
                    gameView?.setDebugMode("🎤 Traitement...")
                }
                override fun onError(error: Int) {
                    val errorMsg = when(error) {
                        SpeechRecognizer.ERROR_NO_MATCH -> "Pas compris"
                        SpeechRecognizer.ERROR_NETWORK -> "Erreur réseau"
                        SpeechRecognizer.ERROR_AUDIO -> "Erreur audio"
                        else -> "Erreur micro"
                    }
                    gameView?.setDebugMode("⚠️ $errorMsg - Redémarrage...")
                    
                    // Redémarrer automatiquement
                    if (isListening) {
                        startListening()
                    }
                }
                override fun onResults(results: Bundle?) {
                    results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.let { matches ->
                        for (result in matches) {
                            val command = result.lowercase(Locale.getDefault()).trim()
                            gameView?.setDebugMode("Commande: '$command'")
                            handleVoiceCommand(command)
                        }
                    }
                    // Redémarrer l'écoute
                    if (isListening) {
                        startListening()
                    }
                }
                override fun onPartialResults(partialResults: Bundle?) {}
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
            
            startListening()
            
        } catch (e: Exception) {
            gameView?.setDebugMode("❌ Erreur init: ${e.message}")
        }
    }
    
    private fun startListening() {
        try {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "fr-FR")
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            }
            speechRecognizer?.startListening(intent)
            isListening = true
            
        } catch (e: Exception) {
            gameView?.setDebugMode("❌ Erreur écoute: ${e.message}")
        }
    }
    
    private fun handleVoiceCommand(command: String) {
        when {
            command.contains("hop") || command.contains("saute") || command.contains("saut") -> {
                gameView?.jump()
            }
            command.contains("paf") || command.contains("pan") || command.contains("tir") -> {
                gameView?.shoot()
            }
            command.contains("boum") || command.contains("boom") || command.contains("explose") -> {
                gameView?.explode()
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        isListening = false
        speechRecognizer?.destroy()
    }
}
