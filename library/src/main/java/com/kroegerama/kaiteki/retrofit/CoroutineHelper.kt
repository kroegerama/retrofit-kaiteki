package com.kroegerama.kaiteki.retrofit

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.*
import retrofit2.Response
import java.io.Closeable
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.random.Random

sealed class Result<out R> {
    data class Success<out T>(val data: T?) : Result<T>()
    data class NoSuccess(val code: Int) : Result<Nothing>()
    object Cancelled : Result<Nothing>()
    data class Error(val exception: Exception) : Result<Nothing>()

    val isSuccess get() = this is Success
    val isNoSuccess get() = this is NoSuccess
    val isCancelled get() = this === Cancelled
    val isError get() = this is Error

    fun getOrNull() = if (this is Success) data else null
    fun <E> map(mapFun: (R?) -> E?) = if (this is Success) Success(mapFun(data)) else this

    // better toString name for objects, data classes will automatically overwrite this
    override fun toString(): String = this::class.java.simpleName
}

enum class ListingState {
    IDLE,
    RUNNING,
    RETRYING,
    FINISHED;

    val isRunning get() = this === RUNNING || this === RETRYING
}

class Listing<T>(
        val result: LiveData<Result<T>>,
        val state: LiveData<ListingState>,
        private val updateFun: () -> Unit,
        private val cancelFun: () -> Unit
) : Closeable {
    override fun close() = cancel()

    fun update() = updateFun.invoke()
    fun cancel() = cancelFun.invoke()
}

private typealias ApiFun<T> = suspend () -> Response<T>
private typealias RenewFun<T> = suspend (counter: Int, response: Response<T>) -> Boolean

val DefaultRenewFun: RenewFun<*> = { _, r -> r.code() == 401 }

suspend fun <T> retrofitCall(
        renewFun: RenewFun<T> = DefaultRenewFun,
        retryCount: Int = 0,
        block: ApiFun<T>
): Result<T> {
    lateinit var lastResult: Result<T>
    repeat(retryCount + 1) { counter ->
        val response = try {
            withContext(Dispatchers.IO) { block.invoke() }
        } catch (c: CancellationException) {
            return Result.Cancelled
        } catch (e: Exception) {
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
        block: ApiFun<T>
) = async(context, start) { retrofitCall(renewFun, retryCount, block) }

fun <T> CoroutineScope.retrofitListing(
        resultLiveData: MutableLiveData<Result<T>> = MutableLiveData(),
        stateLiveData: MutableLiveData<ListingState> = MutableLiveData(),
        launchNow: Boolean = true,
        renewFun: RenewFun<T> = DefaultRenewFun,
        retryCount: Int = 0,
        block: ApiFun<T>
): Listing<T> {
    stateLiveData.value = ListingState.IDLE

    var job: Job? = null
    val update: () -> Unit = {
        job?.cancel()
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
                stateLiveData.value = ListingState.IDLE
            }
        }
        job = null
    }
    return Listing(resultLiveData, stateLiveData, update, cancel)
}