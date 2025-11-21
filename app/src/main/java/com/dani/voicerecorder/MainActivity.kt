package com.dani.voicerecorder

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var speechRecognizer: SpeechRecognizer
    private lateinit var btnRecord: Button
    private lateinit var btnSend: Button
    private lateinit var btnDocs: Button
    private lateinit var btnNotes: Button
    private lateinit var tvResult: TextView
    private var isListening = false

    private val prefs by lazy { getSharedPreferences("VoiceRecorderPrefs", Context.MODE_PRIVATE) }

    private fun getWhatsAppNumber(): String = prefs.getString("whatsapp_number", "") ?: ""
    private fun getDocsWebhook(): String = prefs.getString("docs_webhook", "") ?: ""
    private fun getNotesLabel(): String = prefs.getString("notes_label", "") ?: ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btnRecord = findViewById(R.id.btnRecord)
        btnSend = findViewById(R.id.btnSend)
        btnDocs = findViewById(R.id.btnDocs)
        btnNotes = findViewById(R.id.btnNotes)
        tvResult = findViewById(R.id.tvResult)

        checkPermission()
        setupSpeechRecognizer()

        btnRecord.setOnClickListener {
            if (isListening) {
                stopListening()
            } else {
                startListening()
            }
        }

        btnSend.setOnClickListener {
            val text = tvResult.text.toString()
            if (getWhatsAppNumber().isEmpty()) {
                Toast.makeText(this, "Set WhatsApp number in Settings", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (text.isNotEmpty() && text != "Tap to record" && text != "Listening...") {
                sendToWhatsApp(text)
            } else {
                Toast.makeText(this, "No text to send", Toast.LENGTH_SHORT).show()
            }
        }

        btnDocs.setOnClickListener {
            val text = tvResult.text.toString()
            if (getDocsWebhook().isEmpty()) {
                Toast.makeText(this, "Set Docs Webhook in Settings", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (text.isNotEmpty() && text != "Tap to record" && text != "Listening...") {
                sendToDocs(text)
            } else {
                Toast.makeText(this, "No text to send", Toast.LENGTH_SHORT).show()
            }
        }

        btnNotes.setOnClickListener {
            val text = tvResult.text.toString()
            if (text.isNotEmpty() && text != "Tap to record" && text != "Listening...") {
                sendToNotes(text)
            } else {
                Toast.makeText(this, "No text to send", Toast.LENGTH_SHORT).show()
            }
        }

    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun sendToWhatsApp(text: String) {
        try {
            val url = "https://wa.me/${getWhatsAppNumber()}?text=${java.net.URLEncoder.encode(text, "UTF-8")}"
            val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url))
            startActivity(intent)
            tvResult.text = "Tap to record"
        } catch (e: Exception) {
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun sendToNotes(text: String) {
        try {
            val label = getNotesLabel()
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, text)
                if (label.isNotEmpty()) {
                    putExtra(Intent.EXTRA_SUBJECT, label)
                }
                setPackage("com.google.android.keep")
            }
            startActivity(intent)
            tvResult.text = "Tap to record"
        } catch (e: Exception) {
            Toast.makeText(this, "Google Keep not installed", Toast.LENGTH_SHORT).show()
        }
    }

    private fun sendToDocs(text: String) {
        Toast.makeText(this, "Sending...", Toast.LENGTH_SHORT).show()
        Thread {
            try {
                val webhook = getDocsWebhook()
                val url = java.net.URL(webhook)
                val conn = url.openConnection() as java.net.HttpURLConnection
                conn.instanceFollowRedirects = true
                conn.requestMethod = "POST"
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
                conn.connectTimeout = 15000
                conn.readTimeout = 15000
                conn.outputStream.write("text=${java.net.URLEncoder.encode(text, "UTF-8")}".toByteArray())
                val response = conn.responseCode
                runOnUiThread {
                    when (response) {
                        in 200..302 -> {
                            Toast.makeText(this, "Saved to Docs!", Toast.LENGTH_SHORT).show()
                            tvResult.text = "Tap to record"
                        }
                        429 -> Toast.makeText(this, "Too many requests. Wait a minute.", Toast.LENGTH_LONG).show()
                        431 -> Toast.makeText(this, "Text too long", Toast.LENGTH_SHORT).show()
                        401, 403 -> Toast.makeText(this, "Access denied. Check script permissions.", Toast.LENGTH_LONG).show()
                        404 -> Toast.makeText(this, "Webhook not found. Check URL.", Toast.LENGTH_SHORT).show()
                        500 -> Toast.makeText(this, "Server error. Try again.", Toast.LENGTH_SHORT).show()
                        else -> Toast.makeText(this, "Error: $response", Toast.LENGTH_SHORT).show()
                    }
                }
                conn.disconnect()
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    private fun checkPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.RECORD_AUDIO), 1)
        }
    }

    private fun setupSpeechRecognizer() {
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                tvResult.text = "Listening..."
            }
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {
                isListening = false
                btnRecord.text = "🎤"
            }
            override fun onError(error: Int) {
                isListening = false
                btnRecord.text = "🎤"
                tvResult.text = when (error) {
                    1 -> "Network timeout"
                    2 -> "Network error"
                    3 -> "Audio error"
                    4 -> "Server error"
                    5 -> "Client error"
                    6 -> "No speech detected"
                    7 -> "No speech recognized"
                    8 -> "Recognizer busy"
                    9 -> "Permission denied"
                    else -> "Error: $error"
                }
            }
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    tvResult.text = matches[0]
                }
            }
            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    tvResult.text = matches[0]
                }
            }
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
    }

    private fun startListening() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "he-IL")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }
        speechRecognizer.startListening(intent)
        isListening = true
        btnRecord.text = "⏹"
    }

    private fun stopListening() {
        speechRecognizer.stopListening()
        isListening = false
        btnRecord.text = "🎤"
    }

    override fun onDestroy() {
        super.onDestroy()
        speechRecognizer.destroy()
    }
}
