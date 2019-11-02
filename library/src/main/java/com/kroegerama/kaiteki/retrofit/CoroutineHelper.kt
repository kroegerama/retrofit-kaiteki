package com.kroegerama.kaiteki.retrofit

import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.*
import retrofit2.Response
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.random.Random

@Deprecated("Use RetrofitResponse<T> instead", replaceWith = ReplaceWith("RetrofitResponse<T>"))
typealias Result<T> = RetrofitResponse<T>

private typealias ApiFun<T> = suspend () -> Response<T>
private typealias RenewFun<T> = suspend (counter: Int, response: Response<T>) -> Boolean

val DefaultRenewFun: RenewFun<*> = { _, r -> r.code() == 401 }

suspend fun <T> retrofitCall(
    renewFun: RenewFun<T> = DefaultRenewFun,
    retryCount: Int = 0,
    block: ApiFun<T>
): RetrofitResponse<T> {
    lateinit var lastResult: RetrofitResponse<T>
    repeat(retryCount + 1) { counter ->
        val response = try {
            withContext(Dispatchers.IO) { block.invoke() }
        } catch (c: CancellationException) {
            return RetrofitResponse.Cancelled
        } catch (e: Exception) {
            return RetrofitResponse.Error(e)
        }
        if (response.isSuccessful) {
            return RetrofitResponse.Success(response.body())
        } else {
            lastResult = RetrofitResponse.NoSuccess(response.code(), response.errorBody())

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
    resultLiveData: MutableLiveData<RetrofitResponse<T>> = MutableLiveData(),
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
            withContext(Dispatchers.Main) { stateLiveData.value = ListingState.RUNNING }

            val wrappedRenew: RenewFun<T> = { count, response ->
                renewFun(count, response).also { doRenew ->
                    if (doRenew) {
                        withContext(Dispatchers.Main) { stateLiveData.value = ListingState.RETRYING }
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