package com.martmists.packend

import com.fasterxml.jackson.core.util.DefaultIndenter
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter
import com.fasterxml.jackson.databind.SerializationFeature
import com.martmists.packend.extensions.assertFound
import com.martmists.packend.tools.Verify
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.ContentNegotiation
import io.ktor.http.HttpStatusCode
import io.ktor.jackson.jackson
import io.ktor.request.receiveText
import io.ktor.response.respond
import io.ktor.response.respondBytes
import io.ktor.response.respondRedirect
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream


fun main(args: Array<String>) {
    val jsonc = JSONObject(File("config.json").readText())
    val conf = Config(jsonc["key"] as String, jsonc["packs"] as JSONObject)
    val verifier = Verify(conf.key)

    embeddedServer(Netty, 4000) {
        install(ContentNegotiation) {
            jackson {
                configure(SerializationFeature.INDENT_OUTPUT, true)
                setDefaultPrettyPrinter(DefaultPrettyPrinter().apply {
                    indentArraysWith(DefaultPrettyPrinter.FixedSpaceIndenter.instance)
                    indentObjectsWith(DefaultIndenter("    ", "\n"))
                })
            }
        }

        routing {
            get("/api/packs") {
                val packs = File("packs").listFiles().map { it.name }
                call.respond(mapOf("packs" to packs))
            }

            get("/api/{pack}/versions") {
                val pack = call.parameters["pack"]
                if (call.assertFound("packs/$pack")) {
                    val packs = File("packs/$pack").listFiles().map { it.name } + listOf("latest", "experimental")
                    call.respond(mapOf("versions" to packs))
                }
            }

            get("/api/{pack}/{version}/manifest") {
                val pack = call.parameters["pack"]
                val version = call.parameters["version"]

                if (call.assertFound("packs/$pack/$version")) {
                    call.respond(File("packs/$pack/$version/manifest.json").reader().readText())
                }
            }

            get("/api/{pack}/{version}/modpack.zip") {
                val pack = call.parameters["pack"]
                var version = call.parameters["version"]

                when (version) {
                    "experimental" -> {
                        call.respondRedirect("${conf.packs.get(pack)}/archive/master.zip", true)
                    }
                    "latest" -> {
                        version = File("packs/$pack").listFiles().map { it.name }.sortedDescending().first()
                    }
                }
                if (call.assertFound("packs/$pack/$version")) {
                    val baos = ByteArrayOutputStream()
                    val zipfile = ZipOutputStream(baos)

                    File("packs/$pack/$version/").walk().forEach {
                        if (!it.isDirectory) {
                            val path = it.path.replace("\\", "/").substringAfter("$version/")
                            zipfile.putNextEntry(ZipEntry(path))
                            zipfile.write(it.readBytes())
                            zipfile.closeEntry()
                        }
                    }

                    zipfile.close()
                    call.respondBytes(baos.toByteArray())
                }
            }

            // Github updates
            post("/github") {
                if (verifier.validate(call)) {
                    val text = call.receiveText()
                    val json = JSONObject(text)
                    if (json["action"] == "published") {
                        val release = json["release"] as JSONObject
                        val repo = json["repository"] as JSONObject
                        val version = release["tag_name"]
                        val branch = release["target_commitish"]
                        val name = repo["name"]
                        val zip = DownloadManager.downloadZip("${repo["html_url"]}/archive/$branch.zip")
                        var ze: ZipEntry? = zip.nextEntry
                        while (ze != null) {
                            println("Unzipping " + ze.name)
                            val out = FileOutputStream("packs/$name/$version/${ze.name}")
                            var c = zip.read()
                            while (c != -1) {
                                out.write(c)
                                c = zip.read()
                            }
                            zip.closeEntry()
                            out.close()
                            ze = zip.nextEntry
                        }
                        zip.close()
                    }
                }
                call.respond(HttpStatusCode.OK)
            }

        }
    }.start(wait = true)
}
