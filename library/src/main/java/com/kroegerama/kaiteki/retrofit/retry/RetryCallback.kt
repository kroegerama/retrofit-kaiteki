package com.kroegerama.kaiteki.retrofit.retry

import android.util.Log
import com.kroegerama.kaiteki.retrofit.TimeoutExeption
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.*
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit


class RetryCallback<T>(
        val call: Call<T>,
        val delegate: Callback<T>,
        val executor: ScheduledExecutorService,
        val retryCount: Int,
        val retries: Int = 0) : Callback<T> {

    override fun onResponse(call: Call<T>, response: Response<T>) {
        delegate.onResponse(call, response)
    }

    override fun onFailure(call: Call<T>, t: Throwable) {
        if (retries < retryCount) {
            retryCall()
        } else {
            delegate.onFailure(call, TimeoutExeption(t))
        }
    }

    private fun retryCall() {
        val delay = (1 shl retries) * 1000 + random.nextInt(1001).toLong()
        Log.d(TAG, "Retry No. ${retries + 1}/$retryCount with delay $delay")
        executor.schedule({
            call.clone().enqueue(RetryCallback(call, delegate, executor, retryCount, retries + 1))
        }, delay, TimeUnit.MILLISECONDS)
    }

    companion object {
        private const val TAG = "RetryCallback"
        private val random = Random()
    }
}