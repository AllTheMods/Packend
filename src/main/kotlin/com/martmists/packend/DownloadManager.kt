package com.martmists.packend

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.util.zip.ZipInputStream

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