package com.dicypruss.hangcy.screening

import com.dicypruss.hangcy.sim.SimLine

object CallSimMatcher {
    fun matchByAccountLabel(label: String?, lines: List<SimLine>): Int? {
        val trimmed = label?.trim().orEmpty()
        if (trimmed.isEmpty() || lines.isEmpty()) {
            return null
        }
        val exact = lines.filter { it.displayName.equals(trimmed, ignoreCase = true) }
        if (exact.size == 1) {
            return exact[0].subscriptionId
        }
        if (exact.size > 1) {
            return null
        }
        val contained = lines.filter { line ->
            line.displayName.length >= 3 && trimmed.contains(line.displayName, ignoreCase = true)
        }
        return if (contained.size == 1) contained[0].subscriptionId else null
    }

    fun matchByRingingSubIds(ringingSubIds: List<Int>): Int? {
        return ringingSubIds.distinct().singleOrNull()
    }

    fun matchBySlotOrder(sortOrder: Int?, lines: List<SimLine>): Int? {
        if (sortOrder == null || lines.isEmpty()) {
            return null
        }
        val bySlot = lines.filter { it.slotIndex == sortOrder }
        return if (bySlot.size == 1) bySlot[0].subscriptionId else null
    }

    fun matchSubscriptionId(handleId: String?, lines: List<SimLine>): Int? {
        if (handleId.isNullOrBlank() || lines.isEmpty()) {
            return null
        }
        val trimmed = handleId.trim()
        val asInt = trimmed.toIntOrNull()
        if (asInt != null) {
            lines.find { it.subscriptionId == asInt }?.let { return it.subscriptionId }
            val bySlot = lines.filter { it.slotIndex == asInt }
            if (bySlot.size == 1) {
                return bySlot[0].subscriptionId
            }
        }
        return lines.find { line -> iccMatches(line.iccId, trimmed) }?.subscriptionId
    }

    internal fun iccMatches(iccId: String, handleId: String): Boolean {
        if (iccId.isEmpty()) {
            return false
        }
        if (iccId.equals(handleId, ignoreCase = true)) {
            return true
        }
        val icc = stripTrailingPad(iccId)
        val handle = stripTrailingPad(handleId)
        if (icc.equals(handle, ignoreCase = true)) {
            return true
        }
        if (icc.length < 10 || handle.length < 10) {
            return false
        }
        return handle.startsWith(icc, ignoreCase = true) ||
            icc.startsWith(handle, ignoreCase = true)
    }

    private fun stripTrailingPad(value: String): String =
        value.trim().trimEnd { it == 'F' || it == 'f' }
}
