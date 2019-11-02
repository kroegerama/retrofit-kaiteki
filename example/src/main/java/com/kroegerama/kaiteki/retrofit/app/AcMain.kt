package com.kroegerama.kaiteki.retrofit.app

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.kroegerama.kaiteki.retrofit.DebugInterceptor
import com.kroegerama.kaiteki.retrofit.retrofitCall
import com.kroegerama.kaiteki.retrofit.retrofitCallAsync
import com.kroegerama.kaiteki.retrofit.retry.RetryCallAdapterFactory
import kotlinx.android.synthetic.main.ac_main.*
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
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
            .addConverterFactory(GsonConverterFactory.create())
            .addConverterFactory(ScalarsConverterFactory.create())
            .baseUrl("https://jsonplaceholder.typicode.com")
            .build()

        retrofit.create(KotlinAPI::class.java)
    }

    private val vm by lazy { ViewModelProviders.of(this).get(TestViewModel::class.java) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.ac_main)

        textView.setOnClickListener { vm.update() }
        textView.setOnLongClickListener { vm.setPostId(1); vm.setPostId(2); true }

        vm.post.observe(this, Observer { result ->
            Log.d("LiveData", "Emitted: $result")
        })
        vm.state.observe(this, Observer { state ->
            Log.d("LiveData", "State: $state")
        })
        vm.running.observe(this, Observer { running ->
            textView.setBackgroundColor(
                if (running) {
                    Color.GREEN
                } else {
                    Color.WHITE
                }
            )
        })
    }

    private fun cancelTest() {
        CoroutineScope(Dispatchers.Main).launch {
            val job = Job()
            launch(job) {
                Log.d("CancelTest", retrofitCall { api.getPostSuspending(1).also { delay(500) } }.toString())
            }
            delay(100)
            job.cancel()
        }
    }

    private fun suspendTest() {
        CoroutineScope(Dispatchers.Main).launch {
            val items = listOf(
                async(Dispatchers.IO) {
                    delay(1000)
                    retrofitCall {
                        api.getPostSuspending(1).also {
                            Log.d(TAG, "Suspend result: After1 $it")
                        }
                    }
                },
                retrofitCallAsync(Dispatchers.IO) {
                    delay(2000)
                    api.getPostSuspending(2).also {
                        Log.d(TAG, "Suspend result: After2 $it")
                    }
                }
            )
            Log.d(TAG, "Suspend result incoming...")
            val result = items.awaitAll()

            Log.d(TAG, "Suspend result: $result")
        }
    }

    companion object {
        private const val TAG = "AcMain"
    }
}
