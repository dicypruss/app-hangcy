package com.dicypruss.hangcy.screening

object RejectPolicy {
    fun shouldRejectIncoming(enabled: Boolean, incoming: Boolean): Boolean =
        incoming && enabled
}
