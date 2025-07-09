package com.example.stickwar

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MotionEvent
import android.Manifest
import android.content.pm.PackageManager
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.util.*

class MainActivity : AppCompatActivity() {
    
    private var gameView: StickFigureGameView? = null
    private var isVoiceMode = false
    
    companion object {
        private const val PERMISSION_REQUEST_CODE = 1
        private const val VOICE_REQUEST_CODE = 100
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        gameView = findViewById(R.id.gameView)
        
        // Tester les permissions et essayer la reconnaissance vocale
        if (checkPermissions()) {
            tryVoiceRecognition()
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
                tryVoiceRecognition()
            } else {
                gameView?.setDebugMode("❌ Permission refusée - Mode tactile")
                setTouchMode()
            }
        }
    }
    
    private fun tryVoiceRecognition() {
        try {
            // Méthode 1: Intent direct Huawei/Android
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "fr-FR")
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Dites: hop, paf, boum")
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
                putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, packageName)
            }
            
            // Vérifier si une app peut gérer cette intent
            if (intent.resolveActivity(packageManager) != null) {
                gameView?.setDebugMode("🎤 Mode vocal activé! Tapez pour parler")
                isVoiceMode = true
            } else {
                gameView?.setDebugMode("❌ Aucune app de reconnaissance - Mode tactile")
                setTouchMode()
            }
        } catch (e: Exception) {
            gameView?.setDebugMode("❌ Erreur vocal: ${e.message} - Mode tactile")
            setTouchMode()
        }
    }
    
    private fun setTouchMode() {
        isVoiceMode = false
        gameView?.setDebugMode("🎮 Mode tactile: Gauche=HOP, Centre=PAF, Droite=BOUM")
    }
    
    private fun startVoiceRecognition() {
        try {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "fr-FR")
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Dites: hop, paf, boum")
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
            }
            startActivityForResult(intent, VOICE_REQUEST_CODE)
        } catch (e: Exception) {
            gameView?.setDebugMode("❌ Erreur lancement vocal: ${e.message}")
        }
    }
    
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        if (requestCode == VOICE_REQUEST_CODE && resultCode == RESULT_OK) {
            val results = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            results?.let { matches ->
                for (result in matches) {
                    val command = result.lowercase(Locale.getDefault()).trim()
                    gameView?.setDebugMode("Commande: '$command' - Tapez pour parler encore")
                    handleVoiceCommand(command)
                    break // Prendre la première commande
                }
            }
        } else if (requestCode == VOICE_REQUEST_CODE) {
            gameView?.setDebugMode("🎤 Reconnaissance annulée - Tapez pour réessayer")
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
            else -> {
                gameView?.setDebugMode("❓ '$command' non reconnu - Dites: hop, paf, boum")
            }
        }
    }
    
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (event?.action == MotionEvent.ACTION_DOWN) {
            if (isVoiceMode) {
                // En mode vocal, tap = lancer reconnaissance
                startVoiceRecognition()
            } else {
                // En mode tactile, tap = commande directe
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
            }
            return true
        }
        return super.onTouchEvent(event)
    }
}
