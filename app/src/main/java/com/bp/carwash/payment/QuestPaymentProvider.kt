package com.bp.carwash.payment

/**
 * Production provider for the Quest QT850.
 *
 * Quest exposes two integration paths for terminal apps:
 *
 *  1. **On-terminal hand-off** — the custom app launches Quest's payment
 *     app with the sale amount and receives an approved/declined result
 *     back. Quest supplies the intent contract / AIDL and whitelists the
 *     calling package when the terminal fleet is provisioned.
 *  2. **Cloud EFTPOS API** — a REST API where the app creates a sale
 *     session in Quest's cloud and the terminal's payment app picks it up.
 *
 * Both require merchant onboarding with Quest (integration keys, terminal
 * fleet registration, and app signing via Quest's MAM for deployment).
 * Contact: integrations@questps.com.au / +61 3 8807 4400.
 *
 * Until those credentials exist this class fails fast so the simulated
 * provider is never silently replaced by a half-configured one.
 */
class QuestPaymentProvider : PaymentProvider {
    override suspend fun purchase(amountCents: Long, reference: String): PaymentResult {
        // TODO(quest): replace with Quest's sale intent/API call once the
        // integration pack is issued for this merchant + fleet.
        throw NotImplementedError(
            "Quest integration not configured. Obtain the QT850 integration pack " +
                "from Quest Payment Systems and implement the sale hand-off here."
        )
    }
}
