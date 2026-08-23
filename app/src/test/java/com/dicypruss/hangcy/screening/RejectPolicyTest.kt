package com.dicypruss.hangcy.screening

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RejectPolicyTest {
    @Test
    fun rejectsAllWhenNoSimsAndGlobalOn() {
        assertTrue(
            RejectPolicy.shouldRejectIncoming(
                incoming = true,
                matchedSubId = null,
                rejectBySubId = emptyMap(),
                rejectWhenNoSims = true,
            ),
        )
    }

    @Test
    fun allowsAllWhenNoSimsAndGlobalOff() {
        assertFalse(
            RejectPolicy.shouldRejectIncoming(
                incoming = true,
                matchedSubId = null,
                rejectBySubId = emptyMap(),
                rejectWhenNoSims = false,
            ),
        )
    }

    @Test
    fun rejectsMatchedSimWhenThatToggleIsOn() {
        assertTrue(
            RejectPolicy.shouldRejectIncoming(
                incoming = true,
                matchedSubId = 2,
                rejectBySubId = mapOf(1 to false, 2 to true),
                rejectWhenNoSims = false,
            ),
        )
    }

    @Test
    fun allowsMatchedSimWhenThatToggleIsOff() {
        assertFalse(
            RejectPolicy.shouldRejectIncoming(
                incoming = true,
                matchedSubId = 1,
                rejectBySubId = mapOf(1 to false, 2 to true),
                rejectWhenNoSims = true,
            ),
        )
    }

    @Test
    fun unmatchedWithTwoSimsDoesNotReject() {
        assertFalse(
            RejectPolicy.shouldRejectIncoming(
                incoming = true,
                matchedSubId = null,
                rejectBySubId = mapOf(1 to true, 2 to true),
                rejectWhenNoSims = true,
            ),
        )
    }

    @Test
    fun unmatchedWithOneSimUsesThatToggle() {
        assertTrue(
            RejectPolicy.shouldRejectIncoming(
                incoming = true,
                matchedSubId = null,
                rejectBySubId = mapOf(1 to true),
                rejectWhenNoSims = false,
            ),
        )
    }

    @Test
    fun allowsOutgoingEvenIfTogglesAreOn() {
        assertFalse(
            RejectPolicy.shouldRejectIncoming(
                incoming = false,
                matchedSubId = 2,
                rejectBySubId = mapOf(2 to true),
                rejectWhenNoSims = true,
            ),
        )
    }

    @Test
    fun missingSimKeyDoesNotReject() {
        assertFalse(
            RejectPolicy.shouldRejectIncoming(
                incoming = true,
                matchedSubId = 9,
                rejectBySubId = mapOf(1 to true),
                rejectWhenNoSims = true,
            ),
        )
    }
}
