package com.b.a2.di

import android.util.Log
import com.b.a2.BuildConfig
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

/**
 * 智能 HTTP 日志拦截器
 * 自动检测 Streaming 请求（如下载文件），对于 Streaming 请求只记录 HEADERS，避免 OOM
 * 对于普通请求，记录请求和响应头（响应体由调用方处理，避免消费流）
 */
class SmartHttpLoggingInterceptor : Interceptor {
    
    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        
        // 记录请求
        logRequest(request)
        
        // 执行请求，获取响应
        val response = chain.proceed(request)
        
        // 检查是否是 Streaming 请求（下载文件）
        val isStreamingRequest = isStreamingRequest(request, response)
        
        // 根据请求类型选择不同的日志记录方式
        if (isStreamingRequest) {
            // Streaming 请求：只记录 HEADERS，不记录响应体，避免 OOM
            logResponseHeaders(response, isStreaming = true)
        } else {
            // 普通请求：记录响应头，响应体由调用方处理（避免消费流）
            logResponseHeaders(response, isStreaming = false)
        }
        
        return response
    }
    
    /**
     * 判断是否是 Streaming 请求（下载文件）
     * 通过检查 URL、响应头的 Content-Type 和大小来判断
     */
    private fun isStreamingRequest(request: okhttp3.Request, response: Response): Boolean {
        // 1. 检查 URL 是否包含文件扩展名
        val url = request.url.toString().lowercase()
        val fileExtensions = listOf(".apk", ".zip", ".rar", ".pdf", ".mp4", ".mp3", ".jpg", ".png", ".gif")
        if (fileExtensions.any { url.contains(it) }) {
            return true
        }
        
        // 2. 检查响应体的 Content-Type
        val contentType = response.header("Content-Type")?.lowercase() ?: ""
        val streamingContentTypes = listOf(
            "application/vnd.android.package-archive", // APK
            "application/zip",
            "application/octet-stream",
            "application/x-zip-compressed",
            "video/",
            "audio/",
            "image/"
        )
        if (streamingContentTypes.any { contentType.contains(it) }) {
            return true
        }
        
        // 3. 检查响应体大小，如果超过 1MB 认为是下载请求
        val contentLength = response.header("Content-Length")?.toLongOrNull()
        if (contentLength != null && contentLength > 1024 * 1024) { // 1MB
            return true
        }
        
        return false
    }
    
    private fun logRequest(request: okhttp3.Request) {
        if (BuildConfig.DEBUG) {
            Log.d("OkHttp", "--> ${request.method} ${request.url}")
            request.headers.forEach { header ->
                Log.d("OkHttp", "${header.first}: ${header.second}")
            }
            if (request.body != null) {
                Log.d("OkHttp", "--> END ${request.method} (request body)")
            } else {
                Log.d("OkHttp", "--> END ${request.method}")
            }
        }
    }
    
    private fun logResponseHeaders(response: Response, isStreaming: Boolean = false) {
        if (BuildConfig.DEBUG) {
            Log.d("OkHttp", "<-- ${response.code} ${response.message} ${response.request.url}")
            response.headers.forEach { header ->
                Log.d("OkHttp", "${header.first}: ${header.second}")
            }
            if (isStreaming) {
                Log.d("OkHttp", "<-- END HTTP (streaming response body omitted)")
            } else {
                Log.d("OkHttp", "<-- END HTTP")
            }
        }
    }
}

