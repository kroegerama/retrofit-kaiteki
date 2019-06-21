package com.kroegerama.kaiteki.retrofit

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.*
import retrofit2.Response
import java.io.Closeable
import java.io.IOException
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.random.Random

sealed class Result<out T> {
    data class Success<out T>(val data: T?) : Result<T>()
    data class NoSuccess(val code: Int) : Result<Nothing>()
    data class Error(val exception: Exception) : Result<Nothing>()

    fun getOrNull() = if (this is Success) data else null
}

enum class ListingState {
    IDLE,
    CANCELLED,
    RUNNING,
    RETRYING,
    FINISHED;

    val isRunning get() = this == RUNNING || this == RETRYING
}

class Listing<T>(
        val result: LiveData<Result<T>>,
        val state: LiveData<ListingState>,
        private val update: () -> Unit,
        private val cancel: () -> Unit
) : Closeable {
    override fun close() = cancel()

    fun update() = update.invoke()
    fun cancel() = cancel.invoke()
}

typealias RenewFun<T> = suspend (counter: Int, response: Response<T>) -> Boolean

val DefaultRenewFun: RenewFun<*> = { _, r -> r.code() == 401 }

suspend fun <T> retrofitCall(
        renewFun: RenewFun<T> = DefaultRenewFun,
        retryCount: Int = 0,
        block: suspend () -> Response<T>
): Result<T> {
    var lastResult: Result<T> = Result.Error(IllegalStateException())
    repeat(retryCount + 1) { counter ->
        val response = try {
            withContext(Dispatchers.IO) { block.invoke() }
        } catch (e: IOException) {
            return Result.Error(e)
        }
        if (response.isSuccessful) {
            return Result.Success(response.body())
        } else {
            lastResult = Result.NoSuccess(response.code())

            val doRenew = counter < retryCount && renewFun(counter, response)
            if (doRenew) {
                delay(Random.nextLong(50, 500))
            } else {
                return lastResult
            }
        }
    }
    return lastResult
}

fun <T> CoroutineScope.retrofitCallAsync(
        context: CoroutineContext = EmptyCoroutineContext,
        start: CoroutineStart = CoroutineStart.DEFAULT,
        renewFun: RenewFun<T> = DefaultRenewFun,
        retryCount: Int = 0,
        block: suspend () -> Response<T>
) = async(context, start) { retrofitCall(renewFun, retryCount, block) }

fun <T> CoroutineScope.retrofitListing(
        resultLiveData: MutableLiveData<Result<T>> = MutableLiveData(),
        stateLiveData: MutableLiveData<ListingState> = MutableLiveData(),
        launchNow: Boolean = true,
        renewFun: RenewFun<T> = DefaultRenewFun,
        retryCount: Int = 0,
        block: suspend () -> Response<T>
): Listing<T> {
    stateLiveData.value = ListingState.IDLE

    var job: Job? = null
    val update: () -> Unit = {
        job?.let { oldJob ->
            oldJob.cancel()
            stateLiveData.value = ListingState.CANCELLED
        }
        job = launch {
            withContext(Dispatchers.Main) {
                stateLiveData.value = ListingState.RUNNING
            }
            val wrappedRenew: RenewFun<T> = { count, response ->
                renewFun(count, response).also { doRenew ->
                    if (doRenew) {
                        withContext(Dispatchers.Main) {
                            stateLiveData.value = ListingState.RETRYING
                        }
                    }
                }
            }
            val response = retrofitCall(wrappedRenew, retryCount, block)
            withContext(Dispatchers.Main) {
                resultLiveData.value = response
                stateLiveData.value = ListingState.FINISHED
            }
            job = null
        }
    }.apply {
        if (launchNow) {
            invoke()
        }
    }

    val cancel: () -> Unit = {
        job?.run {
            if (isActive) {
                cancel()
                stateLiveData.value = ListingState.CANCELLED
            }
        }
        job = null
    }
    return Listing(resultLiveData, stateLiveData, update, cancel)
}