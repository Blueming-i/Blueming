package com.example.guiwulite.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class AssetRepository(context: Context) {
    private val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    fun loadAssets(): MutableList<AssetItem> {
        val json = prefs.getString(KEY_ASSETS, null)
        if (json.isNullOrBlank()) {
            return mutableListOf()
        }
        val type = object : TypeToken<MutableList<AssetItem>>() {}.type
        return gson.fromJson(json, type) ?: mutableListOf()
    }

    fun saveAssets(items: List<AssetItem>) {
        prefs.edit().putString(KEY_ASSETS, gson.toJson(items)).apply()
    }

    fun loadWishlist(): MutableList<WishItem> {
        val json = prefs.getString(KEY_WISHLIST, null)
        if (json.isNullOrBlank()) {
            return mutableListOf()
        }
        val type = object : TypeToken<MutableList<WishItem>>() {}.type
        return gson.fromJson(json, type) ?: mutableListOf()
    }

    fun saveWishlist(items: List<WishItem>) {
        prefs.edit().putString(KEY_WISHLIST, gson.toJson(items)).apply()
    }

    fun loadCategories(): MutableList<String> {
        val json = prefs.getString(KEY_CATEGORIES, null)
        if (json.isNullOrBlank()) {
            return mutableListOf(ALL_CATEGORIES, UNCATEGORIZED)
        }
        val type = object : TypeToken<MutableList<String>>() {}.type
        return gson.fromJson(json, type) ?: mutableListOf(ALL_CATEGORIES, UNCATEGORIZED)
    }

    fun saveCategories(items: List<String>) {
        prefs.edit().putString(KEY_CATEGORIES, gson.toJson(items)).apply()
    }

    companion object {
        private const val PREF_NAME = "guiwu_asset_prefs"
        private const val KEY_ASSETS = "assets"
        private const val KEY_WISHLIST = "wishlist"
        private const val KEY_CATEGORIES = "categories"
        private const val ALL_CATEGORIES = "\u5168\u90E8\u5206\u7C7B"
        private const val UNCATEGORIZED = "\u672A\u5206\u7C7B"
    }
}
