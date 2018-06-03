package com.kroegerama.kaiteki.retrofit.retry

import retrofit2.Call
import retrofit2.CallAdapter
import retrofit2.Retrofit
import java.lang.reflect.Type
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService


object RetryCallAdapterFactory : CallAdapter.Factory() {

    private val executor: ScheduledExecutorService by lazy { Executors.newScheduledThreadPool(1) }

    override fun get(returnType: Type, annotations: Array<out Annotation>, retrofit: Retrofit): CallAdapter<*, *> {
        val retryCount = annotations.firstOrNull { it is Retry }?.let { (it as Retry).value } ?: -1
        val delegate: CallAdapter<Any, Any> = retrofit.nextCallAdapter(this, returnType, annotations) as CallAdapter<Any, Any>

        return object : CallAdapter<Any, Any> {
            override fun responseType(): Type = delegate.responseType()

            override fun adapt(call: Call<Any>): Any {
                return delegate.adapt(if (retryCount >= 0) RetryCall(call, executor, retryCount) else call)
            }
        }
    }
}