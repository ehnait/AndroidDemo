package com.b.a2.viewmodel

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.b.a2.data.AppInfo
import com.b.a2.repository.AppRepository
import com.b.a2.repository.DownloadResult
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

sealed class AppInfoState {
    data object Loading : AppInfoState()
    data class Success(val appInfo: AppInfo) : AppInfoState()
    data class Error(val message: String) : AppInfoState()
}

@HiltViewModel
open class AppViewModel @Inject constructor(
    private val repository: AppRepository,
    @ApplicationContext private val appContext: Context,
) : ViewModel() {

    // 使用 stateIn 将 Flow 转换为 StateFlow，自动处理收集和状态管理
    // SharingStarted.WhileSubscribed(5000) 表示当有订阅者时开始收集，最后一个订阅者取消后5秒停止
    val appInfoState: StateFlow<AppInfoState> = repository.getAppInfo()
        .map { result ->
            result.fold(
                onSuccess = { appInfo ->
                    AppInfoState.Success(appInfo)
                },
                onFailure = { error ->
                    AppInfoState.Error(error.message ?: "Unknown error occurred")
                }
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AppInfoState.Loading
        )

    /**
     * 刷新应用信息
     * 如果需要手动刷新，可以调用此方法
     */
    fun refreshAppInfo() {
        // stateIn 会自动处理，如果需要强制刷新，可以重新订阅
        // 或者可以在 Repository 中添加刷新逻辑
    }


    fun downloadApk(
        url: String,
        onProgress: (DownloadResult.Progress) -> Unit,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            repository.downloadApk(url)
                .collect { result ->
                    when (result) {
                        is DownloadResult.Progress -> onProgress(result)
                        is DownloadResult.Success -> {
                            onSuccess(result.file.absolutePath)
                            launch(Dispatchers.IO) {
                                delay(1000) // 延迟1秒
                                installApk(appContext, result.file.absolutePath)
                            }
                        }

                        is DownloadResult.Error -> {
                            onError(result.exception.message ?: "Download failed")
                        }
                    }
                }
        }
    }

    /**
     * 安装APK文件
     * 该方法处理了不同Android版本的兼容性问题，确保在各种设备上都能正常安装
     * 
     * @param context 上下文对象
     * @param filePath APK文件的完整路径
     */
    private fun installApk(context: Context, filePath: String) {
        try {
            val file = File(filePath)
            
            // 1. 检查文件是否存在
            if (!file.exists()) {
                Log.e("AppViewModel", "APK file does not exist: $filePath")
                return
            }
            
            // 2. 检查文件是否可读
            if (!file.canRead()) {
                Log.e("AppViewModel", "APK file is not readable: $filePath")
                // 尝试设置读取权限
                file.setReadable(true, false)
            }
            
            // 3. 获取FileProvider URI（authority 必须与 AndroidManifest 中 provider 的 android:authorities 一致）
            val authority = "${context.packageName}.provider"
            val apkUri = try {
                FileProvider.getUriForFile(context, authority, file)
            } catch (e: IllegalArgumentException) {
                Log.e("AppViewModel", "FileProvider error: ${e.message}")
                // 如果FileProvider失败，尝试使用file:// URI（仅用于Android 7.0以下）
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
                    Uri.fromFile(file)
                } else {
                    Log.e("AppViewModel", "Cannot use file:// URI on Android N+")
                    return
                }
            }
            
            // 4. 创建安装Intent
            val intent = Intent(Intent.ACTION_VIEW).apply {
                // 设置数据和类型
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                
                // 添加必要的Flags
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            
            // 5. 检查是否有应用可以处理这个Intent
            val packageManager = context.packageManager
            val resolveInfo = packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
            if (resolveInfo.isEmpty()) {
                Log.e("AppViewModel", "No app can handle the install intent")
                return
            }
            
            // 6. 启动安装Activity
            context.startActivity(intent)
            Log.d("AppViewModel", "Install intent started successfully for: $filePath")
            
        } catch (e: SecurityException) {
            Log.e("AppViewModel", "SecurityException during APK installation: ${e.message}", e)
        } catch (e: IllegalStateException) {
            Log.e("AppViewModel", "IllegalStateException during APK installation: ${e.message}", e)
        } catch (e: Exception) {
            Log.e("AppViewModel", "Unexpected error during APK installation: ${e.message}", e)
        }
    }

    fun openUrlInBrowser(context: Context, url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse(url)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) // 重点：加上这个flag
            context.startActivity(intent)
        } catch (e: Exception) {
            // 处理没有浏览器的情况
            e.printStackTrace()
        }
    }
}

