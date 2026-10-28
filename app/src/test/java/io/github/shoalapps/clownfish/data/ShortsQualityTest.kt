package io.github.shoalapps.clownfish.data

import org.junit.Assert.assertEquals
import org.junit.Test

class ShortsQualityTest {
    @Test fun defaultsPreserveBestAvailableOnBothNetworkTypes() {
        assertEquals(Int.MAX_VALUE, ShortsQuality.maxHeight(Settings(), unmetered = true))
        assertEquals(Int.MAX_VALUE, ShortsQuality.maxHeight(Settings(), unmetered = false))
    }

    @Test fun selectsTheLimitForTheActualNetwork() {
        val settings = Settings(shortsQualityUnmetered = 2160, shortsQualityMetered = 480)
        assertEquals(2160, ShortsQuality.maxHeight(settings, unmetered = true))
        assertEquals(480, ShortsQuality.maxHeight(settings, unmetered = false))
        assertEquals(Int.MAX_VALUE, ShortsQuality.maxHeight(settings.copy(shortsQualityMetered = -1), unmetered = false))
    }
}
