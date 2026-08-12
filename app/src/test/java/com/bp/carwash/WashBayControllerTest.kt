package com.bp.carwash

import com.bp.carwash.catalog.Product
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test

/**
 * Coin-pulse electrical contract: pulse counts, train shape, and timing.
 * Uses virtual time — the full train "runs" instantly.
 */
class WashBayControllerTest {

    private class RecordingOutput : PulseOutput {
        val transitions = mutableListOf<Boolean>()
        override suspend fun setLine(closed: Boolean) {
            transitions.add(closed)
        }
    }

    private lateinit var line: RecordingOutput

    private fun product(id: String, cents: Long) =
        Product(id, "SKU-$id", "N", "D", priceCents = cents, displayOrder = 1)

    private val quick = product("quick", 10_00)
    private val express = product("express", 20_00)
    private val deluxe = product("deluxe", 30_00)
    private val ultimate = product("ultimate", 40_00)

    @Before
    fun setUp() {
        WashBayController.resetForTest()
        line = RecordingOutput()
        WashBayController.output = line
    }

    @After
    fun tearDown() {
        WashBayController.resetForTest()
    }

    @Test
    fun `default one dollar coin value gives one pulse per dollar`() {
        assertEquals(10, WashBayController.pulseCountFor(quick))
        assertEquals(20, WashBayController.pulseCountFor(express))
        assertEquals(30, WashBayController.pulseCountFor(deluxe))
        assertEquals(40, WashBayController.pulseCountFor(ultimate))
    }

    @Test
    fun `five dollar coin value shortens the train`() {
        WashBayController.config = CoinPulseConfig(coinValueCents = 5_00)
        assertEquals(2, WashBayController.pulseCountFor(quick))
        assertEquals(4, WashBayController.pulseCountFor(express))
        assertEquals(6, WashBayController.pulseCountFor(deluxe))
        assertEquals(8, WashBayController.pulseCountFor(ultimate))
    }

    @Test
    fun `coin value that does not divide a tier price is rejected`() {
        WashBayController.config = CoinPulseConfig(coinValueCents = 3_00)
        assertThrows(IllegalArgumentException::class.java) {
            WashBayController.pulseCountFor(quick)
        }
    }

    @Test
    fun `train alternates closed-open and leaves the line open`() = runTest {
        WashBayController.pulse(quick, "ref")

        // 10 pulses = 20 transitions: closed, open, closed, open, ...
        assertEquals(20, line.transitions.size)
        line.transitions.forEachIndexed { i, closed ->
            assertEquals("transition $i", i % 2 == 0, closed)
        }
        assertFalse("line must end open", line.transitions.last())
    }

    @Test
    fun `train duration matches width plus gap per pulse`() = runTest {
        WashBayController.config = CoinPulseConfig(
            coinValueCents = 100, pulseWidthMs = 100, pulseGapMs = 100
        )
        WashBayController.output = line
        val start = currentTime
        WashBayController.pulse(deluxe, "ref")
        // 30 pulses × (100ms closed + 100ms open) = 6000ms
        assertEquals(6_000, currentTime - start)
    }

    @Test
    fun `train is recorded with tier receipt and count`() = runTest {
        WashBayController.pulse(ultimate, "RCPT-1")
        val train = WashBayController.lastPulse!!
        assertEquals("ultimate", train.productId)
        assertEquals("RCPT-1", train.receiptRef)
        assertEquals(40, train.pulseCount)
    }
}
