package com.martmists.packend

data class Config(
        val key: String,
        val packs: Map<String, String>,
        val blacklisted: List<String>
)