package com.dicypruss.hangcy.screening

import android.telecom.Call
import android.telecom.CallScreeningService
import com.dicypruss.hangcy.history.RejectedCall
import com.dicypruss.hangcy.history.RejectedCallStore
import com.dicypruss.hangcy.notify.BlockedCallNotifier
import com.dicypruss.hangcy.prefs.RejectPreferences

class HangcyCallScreeningService : CallScreeningService() {
    override fun onScreenCall(callDetails: Call.Details) {
        val reject = RejectPolicy.shouldRejectIncoming(
            enabled = RejectPreferences(this).isRejectIncoming(),
            incoming = callDetails.callDirection == Call.Details.DIRECTION_INCOMING,
        )
        val response = CallScreeningService.CallResponse.Builder()
        if (reject) {
            response.setDisallowCall(true)
            response.setRejectCall(true)
            response.setSkipNotification(true)
        }
        respondToCall(callDetails, response.build())
        if (reject) {
            val number = callDetails.handle?.schemeSpecificPart.orEmpty()
            try {
                RejectedCallStore.get(this).append(
                    RejectedCall(
                        atMillis = System.currentTimeMillis(),
                        number = number,
                    ),
                )
            } catch (_: Exception) {
            }
            BlockedCallNotifier(this).notifyRejected(number.ifBlank { null })
        }
    }
}
