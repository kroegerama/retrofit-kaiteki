package com.kroegerama.kaiteki.retrofit.dsl

import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class KaitekiCallback<T> : Callback<T> {

    private var before: (() -> Unit)? = null
    private var after: (() -> Unit)? = null

    private var onResponse: ((Response<T>) -> Unit)? = null
    private var onFailure: ((Throwable) -> Unit)? = null

    private var onSuccess: ((Response<T>) -> Unit)? = null
    private var onNoSuccess: ((Response<T>) -> Unit)? = null
    private var onError: ((Response<T>) -> Unit)? = null

    override fun onFailure(call: Call<T>, t: Throwable) {
        onFailure?.invoke(t)
        after?.invoke()
    }

    override fun onResponse(call: Call<T>, response: Response<T>) {
        onResponse?.invoke(response)

        if (response.isSuccessful) {
            onSuccess?.invoke(response)
        } else {
            onNoSuccess?.invoke(response)
            if (response.code() in 400..599) {
                onError?.invoke(response)
            }
        }

        after?.invoke()
    }

    /**
     * will always be called before any listener will be called
     */
    fun before(listener: () -> Unit) {
        before = listener
    }

    /**
     * will always be called after any listener was called
     */
    fun after(listener: () -> Unit) {
        after = listener
    }

    /**
     * will be called when a response arrives (not checked for success)
     */
    fun onResponse(block: Response<T>.() -> Unit) {
        onResponse = block
    }

    /**
     * will be called when a response arrives and isSuccessful is true
     */
    fun onSuccess(block: Response<T>.() -> Unit) {
        onSuccess = block
    }

    /**
     * will be called when a response arrives and isSuccessful is false
     */
    fun onNoSuccess(block: Response<T>.() -> Unit) {
        onNoSuccess = block
    }

    /**
     * will be called when a response arrives, isSuccessful is false, and the response code is in
     * the range 400..599
     */
    fun onError(block: Response<T>.() -> Unit) {
        onError = block
    }

    /**
     * will be called when a network exception occurred
     */
    fun onFailure(listener: (Throwable) -> Unit) {
        onFailure = listener
    }

    fun doBefore() = before?.invoke()
}

inline fun <T> Call<T>.enqueue(block: KaitekiCallback<T>.() -> Unit) {
    KaitekiCallback<T>().apply {
        block.invoke(this)
        doBefore()
        enqueue(this)
    }
}