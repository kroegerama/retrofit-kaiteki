package com.kroegerama.kaiteki.retrofit.app;

import com.kroegerama.kaiteki.retrofit.DebugInterceptor;
import com.kroegerama.kaiteki.retrofit.RetryCallAdapterFactory;

import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public class JavaTest {

    public void test() {
        OkHttpClient client = new OkHttpClient.Builder()
                .addNetworkInterceptor(DebugInterceptor.INSTANCE)
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .client(client)
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
