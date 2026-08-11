package com.bp.carwash.payment

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class QuestPaymentProviderTest {

    /**
     * Guard rail: the Quest provider must fail fast until the real
     * integration is implemented, so it can never be swapped in
     * half-configured and silently drop sales.
     */
    @Test
    fun `unconfigured quest provider fails fast`() = runTest {
        try {
            QuestPaymentProvider().purchase(10_00, "ref")
            fail("expected NotImplementedError")
        } catch (e: NotImplementedError) {
            assertTrue(e.message!!.contains("Quest"))
        }
    }
}
