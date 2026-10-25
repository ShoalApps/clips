package io.github.tintloom.clips

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ClipLinksTest {
    @Test fun youtubeShorts() {
        assertEquals(ClipLinks.YouTubeShort("dQw4w9WgXcQ"), ClipLinks.parse("https://www.youtube.com/shorts/dQw4w9WgXcQ?si=abc"))
        assertEquals(ClipLinks.YouTubeShort("dQw4w9WgXcQ"), ClipLinks.parse("look at this! https://youtube.com/shorts/dQw4w9WgXcQ"))
    }

    @Test fun tiktokVideosUseTheOfficialPlayer() {
        val t = ClipLinks.parse("https://www.tiktok.com/@scout2015/video/6718335390845095173?lang=en") as ClipLinks.Embed
        assertEquals("TikTok", t.site)
        assertTrue(t.embedUrl.startsWith("https://www.tiktok.com/player/v1/6718335390845095173"))
    }

    @Test fun tiktokShortLinksNeedResolving() {
        assertTrue(ClipLinks.parse("https://vm.tiktok.com/ZMabc123/") is ClipLinks.Unresolved)
        val resolved = ClipLinks.fromResolved("https://www.tiktok.com/@a.b/video/7123456789012345678", "https://vm.tiktok.com/ZMabc123/") as ClipLinks.Embed
        assertTrue(resolved.embedUrl.contains("7123456789012345678"))
    }

    @Test fun instagramReels() {
        val r = ClipLinks.parse("https://www.instagram.com/reel/C1a2B3c4D5e/?igsh=xyz") as ClipLinks.Embed
        assertEquals("https://www.instagram.com/reel/C1a2B3c4D5e/embed/", r.embedUrl)
    }

    @Test fun otherLinksAreIgnored() {
        assertNull(ClipLinks.parse("https://example.com/video/123456789"))
        assertNull(ClipLinks.parse("no link here"))
    }
}
