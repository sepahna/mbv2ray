package com.v2ray.ang.handler

import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class ApkUpdateInstallerTest {

    @Test
    fun `assertDownloadComplete accepts matching length`() {
        assertDoesNotThrow {
            ApkUpdateInstaller.assertDownloadComplete(fileLength = 4096, expectedTotal = 4096)
        }
    }

    @Test
    fun `assertDownloadComplete rejects truncated download`() {
        assertThrows(IllegalStateException::class.java) {
            ApkUpdateInstaller.assertDownloadComplete(fileLength = 2048, expectedTotal = 4096)
        }
    }

    @Test
    fun `assertDownloadComplete rejects tiny files`() {
        assertThrows(IllegalStateException::class.java) {
            ApkUpdateInstaller.assertDownloadComplete(fileLength = 128, expectedTotal = -1)
        }
    }

    @Test
    fun `assertZipLooksLikeApk accepts zip with manifest`(@TempDir dir: File) {
        val apk = File(dir, "ok.apk")
        ZipOutputStream(apk.outputStream()).use { zip ->
            zip.putNextEntry(ZipEntry("AndroidManifest.xml"))
            zip.write(byteArrayOf(1, 2, 3, 4))
            zip.closeEntry()
        }
        assertDoesNotThrow { ApkUpdateInstaller.assertZipLooksLikeApk(apk) }
    }

    @Test
    fun `assertZipLooksLikeApk rejects non apk zip`(@TempDir dir: File) {
        val apk = File(dir, "bad.apk")
        ZipOutputStream(apk.outputStream()).use { zip ->
            zip.putNextEntry(ZipEntry("readme.txt"))
            zip.write("nope".toByteArray())
            zip.closeEntry()
        }
        assertThrows(IllegalStateException::class.java) {
            ApkUpdateInstaller.assertZipLooksLikeApk(apk)
        }
    }
}
