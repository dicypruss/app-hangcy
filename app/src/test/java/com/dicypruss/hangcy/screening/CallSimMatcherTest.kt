package com.dicypruss.hangcy.screening

import com.dicypruss.hangcy.sim.SimLine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CallSimMatcherTest {
    private val lines = listOf(
        SimLine(subscriptionId = 1, iccId = "89001", displayName = "A", slotIndex = 0),
        SimLine(subscriptionId = 2, iccId = "89002", displayName = "B", slotIndex = 1),
    )

    @Test
    fun matchesNumericSubscriptionId() {
        assertEquals(2, CallSimMatcher.matchSubscriptionId("2", lines))
    }

    @Test
    fun matchesIccId() {
        assertEquals(1, CallSimMatcher.matchSubscriptionId("89001", lines))
    }

    @Test
    fun unknownHandleIsNull() {
        assertNull(CallSimMatcher.matchSubscriptionId("nope", lines))
    }

    @Test
    fun blankHandleIsNull() {
        assertNull(CallSimMatcher.matchSubscriptionId("  ", lines))
        assertNull(CallSimMatcher.matchSubscriptionId(null, lines))
    }

    @Test
    fun matchesPaddedIccId() {
        assertEquals(1, CallSimMatcher.matchSubscriptionId("89001FFFFFFFFFFF", lines))
    }

    @Test
    fun matchesSlotIndexWhenNotASubscriptionId() {
        assertEquals(1, CallSimMatcher.matchSubscriptionId("0", lines))
    }

    @Test
    fun numericIdNotInListIsNotForced() {
        assertNull(CallSimMatcher.matchSubscriptionId("9", lines))
    }

    @Test
    fun matchesExactAccountLabel() {
        val dual = listOf(
            SimLine(5, "89005", "epic", 0),
            SimLine(1, "89001", "MTS RUS", 1),
        )
        assertEquals(1, CallSimMatcher.matchByAccountLabel("MTS RUS", dual))
        assertEquals(5, CallSimMatcher.matchByAccountLabel("epic", dual))
    }

    @Test
    fun matchesContainedAccountLabelOnce() {
        val dual = listOf(
            SimLine(5, "89005", "epic", 0),
            SimLine(1, "89001", "MTS RUS", 1),
        )
        assertEquals(1, CallSimMatcher.matchByAccountLabel("Cyta — MTS RUS", dual))
    }

    @Test
    fun ambiguousAccountLabelIsNull() {
        val dual = listOf(
            SimLine(5, "89005", "MTS", 0),
            SimLine(1, "89001", "MTS RUS", 1),
        )
        assertNull(CallSimMatcher.matchByAccountLabel("MTS RUS extra", dual))
    }

    @Test
    fun matchesUniqueSlotOrder() {
        assertEquals(2, CallSimMatcher.matchBySlotOrder(1, lines))
        assertNull(CallSimMatcher.matchBySlotOrder(9, lines))
    }

    @Test
    fun matchesSingleRingingSubId() {
        assertEquals(1, CallSimMatcher.matchByRingingSubIds(listOf(1)))
    }

    @Test
    fun ringingNoneOrTwoIsNull() {
        assertNull(CallSimMatcher.matchByRingingSubIds(emptyList()))
        assertNull(CallSimMatcher.matchByRingingSubIds(listOf(1, 5)))
        assertEquals(1, CallSimMatcher.matchByRingingSubIds(listOf(1, 1)))
    }
}
