package com.example.wificamerastreaming.discovery

import android.content.Context
import java.util.UUID

object DeviceIdProvider {
    private const val PREFS_NAME = "device_prefs"
    private const val KEY_DEVICE_ID = "device_id"

    fun getDeviceId(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        var id = prefs.getString(KEY_DEVICE_ID, null)
        if (id == null) {
            id = UUID.randomUUID().toString().take(8) // короткий уникальный суффикс
            prefs.edit().putString(KEY_DEVICE_ID, id).apply()
        }
        return id
    }
}
