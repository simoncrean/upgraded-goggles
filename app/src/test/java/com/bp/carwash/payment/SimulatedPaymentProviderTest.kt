package com.bp.carwash.payment

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test

class SimulatedPaymentProviderTest {

    @Test
    fun `purchase approves with a SIM receipt reference`() = runTest {
        val result = SimulatedPaymentProvider().purchase(10_00, "BPCW-test")
        assertTrue(result is PaymentResult.Approved)
        result as PaymentResult.Approved
        assertTrue(
            "receipt ref should be SIM-nnnnnn, was ${result.receiptRef}",
            result.receiptRef.matches(Regex("""SIM-\d{6}"""))
        )
    }

    @Test
    fun `every purchase gets its own receipt reference over many runs`() = runTest {
        val provider = SimulatedPaymentProvider()
        val refs = (1..50).map {
            (provider.purchase(10_00, "ref-$it") as PaymentResult.Approved).receiptRef
        }
        // Random 6-digit refs: 50 draws should not all collapse to one value.
        assertTrue(refs.distinct().size > 1)
    }
}
