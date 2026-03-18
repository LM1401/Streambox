package com.example.streambox

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory

class StreamBoxApplication : Application(), ImageLoaderFactory {
    override fun onCreate() {
        super.onCreate()
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .okHttpClient {
                NetworkUtils.getUnsafeOkHttpClient()
            }
            .crossfade(true)
            .build()
    }
}
