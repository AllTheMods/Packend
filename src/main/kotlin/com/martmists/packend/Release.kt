package com.martmists.packend

data class Release(
        val action: String,
        val release: Map<String, Any>,
        val repository: Map<String, Any>,
        val sender: Map<String, Any>
)
