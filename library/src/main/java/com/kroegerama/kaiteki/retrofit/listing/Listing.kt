package com.kroegerama.kaiteki.retrofit.listing

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import com.kroegerama.kaiteki.retrofit.dsl.enqueue
import retrofit2.Call

enum class Status {
    RUNNING,
    SUCCESS,
    FAILED
}

@Suppress("DataClassPrivateConstructor")
data class NetworkState private constructor(
        val status: Status,
        val msg: String? = null,
        val code: Int? = null) {
    companion object {
        val LOADED = NetworkState(Status.SUCCESS)
        val LOADING = NetworkState(Status.RUNNING)
        fun error(msg: String?, code: Int? = null) = NetworkState(Status.FAILED, msg, code)
    }
}

data class Listing<T>(
        val result: LiveData<T>,
        val networkState: LiveData<NetworkState>,
        val retry: () -> Unit
)

fun <T> Call<T>.createListing(): Listing<T> {
    val liveData = MutableLiveData<T>()
    val networkState = MutableLiveData<NetworkState>()

    val block: Call<T>.() -> Unit = {
        enqueue {
            before {
                networkState.postValue(NetworkState.LOADING)
            }
            onFailure { t ->
                networkState.postValue(NetworkState.error(t.message))
            }
            onSuccess {
                liveData.postValue(body())
                networkState.postValue(NetworkState.LOADED)
            }
            onNoSuccess {
                networkState.postValue(NetworkState.error("Response Code: ${code()}", code()))
            }
        }
    }
    apply(block)
    return Listing(liveData, networkState, { clone().apply(block) })
}