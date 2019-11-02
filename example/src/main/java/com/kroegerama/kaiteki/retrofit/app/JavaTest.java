package com.kroegerama.kaiteki.retrofit.app;

import android.content.Context;

import com.kroegerama.kaiteki.retrofit.DebugInterceptor;
import com.kroegerama.kaiteki.retrofit.retry.RetryCallAdapterFactory;

import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public class JavaTest {

    public void test(Context context) {
        OkHttpClient.Builder client = new OkHttpClient.Builder();
        if (BuildConfig.DEBUG) {
            client.addNetworkInterceptor(DebugInterceptor.INSTANCE);
        }

        Retrofit retrofit = new Retrofit.Builder()
                .client(client.build())
                .addCallAdapterFactory(RetryCallAdapterFactory.INSTANCE)
                .build();

        JavaAPI javaAPI = retrofit.create(JavaAPI.class);
        javaAPI.getPost(1).enqueue(new Callback<Post>() {
            @Override
            public void onResponse(Call<Post> call, Response<Post> response) {

            }

            @Override
            public void onFailure(Call<Post> call, Throwable t) {

            }
        });

        KotlinAPI kotlinAPI = retrofit.create(KotlinAPI.class);
        kotlinAPI.getPost(1).enqueue(new Callback<Post>() {
            @Override
            public void onResponse(Call<Post> call, Response<Post> response) {

            }

            @Override
            public void onFailure(Call<Post> call, Throwable t) {

            }
        });
    }
}
