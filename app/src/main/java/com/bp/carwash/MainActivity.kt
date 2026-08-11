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

        bindTier(R.id.tierQuick, WashTier.QUICK)
        bindTier(R.id.tierExpress, WashTier.EXPRESS)
        bindTier(R.id.tierDeluxe, WashTier.DELUXE)
        bindTier(R.id.tierUltimate, WashTier.ULTIMATE)

        findViewById<Button>(R.id.cancelButton).setOnClickListener { returnToMenu() }
        findViewById<Button>(R.id.doneButton).setOnClickListener { returnToMenu() }
    }

    private fun bindTier(includeId: Int, tier: WashTier) {
        val card = findViewById<View>(includeId)
        card.findViewById<TextView>(R.id.tierName).text = getString(tier.nameRes)
        card.findViewById<TextView>(R.id.tierDesc).text = getString(tier.descRes)
        card.findViewById<TextView>(R.id.tierPrice).text = tier.priceLabel
        if (tier.featured) {
            card.setBackgroundResource(R.drawable.bg_tier_card_featured)
        }
        // Styling variant exposed for UI tests (drawable identity isn't
        // comparable across inflations).
        card.tag = if (tier.featured) "tier_card_featured" else "tier_card_regular"
        card.setOnClickListener { startPurchase(tier) }
    }

    private fun startPurchase(tier: WashTier) {
        if (flipper.displayedChild != SCREEN_MENU) return

        findViewById<TextView>(R.id.processingAmount).text = tier.priceLabel
        findViewById<TextView>(R.id.processingTier).text = getString(tier.nameRes)
        flipper.displayedChild = SCREEN_PROCESSING

        activeJob = lifecycleScope.launch {
            val reference = "BPCW-${System.currentTimeMillis()}"
            when (val result = PaymentGateway.provider.purchase(tier.priceCents, reference)) {
                is PaymentResult.Approved -> showApproved(tier, result.receiptRef)
                is PaymentResult.Declined -> showDeclined(result.reason)
                PaymentResult.Cancelled -> returnToMenu()
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun showApproved(tier: WashTier, receiptRef: String) {
        // The app pulses the carwash controller directly — no customer code.
        WashBayController.pulse(tier, receiptRef)

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
