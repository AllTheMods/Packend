package com.martmists.packend

import com.fasterxml.jackson.core.util.DefaultIndenter
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter
import com.fasterxml.jackson.databind.SerializationFeature
import com.google.gson.Gson
import com.martmists.packend.extensions.assertFound
import com.martmists.packend.extensions.containsFile
import com.martmists.packend.tools.DownloadManager
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
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream


fun main(args: Array<String>) {
    val conf = Gson().fromJson(File("config.json").readText(), Config::class.java)
    val verifier = Verify(conf.key)

    embeddedServer(Netty, conf.port) {
        install(ContentNegotiation) {
            // JSON output
            jackson {
                configure(SerializationFeature.INDENT_OUTPUT, true)
                setDefaultPrettyPrinter(DefaultPrettyPrinter().apply {
                    indentArraysWith(DefaultPrettyPrinter.FixedSpaceIndenter.instance)
                    indentObjectsWith(DefaultIndenter("    ", "\n"))
                })
            }
        }

        routing {

            // Indexing
            get("/api/packs") {
                // Return all packs
                val packs = File("packs").listFiles().map { it.name }
                call.respond(mapOf("packs" to packs))
            }

            get("/api/{pack}/versions") {
                val pack = call.parameters["pack"]

                // Return all versions of a pack
                if (call.assertFound("packs/$pack")) {
                    val versions = File("packs/$pack").listFiles()
                    val versions_full = versions.map { it.name } + listOf("latest", "experimental")
                    call.respond(mapOf("versions" to versions_full))
                }
            }

            // Downloads
            get("/api/{pack}/{version}/modpack.zip") {
                val pack = call.parameters["pack"]
                var version = call.parameters["version"]

                when (version) {
                    // Dynamically pick versions
                    "experimental" -> {
                        call.respondRedirect("${conf.packs[pack]}/archive/master.zip", true)
                    }
                    "latest" -> {
                        version = File("packs/$pack").listFiles().map { it.name }.sortedDescending().first()
                    }
                }

                // Create zip
                if (call.assertFound("packs/$pack/$version")) {
                    val baos = ByteArrayOutputStream()
                    val zipfile = ZipOutputStream(baos)

                    File("packs/$pack/$version/").walk().forEach {
                        if (!it.isDirectory) {
                            // Add each file with relative path
                            val path = it.path.replace("\\", "/").substringAfter("$version/")
                            zipfile.putNextEntry(ZipEntry(path))
                            zipfile.write(it.readBytes())
                            zipfile.closeEntry()
                        }
                    }

                    zipfile.close()
                    // Send zip to client
                    call.respondBytes(baos.toByteArray())
                }
            }

            // Github updates
            post("/github") {
                val text = call.receiveText()
                if (verifier.validate(call.request.headers["X-Hub-Signature"] ?: "", text)) {
                    if (call.request.headers["X-GitHub-Event"] == "release") {
                        val json = Gson().fromJson(text, Release::class.java)
                        val release = json.release
                        val repo = json.repository
                        val version = release["tag_name"]
                        val name = repo["name"]

                        // Only use repos that are in the config
                        if (conf.packs.containsKey(repo["name"])) {
                            // Download zip
                            val url = "${repo["html_url"]}/archive/$version.zip"
                            val zip = DownloadManager.downloadZip(url)

                            File("packs/$name/$version").mkdirs()
                            var ze: ZipEntry? = zip.nextEntry

                            while (ze != null) {
                                // Extract all files
                                if (!ze.isDirectory and !conf.blacklisted.containsFile(ze.name)) {
                                    val out = File("packs/$name/$version/${ze.name.substringAfter("$version/")}")
                                    out.mkdirs()
                                    out.delete()
                                    zip.copyTo(out.outputStream())
                                }

                                zip.closeEntry()
                                ze = zip.nextEntry
                            }
                            zip.close()
                        }
                    }
                    call.respond(HttpStatusCode.OK)
                }
            }

            // SKCraft Launcher Integration
            get("/api/packs.json"){
                val packs = mutableListOf<Map<String, Any>>()
                File("packs").listFiles().forEach {
                    val latest = File("packs/${it.name}").listFiles().map { it.name }.sortedDescending().first()
                    packs.add(mapOf(
                            "name" to it.name,
                            "title" to it.name.replace("-", " "),
                            "version" to latest,
                            "location" to "packs/${it.name}/manifest.json",
                            "priority" to 1
                    ))
                }

                call.respond(mapOf("minimumVersion" to 1, "packages" to packs.toTypedArray()))
            }

            get("/api/packs/{pack}/{version}/manifest.json"){
                val pack = call.parameters["pack"]
                val version = call.parameters["version"]

                if (call.assertFound("packs/$pack/$version")){
                    call.respond(File("packs/$pack/$version/manifest.json").readText())
                }
            }

            get("/api/news.html"){
                call.respond(File("news.html").readText())
            }

            get("/api/launcher.json"){
                TODO("Not Implemented")
            }
        }
    }.start(wait = true)
}
