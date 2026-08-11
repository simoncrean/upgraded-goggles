package com.bp.carwash

import com.bp.carwash.payment.PaymentProvider
import com.bp.carwash.payment.PaymentResult
import kotlinx.coroutines.awaitCancellation

/** Approves instantly and records what was charged. */
class RecordingApproveProvider : PaymentProvider {
    var lastAmountCents: Long? = null
    var lastReference: String? = null

    override suspend fun purchase(amountCents: Long, reference: String): PaymentResult {
        lastAmountCents = amountCents
        lastReference = reference
        return PaymentResult.Approved("TEST-$amountCents")
    }
}

/** Declines instantly. */
class DecliningProvider(private val reason: String = "Card declined") : PaymentProvider {
    override suspend fun purchase(amountCents: Long, reference: String) =
        PaymentResult.Declined(reason)
}

/** Never completes — customer walked away; lets tests drive Cancel. */
class HangingProvider : PaymentProvider {
    override suspend fun purchase(amountCents: Long, reference: String): PaymentResult {
        awaitCancellation()
    }
}
