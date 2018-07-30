package com.martmists.packend.extensions

fun List<String>.containsFile(filepath: String): Boolean{
    for (file in this) {
        if (file.endsWith("/")) {
            if (filepath.toLowerCase().contains(file))
                return true
        } else {
            if (filepath.toLowerCase().endsWith(file))
                return true
        }
    }
    return false
}