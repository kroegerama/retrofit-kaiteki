package com.kroegerama.kaiteki.retrofit

import java.security.MessageDigest

private val sha1 by lazy {
    MessageDigest.getInstance("SHA-1")
}

fun String.toSha1Hash(): String {
    val bytes = sha1.digest(toByteArray())
    val result = StringBuilder(bytes.size * 2)

    bytes.forEach {
        result.append(it.toString(16).padStart(2, '0'))
    }

    return result.toString()
}