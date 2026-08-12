package com.bp.carwash.catalog

import android.content.Context
import kotlinx.serialization.json.Json

/** Where the catalogue comes from. Mirrors the PaymentProvider pattern. */
interface CatalogSource {
    suspend fun fetch(): ProductCatalog
}

/**
 * Shared parser configuration. `ignoreUnknownKeys` keeps deployed terminals
 * forward-compatible with newer API fields.
 */
val catalogJson = Json { ignoreUnknownKeys = true }

/**
 * Default source: the catalogue bundled at `assets/catalog.json`. Terminals
 * must sell even when offline, so a bundled catalogue always exists; the
 * API source will layer on top of it (fetch-then-cache, bundled fallback).
 */
class BundledCatalogSource(private val context: Context) : CatalogSource {
    // Deliberately synchronous: the asset is ~1 KB and the menu must be
    // sellable the moment it appears. The API source is where real
    // suspension (network) will happen.
    override suspend fun fetch(): ProductCatalog =
        context.assets.open("catalog.json").bufferedReader().use { reader ->
            catalogJson.decodeFromString<ProductCatalog>(reader.readText())
        }
}

/**
 * Catalogue API source — endpoint and auth to be defined. Fails fast so it
 * can never be swapped in half-configured; implement once the API contract
 * exists (expected shape: GET /catalog returning the ProductCatalog
 * document this app already parses).
 */
class ApiCatalogSource : CatalogSource {
    override suspend fun fetch(): ProductCatalog {
        throw NotImplementedError(
            "Catalogue API not defined yet. Implement fetch() against the " +
                "agreed endpoint and swap it in via CatalogProvider.source."
        )
    }
}

/** Single swap point, like PaymentGateway. Tests substitute fakes here. */
object CatalogProvider {
    var source: CatalogSource? = null

    fun source(context: Context): CatalogSource =
        source ?: BundledCatalogSource(context.applicationContext).also { source = it }
}
