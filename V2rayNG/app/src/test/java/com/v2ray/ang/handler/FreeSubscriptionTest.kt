package com.v2ray.ang.handler

import com.v2ray.ang.AppConfig
import com.v2ray.ang.dto.entities.SubscriptionItem
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.Base64

class FreeSubscriptionTest {

    @Test
    fun blankAndEmptyBase64AreAnEmptyFeed() {
        assertTrue(FreeSubscription.isExplicitlyEmptyFeed(""))
        assertTrue(FreeSubscription.isExplicitlyEmptyFeed(" \n\t"))
        assertTrue(FreeSubscription.isExplicitlyEmptyFeed(Base64.getEncoder().encodeToString("\n".toByteArray())))
    }

    @Test
    fun linksAndUnreadableTextAreNotAnEmptyFeed() {
        val links = "vless://11111111-1111-1111-1111-111111111111@example.com:443#free"
        assertFalse(FreeSubscription.isExplicitlyEmptyFeed(links))
        assertFalse(FreeSubscription.isExplicitlyEmptyFeed(Base64.getEncoder().encodeToString(links.toByteArray())))
        assertFalse(FreeSubscription.isExplicitlyEmptyFeed("<html>temporarily unavailable</html>"))
        assertFalse(FreeSubscription.isExplicitlyEmptyFeed("not a subscription"))
    }

    @Test
    fun pinLocksTheFeedWithoutForgettingTheLastUpdate() {
        val item = SubscriptionItem(
            remarks = "changed",
            url = "https://example.invalid/other",
            enabled = false,
            autoUpdate = false,
            lastUpdated = 42L,
        )

        FreeSubscription.pin(item)

        assertEquals(FreeSubscription.REMARKS, item.remarks)
        assertEquals(AppConfig.FREE_SUBSCRIPTION_URL, item.url)
        assertTrue(item.enabled)
        assertTrue(item.autoUpdate)
        assertEquals(AppConfig.SUBSCRIPTION_MIN_INTERVAL_MINUTES, item.updateInterval)
        assertEquals(42L, item.lastUpdated)
    }
}
