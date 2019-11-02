package com.kroegerama.kaiteki.retrofit

import android.util.Log
import okhttp3.Interceptor
import okhttp3.Response

object DebugInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        Log.d(
            "OkHttp",
            "Request\n" +
                    "\tMethod: ${request.method()}\n" +
                    "\tURL: ${request.url()}\n" +
                    "\tHeaders: ${request.headers().toMultimap()}"
        )
        val response = chain.proceed(request)
        Log.d(
            "OkHttp",
            "Response\n" +
                    "\tCode: ${response.code()} ${response.message()}\n" +
                    "\tHeaders: ${response.headers().toMultimap()}"
        )
        return response
    }

}