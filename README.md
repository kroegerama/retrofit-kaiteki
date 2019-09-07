<img height="150" src="logo/logo.png">

[![Release](https://jitpack.io/v/com.kroegerama/retrofit-kaiteki.svg)](https://jitpack.io/#com.kroegerama/retrofit-kaiteki)
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
  implementation 'com.kroegerama:retrofit-kaiteki:2.0.0'
}
```

## Current components

#### Retrofit LiveData extension function
Convert any retrofit call to LiveData (Android Architecture Components).

#### Retrofit DSL
Invoke your retrofit calls using a simple domain specific language.

#### Debug Interceptor
Allows you to see outgoing requests and the incoming responses in your **logcat**

#### Retry Call Annotation
Annotate your retrofit calls and let them automatically be retried.
Allows to set the retry count per call.

#### Cache Call Annotation
Annotate your retrofit calls and let them automatically be cached.
Allows to set a debounce time and a maximum age per call.

- **debounce**: Load from cache and avoid a network call, if the cached value is younger than specified amount of milliseconds. Set to 0 to disable.
- **maxAge**: Load from cache and enqueue a network call, if the cached value is younger than the specified amount of milliseconds. **onSuccess()** may be called **twice**. Once with cached data and once with the updated data from network. Set to 0 to disable.

## Some examples

See [**Wiki**](https://github.com/kroegerama/retrofit-kaiteki/wiki) for more documentation and examples.

### DSL

```kotlin
myApi.myCall(param).enqueue {
    onSuccess {
        Log.d("onSuccess", body().toString())
        textView.text = body().toString()
    }
    onNoSuccess {
        Log.d("onNoSuccess", body().toString())
    }
    onFailure { t ->
        Log.d("onFailure", "" + t.toString())
        textView.text = t.toString()
    }
}
```

### LiveData

```kotlin
val listing = myApi.myCall(param).createListing()

val networkStateLiveData = listing.networkState
val resultLiveData = listing.result

listing.retry()
```

### Retry and Cache Annotations

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
