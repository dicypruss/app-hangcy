package com.dicypruss.hangcy.screening

object RejectPolicy {
    fun shouldRejectIncoming(
        incoming: Boolean,
        matchedSubId: Int?,
        rejectBySubId: Map<Int, Boolean>,
        rejectWhenNoSims: Boolean,
    ): Boolean {
        if (!incoming) {
            return false
        }
        if (rejectBySubId.isEmpty()) {
            return rejectWhenNoSims
        }
        val subId = matchedSubId
            ?: rejectBySubId.keys.singleOrNull()
        if (subId == null) {
            return false
        }
        return rejectBySubId[subId] == true
    }
}
