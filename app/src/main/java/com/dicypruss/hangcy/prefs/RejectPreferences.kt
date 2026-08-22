package com.dicypruss.hangcy.prefs

import android.content.Context

class RejectPreferences(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun isRejectIncoming(): Boolean = prefs.getBoolean(KEY_REJECT_INCOMING, false)

    fun setRejectIncoming(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_REJECT_INCOMING, enabled).commit()
    }

    private companion object {
        const val PREFS = "hangcy"
        const val KEY_REJECT_INCOMING = "reject_incoming"
    }
}
