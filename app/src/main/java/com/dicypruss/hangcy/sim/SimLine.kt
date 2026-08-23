package com.dicypruss.hangcy.sim

data class SimLine(
    val subscriptionId: Int,
    val iccId: String,
    val displayName: String,
    val slotIndex: Int,
)
