package com.kroegerama.kaiteki.retrofit.app

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import com.kroegerama.kaiteki.retrofit.CacheCallAdapterFactory
import com.kroegerama.kaiteki.retrofit.DebugInterceptor
import com.kroegerama.kaiteki.retrofit.DefaultCacheHandler
import com.kroegerama.kaiteki.retrofit.RetryCallAdapterFactory
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
            client.addNetworkInterceptor(DebugInterceptor)
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

        textView.setOnClickListener { loadData() }
        loadData()
    }

    private fun loadData() {
        api.getPost(1).enqueue(object : Callback<Post> {
            override fun onResponse(call: Call<Post>, response: Response<Post>) {
                Log.d("onResponse", response.body().toString())
                textView.text = response.body().toString()
            }

            override fun onFailure(call: Call<Post>, t: Throwable) {
                Log.d("onFailure", "" + t.toString())
                textView.text = t.toString()
            }
        })
    }
}
