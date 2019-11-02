package com.kroegerama.kaiteki.retrofit.cache

import retrofit2.Call
import retrofit2.CallAdapter
import retrofit2.Retrofit
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService


class CacheCallAdapterFactory(
    private val handler: CacheHandler
) : CallAdapter.Factory() {

    private val executor: ScheduledExecutorService by lazy { Executors.newScheduledThreadPool(1) }

    override fun get(returnType: Type, annotations: Array<out Annotation>, retrofit: Retrofit): CallAdapter<*, *>? {
        val cache: Cache? = annotations.firstOrNull { it is Cache }?.let { it as Cache }
        val delegate: CallAdapter<Any, Any> = retrofit.nextCallAdapter(this, returnType, annotations) as CallAdapter<Any, Any>

        if (returnType !is ParameterizedType) {
            throw IllegalStateException("Cached call must have generic type (e.g., Call<ResponseBody>)")
        }

        val type = returnType.actualTypeArguments[0]

        return object : CallAdapter<Any, Any> {
            override fun responseType(): Type = delegate.responseType()

            override fun adapt(call: Call<Any>): Any {
                return delegate.adapt(if (cache != null) CacheCall(retrofit, cache, annotations, type, call, executor, handler) else call)
            }
        }
    }
}