package com.dicypruss.hangcy.screening

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RejectPolicyTest {
    @Test
    fun rejectsIncomingWhenEnabled() {
        assertTrue(RejectPolicy.shouldRejectIncoming(enabled = true, incoming = true))
    }

    @Test
    fun allowsIncomingWhenDisabled() {
        assertFalse(RejectPolicy.shouldRejectIncoming(enabled = false, incoming = true))
    }

    @Test
    fun allowsOutgoingWhenEnabled() {
        assertFalse(RejectPolicy.shouldRejectIncoming(enabled = true, incoming = false))
    }

    @Test
    fun allowsOutgoingWhenDisabled() {
        assertFalse(RejectPolicy.shouldRejectIncoming(enabled = false, incoming = false))
    }
}
