package com.bp.carwash

import android.util.Log

/**
 * Unlocks the purchased wash by pulsing the carwash controller.
 *
 * Wire [pulse] to the site's bay hardware — typically either:
 *  - a relay/pulse interface (e.g. the terminal's serial port RS232 per the
 *    QT850 spec, or a network I/O module driving the bay PLC), or
 *  - the wash controller vendor's local API.
 */
object WashBayController {

    private const val TAG = "WashBayController"

    data class Pulse(val tier: WashTier, val receiptRef: String)

    /** Last pulse fired — observable so tests can assert unlock behaviour. */
    @Volatile
    var lastPulse: Pulse? = null
        private set

    /** Fires the unlock pulse for the given wash tier. */
    fun pulse(tier: WashTier, receiptRef: String) {
        lastPulse = Pulse(tier, receiptRef)
        // TODO(site-integration): replace with the real pulse output.
        // Pulse count/duration usually encodes the tier on carwash entry
        // controllers, e.g. QUICK=1 pulse .. ULTIMATE=4 pulses.
        Log.i(TAG, "PULSE wash unlock: tier=${tier.name} ref=$receiptRef")
    }

    fun resetForTest() {
        lastPulse = null
    }
}
