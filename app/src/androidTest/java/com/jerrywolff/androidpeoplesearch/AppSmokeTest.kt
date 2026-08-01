package com.jerrywolff.androidpeoplesearch

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test

class AppSmokeTest {
    @get:Rule val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun homeScreenShowsPublicSourceNotice() {
        composeRule.onNodeWithText("Search public sources", substring = true).assertIsDisplayed()
    }
}
