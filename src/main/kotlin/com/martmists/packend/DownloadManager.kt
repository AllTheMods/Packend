package com.martmists.packend

import okhttp3.*
import java.util.zip.*

object DownloadManager {
    val http = OkHttpClient()
    fun get(url: String): Response {
        return http.newCall(Request.Builder().apply {
            get()
            url(url)
        }.build()).execute()
    }

    fun downloadZip(url: String): ZipInputStream {
        val response = get(url)
        return ZipInputStream(response.body()!!.byteStream())
    }
}