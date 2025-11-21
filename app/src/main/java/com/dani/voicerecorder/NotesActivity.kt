package com.dani.voicerecorder

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.io.File

class NotesActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notes)

        val tvNotes = findViewById<TextView>(R.id.tvNotes)
        val file = File(filesDir, "notes.txt")

        if (file.exists()) {
            tvNotes.text = file.readText()
        } else {
            tvNotes.text = "No notes yet"
        }
    }
}
