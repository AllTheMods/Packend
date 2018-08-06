package com.martmists.packend

data class Config(
        val key: String,
        val port: Int,
        val packs: Map<String, String>,
        val blacklisted: List<String>
)