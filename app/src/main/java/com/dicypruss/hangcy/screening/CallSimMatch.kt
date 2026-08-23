package com.dicypruss.hangcy.screening

data class CallSimMatch(
    val subscriptionId: Int?,
    val accountLabel: String?,
    val handleId: String?,
    val callStates: Map<Int, Int>,
)
