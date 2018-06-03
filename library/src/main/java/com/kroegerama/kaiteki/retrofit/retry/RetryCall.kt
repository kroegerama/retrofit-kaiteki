package com.kroegerama.kaiteki.retrofit.retry

import okhttp3.Request
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.concurrent.ScheduledExecutorService


class RetryCall<T>(
        val delegate: Call<T>,
        val executor: ScheduledExecutorService,
        val retryCount: Int) : Call<T> {

    override fun enqueue(callback: Callback<T>) {
        delegate.enqueue(RetryCallback(delegate, callback, executor, retryCount))
    }

    override fun isExecuted(): Boolean {
        return delegate.isExecuted
    }

    override fun clone(): Call<T> {
        return RetryCall(delegate.clone(), executor, retryCount)
    }

    override fun isCanceled(): Boolean {
        return delegate.isCanceled
    }

    override fun cancel() {
        delegate.cancel()
    }

    override fun execute(): Response<T> {
        return delegate.execute()
    }

    override fun request(): Request {
        return delegate.request()
    }


}