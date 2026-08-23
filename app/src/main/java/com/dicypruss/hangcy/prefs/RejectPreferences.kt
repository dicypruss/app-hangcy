package com.dicypruss.hangcy.prefs

import android.content.Context

class RejectPreferences(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun isRejectWhenUnknown(): Boolean = prefs.getBoolean(KEY_REJECT_WHEN_UNKNOWN, false)

    fun setRejectWhenUnknown(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_REJECT_WHEN_UNKNOWN, enabled).commit()
    }

    fun isRejectForSubscription(subscriptionId: Int): Boolean =
        prefs.getBoolean(keyForSubscription(subscriptionId), false)

    fun setRejectForSubscription(subscriptionId: Int, enabled: Boolean) {
        prefs.edit().putBoolean(keyForSubscription(subscriptionId), enabled).commit()
    }

    private fun keyForSubscription(subscriptionId: Int): String = "$KEY_REJECT_SUB_PREFIX$subscriptionId"

    private companion object {
        const val PREFS = "hangcy"
        const val KEY_REJECT_WHEN_UNKNOWN = "reject_incoming"
        const val KEY_REJECT_SUB_PREFIX = "reject_sub_"
    }
}
