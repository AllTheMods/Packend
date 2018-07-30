package com.martmists.packend.tools

import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import java.util.zip.ZipInputStream

object DownloadManager {
    private val http = OkHttpClient.Builder().apply{
        protocols(listOf(Protocol.HTTP_1_1))
    }.build()
    fun get(url: String): Response {
        return http.newCall(Request.Builder().apply {
            url(url)
        }.build()).execute()
    }

    fun downloadZip(url: String): ZipInputStream {
        val response = get(url)
        return ZipInputStream(response.body()!!.byteStream())
    }
}