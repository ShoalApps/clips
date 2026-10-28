package io.github.shoalapps.clownfish

import android.content.Intent
import android.net.Uri
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Live feed check; it reads public Shorts but does not post or change account settings. */
@RunWith(AndroidJUnit4::class)
class ShortsFeedTest {
    @get:Rule val compose = createAndroidComposeRule<ClipsActivity>()

    @Test fun shortsFeedShowsAUsableVideo() {
        compose.activityRule.scenario.onActivity {
            it.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("clownfish://tab/shorts")).setPackage(it.packageName))
        }
        val comments = hasContentDescription("Comments")
        val retry = hasText("Try again")
        fun count(matcher: androidx.compose.ui.test.SemanticsMatcher) =
            runCatching { compose.onAllNodes(matcher).fetchSemanticsNodes().size }.getOrDefault(0)
        compose.waitUntil(60_000) { count(comments) > 0 || count(retry) > 0 }
        assertTrue("Shorts showed an error", count(retry) == 0 && count(comments) > 0)
    }
}
