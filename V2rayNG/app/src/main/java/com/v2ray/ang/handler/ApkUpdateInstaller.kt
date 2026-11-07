package com.v2ray.ang.handler

import android.content.Context
import android.content.Intent
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
            val file = File(context.cacheDir, "barani-update.apk")
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
                }
            }
            if (file.length() < 1024) {
                file.delete()
                error("Downloaded file is empty")
            }
            file
        }
    }

    fun canInstallPackages(context: Context): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.O ||
            context.packageManager.canRequestPackageInstalls()
    }

    fun install(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${BuildConfig.APPLICATION_ID}.cache",
            file
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}
