package com.v2ray.ang.handler

import com.v2ray.ang.AppConfig
import com.v2ray.ang.dto.entities.SubscriptionItem
import java.util.Base64

/**
 * The built-in «رایگان» group. Its address is fixed; the site response is the whole list.
 * A successful empty feed clears the group. An error response does not.
 */
object FreeSubscription {
    const val REMARKS = "رایگان"

    fun isFree(id: String?): Boolean = id == AppConfig.FREE_SUBSCRIPTION_ID

    /** Locks the fields the user must not change. Other fields, such as last update, stay. */
    fun pin(item: SubscriptionItem) {
        item.remarks = REMARKS
        item.url = AppConfig.FREE_SUBSCRIPTION_URL
        item.enabled = true
        item.autoUpdate = true
        item.updateInterval = AppConfig.SUBSCRIPTION_MIN_INTERVAL_MINUTES
        item.allowInsecureUrl = false
    }

    /**
     * True only for a deliberate empty list: a blank body, or base64 that decodes to blank.
     * Invalid text is not empty, so an error page cannot wipe the saved configs.
     */
    fun isExplicitlyEmptyFeed(body: String): Boolean {
        val compact = body.trim().replace("\\s".toRegex(), "")
        if (compact.isEmpty()) return true
        return try {
            String(Base64.getDecoder().decode(compact), Charsets.UTF_8).isBlank()
        } catch (_: IllegalArgumentException) {
            false
        }
    }
}
