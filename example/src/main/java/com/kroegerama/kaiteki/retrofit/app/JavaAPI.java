package com.kroegerama.kaiteki.retrofit.app;

import com.kroegerama.kaiteki.retrofit.Retry;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface JavaAPI {

    @Retry(5)
    @GET("posts/{id}")
    Call<Post> getPost(@Path("id") int id);
}
