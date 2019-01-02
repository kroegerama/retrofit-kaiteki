package com.kroegerama.kaiteki.retrofit.listing

import android.content.SharedPreferences
import androidx.lifecycle.MutableLiveData
import com.google.gson.Gson
import retrofit2.Call
import java.lang.reflect.Type

class CachedListing<T>(
        private val converter: ItemConverter<T>,
        private val saveCallback: SaveCallback<T>
) {
    fun <E> Call<E>.createCachedListing(type: Type, fetchNow: Boolean = true): Listing<E> {
        val key = request().url().hashCode()
        val delegate = createListing(false) {
            it ?: return@createListing
            converter.serialize(it).let { serialized ->
                saveCallback.save(key, serialized)
            }
        }
        saveCallback.restore(key)?.let {
            (delegate.result as MutableLiveData<*>).value = converter.parse(it, type)
        }
        if (fetchNow) {
            delegate.retry()
        }
        return delegate
    }
}

interface ItemConverter<T> {
    fun serialize(item: Any): T
    fun <L> parse(serialized: T, type: Type): L?
}

interface SaveCallback<T> {
    fun save(key: Int, value: T)
    fun restore(key: Int): T?
}

class GsonItemConverter : ItemConverter<String> {
    private val gson = Gson()

    override fun serialize(item: Any): String {
        return gson.toJson(item)
    }

    override fun <E> parse(serialized: String, type: Type): E? {
        return try {
            gson.fromJson(serialized, type)
        } catch (e: Exception) {
            null
        }
    }
}

class SharedPreferencesSaveCallback(
        private val preferences: SharedPreferences
) : SaveCallback<String> {

    private fun convertKey(key: Int): String {
        return key.toString()
    }

    override fun save(key: Int, value: String) {
        preferences.edit().putString(convertKey(key), value).apply()
    }

    override fun restore(key: Int): String? {
        return preferences.getString(convertKey(key), null)
    }
}