package com.bp.carwash.payment

/**
 * Single point of provider selection. Production code swaps in
 * [QuestPaymentProvider] here; tests substitute fakes.
 */
object PaymentGateway {
    var provider: PaymentProvider = SimulatedPaymentProvider()
}
