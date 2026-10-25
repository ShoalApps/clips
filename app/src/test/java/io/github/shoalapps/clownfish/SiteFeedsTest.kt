package io.github.shoalapps.clownfish

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SiteFeedsTest {
    private val webView = "Mozilla/5.0 (Linux; Android 14; K; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/141.0.7390.122 Mobile Safari/537.36"

    @Test fun phonesGetMobileChromeWithoutTheInAppBrowserMarkers() {
        val ua = SiteViews.chromeAgent(webView, wide = false)
        assertEquals("Mozilla/5.0 (Linux; Android 14; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/141.0.7390.122 Mobile Safari/537.36", ua)
    }

    @Test fun wideScreensGetDesktopChromeOfTheSameVersion() {
        val ua = SiteViews.chromeAgent(webView, wide = true)
        assertTrue(ua.contains("Chrome/141.0.7390.122"))
        assertFalse(ua.contains("Mobile"))
        assertFalse(ua.contains("Android"))
    }

    @Test fun eachTabKeepsToItsOwnSite() {
        assertTrue(Site.TIKTOK.owns("www.tiktok.com"))
        assertTrue(Site.TIKTOK.owns("v16-webapp.tiktokcdn.com"))
        assertFalse(Site.TIKTOK.owns("evil-tiktok.com"))
        assertTrue(Site.REELS.owns("www.instagram.com"))
        assertFalse(Site.REELS.owns("www.tiktok.com"))
        assertEquals(Site.REELS, Site.of("reels"))
    }
}
