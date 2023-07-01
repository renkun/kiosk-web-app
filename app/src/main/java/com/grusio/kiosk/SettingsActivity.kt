package com.grusio.kiosk

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import com.grusio.adminlocktask.R

class SettingsActivity : AppCompatActivity() {

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var startUrlEditText: EditText
    private lateinit var serverUrlEditText: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_activity)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        startUrlEditText = findViewById(R.id.startUrlEditText)
        serverUrlEditText = findViewById(R.id.serverUrlEditText)

        sharedPreferences = getSharedPreferences("config", Context.MODE_PRIVATE)
        val savedStartUrl = sharedPreferences.getString("startUrl", "")
        val savedServerUrl = sharedPreferences.getString("serverUrl", "")

        startUrlEditText.setText(savedStartUrl)
        serverUrlEditText.setText(savedServerUrl)

        val saveButton = findViewById<Button>(R.id.saveButton)
        saveButton.setOnClickListener {
            val startUrl = startUrlEditText.text.toString()
            val serverUrl = serverUrlEditText.text.toString()
            saveSettings(startUrl, serverUrl)
        }

    }

    private fun saveSettings(startUrl: String, serverUrl: String) {
        val editor = sharedPreferences.edit()
        editor.putString("startUrl", startUrl)
        editor.putString("serverUrl", serverUrl)
        editor.apply()
        finish()
    }

}