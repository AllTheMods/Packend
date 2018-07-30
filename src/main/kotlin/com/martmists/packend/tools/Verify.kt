package com.martmists.packend.tools

import io.ktor.application.ApplicationCall
import io.ktor.request.receiveText
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import javax.xml.bind.DatatypeConverter

class Verify(key: String) {
    private val hasher = Mac.getInstance("HmacSHA1").apply {
        init(SecretKeySpec(key.toByteArray(), "HmacSHA1"))
    }

    private fun hexdigest(body: String): String {
        val hash = hasher.doFinal(body.toByteArray())

        return DatatypeConverter.printHexBinary(hash)
    }

    fun validate(key: String, text: String): Boolean {
        // Verify a request was sent by github using our secret
        val me = "sha1=" + hexdigest(text)
        return me.toLowerCase() == key.toLowerCase()
    }
}
