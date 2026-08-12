package com.bp.carwash

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.bp.carwash.catalog.BundledCatalogSource
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/** The bundled catalogue must load and validate on the real device. */
@RunWith(AndroidJUnit4::class)
class CatalogSourceTest {

    @Test
    fun bundledCatalogLoadsFourValidProducts() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val products = BundledCatalogSource(context).fetch().productsForDisplay()
        assertEquals(4, products.size)
        assertEquals(listOf("quick", "express", "deluxe", "ultimate"), products.map { it.id })
    }
}
