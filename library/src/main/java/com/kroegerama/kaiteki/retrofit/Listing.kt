package com.kroegerama.kaiteki.retrofit

import androidx.lifecycle.LiveData
import java.io.Closeable

class Listing<T>(
    val result: LiveData<RetrofitResponse<T>>,
    val state: LiveData<ListingState>,
    private val updateFun: () -> Unit,
    private val cancelFun: () -> Unit
) : Closeable {
    override fun close() = cancel()

    fun update() = updateFun.invoke()
    fun cancel() = cancelFun.invoke()
}