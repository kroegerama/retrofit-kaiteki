package com.kroegerama.kaiteki.retrofit.app

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import com.google.gson.reflect.TypeToken
import com.kroegerama.kaiteki.retrofit.DebugInterceptor
import com.kroegerama.kaiteki.retrofit.listing.*
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class CachedListingExample {

    private val api: KotlinAPI
    private val cachedListing: CachedListing<String>

    init {
        val client = OkHttpClient.Builder()

        if (BuildConfig.DEBUG) {
            client.addNetworkInterceptor(DebugInterceptor)
        }

        val retrofit = Retrofit.Builder()
                .client(client.build())
                .addConverterFactory(GsonConverterFactory.create())
                .baseUrl("https://jsonplaceholder.typicode.com")
                .build()
        api = retrofit.create(KotlinAPI::class.java)

        val converter = GsonItemConverter()
        val saveCallback = object : SaveCallback<String> {
            private val cache = HashMap<Int, String>()

            override fun save(key: Int, value: String) {
                cache[key] = value
            }

            override fun restore(key: Int): String? {
                return cache[key]
            }
        }
        cachedListing = CachedListing(converter, saveCallback)
    }

    private val listingLiveData = MutableLiveData<Listing<Post>>()
    private val postLiveData: LiveData<Post> = Transformations.switchMap(listingLiveData) { it.result }
    private val networkState: LiveData<NetworkState> = Transformations.switchMap(listingLiveData) { it.networkState }

    fun fetch() {
        cachedListing.run {
            listingLiveData.value = api.getPostNoCacheNoRetry(1).createCachedListing(object : TypeToken<Post>() {}.type)
        }
    }

    fun retry() {
        listingLiveData.value?.retry()
    }
}