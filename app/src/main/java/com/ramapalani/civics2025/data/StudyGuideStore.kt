package com.ramapalani.civics2025.data

import android.content.Context
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

object StudyGuideStore {
    const val SOURCE_URL =
        "https://www.uscis.gov/sites/default/files/document/brochures/USCIS-2025-Civics-Test-Study-Guide.pdf"

    private const val FILE_NAME = "one-nation-one-people.pdf"
    private const val MIN_BYTES = 1_000_000L

    fun file(context: Context): File = File(context.filesDir, FILE_NAME)

    fun isPresent(context: Context): Boolean {
        val pdf = file(context)
        return pdf.exists() && pdf.length() >= MIN_BYTES && isPdf(pdf)
    }

    fun download(context: Context, onProgress: (downloaded: Long, total: Long) -> Unit) {
        val dest = file(context)
        val tmp = File(dest.parentFile, "$FILE_NAME.part")
        tmp.delete()
        val connection = (URL(SOURCE_URL).openConnection() as HttpURLConnection).apply {
            instanceFollowRedirects = true
            connectTimeout = 30_000
            readTimeout = 60_000
            setRequestProperty(
                "User-Agent",
                "Civics2025/1.0 (Android; unofficial practice app)",
            )
            setRequestProperty("Accept", "application/pdf")
        }
        try {
            connection.connect()
            val code = connection.responseCode
            if (code !in 200..299) {
                error("USCIS returned HTTP $code")
            }
            val total = connection.contentLengthLong
            connection.inputStream.use { input ->
                tmp.outputStream().use { output ->
                    val buffer = ByteArray(64 * 1024)
                    var downloaded = 0L
                    while (true) {
                        val n = input.read(buffer)
                        if (n < 0) break
                        output.write(buffer, 0, n)
                        downloaded += n
                        onProgress(downloaded, total)
                    }
                }
            }
            if (tmp.length() < MIN_BYTES || !isPdf(tmp)) {
                tmp.delete()
                error("The download was not the USCIS study guide PDF.")
            }
            dest.delete()
            if (!tmp.renameTo(dest)) {
                tmp.copyTo(dest, overwrite = true)
                tmp.delete()
            }
        } catch (ex: Exception) {
            tmp.delete()
            throw ex
        } finally {
            connection.disconnect()
        }
    }

    private fun isPdf(file: File): Boolean {
        return file.inputStream().use { input ->
            val header = ByteArray(4)
            input.read(header) == 4 && header.contentEquals("%PDF".toByteArray())
        }
    }
}
