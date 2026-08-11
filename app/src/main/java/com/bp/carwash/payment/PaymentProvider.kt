package com.bp.carwash.payment

sealed class PaymentResult {
    data class Approved(val receiptRef: String) : PaymentResult()
    data class Declined(val reason: String) : PaymentResult()
    object Cancelled : PaymentResult()
}

/**
 * Abstraction over the terminal's payment stack.
 *
 * The QT850's card readers (EMV chip, NFC contactless, MSR) are driven by
 * Quest's own payment application, not directly by third-party APKs. Custom
 * apps hand the sale amount to the Quest payment app and receive the result
 * back. Swap [SimulatedPaymentProvider] for [QuestPaymentProvider] once
 * Quest integration credentials are in place.
 */
interface PaymentProvider {
    /**
     * Runs a purchase for [amountCents]. Suspends until the cardholder
     * completes (or abandons) payment. Must be main-safe.
     */
    suspend fun purchase(amountCents: Long, reference: String): PaymentResult
}
