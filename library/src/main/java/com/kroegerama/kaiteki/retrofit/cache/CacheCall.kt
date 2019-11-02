package com.kroegerama.kaiteki.retrofit.cache

import android.util.Log
import okhttp3.Headers
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.ResponseBody
import okio.Buffer
import retrofit2.*
import java.io.IOException
import java.lang.reflect.Type
import java.util.concurrent.ScheduledExecutorService


class CacheCall<T>(
    val retrofit: Retrofit,
    val cacheAnnotation: Cache,
    val annotations: Array<out Annotation>,
    val type: Type,
    val delegate: Call<T>,
    val executor: ScheduledExecutorService,
    val handler: CacheHandler
) : Call<T> {

    private enum class Action {
        NoAction,
        Reload,
        Full
    }

    override fun enqueue(callback: Callback<T>) {
        if (delegate.request().method() == "GET") {
            enqueueWithCache(callback)
        } else {
            delegate.enqueue(object : Callback<T> {
                override fun onResponse(call: Call<T>, response: Response<T>) {
                    executor.execute { callback.onResponse(call, response) }
                }

                override fun onFailure(call: Call<T>, t: Throwable) {
                    executor.execute { callback.onFailure(call, t) }
                }
            })
        }
    }

    private fun checkTimestamp(timestamp: Long): Action {
        val delta = System.currentTimeMillis() - timestamp
        if (cacheAnnotation.debounce > 0 && delta < cacheAnnotation.debounce) {
            Log.d(TAG, "Debounce")
            return Action.NoAction
        }

        if (cacheAnnotation.maxAge > 0 && delta < cacheAnnotation.maxAge) {
            Log.d(TAG, "Reload")
            return Action.Reload
        }

        Log.d(TAG, "Cache too old")
        return Action.Full
    }

    private fun enqueueWithCache(callback: Callback<T>) {
        Thread {
            handler.getCache(buildRequest())?.let outer@{ item ->
                checkTimestamp(item.timestamp).let { action ->
                    when (action) {
                        Action.Full -> return@outer   //Cached data too old
                        Action.NoAction,              //Debounce/Reload
                        Action.Reload -> {
                            val convertedData: T? = bytesToResponse(retrofit, type, annotations, item.data)
                            executor.execute { callback.onResponse(delegate, Response.success(convertedData, Headers.of("kaiteki-cached", "true"))) }
                        }
                    }
                    if (action == Action.NoAction) {  //Debounce
                        return@Thread
                    }
                }
            }

            delegate.enqueue(object : Callback<T> {
                override fun onResponse(call: Call<T>, response: Response<T>) {
                    if (response.isSuccessful) {
                        responseToBytes(retrofit, type, response.body(), annotations)?.let {
                            handler.putCache(response, it)
                        }
                    }
                    executor.execute {
                        callback.onResponse(call, response)
                    }
                }

                override fun onFailure(call: Call<T>, t: Throwable) {
                    executor.execute { callback.onFailure(call, t) }
                }
            })
        }.start()
    }

    private fun buildRequest(): Request = delegate.request().newBuilder().build()

    override fun isExecuted(): Boolean = false

    override fun clone(): Call<T> = CacheCall(retrofit, cacheAnnotation, annotations, type, delegate.clone(), executor, handler)

    override fun isCanceled(): Boolean = false

    override fun cancel() = delegate.cancel()

    override fun execute(): Response<T> = delegate.execute()

    override fun request(): Request = delegate.request()

    private fun <T> responseToBytes(retrofit: Retrofit, type: Type, data: T, annotations: Array<out Annotation>): ByteArray? {
        retrofit.converterFactories().filterNotNull().forEach {
            it.requestBodyConverter(type, annotations, null, retrofit)?.let {
                val buf = Buffer()
                try {
                    (it as Converter<T, RequestBody>).convert(data)?.writeTo(buf)
                } catch (e: IOException) {
                    return@forEach
                }
                return buf.readByteArray()
            }
        }
        return null
    }

    private fun <T> bytesToResponse(retrofit: Retrofit, type: Type, annotations: Array<out Annotation>, data: ByteArray): T? {
        retrofit.converterFactories().filterNotNull().forEach {
            it.responseBodyConverter(type, annotations, retrofit)?.let {
                try {
                    return (it as Converter<ResponseBody, T>).convert(ResponseBody.create(null, data))
                } catch (exc: IOException) {
                    return@forEach
                }
            }
        }

        return null
    }

    companion object {
        const val TAG = "CacheCall"
    }
}