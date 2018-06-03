package com.kroegerama.kaiteki.retrofit.retry

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class Retry(val value: Int = 3)