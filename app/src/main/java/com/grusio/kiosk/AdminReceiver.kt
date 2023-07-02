package com.grusio.kiosk

import android.app.admin.DeviceAdminReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.annotation.Nullable
import androidx.appcompat.app.AppCompatActivity


class AdminReceiver : DeviceAdminReceiver() {
//
//    fun onEnabled(context: Context?, intent: Intent?) {}
//
//    fun onDisabled(context: Context?, intent: Intent?) {}

    companion object {

        fun getComponentName(context: Context): ComponentName {
            return ComponentName(context.applicationContext, AdminReceiver::class.java)
        }

    }

}