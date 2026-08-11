package com.bp.carwash

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Regression tests for the retail catalogue. These pin the agreed price
 * points — a failing test here means the money amounts changed.
 */
class WashTierTest {

    @Test
    fun `catalogue has exactly four tiers`() {
        assertEquals(4, WashTier.entries.size)
    }

    @Test
    fun `price points are 10 20 30 40 dollars`() {
        assertEquals(10_00L, WashTier.QUICK.priceCents)
        assertEquals(20_00L, WashTier.EXPRESS.priceCents)
        assertEquals(30_00L, WashTier.DELUXE.priceCents)
        assertEquals(40_00L, WashTier.ULTIMATE.priceCents)
    }

    @Test
    fun `price labels render whole dollars`() {
        assertEquals("$10", WashTier.QUICK.priceLabel)
        assertEquals("$20", WashTier.EXPRESS.priceLabel)
        assertEquals("$30", WashTier.DELUXE.priceLabel)
        assertEquals("$40", WashTier.ULTIMATE.priceLabel)
    }

    @Test
    fun `prices are strictly ascending`() {
        val prices = WashTier.entries.map { it.priceCents }
        assertEquals(prices.sorted(), prices)
        assertEquals(prices.distinct().size, prices.size)
    }

    @Test
    fun `only ultimate is featured`() {
        assertEquals(listOf(WashTier.ULTIMATE), WashTier.entries.filter { it.featured })
    }

    @Test
    fun `all prices are whole dollars`() {
        WashTier.entries.forEach { tier ->
            assertTrue("${tier.name} not a whole dollar", tier.priceCents % 100 == 0L)
        }
    }
}
