package com.dicypruss.hangcy.sim

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.telephony.SubscriptionManager
import androidx.core.content.ContextCompat

class SimRepository(private val context: Context) {
    fun lines(): List<SimLine> {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE)
            != PackageManager.PERMISSION_GRANTED
        ) {
            return emptyList()
        }
        val manager = context.getSystemService(SubscriptionManager::class.java) ?: return emptyList()
        val infos = try {
            manager.activeSubscriptionInfoList
        } catch (_: SecurityException) {
            null
        } ?: return emptyList()
        return infos.map { info ->
            val slot = info.simSlotIndex
            val name = info.displayName?.toString()?.takeIf { it.isNotBlank() }
                ?: info.carrierName?.toString()?.takeIf { it.isNotBlank() }
                ?: "SIM ${slot + 1}"
            SimLine(
                subscriptionId = info.subscriptionId,
                iccId = info.iccId.orEmpty(),
                displayName = name,
                slotIndex = slot,
            )
        }.sortedBy { it.slotIndex }
    }
}
