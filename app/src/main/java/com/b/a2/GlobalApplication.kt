package com.b.a2

import android.app.Application
import android.os.Build.VERSION.SDK_INT
import coil.Coil
import coil.ImageLoader
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.disk.DiskCache
import coil.memory.MemoryCache
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class GlobalApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        setupCoil()
    }

    private fun setupCoil() {
        val imageLoader = ImageLoader.Builder(this)
            .components {
                // GIF 支持
                if (SDK_INT >= 28) {
                    add(ImageDecoderDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
            }
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25) // 使用 25% 可用内存
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(this.cacheDir.resolve("image_cache"))
                    .maxSizePercent(0.02) // 使用 2% 磁盘空间
                    .build()
            }
            .crossfade(true) // 启用淡入淡出效果
            .build()

        // 设置为默认 ImageLoader
        Coil.setImageLoader(imageLoader)
    }
}