package com.kroegerama.kaiteki.retrofit.app

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kroegerama.kaiteki.architecture.autoClose
import com.kroegerama.kaiteki.retrofit.DebugInterceptor
import com.kroegerama.kaiteki.retrofit.retrofitListing
import kotlinx.coroutines.delay
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory

class TestViewModel : ViewModel() {
    private val api by lazy {
        val client = OkHttpClient.Builder()

        if (BuildConfig.DEBUG) {
            client.addInterceptor(DebugInterceptor)
        }

        val retrofit = Retrofit.Builder()
            .client(client.build())
            .addConverterFactory(GsonConverterFactory.create())
            .addConverterFactory(ScalarsConverterFactory.create())
            .baseUrl("https://jsonplaceholder.typicode.com")
            .build()

        retrofit.create(KotlinAPI::class.java)
    }

    private val postId = MutableLiveData<Int>().apply { value = 1 }

    private val listing = Transformations.map(postId) { postId ->
        viewModelScope.retrofitListing { api.getPostSuspending(postId).also { delay(500) } }
    }.autoClose()

    val post = Transformations.switchMap(listing) { it.result }
    val state = Transformations.switchMap(listing) { it.state }
    val running = Transformations.map(state) { it.isRunning }

    fun setPostId(id: Int) {
        postId.value = id
    }

    fun update() {
        listing.value?.update()
    }
}