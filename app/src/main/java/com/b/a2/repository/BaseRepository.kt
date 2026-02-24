package com.b.a2.repository

import android.util.Log
import com.b.a2.data.BaseResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.ResponseBody
import retrofit2.HttpException
import retrofit2.Response
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

data class BusinessException(
    val code: Int,
    override val message: String?
) : Exception(message)

sealed class DownloadResult {
    data class Progress(val bytesDownloaded: Long, val total: Long, val percent: Float) : DownloadResult()
    data class Success(val file: File) : DownloadResult()
    data class Error(val exception: Throwable) : DownloadResult()
}

abstract class BaseRepository {

    /**
     * 安全的 API 调用方法
     * 自动处理业务异常和网络异常，统一转换为 Result
     * 
     * @param apiCall API 调用的挂起函数
     * @return Flow<Result<T>> 包含成功或失败结果的流
     */
    protected fun <T> safeApiCall(apiCall: suspend () -> BaseResponse<T>): Flow<Result<T>> = flow {
        try {
            val response = apiCall()
            when {
                response.result == 0 -> emit(Result.success(response.data))
                else -> throw BusinessException(response.result, response.message)
            }
        } catch (t: Throwable) {
            emit(Result.failure(processException(t)))
        }
    }.flowOn(Dispatchers.IO)

    /**
     * 安全下载文件方法
     * 使用流式下载避免OOM，支持进度回调
     * 
     * @param downloadCall 下载请求的挂起函数
     * @param savePath 文件保存路径
     * @return Flow<DownloadResult> 下载结果流，包含进度、成功或错误信息
     */
    protected fun safeDownload(
        downloadCall: suspend () -> Response<ResponseBody>,
        savePath: String,
    ): Flow<DownloadResult> = flow {
        var response: Response<ResponseBody>? = null
        var targetFile: File? = null
        try {
            // 1. 执行下载请求并验证响应
            response = downloadCall().also { resp ->
                if (!resp.isSuccessful) {
                    throw IOException("HTTP ${resp.code()}: ${resp.message()}")
                }
                if (resp.body() == null) {
                    throw IOException("Empty response body")
                }
            }

            // 2. 准备目标文件
            targetFile = File(savePath).apply {
                parentFile?.mkdirs()
                if (exists()) {
                    delete() // 清理旧文件
                }
            }

            // 3. 获取响应体并读取内容长度
            val responseBody = response.body()!!
            val totalBytes = responseBody.contentLength()
            
            // 4. 发送初始进度（0%）
            emit(DownloadResult.Progress(0, totalBytes, 0f))

            // 5. 流式写入，避免OOM
            // 使用较小的缓冲区（4KB），在内存紧张时更安全
            // 移除 BufferedInputStream，直接使用 InputStream，减少内存占用
            responseBody.byteStream().use { inputStream ->
                FileOutputStream(targetFile).use { outputStream ->
                    val bufferSize = 4096 // 4KB 缓冲区，在内存紧张时更安全
                    val buffer = ByteArray(bufferSize)
                    var bytesCopied = 0L
                    var lastProgressPercent = -1f // 用于控制进度更新频率
                    var lastBytesUpdate = 0L // 用于未知总大小时的更新控制
                    
                    while (true) {
                        val readLength = inputStream.read(buffer)
                        if (readLength == -1) break
                        
                        outputStream.write(buffer, 0, readLength)
                        bytesCopied += readLength
                        
                        // 计算进度百分比
                        val percent = when {
                            totalBytes > 0 -> bytesCopied.toFloat() / totalBytes * 100
                            else -> -1f // 未知总大小
                        }
                        
                        // 进度更新策略
                        val shouldUpdate = when {
                            // 已知总大小：每1%更新一次，或下载完成时更新
                            percent >= 0 && (percent - lastProgressPercent >= 1f || bytesCopied == totalBytes) -> {
                                lastProgressPercent = percent
                                true
                            }
                            // 未知总大小：每1MB更新一次
                            totalBytes < 0 && (bytesCopied - lastBytesUpdate >= 1024 * 1024 || readLength < bufferSize) -> {
                                lastBytesUpdate = bytesCopied
                                true
                            }
                            else -> false
                        }
                        
                        if (shouldUpdate) {
                            emit(DownloadResult.Progress(bytesCopied, totalBytes, percent))
                            if (percent >= 0) {
                                Log.d(
                                    "BaseRepository",
                                    "Download progress: ${String.format("%.1f", percent)}% ($bytesCopied/$totalBytes bytes)"
                                )
                            } else {
                                Log.d(
                                    "BaseRepository",
                                    "Download progress: ${bytesCopied / (1024 * 1024)}MB (total size unknown)"
                                )
                            }
                        }
                    }
                    
                    // 确保数据写入磁盘
                    outputStream.flush()
                }
            }
            
            // 6. 验证文件是否成功创建
            if (!targetFile.exists() || targetFile.length() == 0L) {
                throw IOException("Downloaded file is empty or not created")
            }
            
            emit(DownloadResult.Success(targetFile))
            Log.d("BaseRepository", "Download completed: ${targetFile.absolutePath}")
            
        } catch (e: Throwable) {
            // 7. 发生错误时清理不完整的文件
            try {
                targetFile?.takeIf { it.exists() }?.delete()
            } catch (deleteException: Exception) {
                Log.w("BaseRepository", "Failed to delete incomplete file: ${deleteException.message}")
            }
            emit(DownloadResult.Error(processException(e)))
            Log.e("BaseRepository", "Download failed: ${e.message}", e)
        }
    }.flowOn(Dispatchers.IO)
        .catch { e ->
            // Flow层面的异常处理
            emit(DownloadResult.Error(processException(e)))
            Log.e("BaseRepository", "Flow error in safeDownload: ${e.message}", e)
        }

    protected fun processException(t: Throwable): Exception = when (t) {
        is BusinessException -> Exception("Business Error [${t.code}]: ${t.message}", t)
        is HttpException -> Exception("Server Error: HTTP ${t.code()}", t)
        is IOException -> Exception("Network Error: ${t.message}", t)
        else -> Exception("Unknown Error: ${t.message ?: "Please check the log"}", t)
    }
}

