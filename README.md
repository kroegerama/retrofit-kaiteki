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
  implementation 'com.kroegerama:retrofit-kaiteki:1.1.0'
}
```

## Current components

### Debug Interceptor
Allows you to see outgoing requests and the incoming responses in your **logcat**

### Retry Call Annotation
Annotate your retrofit calls and let them automatically be retried.
Allows to set the retry count per call.

### Cache Call Annotation
Annotate your retrofit calls and let them automatically be cached.
Allows to set a debounce time and a maximum age per call.

- **debounce**: Load from cache and avoid a network call, if the cached value is younger than specified amount of milliseconds. Set to 0 to disable.
- **maxAge**: Load from cache and enqueue a network call, if the cached value is younger than the specified amount of milliseconds. **onSuccess()** may be called **twice**. Once with cached data and once with the updated data from network. Set to 0 to disable.

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
                .addCallAdapterFactory(CacheCallAdapterFactory(DefaultCacheHandler(this)))
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
                        .addCallAdapterFactory(new CacheCallAdapterFactory(new DefaultCacheHandler(context, DefaultCacheHandler.DEFAULT_DISK_SIZE, DefaultCacheHandler.DEFAULT_MEM_CACHE_ENTRIES)))
                        .addConverterFactory(...)
                        .baseUrl(...)
                        .build();

JavaAPI javaAPI = retrofit.create(MyAPI.class);
```

## Annotate your API

### Kotlin

```kotlin
interface MyAPI {
  @Retry(3)
  @Cache(5 * 1000, 10 * 1000)
  @GET("...")
  fun getPost(@Path("id") id: Int): Call<Post>

  @Cache(5 * 1000, 10 * 1000)
  @GET("...")
  fun getPostCachedNoRetry(@Path("id") id: Int): Call<Post>

  @GET("..."")
  fun getPostNoCacheNoRetry(@Path("id") id: Int): Call<Post>
}
```

### Java

```java
public interface MyAPI {
    @Retry(5)
    @Cache(debounce = 1000, maxAge = 5000)
    @GET("...")
    Call<Post> getPost(@Path("id") int id);

    @Cache(debounce = 1000, maxAge = 5000)
    @GET("...")
    Call<Post> getPostCachedNoRetry(@Path("id") int id);

    @GET("...")
    Call<Post> getPostNoCacheNoRetry(@Path("id") int id);
}
```
