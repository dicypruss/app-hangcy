package com.dicypruss.hangcy.screening

import android.content.Context
import android.os.Build
import android.telecom.PhoneAccount
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import android.util.Log
import com.dicypruss.hangcy.sim.SimLine

class CallSimResolver(context: Context) {
    private val appContext = context.applicationContext

    fun match(handle: PhoneAccountHandle?, lines: List<SimLine>): CallSimMatch {
        val callStates = snapshotCallStates(lines)
        val account = handle?.let { phoneAccount(it) }
        val fromHandle = handle?.let { matchFromHandle(it, account, lines) }
        val matched = fromHandle
            ?: CallSimMatcher.matchByRingingSubIds(
                callStates.filter { it.value == TelephonyManager.CALL_STATE_RINGING }.keys.toList(),
            )
        Log.i(
            TAG,
            "sim match handleId=${handle?.id} label=${account?.label} subId=$matched states=$callStates",
        )
        return CallSimMatch(
            subscriptionId = matched,
            accountLabel = account?.label?.toString(),
            handleId = handle?.id,
            callStates = callStates,
        )
    }

    private fun matchFromHandle(
        handle: PhoneAccountHandle,
        account: PhoneAccount?,
        lines: List<SimLine>,
    ): Int? {
        if (lines.isEmpty()) {
            return null
        }
        return CallSimMatcher.matchByAccountLabel(account?.label?.toString(), lines)
            ?: CallSimMatcher.matchBySlotOrder(sortOrder(account), lines)
            ?: CallSimMatcher.matchSubscriptionId(handle.id, lines)
            ?: matchByTelephony(handle, lines)
    }

    private fun snapshotCallStates(lines: List<SimLine>): Map<Int, Int> =
        lines.associate { line -> line.subscriptionId to callStateFor(line.subscriptionId) }

    private fun callStateFor(subId: Int): Int {
        return try {
            val base = appContext.getSystemService(TelephonyManager::class.java)
                ?: return TelephonyManager.CALL_STATE_IDLE
            val tm = base.createForSubscriptionId(subId)
            if (Build.VERSION.SDK_INT >= 31) {
                tm.callStateForSubscription
            } else {
                tm.callState
            }
        } catch (_: SecurityException) {
            TelephonyManager.CALL_STATE_IDLE
        } catch (_: Exception) {
            TelephonyManager.CALL_STATE_IDLE
        }
    }

    private fun sortOrder(account: PhoneAccount?): Int? {
        val extras = account?.extras ?: return null
        if (!extras.containsKey(SORT_ORDER_EXTRA)) {
            return null
        }
        return extras.getInt(SORT_ORDER_EXTRA)
    }

    private fun matchByTelephony(handle: PhoneAccountHandle, lines: List<SimLine>): Int? {
        if (Build.VERSION.SDK_INT < 30) {
            return null
        }
        return try {
            val telephony = appContext.getSystemService(TelephonyManager::class.java) ?: return null
            val subId = telephony.getSubscriptionId(handle)
            subId.takeIf { SubscriptionManager.isValidSubscriptionId(it) }
                ?.takeIf { id -> lines.any { it.subscriptionId == id } }
        } catch (_: SecurityException) {
            null
        } catch (_: Exception) {
            null
        }
    }

    private fun phoneAccount(handle: PhoneAccountHandle): PhoneAccount? {
        val telecom = appContext.getSystemService(TelecomManager::class.java) ?: return null
        return try {
            val direct = telecom.getPhoneAccount(handle)
            if (direct != null && !direct.label.isNullOrBlank()) {
                return direct
            }
            registeredHandle(telecom, handle)?.let { telecom.getPhoneAccount(it) } ?: direct
        } catch (_: SecurityException) {
            null
        } catch (_: Exception) {
            null
        }
    }

    private fun registeredHandle(
        telecom: TelecomManager,
        handle: PhoneAccountHandle,
    ): PhoneAccountHandle? {
        val registered = try {
            telecom.callCapablePhoneAccounts
        } catch (_: SecurityException) {
            return null
        }
        registered.firstOrNull { it == handle }?.let { return it }
        return registered.filter { it.id == handle.id }.singleOrNull()
    }

    companion object {
        private const val SORT_ORDER_EXTRA = "android.telecom.extra.SORT_ORDER"
        private const val TAG = "Hangcy"
    }
}
