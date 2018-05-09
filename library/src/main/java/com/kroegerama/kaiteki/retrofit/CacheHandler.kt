package com.kroegerama.kaiteki.retrofit

import android.content.Context
import android.util.Log
import android.util.LruCache
import com.jakewharton.disklrucache.DiskLruCache
import okhttp3.Request
import retrofit2.Response
import java.io.File
import java.io.IOException
import java.net.URL
import java.nio.charset.Charset


interface CacheHandler {
    fun <T> putCache(response: Response<T>, rawResponse: ByteArray)
    fun getCache(request: Request): CacheItem?
}

class CacheItem(val timestamp: Long, val data: ByteArray)

class DefaultCacheHandler(
        private val cacheFile: File,
        private val cacheSize: Long = DEFAULT_DISK_SIZE,
        private val memCacheEntries: Int = DEFAULT_MEM_CACHE_ENTRIES) : CacheHandler {

    constructor(context: Context, cacheSize: Long = DEFAULT_DISK_SIZE, memCacheEntries: Int = DEFAULT_MEM_CACHE_ENTRIES) : this(
            File(context.cacheDir, "retrofit.cache"), cacheSize, memCacheEntries)

    private val diskCache: DiskLruCache by lazy {
        DiskLruCache.open(cacheFile, 1, 1, cacheSize)
    }
    private val memoryCache: LruCache<String, CacheItem> by lazy {
        LruCache<String, CacheItem>(memCacheEntries)
    }

    override fun <T> putCache(response: Response<T>, rawResponse: ByteArray) {
        val cacheKey = generateKey(response.raw().request().url().url())
        memoryCache.put(cacheKey, CacheItem(System.currentTimeMillis(), rawResponse))

        try {
            val editor = diskCache.edit(cacheKey)
            val data = String(rawResponse, Charset.defaultCharset())
            editor.set(0, "${System.currentTimeMillis()}#$data")
            editor.commit()
        } catch (e: IOException) {
            Log.e(TAG, "", e)
        }
    }

    override fun getCache(request: Request): CacheItem? {
        val cacheKey = generateKey(request.url().url())
        memoryCache.get(cacheKey)?.let {
            Log.d(TAG, "Memory hit")
            return it
        }

        return try {
            diskCache.get(cacheKey)?.let {
                Log.d(TAG, "Disk hit")
                val data = it.getString(0)
                val idx = data.indexOf('#')
                if (idx < 1) {
                    return null
                }
                return CacheItem(data.substring(0 until idx).toLong(), data.substring(idx + 1).toByteArray())
            }
        } catch (e: IOException) {
            null
        }
    }

    private fun generateKey(url: URL): String {
        return url.toString().toSha1Hash()
    }

    companion object {
        const val DEFAULT_DISK_SIZE = (1024 * 1024).toLong()
        const val DEFAULT_MEM_CACHE_ENTRIES = 25

        private const val TAG = "DefaultCacheHandler"
    }
}