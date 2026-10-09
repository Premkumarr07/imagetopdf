package com.example.imagetopdf.core.logging

import android.util.Log
import com.example.imagetopdf.BuildConfig

object AppLogger {
    private const val TAG = "ImageToPDF"

    fun e(throwable: Throwable, message: String? = null) {
        if (BuildConfig.DEBUG) {
            Log.e(TAG, message ?: throwable.message ?: "error", throwable)
        }
    }

    fun w(message: String) {
        if (BuildConfig.DEBUG) {
            Log.w(TAG, message)
        }
    }
}
