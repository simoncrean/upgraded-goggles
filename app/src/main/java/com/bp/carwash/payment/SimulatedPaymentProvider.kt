package com.bp.carwash.payment

import kotlinx.coroutines.delay
import kotlin.random.Random

/**
 * Development/demo provider: approves after a short "present card" pause.
 * Lets the full retail flow be exercised on any Android device before the
 * Quest integration is wired up.
 */
class SimulatedPaymentProvider : PaymentProvider {
    override suspend fun purchase(amountCents: Long, reference: String): PaymentResult {
        delay(2_500)
        return PaymentResult.Approved(
            receiptRef = "SIM-" + Random.nextInt(100_000, 999_999)
        )
    }
}
