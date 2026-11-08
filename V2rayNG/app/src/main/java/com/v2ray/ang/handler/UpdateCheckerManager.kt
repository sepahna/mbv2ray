package com.v2ray.ang.handler

import android.os.Build
import com.v2ray.ang.AppConfig
import com.v2ray.ang.BuildConfig
import com.v2ray.ang.dto.CheckUpdateResult
import com.v2ray.ang.dto.GitHubRelease
import com.v2ray.ang.dto.UrlContentRequest
import com.v2ray.ang.extension.concatUrl
import com.v2ray.ang.util.HttpUtil
import com.v2ray.ang.util.JsonUtil
import com.v2ray.ang.util.LogUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object UpdateCheckerManager {
    suspend fun checkForUpdate(includePreRelease: Boolean = false): CheckUpdateResult = withContext(Dispatchers.IO) {
        val url = if (includePreRelease) {
            AppConfig.APP_API_URL
        } else {
            AppConfig.APP_API_URL.concatUrl("latest")
        }

        val proxyUsername = SettingsManager.getSocksUsername()
        val proxyPassword = SettingsManager.getSocksPassword()

        var response = HttpUtil.getUrlContent(
            UrlContentRequest(
                url = url,
                timeout = 5000
            )
        )
        if (response.isNullOrEmpty()) {
            val httpPort = SettingsManager.getHttpPort()
            response = HttpUtil.getUrlContent(
                UrlContentRequest(
                    url = url,
                    timeout = 5000,
                    httpPort = httpPort,
                    proxyUsername = proxyUsername,
                    proxyPassword = proxyPassword
                )
            )
                ?: throw IllegalStateException("Failed to get response")
        }

        val latestRelease = if (includePreRelease) {
            JsonUtil.fromJsonSafe(response, Array<GitHubRelease>::class.java)
                ?.firstOrNull()
                ?: throw IllegalStateException("No pre-release found")
        } else {
            JsonUtil.fromJsonSafe(response, GitHubRelease::class.java)
        }
        if (latestRelease == null) {
            return@withContext CheckUpdateResult(hasUpdate = false)
        }

        val latestVersion = latestRelease.tagName.removePrefix("v")
        LogUtil.i(
            AppConfig.TAG,
            "Found new version: $latestVersion (current: ${BuildConfig.VERSION_NAME})"
        )

        val comparableVersion = versionParts(latestVersion) ?: return@withContext CheckUpdateResult(hasUpdate = false)
        val currentVersion = versionParts(BuildConfig.VERSION_NAME) ?: return@withContext CheckUpdateResult(hasUpdate = false)
        return@withContext if (compareVersions(comparableVersion, currentVersion) > 0) {
            val downloadUrl = selectApkDownloadUrl(
                assets = latestRelease.assets.map { it.name to it.browserDownloadUrl },
                abi = Build.SUPPORTED_ABIS[0],
                applicationId = BuildConfig.APPLICATION_ID,
            )
            CheckUpdateResult(
                hasUpdate = true,
                latestVersion = latestVersion,
                releaseNotes = latestRelease.body,
                downloadUrl = downloadUrl,
                isPreRelease = latestRelease.prerelease
            )
        } else {
            CheckUpdateResult(hasUpdate = false)
        }
    }

    private fun versionParts(raw: String): List<Int>? {
        val cleaned = raw.removePrefix("v").trim()
        if (cleaned.isEmpty() || !cleaned.first().isDigit()) return null
        return cleaned.split('.', '-', '_').map { token ->
            token.takeWhile { it.isDigit() }.toIntOrNull() ?: 0
        }
    }

    private fun compareVersions(version1: List<Int>, version2: List<Int>): Int {
        for (i in 0 until maxOf(version1.size, version2.size)) {
            val num1 = version1.getOrElse(i) { 0 }
            val num2 = version2.getOrElse(i) { 0 }
            if (num1 != num2) return num1 - num2
        }
        return 0
    }

    /**
     * Prefers a matching ABI APK for the current distribution, then universal.
     * Avoids picking an ABI asset that only matches because "universal" contains no ABI token.
     */
    fun selectApkDownloadUrl(
        assets: List<Pair<String, String>>,
        abi: String,
        applicationId: String,
    ): String {
        val apks = assets.filter { it.first.endsWith(".apk", ignoreCase = true) }
        if (apks.isEmpty()) {
            throw IllegalStateException("No compatible APK found")
        }
        val fDroid = "fdroid"
        val wantsFdroid = applicationId.contains(fDroid, ignoreCase = true)
        fun matchesDistribution(name: String): Boolean {
            val isFdroidAsset = name.contains(fDroid, ignoreCase = true)
            return if (wantsFdroid) isFdroidAsset else !isFdroidAsset
        }
        val distributionApks = apks.filter { matchesDistribution(it.first) }.ifEmpty { apks }
        val abiMatch = distributionApks.firstOrNull { it.first.contains(abi, ignoreCase = true) }
        val universal = distributionApks.firstOrNull {
            it.first.contains("universal", ignoreCase = true)
        }
        return (abiMatch ?: universal ?: distributionApks.first()).second
    }
}
