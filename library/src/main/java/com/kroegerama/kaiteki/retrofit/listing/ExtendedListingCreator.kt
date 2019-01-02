package com.kroegerama.kaiteki.retrofit.listing

import android.os.Handler
import androidx.lifecycle.MutableLiveData
import com.kroegerama.kaiteki.retrofit.dsl.enqueue
import retrofit2.Call
import retrofit2.Response
import java.util.*

typealias RetryFunc <T> = (counter: Int, response: Response<T>?, failure: Throwable?) -> Boolean
typealias RefreshTokenFunc <T> = (counter: Int, response: Response<*>) -> Call<T>?
typealias RefreshResultValidator <T> = (token: T?) -> Boolean

class ExtendedListingCreator<R>(
        private val shouldRetry: RetryFunc<*>,
        private val createRefreshTokenCall: RefreshTokenFunc<R>,
        private val shouldRetryRefresh: RetryFunc<R>,
        private val handleNewToken: RefreshResultValidator<R>
) {
    companion object {
        private const val MIN_DELAY = 50
        private const val MAX_DELAY = 200

        private val random = Random()
        private val handler = Handler()

        private fun post(function: () -> Unit) {
            handler.postDelayed(function, MIN_DELAY.toLong() + random.nextInt(MAX_DELAY - MIN_DELAY + 1))
        }
    }

    fun <T> Call<T>.createListing(fetchNow: Boolean = true): Listing<T> {
        val liveData = MutableLiveData<T>()
        val networkState = MutableLiveData<NetworkState>()

        lateinit var onRefreshTokenSuccess: (Response<T>, Response<R>) -> Unit
        lateinit var refreshToken: (Response<T>) -> Unit
        lateinit var main: Call<T>.() -> Unit

        var retryCounter = 0
        var retryRefreshCounter = 0

        val onFailure: (Throwable) -> Unit = { t ->
            if (shouldRetry.invoke(retryCounter++, null, t)) {
                post {
                    clone().apply(main)
                }
            } else {
                networkState.postValue(NetworkState.Error(t.message))
            }
        }
        val onNoSuccess: (Response<T>) -> Unit = { response ->
            if (shouldRetry.invoke(retryCounter++, response, null)) {
                post {
                    clone().apply(main)
                }
            } else {
                networkState.postValue(NetworkState.Error("Response Code: ${response.code()}", response.code()))
            }
        }
        val onSuccess: (T?) -> Unit = { result ->
            retryCounter = 0

            liveData.postValue(result)
            networkState.postValue(NetworkState.LOADED)
        }

        onRefreshTokenSuccess = onRefreshTokenSuccess@{ originalResponse, refreshResponse ->
            val refreshSuccessful = handleNewToken.invoke(refreshResponse.body())

            if (!refreshSuccessful) {
                onNoSuccess.invoke(originalResponse)
                return@onRefreshTokenSuccess
            }

            clone().enqueue {
                onFailure { t ->
                    onFailure.invoke(t)
                }
                onNoSuccess {
                    onNoSuccess.invoke(this)
                }
                onSuccess {
                    onSuccess.invoke(body())
                }
            }
        }

        refreshToken = refreshToken@{ originalResponse ->
            val refreshFunc = createRefreshTokenCall(retryRefreshCounter, originalResponse)

            if (refreshFunc == null) {
                onNoSuccess.invoke(originalResponse)
                return@refreshToken
            }

            refreshFunc.enqueue {
                onFailure { t ->
                    if (shouldRetryRefresh.invoke(retryRefreshCounter++, null, t)) {
                        post {
                            refreshToken.invoke(originalResponse)
                        }
                    } else {
                        onFailure.invoke(t)
                    }
                }
                onNoSuccess {
                    if (shouldRetryRefresh.invoke(retryRefreshCounter++, this, null)) {
                        post {
                            refreshToken.invoke(originalResponse)
                        }
                    } else {
                        onNoSuccess.invoke(originalResponse)
                    }
                }
                onSuccess {
                    retryRefreshCounter = 0
                    onRefreshTokenSuccess.invoke(originalResponse, this)
                }
            }
        }

        main = {
            retryRefreshCounter = 0

            networkState.postValue(NetworkState.LOADING)
            enqueue {
                onFailure { t ->
                    onFailure.invoke(t)
                }
                onNoSuccess {
                    refreshToken.invoke(this)
                }
                onSuccess {
                    onSuccess.invoke(body())
                }
            }
        }
        if (fetchNow) {
            apply(main)
        }
        return Listing(liveData, networkState) { clone().apply(main) }
    }
}