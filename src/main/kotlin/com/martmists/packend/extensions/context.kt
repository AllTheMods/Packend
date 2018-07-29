package com.martmists.packend.extensions

import io.ktor.application.ApplicationCall
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond
import java.io.File

suspend fun ApplicationCall.assertFound(path: String): Boolean {
    val e = File(path.replace("..", ".")).exists()
    if (!e)
        respond(HttpStatusCode.NotFound)

    return e
}