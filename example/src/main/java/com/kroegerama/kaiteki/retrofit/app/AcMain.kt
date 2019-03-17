package com.kroegerama.kaiteki.retrofit.app

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import com.kroegerama.kaiteki.retrofit.CacheCallAdapterFactory
import com.kroegerama.kaiteki.retrofit.DebugInterceptor
import com.kroegerama.kaiteki.retrofit.DefaultCacheHandler
import com.kroegerama.kaiteki.retrofit.dsl.enqueue
import com.kroegerama.kaiteki.retrofit.listing.ExtendedListingCreator
import com.kroegerama.kaiteki.retrofit.retry.RetryCallAdapterFactory
import kotlinx.android.synthetic.main.ac_main.*
import okhttp3.OkHttpClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory

class AcMain : AppCompatActivity() {

    private val api by lazy {
        val client = OkHttpClient.Builder()

        if (BuildConfig.DEBUG) {
            client.addInterceptor(DebugInterceptor)
        }

        val retrofit = Retrofit.Builder()
                .client(client.build())
                .addCallAdapterFactory(RetryCallAdapterFactory)
                .addCallAdapterFactory(CacheCallAdapterFactory(DefaultCacheHandler(this)))
                .addConverterFactory(GsonConverterFactory.create())
                .addConverterFactory(ScalarsConverterFactory.create())
                .baseUrl("https://jsonplaceholder.typicode.com")
                .build()

        retrofit.create(KotlinAPI::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.ac_main)

        val elc = ExtendedListingCreator(
                shouldRetry = { counter, response, failure ->
                    Log.d(">> shouldRetry", "$counter, $response, $failure")
                    counter < 5
                },
                createRefreshTokenCall = { counter, response ->
                    Log.d(">> createRefreshToken", "$counter, $response")
                    if (counter < 5) {
                        api.getNotExisting() as Call<Any>
                    } else {
                        api.getPostNoCacheNoRetry(1) as Call<Any>
                    }
                },
                shouldRetryRefresh = { counter, response, failure ->
                    Log.d(">> shouldRetryRefresh", "$counter, $response, $failure")
                    counter < 5
                },
                handleNewToken = { token ->
                    Log.d(">> handleNewToken", "$token")
                    true
                }
        )

        textView.setOnClickListener { loadData() }
        textView.setOnLongClickListener {
            elc.apply {
                val listing = api.getNotExisting().createListing()
                listing.result.observe(this@AcMain, Observer { post ->
                    Toast.makeText(this@AcMain, post.toString(), Toast.LENGTH_LONG).show()
                })
                listing.networkState.observe(this@AcMain, Observer { state ->
                    Log.d(">> networkState", "$state")
                })
            }
            true
        }

        loadDataDSL()
    }

    private fun loadData() {
        api.getPost(1).enqueue(object : Callback<Post> {
            override fun onResponse(call: Call<Post>, response: Response<Post>) {
                Log.d("onResponse", response.body().toString())
                Log.d("header", response.headers().toMultimap().toString())
                textView.text = response.body().toString()
            }

            override fun onFailure(call: Call<Post>, t: Throwable) {
                Log.d("onFailure", "" + t.toString())
                textView.text = t.toString()
            }
        })
    }

    private fun loadDataDSL() {
        api.getPost(1).enqueue {
            before {
                Log.d("before", "Test")
            }
            onSuccess {
                Log.d("onSuccess", body().toString())
                textView.text = body().toString()
            }
            onNoSuccess {
                Log.d("onNoSuccess", body().toString())
            }
            onFailure { t ->
                Log.d("onFailure", "" + t.toString())
                textView.text = t.toString()
            }
            after {
                Log.d("after", "Test")
            }
        }
    }
}
