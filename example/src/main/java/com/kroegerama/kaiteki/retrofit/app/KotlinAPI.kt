package com.kroegerama.kaiteki.retrofit.app

import com.kroegerama.kaiteki.retrofit.cache.Cache
import com.kroegerama.kaiteki.retrofit.retry.Retry
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path

interface KotlinAPI {

    @Retry(3)
    @Cache(5 * 1000, 10 * 1000)
    @GET("posts/{id}")
    fun getPost(@Path("id") id: Int): Call<Post>

    @Cache(5 * 1000, 10 * 1000)
    @GET("posts/{id}")
    fun getPostCachedNoRetry(@Path("id") id: Int): Call<Post>

    @GET("posts/{id}")
    fun getPostNoCacheNoRetry(@Path("id") id: Int): Call<Post>

    @GET("notExisting")
    fun getNotExisting(): Call<String>
}