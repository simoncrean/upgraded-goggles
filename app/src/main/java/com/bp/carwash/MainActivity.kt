package com.bp.carwash

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import android.widget.ViewFlipper
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import com.bp.carwash.catalog.CatalogProvider
import com.bp.carwash.catalog.Product
import com.bp.carwash.payment.PaymentGateway
import com.bp.carwash.payment.PaymentResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private companion object {
        const val SCREEN_MENU = 0
        const val SCREEN_PROCESSING = 1
        const val SCREEN_RESULT = 2

        /** Unattended kiosk: fall back to the menu if a screen is left idle. */
        const val RESULT_TIMEOUT_MS = 30_000L
    }

    private val cardIds =
        listOf(R.id.tierQuick, R.id.tierExpress, R.id.tierDeluxe, R.id.tierUltimate)

    private lateinit var flipper: ViewFlipper
    private var activeJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Kiosk posture: keep the screen alive, hide system bars.
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        flipper = findViewById(R.id.flipper)

        loadCatalog()

        findViewById<Button>(R.id.cancelButton).setOnClickListener { returnToMenu() }
        findViewById<Button>(R.id.doneButton).setOnClickListener { returnToMenu() }
    }

    /** Products come from the catalogue (bundled JSON today, API later). */
    private fun loadCatalog() {
        lifecycleScope.launch {
            val products = CatalogProvider.source(this@MainActivity).fetch()
                .productsForDisplay()
            cardIds.forEachIndexed { index, cardId ->
                val card = findViewById<View>(cardId)
                val product = products.getOrNull(index)
                if (product == null) {
                    card.visibility = View.INVISIBLE
                } else {
                    card.visibility = View.VISIBLE
                    bindCard(card, product)
                }
            }
        }
    }

    private fun bindCard(card: View, product: Product) {
        card.findViewById<TextView>(R.id.tierName).text = product.name
        card.findViewById<TextView>(R.id.tierDesc).text = product.description
        card.findViewById<TextView>(R.id.tierPrice).text = product.priceLabel
        card.setBackgroundResource(
            if (product.featured) R.drawable.bg_tier_card_featured
            else R.drawable.bg_tier_card
        )
        card.findViewById<TextView>(R.id.tierBadge).visibility =
            if (product.featured) View.VISIBLE else View.GONE
        // Styling variant exposed for UI tests (drawable identity isn't
        // comparable across inflations).
        card.tag = if (product.featured) "tier_card_featured" else "tier_card_regular"
        card.setOnClickListener { startPurchase(product) }
    }

    private fun startPurchase(product: Product) {
        if (flipper.displayedChild != SCREEN_MENU) return

        findViewById<TextView>(R.id.processingAmount).text = product.priceLabel
        findViewById<TextView>(R.id.processingTier).text = product.name
        flipper.displayedChild = SCREEN_PROCESSING

        activeJob = lifecycleScope.launch {
            // SKU on the reference ties the transaction to the product
            // for back-office reconciliation.
            val reference = "${product.sku}-${System.currentTimeMillis()}"
            when (val result = PaymentGateway.provider.purchase(product.priceCents, reference)) {
                is PaymentResult.Approved -> showApproved(product, result.receiptRef)
                is PaymentResult.Declined -> showDeclined(result.reason)
                PaymentResult.Cancelled -> returnToMenu()
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun showApproved(product: Product, receiptRef: String) {
        // Fire the coin-pulse train on its own job: it must not delay the
        // approved screen, and OK/timeout must not cancel it mid-credit.
        lifecycleScope.launch { WashBayController.pulse(product, receiptRef) }

        findViewById<TextView>(R.id.resultIcon).apply {
            text = "✓"
            setTextColor(ContextCompat.getColor(context, R.color.success))
        }
        findViewById<TextView>(R.id.resultTitle).text = getString(R.string.approved_title)
        findViewById<TextView>(R.id.resultSubtitle).text = getString(R.string.approved_subtitle)
        findViewById<TextView>(R.id.resultInstruction).apply {
            visibility = View.VISIBLE
            text = getString(R.string.approved_instruction)
        }
        findViewById<Button>(R.id.doneButton).text = getString(android.R.string.ok)
        showResult()
    }

    @SuppressLint("SetTextI18n")
    private fun showDeclined(reason: String) {
        findViewById<TextView>(R.id.resultIcon).apply {
            text = "✕"
            setTextColor(ContextCompat.getColor(context, R.color.error))
        }
        findViewById<TextView>(R.id.resultTitle).text = getString(R.string.declined_title)
        findViewById<TextView>(R.id.resultSubtitle).text =
            reason.ifBlank { getString(R.string.declined_subtitle) }
        findViewById<TextView>(R.id.resultInstruction).visibility = View.GONE
        findViewById<Button>(R.id.doneButton).text = getString(R.string.try_again)
        showResult()
    }

    private fun showResult() {
        flipper.displayedChild = SCREEN_RESULT
        activeJob = lifecycleScope.launch {
            delay(RESULT_TIMEOUT_MS)
            returnToMenu()
        }
    }

    private fun returnToMenu() {
        activeJob?.cancel()
        activeJob = null
        flipper.displayedChild = SCREEN_MENU
    }

    @SuppressLint("MissingSuperCall")
    @Deprecated("Kiosk app: back is intentionally a no-op on the menu")
    override fun onBackPressed() {
        if (flipper.displayedChild != SCREEN_MENU) returnToMenu()
        // Swallow back on the menu so customers can't leave the app.
    }
}
