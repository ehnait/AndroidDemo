package com.b.a2.repository

import android.content.Context
import android.content.SharedPreferences
import android.os.Environment
import com.b.a2.api.ApiService
import com.b.a2.data.AppInfo
import com.b.a2.utils.fromJson
import com.b.a2.utils.toJson
import dagger.hilt.android.qualifiers.ApplicationContext
import com.b.a2.di.BackgroundScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import java.io.File
import javax.inject.Inject

class AppRepository @Inject constructor(
    private val apiService: ApiService,
    private val sharedPref: SharedPreferences,
    @ApplicationContext private val appContext: Context,
    @BackgroundScope private val backgroundScope: CoroutineScope,
) : BaseRepository() {
    companion object {
        private const val TAG = "AppRepository"
        private const val KEY_APP_INFO = "app_info"
    }

    /**
     * 获取应用信息
     * 优先从本地缓存读取，如果本地有数据则直接返回，网络请求在后台更新缓存
     * 如果本地没有数据，则等待网络请求结果
     * 
     * @return Flow<Result<AppInfo>> 应用信息流
     */
    fun getAppInfo(): Flow<Result<AppInfo>> = flow {
        // 1. 优先从SharedPreferences读取
        val localAppInfo = sharedPref.getString(KEY_APP_INFO, null)?.fromJson<AppInfo>()
        
        if (localAppInfo != null) {
            // 本地有数据，直接返回本地数据（只 emit 一次）
            emit(Result.success(localAppInfo))
            
            // 在后台请求网络更新缓存（不阻塞，不 emit）
            // 使用独立的作用域，不阻塞 Flow
            backgroundScope.launch {
                try {
                    val response = apiService.getAppInfo()
                    if (response.result == 0 && response.data != null) {
                        // 网络成功，更新本地缓存（下次进入应用时使用）
                        sharedPref.edit().putString(KEY_APP_INFO, response.data.toJson()).apply()
                    }
                } catch (e: Exception) {
                    // 网络失败，静默失败，不影响当前使用的本地数据
                    // 不记录日志，避免干扰
                }
            }
        } else {
            // 本地没有数据，等待网络请求结果
            emitAll(
                safeApiCall { apiService.getAppInfo() }
                    .map { result ->
                        result.fold(
                            onSuccess = { appInfo ->
                                // 网络成功，保存到SharedPreferences
                                sharedPref.edit().putString(KEY_APP_INFO, appInfo.toJson()).apply()
                                Result.success(appInfo)
                            },
                            onFailure = { error ->
                                // 网络失败，返回错误
                                Result.failure(error)
                            }
                        )
                    }
            )
        }
    }


    fun downloadApk(url: String): Flow<DownloadResult> {
        //下载APK时，保存到你App的私有目录（如context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)），这样不需要任何存储权限。
        val dir = appContext.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        val fileName = "xxx.apk"
        val savePath = File(dir, fileName).absolutePath
        return safeDownload(
            downloadCall = { apiService.downloadFile(url) }, // 你的Retrofit下载接口
            savePath = savePath,
        )
    }
}


