package com.dicypruss.hangcy.screening

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.TelephonyManager
import com.dicypruss.hangcy.sim.SimLine
import java.io.File

object ScreeningDump {
    const val FILE_NAME = "last_screen.txt"

    fun write(
        context: Context,
        direction: Int,
        caller: String?,
        match: CallSimMatch,
        lines: List<SimLine>,
        rejectBySubId: Map<Int, Boolean>,
        rejectWhenNoSims: Boolean,
        reject: Boolean,
    ) {
        val versionName = try {
            val info = context.packageManager.getPackageInfo(context.packageName, 0)
            val code = if (Build.VERSION.SDK_INT >= 28) {
                info.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                info.versionCode.toLong()
            }
            "${info.versionName} ($code)"
        } catch (_: PackageManager.NameNotFoundException) {
            "unknown"
        }
        val body = buildString {
            appendLine("at=${System.currentTimeMillis()}")
            appendLine("version=$versionName")
            appendLine("direction=$direction")
            appendLine("caller=${caller.orEmpty()}")
            appendLine("handleId=${match.handleId ?: "null"}")
            appendLine("label=${match.accountLabel ?: "null"}")
            appendLine("matched=${match.subscriptionId ?: "null"}")
            appendLine("reject=$reject")
            appendLine("rejectWhenNoSims=$rejectWhenNoSims")
            lines.forEach { line ->
                val state = match.callStates[line.subscriptionId]
                    ?: TelephonyManager.CALL_STATE_IDLE
                appendLine(
                    "line sub=${line.subscriptionId} slot=${line.slotIndex} " +
                        "name=${line.displayName} callState=${callStateName(state)} " +
                        "reject=${rejectBySubId[line.subscriptionId] == true}",
                )
            }
        }
        try {
            val dir = context.filesDir
            val target = File(dir, FILE_NAME)
            val tmp = File(dir, "$FILE_NAME.tmp")
            tmp.writeText(body)
            if (target.exists()) {
                target.delete()
            }
            if (!tmp.renameTo(target)) {
                target.writeText(body)
                tmp.delete()
            }
        } catch (_: Exception) {
        }
    }

    private fun callStateName(state: Int): String = when (state) {
        TelephonyManager.CALL_STATE_IDLE -> "IDLE"
        TelephonyManager.CALL_STATE_RINGING -> "RINGING"
        TelephonyManager.CALL_STATE_OFFHOOK -> "OFFHOOK"
        else -> state.toString()
    }
}
