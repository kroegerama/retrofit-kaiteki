package com.kroegerama.kaiteki.retrofit.listing

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.kroegerama.kaiteki.retrofit.dsl.enqueue
import retrofit2.Call

enum class Status {
    RUNNING,
    SUCCESS,
    FAILED
}

sealed class NetworkState(
        val status: Status
) {
    object LOADED : NetworkState(Status.SUCCESS)
    object LOADING : NetworkState(Status.RUNNING)
    data class Error(val message: String?, val code: Int? = null) : NetworkState(Status.FAILED)

    val isRunning get() = status == Status.RUNNING

    override fun toString(): String {
        return status.toString()
    }
}

data class Listing<T>(
        val result: LiveData<T>,
        val networkState: LiveData<NetworkState>,
        private val retry: () -> Unit
) {
    fun retry() = retry.invoke()
}

fun <T> Call<T>.createListing(fetchNow: Boolean = true) = createListing(fetchNow, null)

internal fun <T> Call<T>.createListing(fetchNow: Boolean = true, successHook: ((T?) -> Unit)? = null): Listing<T> {
    val liveData = MutableLiveData<T>()
    val networkState = MutableLiveData<NetworkState>()

    val block: Call<T>.() -> Unit = {
        networkState.postValue(NetworkState.LOADING)
        enqueue {
            onFailure { t ->
                networkState.postValue(NetworkState.Error(t.message))
            }
            onSuccess {
                liveData.postValue(body())
                successHook?.invoke(body())
                networkState.postValue(NetworkState.LOADED)
            }
            onNoSuccess {
                networkState.postValue(NetworkState.Error("Response Code: ${code()}", code()))
            }
        }
    }
    if (fetchNow) {
        apply(block)
    }
    return Listing(liveData, networkState) { clone().apply(block) }
}