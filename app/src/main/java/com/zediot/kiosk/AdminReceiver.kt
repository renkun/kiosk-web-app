package com.zediot.kiosk

import android.app.admin.DeviceAdminReceiver
import android.content.ComponentName
import android.content.Context


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