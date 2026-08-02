package com.jerrywolff.androidpeoplesearch

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.jerrywolff.androidpeoplesearch.model.ConfidenceLevel
import com.jerrywolff.androidpeoplesearch.model.PersonSearchResult
import com.jerrywolff.androidpeoplesearch.model.SourceCategory
import com.jerrywolff.androidpeoplesearch.search.SourceResultCount
import com.jerrywolff.androidpeoplesearch.ui.ResultsScreen
import com.jerrywolff.androidpeoplesearch.ui.SearchUiState
import com.jerrywolff.androidpeoplesearch.ui.theme.AndroidPeopleSearchTheme
import org.junit.Rule
import org.junit.Test

class AppSmokeTest {
    @get:Rule val composeRule = createComposeRule()

    @Test
    fun resultsAreAlphabeticalAndFilterBySourceCount() {
        val results = listOf(
            result("Zulu Person", "Bing public web references", "bing_web"),
            result("Ada Person", "DuckDuckGo public web references", "duckduckgo_web"),
        )
        composeRule.setContent {
            AndroidPeopleSearchTheme {
                ResultsScreen(
                    state = SearchUiState(
                        results = results,
                        sourceCounts = listOf(
                            SourceResultCount("Bing public web references", 1, false),
                            SourceResultCount("DuckDuckGo public web references", 1, false),
                        ),
                    ),
                    onBack = {},
                    onSelect = {},
                )
            }
        }

        composeRule.onNodeWithText("Name A-Z").assertIsDisplayed()
        composeRule.onNodeWithText("All sources (2)").assertIsSelected()
        composeRule.onNodeWithText("DuckDuckGo public web references: 1 found").performClick().assertIsSelected()
        composeRule.onNodeWithText("All sources (2)").assertIsNotSelected()
        composeRule.onNodeWithText("Ada Person").assertIsDisplayed()
        composeRule.onNodeWithText("Zulu Person").assertDoesNotExist()
    }

    private fun result(title: String, sourceName: String, providerId: String) = PersonSearchResult(
        id = providerId,
        providerId = providerId,
        title = title,
        description = "Public profile for $title",
        url = "https://example.com/$providerId",
        sourceName = sourceName,
        category = SourceCategory.WEB,
        confidence = ConfidenceLevel.MEDIUM,
        confidenceScore = 60,
    )
}
