[![Release](https://jitpack.io/v/kroegerama/retrofit-kaiteki.svg)](https://jitpack.io/#kroegerama/retrofit-kaiteki)
[![Build Status](https://travis-ci.org/kroegerama/retrofit-kaiteki.svg?branch=master)](https://travis-ci.org/kroegerama/retrofit-kaiteki)

# Retrofit Kaiteki
A collection of Retrofit convenience classes

## Importing the library

### 1. Add jitpack to your **toplevel** gradle file:

```gradle
allprojects {
  repositories {
    ...
    
    /* ADD THE FOLLOWING LINE */
    maven { url 'https://jitpack.io' }
  }
}
```

### 2. Add the library to your **local** gradle file:

```gradle
dependencies {
  implementation 'com.kroegerama:retrofit-kaiteki:1.0.0'
}
```

## Current components

### Debug Interceptor
Allows you to see outgoing requests and the incoming responses in your **logcat**

### Retry annotation
Annotate your retrofit calls and let them automatically be retried.

## Usage

### Kotlin

```kotlin
val client = OkHttpClient.Builder()

if (BuildConfig.DEBUG) {
  client.addNetworkInterceptor(DebugInterceptor)
}

val retrofit = Retrofit.Builder()
                .client(client.build())
                .addCallAdapterFactory(RetryCallAdapterFactory)
                .addConverterFactory(...)
                .baseUrl(...)
                .build()

val api = retrofit.create(MyAPI::class.java)
```

### Java

```java
OkHttpClient.Builder client = new OkHttpClient.Builder();
if (BuildConfig.DEBUG) {
  client.addNetworkInterceptor(DebugInterceptor.INSTANCE);
}

Retrofit retrofit = new Retrofit.Builder()
                        .client(client.build())
                        .addCallAdapterFactory(RetryCallAdapterFactory.INSTANCE)
                        .addConverterFactory(...)
                        .baseUrl(...)
                        .build();

JavaAPI javaAPI = retrofit.create(MyAPI.class);
```

## Annotate your API

### Kotlin

```kotlin
interface MyAPI {
    @Retry(5)
    @GET("...")
    fun getPost(@Path("id") id: Int): Call<Post>
}
```

### Java

```java
public interface MyAPI {
    @Retry(5)
    @GET("...")
    Call<Post> getPost(@Path("id") int id);
}
```
