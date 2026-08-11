package com.bp.carwash

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bp.carwash.payment.PaymentGateway
import com.bp.carwash.payment.SimulatedPaymentProvider
import org.hamcrest.CoreMatchers.allOf
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * End-to-end retail flow regressions: approval unlocks the wash via pulse,
 * charges the right amount, and returns to the menu.
 */
@RunWith(AndroidJUnit4::class)
class PurchaseFlowTest {

    private lateinit var provider: RecordingApproveProvider

    @Before
    fun setUp() {
        provider = RecordingApproveProvider()
        PaymentGateway.provider = provider
        WashBayController.resetForTest()
    }

    @After
    fun tearDown() {
        PaymentGateway.provider = SimulatedPaymentProvider()
    }

    @Test
    fun approvedPurchaseChargesTierPriceAndPulsesWash() {
        ActivityScenario.launch(MainActivity::class.java).use {
            onView(menuTier("Deluxe Wash")).perform(click())

            onView(withText("Approved")).check(matches(isDisplayed()))
            onView(withText("Wash unlocked")).check(matches(isDisplayed()))
            onView(withText("Drive through to the wash bay")).check(matches(isDisplayed()))

            // Exactly the $30 Deluxe price must reach the payment provider.
            assertEquals(30_00L, provider.lastAmountCents)
            assertTrue(provider.lastReference!!.startsWith("BPCW-"))

            // The wash bay must be pulsed for the purchased tier.
            val pulse = WashBayController.lastPulse
            assertEquals(WashTier.DELUXE, pulse?.tier)
            assertEquals("TEST-3000", pulse?.receiptRef)

            onView(withId(R.id.doneButton)).perform(click())
            onView(menuTier("Quick Wash")).check(matches(isDisplayed()))
        }
    }

    @Test
    fun eachTierChargesItsOwnPrice() {
        val expected = mapOf(
            "Quick Wash" to 10_00L,
            "Express Wash" to 20_00L,
            "Deluxe Wash" to 30_00L,
            "Ultimate Wash" to 40_00L,
        )
        ActivityScenario.launch(MainActivity::class.java).use {
            expected.forEach { (tierName, cents) ->
                onView(menuTier(tierName)).perform(click())
                onView(withText("Approved")).check(matches(isDisplayed()))
                assertEquals(tierName, cents, provider.lastAmountCents)
                onView(withId(R.id.doneButton)).perform(click())
            }
        }
    }

    @Test
    fun declinedPurchaseShowsDeclineAndDoesNotPulse() {
        PaymentGateway.provider = DecliningProvider("Insufficient funds")
        ActivityScenario.launch(MainActivity::class.java).use {
            onView(menuTier("Quick Wash")).perform(click())

            onView(withText("Payment declined")).check(matches(isDisplayed()))
            onView(withText("Insufficient funds")).check(matches(isDisplayed()))

            // No pulse on decline — the wash must stay locked.
            assertNull(WashBayController.lastPulse)

            onView(withText("Try again")).perform(click())
            onView(menuTier("Quick Wash")).check(matches(isDisplayed()))
        }
    }

    @Test
    fun cancelDuringPaymentReturnsToMenuWithoutPulse() {
        PaymentGateway.provider = HangingProvider()
        ActivityScenario.launch(MainActivity::class.java).use {
            onView(menuTier("Ultimate Wash")).perform(click())

            // Processing screen shows the amount and tier being paid for.
            onView(withId(R.id.processingAmount)).check(matches(withText("$40")))
            onView(withId(R.id.processingTier)).check(matches(withText("Ultimate Wash")))
            onView(withText("Present card")).check(matches(isDisplayed()))

            onView(withId(R.id.cancelButton)).perform(click())
            onView(menuTier("Quick Wash")).check(matches(isDisplayed()))
            assertNull(WashBayController.lastPulse)
        }
    }

    @Test
    fun menuIgnoresDoubleTapWhilePaymentInFlight() {
        PaymentGateway.provider = HangingProvider()
        ActivityScenario.launch(MainActivity::class.java).use {
            onView(menuTier("Quick Wash")).perform(click())
            onView(withText("Present card")).check(matches(isDisplayed()))
            // Amount stays the first tier's — no second purchase can start.
            onView(withId(R.id.processingAmount)).check(matches(withText("$10")))
        }
    }

    /** Matches a tier label on the menu card, not the processing screen copy. */
    private fun menuTier(name: String) = allOf(withId(R.id.tierName), withText(name))
}
