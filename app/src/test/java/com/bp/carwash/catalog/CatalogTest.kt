package com.bp.carwash.catalog

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * The catalogue contract: the bundled document (what the API will serve)
 * parses, validates, and carries the agreed retail prices. The bundled-file
 * tests are the money regression — a failure means the catalogue changed.
 */
class CatalogTest {

    private val bundled: ProductCatalog by lazy {
        catalogJson.decodeFromString<ProductCatalog>(
            File("src/main/assets/catalog.json").readText()
        )
    }

    // ---------- Bundled catalogue pins ----------

    @Test
    fun `bundled catalogue has four products in display order`() {
        val products = bundled.productsForDisplay()
        assertEquals(
            listOf("quick", "express", "deluxe", "ultimate"),
            products.map { it.id }
        )
    }

    @Test
    fun `bundled prices are 10 20 30 40 dollars`() {
        val prices = bundled.productsForDisplay().associate { it.id to it.priceCents }
        assertEquals(10_00L, prices["quick"])
        assertEquals(20_00L, prices["express"])
        assertEquals(30_00L, prices["deluxe"])
        assertEquals(40_00L, prices["ultimate"])
    }

    @Test
    fun `bundled products carry back-office skus`() {
        val skus = bundled.productsForDisplay().associate { it.id to it.sku }
        assertEquals("BPCW-0001", skus["quick"])
        assertEquals("BPCW-0002", skus["express"])
        assertEquals("BPCW-0003", skus["deluxe"])
        assertEquals("BPCW-0004", skus["ultimate"])
    }

    @Test
    fun `bundled catalogue features only ultimate`() {
        assertEquals(
            listOf("ultimate"),
            bundled.products.filter { it.featured }.map { it.id }
        )
    }

    @Test
    fun `bundled prices are whole dollars and ascending`() {
        val prices = bundled.productsForDisplay().map { it.priceCents }
        assertEquals(prices.sorted(), prices)
        assertTrue(prices.all { it % 100 == 0L })
    }

    // ---------- Wire-format behaviour ----------

    @Test
    fun `unknown json keys are ignored for api forward-compatibility`() {
        val catalog = catalogJson.decodeFromString<ProductCatalog>(
            """
            {
              "schemaVersion": 2,
              "someFutureField": {"nested": true},
              "products": [
                {"id": "a", "sku": "SKU-A", "name": "A", "description": "d",
                 "priceCents": 500, "displayOrder": 1, "newBadgeType": "gold"}
              ]
            }
            """
        )
        assertEquals(1, catalog.productsForDisplay().size)
        assertEquals(500L, catalog.products[0].priceCents)
    }

    @Test
    fun `price label renders whole dollars and cent amounts`() {
        fun product(cents: Long) =
            Product("p", "SKU-P", "P", "", priceCents = cents, displayOrder = 1)
        assertEquals("$10", product(10_00).priceLabel)
        assertEquals("$12.50", product(12_50).priceLabel)
        assertEquals("$9.05", product(9_05).priceLabel)
    }

    // ---------- Validation ----------

    private fun product(
        id: String,
        order: Int = 1,
        cents: Long = 100,
        featured: Boolean = false,
        sku: String = "SKU-$id",
    ) = Product(id, sku, "N", "D", priceCents = cents, displayOrder = order, featured = featured)

    @Test
    fun `empty catalogue is rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            ProductCatalog(products = emptyList()).productsForDisplay()
        }
    }

    @Test
    fun `more products than the menu holds is rejected`() {
        val five = (1..5).map { product("p$it", order = it) }
        assertThrows(IllegalArgumentException::class.java) {
            ProductCatalog(products = five).productsForDisplay()
        }
    }

    @Test
    fun `duplicate ids are rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            ProductCatalog(products = listOf(product("a"), product("a", order = 2)))
                .productsForDisplay()
        }
    }

    @Test
    fun `duplicate skus are rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            ProductCatalog(
                products = listOf(product("a", sku = "X"), product("b", order = 2, sku = "X"))
            ).productsForDisplay()
        }
    }

    @Test
    fun `blank sku is rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            ProductCatalog(products = listOf(product("a", sku = " "))).productsForDisplay()
        }
    }

    @Test
    fun `non-positive prices are rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            ProductCatalog(products = listOf(product("a", cents = 0))).productsForDisplay()
        }
    }

    @Test
    fun `multiple featured products are rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            ProductCatalog(
                products = listOf(
                    product("a", featured = true),
                    product("b", order = 2, featured = true),
                )
            ).productsForDisplay()
        }
    }

    @Test
    fun `products are returned sorted by display order`() {
        val shuffled = ProductCatalog(
            products = listOf(product("c", 3), product("a", 1), product("b", 2))
        )
        assertEquals(listOf("a", "b", "c"), shuffled.productsForDisplay().map { it.id })
    }
}
