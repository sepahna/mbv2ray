package com.v2ray.ang.handler

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class UpdateCheckerManagerTest {

    @Test
    fun `selects matching abi playstore apk`() {
        val url = UpdateCheckerManager.selectApkDownloadUrl(
            assets = listOf(
                "mbv2ray_1.0.2_armeabi-v7a.apk" to "https://example/v7a",
                "mbv2ray_1.0.2_arm64-v8a.apk" to "https://example/arm64",
                "mbv2ray_1.0.2_universal.apk" to "https://example/universal",
            ),
            abi = "arm64-v8a",
            applicationId = "ir.mbv2ray.client",
        )
        assertEquals("https://example/arm64", url)
    }

    @Test
    fun `falls back to universal when abi missing`() {
        val url = UpdateCheckerManager.selectApkDownloadUrl(
            assets = listOf(
                "mbv2ray_1.0.2_universal.apk" to "https://example/universal",
            ),
            abi = "arm64-v8a",
            applicationId = "ir.mbv2ray.client",
        )
        assertEquals("https://example/universal", url)
    }

    @Test
    fun `prefers fdroid asset for fdroid application id`() {
        val url = UpdateCheckerManager.selectApkDownloadUrl(
            assets = listOf(
                "mbv2ray_1.0.2_arm64-v8a.apk" to "https://example/play",
                "mbv2ray_1.0.2-fdroid_arm64-v8a.apk" to "https://example/fdroid",
            ),
            abi = "arm64-v8a",
            applicationId = "ir.mbv2ray.client.fdroid",
        )
        assertEquals("https://example/fdroid", url)
    }

    @Test
    fun `throws when no apk assets exist`() {
        assertThrows(IllegalStateException::class.java) {
            UpdateCheckerManager.selectApkDownloadUrl(
                assets = listOf("notes.txt" to "https://example/notes"),
                abi = "arm64-v8a",
                applicationId = "ir.mbv2ray.client",
            )
        }
    }
}
