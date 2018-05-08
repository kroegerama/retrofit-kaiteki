package com.kroegerama.kaiteki.retrofit.app

import com.kroegerama.kaiteki.retrofit.Retry
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path

interface KotlinAPI {

    @Retry(5)
    @GET("posts/{id}")
    fun getPost(@Path("id") id: Int): Call<Post>

}