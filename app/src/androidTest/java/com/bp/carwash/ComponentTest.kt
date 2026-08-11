package com.bp.carwash

import android.view.View
import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isClickable
import androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bp.carwash.payment.PaymentGateway
import com.bp.carwash.payment.SimulatedPaymentProvider
import org.hamcrest.CoreMatchers.allOf
import org.hamcrest.CoreMatchers.not
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Component-level GUI tests: each screen's building blocks render with the
 * right content, styling hooks, and interactivity — independent of flow.
 */
@RunWith(AndroidJUnit4::class)
class ComponentTest {

    @Before
    fun setUp() {
        PaymentGateway.provider = RecordingApproveProvider()
        WashBayController.resetForTest()
    }

    @After
    fun tearDown() {
        PaymentGateway.provider = SimulatedPaymentProvider()
    }

    // ---------- Header ----------

    @Test
    fun headerShowsHeliosLogoWordmarkAndPrompt() {
        ActivityScenario.launch(MainActivity::class.java).use {
            onView(withId(R.id.headerLogo)).check(matches(isDisplayed()))
            onView(withText("bp")).check(matches(isDisplayed()))
            onView(withText("Carwash")).check(matches(isDisplayed()))
            onView(withText("Select your wash")).check(matches(isDisplayed()))
        }
    }

    @Test
    fun footerShowsTerminalTagline() {
        ActivityScenario.launch(MainActivity::class.java).use {
            onView(withText("Powered by Quest QT850")).check(matches(isDisplayed()))
        }
    }

    // ---------- Tier cards ----------

    @Test
    fun everyTierCardIsAClickableUnitWithPriceNameAndDescription() {
        val cards = mapOf(
            R.id.tierQuick to Triple("$10", "Quick Wash", "Rinse & dry"),
            R.id.tierExpress to Triple("$20", "Express Wash", "Wash, wax & dry"),
            R.id.tierDeluxe to Triple("$30", "Deluxe Wash", "Triple foam, wax & dry"),
            R.id.tierUltimate to Triple("$40", "Ultimate Wash", "Full detail shine & protect"),
        )
        ActivityScenario.launch(MainActivity::class.java).use {
            cards.forEach { (cardId, content) ->
                val (price, name, desc) = content
                onView(withId(cardId)).check(matches(allOf(isDisplayed(), isClickable())))
                onView(inCard(cardId, R.id.tierPrice)).check(matches(withText(price)))
                onView(inCard(cardId, R.id.tierName)).check(matches(withText(name)))
                onView(inCard(cardId, R.id.tierDesc)).check(matches(withText(desc)))
            }
        }
    }

    @Test
    fun onlyUltimateCardUsesFeaturedStyling() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                fun styleOf(id: Int) = activity.findViewById<View>(id).tag

                assertEquals("tier_card_featured", styleOf(R.id.tierUltimate))
                listOf(R.id.tierQuick, R.id.tierExpress, R.id.tierDeluxe).forEach { id ->
                    assertEquals("tier_card_regular", styleOf(id))
                }
            }
        }
    }

    @Test
    fun bestValueBadgeShownOnlyOnUltimate() {
        ActivityScenario.launch(MainActivity::class.java).use {
            onView(inCard(R.id.tierUltimate, R.id.tierBadge))
                .check(matches(allOf(isDisplayed(), withText("BEST VALUE"))))
            listOf(R.id.tierQuick, R.id.tierExpress, R.id.tierDeluxe).forEach { cardId ->
                onView(inCard(cardId, R.id.tierBadge)).check(matches(not(isDisplayed())))
            }
        }
    }

    @Test
    fun tierPriceIsRenderedProminentlyAboveName() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val card = activity.findViewById<View>(R.id.tierQuick)
                val price = card.findViewById<TextView>(R.id.tierPrice)
                val name = card.findViewById<TextView>(R.id.tierName)
                assertTrue("price should render larger than name", price.textSize > name.textSize)
                assertTrue("price should sit above name", price.top < name.top)
            }
        }
    }

    // ---------- Processing screen ----------

    @Test
    fun processingScreenShowsLogoSpinnerAmountAndCancel() {
        PaymentGateway.provider = HangingProvider()
        ActivityScenario.launch(MainActivity::class.java).use {
            onView(allOf(withId(R.id.tierName), withText("Express Wash"))).perform(click())

            onView(withId(R.id.processingSunburst)).check(matches(isDisplayed()))
            onView(withId(R.id.processingSpinner)).check(matches(isDisplayed()))
            onView(withId(R.id.processingAmount)).check(matches(allOf(isDisplayed(), withText("$20"))))
            onView(withId(R.id.processingTier)).check(matches(withText("Express Wash")))
            onView(withText("Present card")).check(matches(isDisplayed()))
            onView(withText("Tap, insert or swipe to pay")).check(matches(isDisplayed()))
            onView(withId(R.id.cancelButton)).check(matches(allOf(isDisplayed(), withText("Cancel"))))
        }
    }

    // ---------- Result screen ----------

    @Test
    fun approvedResultComponentsRenderWithoutCodeBox() {
        ActivityScenario.launch(MainActivity::class.java).use {
            onView(allOf(withId(R.id.tierName), withText("Quick Wash"))).perform(click())

            onView(withId(R.id.resultIcon)).check(matches(allOf(isDisplayed(), withText("✓"))))
            onView(withId(R.id.resultTitle)).check(matches(withText("Approved")))
            onView(withId(R.id.resultSubtitle)).check(matches(withText("Wash unlocked")))
            onView(withId(R.id.resultInstruction))
                .check(matches(withText("Drive through to the wash bay")))
            onView(withId(R.id.doneButton)).check(matches(allOf(isDisplayed(), withText("OK"))))
        }
    }

    @Test
    fun declinedResultComponentsRenderWithoutCodeBox() {
        PaymentGateway.provider = DecliningProvider("Card expired")
        ActivityScenario.launch(MainActivity::class.java).use {
            onView(allOf(withId(R.id.tierName), withText("Quick Wash"))).perform(click())

            onView(withId(R.id.resultIcon)).check(matches(allOf(isDisplayed(), withText("✕"))))
            onView(withId(R.id.resultTitle)).check(matches(withText("Payment declined")))
            onView(withId(R.id.resultSubtitle)).check(matches(withText("Card expired")))
            onView(withId(R.id.resultInstruction)).check(matches(not(isDisplayed())))
            onView(withId(R.id.doneButton)).check(matches(allOf(isDisplayed(), withText("Try again"))))
        }
    }

    private fun inCard(cardId: Int, viewId: Int) =
        allOf(withId(viewId), isDescendantOfA(withId(cardId)))
}
