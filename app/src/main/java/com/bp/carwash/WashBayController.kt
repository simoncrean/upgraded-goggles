package com.bp.carwash

import android.util.Log
import com.bp.carwash.catalog.Product
import kotlinx.coroutines.delay

/**
 * Coin-pulse wash unlock — the electrical convention used by carwash entry
 * controllers (Dixmor, GinSan, Hamilton and similar): the payment device
 * emulates a coin acceptor, pulsing the controller's coin input once per
 * coin-value of credit. A $30 wash at $1/pulse is a 30-pulse train.
 *
 * Electrical contract (typical coin-acceptor spec):
 *  - dry relay contact or open-collector output into the coin input
 *  - contact closed [CoinPulseConfig.pulseWidthMs], open
 *    [CoinPulseConfig.pulseGapMs] between pulses
 *  - the controller counts closures; credit = pulses × coin value
 */
data class CoinPulseConfig(
    /** Credit per pulse in cents — must divide every tier price. Site-configurable. */
    val coinValueCents: Long = 100,
    /** Contact-closed time per pulse. */
    val pulseWidthMs: Long = 100,
    /** Contact-open time between pulses. */
    val pulseGapMs: Long = 100,
)

/** The physical coin line. Implementations drive a relay/opto output. */
interface PulseOutput {
    /** Drive the coin line: true = contact closed. Must be main-safe. */
    suspend fun setLine(closed: Boolean)
}

/**
 * Placeholder output: logs line transitions. Replace with the site's real
 * output — the QT850's RS232 port driving a relay module, or a network I/O
 * module wired to the bay controller's coin input.
 */
class LogPulseOutput : PulseOutput {
    override suspend fun setLine(closed: Boolean) {
        Log.i(TAG, if (closed) "coin line CLOSED" else "coin line OPEN")
    }

    private companion object {
        const val TAG = "PulseOutput"
    }
}

object WashBayController {

    var config = CoinPulseConfig()
    var output: PulseOutput = LogPulseOutput()

    data class PulseTrain(val productId: String, val receiptRef: String, val pulseCount: Int)

    /** Last train fired (recorded at start of emission) — observable for tests. */
    @Volatile
    var lastPulse: PulseTrain? = null
        private set

    /** Pulses required to credit [product] at the configured coin value. */
    fun pulseCountFor(product: Product): Int {
        require(product.priceCents % config.coinValueCents == 0L) {
            "Product ${product.id} price ${product.priceCents}c is not a multiple " +
                "of coin value ${config.coinValueCents}c"
        }
        return (product.priceCents / config.coinValueCents).toInt()
    }

    /**
     * Emits the coin-pulse train for the purchased tier. Suspends for the
     * full train duration (count × (width + gap)); callers fire it from a
     * coroutine so the UI is never blocked. The train must run to
     * completion once payment is captured — keep the emitting scope alive
     * for its duration.
     */
    suspend fun pulse(product: Product, receiptRef: String) {
        val count = pulseCountFor(product)
        lastPulse = PulseTrain(product.id, receiptRef, count)
        repeat(count) {
            output.setLine(true)
            delay(config.pulseWidthMs)
            output.setLine(false)
            delay(config.pulseGapMs)
        }
    }

    fun resetForTest() {
        lastPulse = null
        config = CoinPulseConfig()
        output = LogPulseOutput()
    }
}
