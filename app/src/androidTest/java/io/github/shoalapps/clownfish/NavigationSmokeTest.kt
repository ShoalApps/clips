package io.github.shoalapps.clownfish

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NavigationSmokeTest {
    @get:Rule val compose = createAndroidComposeRule<ClipsActivity>()

    @Test fun meteredShortsQualityCanBeInspectedWithoutChangingIt() {
        compose.onNodeWithContentDescription("Settings").performClick()
        compose.onNode(hasScrollAction()).performScrollToNode(hasText("Quality on metered networks"))
        compose.onNodeWithText("Quality on metered networks").performClick()
        compose.onNodeWithText("Shorts quality on metered networks").assertExists()
        compose.onNodeWithText("480p").assertExists()
        compose.onNodeWithText("Close").performClick()
    }

    @Test fun settingsOpens() {
        compose.onNodeWithContentDescription("Settings").performClick()
        compose.onNodeWithText("Settings").assertExists()
    }
}
