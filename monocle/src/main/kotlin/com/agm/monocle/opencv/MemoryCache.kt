package com.agm.monocle.opencv

import android.graphics.Bitmap
import android.util.LruCache

object MemoryCache {
    private var memoryCache: LruCache<String, Bitmap>

    init {
        // Get max available VM memory, exceeding this amount will throw an
        // OutOfMemory exception. Stored in kilobytes as LruCache takes an
        // int in its constructor.
        val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()

        // Use 1/8th of the available memory for this memory cache.
        val cacheSize = maxMemory / 8

        memoryCache = object : LruCache<String, Bitmap>(cacheSize) {

            override fun sizeOf(key: String, bitmap: Bitmap): Int {
                // The cache size will be measured in kilobytes rather than
                // number of items.
                return bitmap.byteCount / 1024
            }
        }
    }

     fun getBitmapFromMemCache(imageKey: String) = memoryCache.get(imageKey)



     fun addBitmapToCache(imageKey: String, bitmap: Bitmap) {
        memoryCache.put(imageKey, bitmap)
    }
}