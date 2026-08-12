package com.bp.carwash.catalog

import kotlinx.serialization.Serializable

/**
 * The product catalogue data model. This is the wire format: today it ships
 * as a bundled `assets/catalog.json`, and the same document will be served
 * by the catalogue API once that contract is defined — parsing ignores
 * unknown keys so newer API fields never break deployed terminals.
 */
@Serializable
data class Product(
    /** Stable identifier, e.g. "deluxe" — used in receipts and telemetry. */
    val id: String,
    val name: String,
    val description: String,
    /** Money is integer cents, never floats. */
    val priceCents: Long,
    /** Position on the menu grid, ascending. */
    val displayOrder: Int,
    /** Highlighted tier — gets the featured styling and BEST VALUE badge. */
    val featured: Boolean = false,
) {
    /** "$10" for whole dollars, "$12.50" otherwise. */
    val priceLabel: String
        get() {
            val dollars = priceCents / 100
            val cents = priceCents % 100
            return if (cents == 0L) "$$dollars" else "$$dollars.%02d".format(cents)
        }
}

@Serializable
data class ProductCatalog(
    val schemaVersion: Int = 1,
    val currency: String = "AUD",
    val updatedAt: String = "",
    val products: List<Product> = emptyList(),
) {
    /**
     * Validates and returns the products in display order. The menu grid
     * holds at most [MAX_PRODUCTS]; a catalogue that can't render is
     * rejected here rather than half-drawn.
     */
    fun productsForDisplay(): List<Product> {
        require(products.isNotEmpty()) { "Catalogue has no products" }
        require(products.size <= MAX_PRODUCTS) {
            "Catalogue has ${products.size} products; the menu holds $MAX_PRODUCTS"
        }
        require(products.map { it.id }.distinct().size == products.size) {
            "Catalogue has duplicate product ids"
        }
        products.forEach {
            require(it.priceCents > 0) { "Product ${it.id} has non-positive price" }
        }
        require(products.count { it.featured } <= 1) {
            "Catalogue has more than one featured product"
        }
        return products.sortedBy { it.displayOrder }
    }

    companion object {
        const val MAX_PRODUCTS = 4
    }
}
