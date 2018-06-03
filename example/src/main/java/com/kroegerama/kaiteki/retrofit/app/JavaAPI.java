package com.kroegerama.kaiteki.retrofit.app;

import com.kroegerama.kaiteki.retrofit.cache.Cache;
import com.kroegerama.kaiteki.retrofit.retry.Retry;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface JavaAPI {

    @Retry(5)
    @Cache(debounce = 1000, maxAge = 5000)
    @GET("posts/{id}")
    Call<Post> getPost(@Path("id") int id);


    @Cache(debounce = 1000, maxAge = 5000)
    @GET("posts/{id}")
    Call<Post> getPostCachedNoRetry(@Path("id") int id);

    @Cache(debounce = 1000, maxAge = 5000)
    @GET("posts/{id}")
    Call<Post> getPostNoCacheNoRetry(@Path("id") int id);
}
