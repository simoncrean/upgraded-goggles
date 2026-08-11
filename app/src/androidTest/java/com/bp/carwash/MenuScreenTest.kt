package com.bp.carwash

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith

/** The 4-button retail menu: every tier, price, and description visible. */
@RunWith(AndroidJUnit4::class)
class MenuScreenTest {

    @Test
    fun allFourTiersWithPricesAndDescriptionsAreShown() {
        ActivityScenario.launch(MainActivity::class.java).use {
            onView(withText("Quick Wash")).check(matches(isDisplayed()))
            onView(withText("Express Wash")).check(matches(isDisplayed()))
            onView(withText("Deluxe Wash")).check(matches(isDisplayed()))
            onView(withText("Ultimate Wash")).check(matches(isDisplayed()))

            onView(withText("$10")).check(matches(isDisplayed()))
            onView(withText("$20")).check(matches(isDisplayed()))
            onView(withText("$30")).check(matches(isDisplayed()))
            onView(withText("$40")).check(matches(isDisplayed()))

            onView(withText("Rinse & dry")).check(matches(isDisplayed()))
            onView(withText("Wash, wax & dry")).check(matches(isDisplayed()))
            onView(withText("Triple foam, wax & dry")).check(matches(isDisplayed()))
            onView(withText("Full detail shine & protect")).check(matches(isDisplayed()))
        }
    }
}
