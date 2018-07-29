package com.martmists.packend.tools

import io.ktor.application.ApplicationCall
import io.ktor.request.receiveText
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import javax.xml.bind.DatatypeConverter

class Verify(key: String) {
    private val hasher = Mac.getInstance("HmacSHA1").apply{
        init(SecretKeySpec(key.toByteArray(), "HmacSHA1"))
    }

    private fun hexdigest(body: String): String {
        val hash = hasher.doFinal(body.toByteArray())

        return DatatypeConverter.printHexBinary(hash)
    }

    suspend fun validate(call: ApplicationCall): Boolean {
        val other = call.request.headers["X-Hub-Signature"] ?: return false
        val me = "sha1=" + hexdigest(call.receiveText())
        return me.toLowerCase() == other.toLowerCase()
    }
}
