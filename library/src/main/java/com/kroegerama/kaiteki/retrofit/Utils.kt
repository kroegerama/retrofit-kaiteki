package com.kroegerama.kaiteki.retrofit

import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
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

fun <T, R> Call<T>.map(mapFunc: (T?) -> R?): Call<R> {
    return object : Call<R> {
        val self = this@map
        val me = this

        override fun enqueue(callback: Callback<R>) {
            self.enqueue(object : Callback<T> {
                override fun onFailure(call: Call<T>, t: Throwable) {
                    callback.onFailure(me, t)
                }

                override fun onResponse(call: Call<T>, response: Response<T>) {
                    callback.onResponse(me, response.map(mapFunc))
                }
            })
        }

        override fun isExecuted() = self.isExecuted

        override fun clone(): Call<R> = self.clone().map(mapFunc)

        override fun isCanceled() = self.isCanceled

        override fun cancel() = self.cancel()

        override fun execute(): Response<R> = self.execute().map(mapFunc)

        override fun request() = self.request()
    }
}

fun <T, R> Response<T>.map(mapFunc: (T?) -> R?): Response<R> = if (isSuccessful) {
    Response.success(code(), mapFunc(body()))
} else {
    Response.error(errorBody(), raw())
}