package com.martmists.packend.extensions

import io.ktor.application.ApplicationCall
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond
import java.io.File

suspend fun ApplicationCall.assertFound(path: String): Boolean {
    // Verify a path exists, raise 404 if not

    var p = path
    // Filter out `..` for security
    while (p.contains(".."))
        p = p.replace("..", ".")
    val e = File(p).exists()
    if (!e)
        respond(HttpStatusCode.NotFound)

    return e
}