package com.v2ray.ang.handler

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.FileProvider
import com.v2ray.ang.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.TimeUnit
import java.util.zip.ZipFile
import kotlin.coroutines.coroutineContext

data class AppUpdateUi(
    val version: String,
    val notes: String = "",
    val downloadUrl: String = "",
    val awaitingChoice: Boolean = false,
    val received: Long = 0,
    val total: Long = -1,
    val error: String? = null,
    val finished: Boolean = false
)

object ApkUpdateInstaller {
    private const val MIN_APK_BYTES = 1024L

    suspend fun download(
        context: Context,
        url: String,
        onProgress: (received: Long, total: Long) -> Unit
    ): File = withContext(Dispatchers.IO) {
        val client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(2, TimeUnit.MINUTES)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()
        val request = Request.Builder().url(url).build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                error("HTTP ${response.code}")
            }
            val body = response.body
            val total = body.contentLength()
            val file = File(context.cacheDir, "mbv2ray-update.apk")
            if (file.exists() && !file.delete()) {
                error("Could not clear previous update file")
            }
            body.byteStream().use { input ->
                file.outputStream().use { output ->
                    val buffer = ByteArray(16 * 1024)
                    var received = 0L
                    while (true) {
                        coroutineContext.ensureActive()
                        val read = input.read(buffer)
                        if (read < 0) break
                        output.write(buffer, 0, read)
                        received += read
                        withContext(Dispatchers.Main) {
                            onProgress(received, total)
                        }
                    }
                    output.flush()
                }
            }
            assertDownloadComplete(file.length(), total)
            assertZipLooksLikeApk(file)
            validatePackage(context, file)
            file
        }
    }

    fun canInstallPackages(context: Context): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.O ||
            context.packageManager.canRequestPackageInstalls()
    }

    fun assertDownloadComplete(fileLength: Long, expectedTotal: Long) {
        if (fileLength < MIN_APK_BYTES) {
            error("Downloaded file is empty")
        }
        if (expectedTotal > 0L && fileLength != expectedTotal) {
            error("Download incomplete ($fileLength / $expectedTotal bytes)")
        }
    }

    fun assertZipLooksLikeApk(file: File) {
        runCatching {
            ZipFile(file).use { zip ->
                if (zip.getEntry("AndroidManifest.xml") == null) {
                    error("Downloaded file is not a valid APK")
                }
            }
        }.getOrElse { cause ->
            throw IllegalStateException("Downloaded file is not a valid APK", cause)
        }
    }

    fun validatePackage(context: Context, file: File) {
        val archive = context.packageManager.getPackageArchiveInfo(file.absolutePath, 0)
            ?: error("Downloaded APK could not be parsed")
        if (archive.packageName != BuildConfig.APPLICATION_ID) {
            error("APK package mismatch: ${archive.packageName}")
        }
        val incomingCode = packageVersionCode(archive)
        val installedCode = installedVersionCode(context)
        if (incomingCode < installedCode) {
            error("Update version is older than the installed app")
        }
    }

    fun install(context: Context, file: File) {
        validatePackage(context, file)
        val uri = FileProvider.getUriForFile(
            context,
            "${BuildConfig.APPLICATION_ID}.cache",
            file
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        val resolvers = context.packageManager.queryIntentActivities(
            intent,
            PackageManager.MATCH_DEFAULT_ONLY
        )
        for (resolveInfo in resolvers) {
            context.grantUriPermission(
                resolveInfo.activityInfo.packageName,
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        }
        if (resolvers.isEmpty()) {
            error("No package installer available")
        }
        context.startActivity(intent)
    }

    private fun packageVersionCode(info: android.content.pm.PackageInfo): Long {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            info.longVersionCode
        } else {
            @Suppress("DEPRECATION")
            info.versionCode.toLong()
        }
    }

    private fun installedVersionCode(context: Context): Long {
        val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.getPackageInfo(
                context.packageName,
                PackageManager.PackageInfoFlags.of(0)
            )
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(context.packageName, 0)
        }
        return packageVersionCode(info)
    }
}
