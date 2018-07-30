package com.martmists.packend.extensions

fun List<String>.containsFile(filepath: String): Boolean{
    for (file in this) {
        if (file.endsWith("/")) {
            if (filepath.contains(file))
                return true
        } else {
            if (filepath.endsWith(file))
                return true
        }
    }
    return false
}