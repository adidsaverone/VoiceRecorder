package com.dani.voicerecorder

import android.content.Context
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val prefs = getSharedPreferences("VoiceRecorderPrefs", Context.MODE_PRIVATE)

        val etWhatsApp = findViewById<EditText>(R.id.etWhatsApp)
        val etDocsWebhook = findViewById<EditText>(R.id.etDocsWebhook)
        val etNotesLabel = findViewById<EditText>(R.id.etNotesLabel)
        val btnSave = findViewById<Button>(R.id.btnSave)

        // Load saved values
        etWhatsApp.setText(prefs.getString("whatsapp_number", ""))
        etDocsWebhook.setText(prefs.getString("docs_webhook", ""))
        etNotesLabel.setText(prefs.getString("notes_label", ""))

        btnSave.setOnClickListener {
            prefs.edit()
                .putString("whatsapp_number", etWhatsApp.text.toString())
                .putString("docs_webhook", etDocsWebhook.text.toString())
                .putString("notes_label", etNotesLabel.text.toString())
                .apply()
            Toast.makeText(this, "Saved!", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}
