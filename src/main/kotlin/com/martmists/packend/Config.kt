package com.martmists.packend

import org.json.JSONObject

data class Config(
        val key: String,
        val packs: JSONObject,
        val blacklisted: List<String>
)