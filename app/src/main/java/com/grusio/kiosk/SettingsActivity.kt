package com.grusio.kiosk

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.grusio.adminlocktask.R

class SettingsActivity : AppCompatActivity() {

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var startUrlEditText: EditText
    private lateinit var serverUrlEditText: EditText
    private lateinit var settingPasswordEditText: EditText
    private lateinit var confirmPasswordEditText: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_activity)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        startUrlEditText = findViewById(R.id.startUrlEditText)
        serverUrlEditText = findViewById(R.id.serverUrlEditText)
        settingPasswordEditText = findViewById(R.id.settingPasswordEditText)
        confirmPasswordEditText = findViewById(R.id.confirmPasswordEditText)

        sharedPreferences = getSharedPreferences("config", Context.MODE_PRIVATE)
        val savedStartUrl = sharedPreferences.getString("startUrl", "")
        val savedServerUrl = sharedPreferences.getString("serverUrl", "")
        val savedSettingPassword = sharedPreferences.getString("settingPassword", "")

        startUrlEditText.setText(savedStartUrl)
        serverUrlEditText.setText(savedServerUrl)

        val saveButton = findViewById<Button>(R.id.saveButton)
        saveButton.setOnClickListener {
            var settingPassword = settingPasswordEditText.text.toString()
            var confirmPassword = confirmPasswordEditText.text.toString()
            val startUrl = startUrlEditText.text.toString()
            val serverUrl = serverUrlEditText.text.toString()
            if(settingPassword.length > 1 && settingPassword != confirmPassword) {
                Toast.makeText(applicationContext, "Invalid Confirm Password...", Toast.LENGTH_SHORT).show()
            }
            else {
                saveSettings(startUrl, serverUrl,settingPassword)
            }
        }

        val cancelButton = findViewById<Button>(R.id.cancelButton)
        cancelButton.setOnClickListener {
            finish()
        }

    }

    private fun saveSettings(startUrl: String, serverUrl: String, settingPassword: String) {
        val editor = sharedPreferences.edit()
        editor.putString("startUrl", startUrl)
        editor.putString("serverUrl", serverUrl)
        editor.putString("settingPassword", settingPassword.sha256())
        editor.apply()
        finish()
    }

}