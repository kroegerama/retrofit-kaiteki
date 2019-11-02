package com.kroegerama.kaiteki.retrofit.cache

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class Cache(
    /** Load from cache and avoid network call,
     * if cached value is younger than specified amount of milliseconds
     * <br />
     * Set to 0 to disable**/
    val debounce: Long = 2 * 1000,

    /** Load from cache and enqueue network call,
     * if cached value is younger than the specified amount of milliseconds
     * <b>onSuccess()</b> may be called <b>twice</b> once with cached data
     * and once with the updated data from network
     * <br />
     * Set to 0 to disable**/
    val maxAge: Long = 5 * 60 * 1000
)