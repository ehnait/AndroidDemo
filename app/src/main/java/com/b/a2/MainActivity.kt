package com.b.a2

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Space
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import coil.load
import com.b.a2.data.AppInfo
import com.b.a2.databinding.ActivityMainBinding
import com.b.a2.ui.dialog.UpdateDialog
import com.b.a2.ui.dialog.UpdateFailedDialog
import com.b.a2.ui.view.SafeWebView
import com.b.a2.utils.dp
import com.b.a2.utils.formatFileSize
import com.b.a2.viewmodel.AppInfoState
import com.b.a2.viewmodel.AppViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 主界面 Activity，负责：
 * - 启动时展示闪屏并拉取应用配置（版本、APK 下载地址等）
 * - 根据服务端版本决定弹更新对话框或直接进入 WebView
 * - 处理 OAuth 回调 Deep Link（abc://oauth2redirect）
 * - 处理 WebView 内文件选择器结果（onActivityResult）
 */
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val appViewModel: AppViewModel by viewModels()
    /** 更新确认弹窗，非空表示已展示过 */
    private var updateDialog: UpdateDialog? = null
    /** 更新失败提示弹窗 */
    private var updateFailedDialog: UpdateFailedDialog? = null
    /** 当前应用配置（版本、默认 URL、APK 地址等），用于判断是否需更新及加载 WebView */
    private var appInfo: AppInfo? = null
    /** 在 WebView 未就绪时收到的 OAuth 回调 Intent，待 WebView 显示后再执行 */
    private var pendingOAuthIntent: Intent? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initView()
        initObservers()
    }

    /** 单任务模式下收到新 Intent 时调用，用于处理 OAuth Deep Link */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)  // 更新当前 Intent

        // 处理 OAuth 回调 Deep Link
        if (intent.action == Intent.ACTION_VIEW) {
            val data = intent.data
            if (data?.toString()?.startsWith("abc://oauth2redirect") == true) {
                handleOAuthCallback(intent)
            }
        }
    }

    /** 接收文件选择器结果并交给 SafeWebView 的 FileProvider 回调 */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // 处理文件选择器结果（标准实现方式）
        if (requestCode == SafeWebView.FILE_CHOOSER_REQUEST_CODE) {
            val uri = data?.data
            val uris = if (resultCode == RESULT_OK && uri != null) arrayOf(uri) else null
            binding.contentContainer.webview.onFileChooseResult(uris)
        }
    }

    /**
     * 处理 OAuth 回调
     * 如果 WebView 已显示则立即处理，否则保存待后续处理
     */
    private fun handleOAuthCallback(intent: Intent) {
        val data = intent.data
        if (data == null) {
            Log.w("MainActivity", "OAuth callback intent has no data")
            return
        }

        // 如果 WebView 已显示，立即处理
        if (binding.contentContainer.root.visibility == View.VISIBLE) {
            val script = "window.exeIntentData('${data.scheme ?: ""}','${data.fragment ?: ""}')"
            binding.contentContainer.webview.post {
                binding.contentContainer.webview.evaluateJavascript(script, null)
            }
            Log.d("MainActivity", "OAuth callback handled: $data")
            pendingOAuthIntent = null
        } else {
            // WebView 还未显示，保存 Intent 待后续处理
            pendingOAuthIntent = intent
            Log.d("MainActivity", "WebView not ready, OAuth callback saved for later")
        }
    }

    /** 初始化闪屏 UI：按地区设置 Logo、版权文案、供应商图标 */
    private fun initView() {
        val area = BuildConfig.AREA
        binding.splashContainer.imgLogo.load(R.drawable.img_logo)
        //进度条百分比
        binding.splashContainer.progressPercentContainer.visibility = View.INVISIBLE
        binding.splashContainer.imgProgressIcon.load(R.drawable.dl_progress_icon)

        binding.splashContainer.tvCopyright.text =
            "Copyright © 2019-2025 ${getString(R.string.app_name)}. All rights reserved."

        if (area == "brazil") {
            addSuppliers(
                R.drawable.img_supplier_gcb,
                R.drawable.img_supplier_siq,
                R.drawable.img_supplier_tether,
                R.drawable.img_supplier_pix,
            )
        } else if (area == "india") {
            addSuppliers(
                R.drawable.img_supplier_gcb,
                R.drawable.img_supplier_siq,
                R.drawable.img_supplier_paytm,
                R.drawable.img_supplier_upi
            )
        } else if (area == "mexico") {
            addSuppliers(
                R.drawable.img_supplier_gcb,
                R.drawable.img_supplier_siq,
                R.drawable.img_supplier_tether,
                R.drawable.img_supplier_spei
            )
        }
    }

    /** 订阅应用配置状态：加载中/失败/成功，成功时比较版本决定弹更新或进 WebView */
    private fun initObservers() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                appViewModel.appInfoState.collect { state ->
                    when (state) {
                        is AppInfoState.Loading -> {
                            // 拉取配置中，界面可保持闪屏或加载态
                        }

                        is AppInfoState.Error -> {
                            Log.e("MainActivity", "Failed to load app info: ${state.message}")
                            if (appInfo == null) {
                                showWebView() // 无缓存时用默认 URL 进入 WebView
                            }
                        }

                        is AppInfoState.Success -> {
                            val newAppInfo = state.appInfo
                            if (appInfo != newAppInfo) {
                                appInfo = newAppInfo
                                if (newAppInfo.apkCode > BuildConfig.VERSION_CODE) {
                                    showUpdateDialog()
                                } else {
                                    showWebView()
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /** 隐藏闪屏、显示 WebView 并加载默认/配置的 URL；若有缓存的 OAuth 回调则执行 */
    private fun showWebView() {
        binding.contentContainer.root.visibility = View.VISIBLE
        binding.splashContainer.root.visibility = View.GONE
        binding.contentContainer.webview.loadUrl(
            appInfo?.resourceUrl ?: BuildConfig.DEFAULT_URL
        )

        // 如果有待处理的 OAuth 回调，现在处理
        pendingOAuthIntent?.let {
            val data = it.data
            if (data != null) {
                val script = "window.exeIntentData('${data.scheme ?: ""}','${data.fragment ?: ""}')"
                binding.contentContainer.webview.post {
                    binding.contentContainer.webview.evaluateJavascript(script, null)
                }
                Log.d("MainActivity", "Pending OAuth callback handled: $data")
            }
            pendingOAuthIntent = null
        }
    }

    /** 弹出更新对话框，用户确认后开始下载 APK 并展示进度；取消则直接进 WebView */
    private fun showUpdateDialog() {
        if (updateDialog == null) {
            val updateDialog = UpdateDialog(onNegative = {
                /* 取消逻辑 */
                showWebView()
            }, onPositive = {
                /* 更新逻辑 */
                appInfo?.let {
                    binding.splashContainer.apply {
                        progressPercentContainer.visibility = View.VISIBLE
                        appViewModel.downloadApk(
                            it.apkUrl,
//                            "https://downv6.qq.com/qqweb/QQ_1/android_apk/9.2.35_0981238b4dfded76.apk",
                            onProgress = { progress ->
                                progressBar.post {
                                    val percent = progress.percent.toInt()
                                    progressBar.progress = percent
                                    tvProgressPercent.text = "${percent}%"
                                    tvProgressDesc.text = getString(
                                        R.string.updating_progress,
                                        progress.bytesDownloaded.formatFileSize(),
                                        progress.total.formatFileSize()
                                    )
                                    // 1. 获取进度条的宽度
                                    val progressBarWidth = progressBar.width
                                    // 2. 计算豆子的目标位置（X 坐标）
                                    // 进度百分比（0~1）
                                    val percentFloat = percent / 100f
                                    // 进度条的起点X
                                    val progressBarStartX = progressBar.x
                                    // 豆子图片宽度
                                    val beanJumpWidth = imgProgressIcon.width
                                    // 计算豆子中心点应该在进度条的哪个位置
                                    val targetX =
                                        progressBarStartX + percentFloat * (progressBarWidth - beanJumpWidth)
                                    // 3. 设置豆子的X坐标
                                    imgProgressIcon.x = targetX
                                }
                            }, onSuccess = { filePath ->
                                // 下载成功
                                tvProgressDesc.text = getString(R.string.upgrade_completed)
                                tvProgressDesc.setTextColor(
                                    ContextCompat.getColor(this@MainActivity, R.color.success)
                                )
                            }, onError = { errorMsg ->
                                // 显示错误
                                tvProgressDesc.text = getString(R.string.upgrade_failed)
                                tvProgressDesc.setTextColor(
                                    ContextCompat.getColor(this@MainActivity, R.color.error)
                                )
                                updateFailedDialog()
                            })
                    }
                }
            })
            updateDialog.show(supportFragmentManager, "UpdateDialog")
        }
    }

    /** 下载失败时延时 1 秒后弹出失败提示，用户可选择重试（浏览器打开）或跳过进 WebView */
    private fun updateFailedDialog() {
        if (updateFailedDialog == null) {
            lifecycleScope.launch {
                delay(1000)
                val updateFailedDialog = UpdateFailedDialog(onNegative = {
                    /* 取消逻辑 */
                    showWebView()
                }, onPositive = {
                    appInfo?.let {
                        appViewModel.openUrlInBrowser(this@MainActivity, url = it.apkUrl)
                    }

                })
                updateFailedDialog.show(supportFragmentManager, "UpdateFailedDialog")
            }
        }
    }

    /** 按地区在闪屏底部添加支付/供应商图标（巴西、印度、墨西哥、菲律宾等不同组合） */
    private fun addSuppliers(vararg drawableResIds: Int) {
        binding.splashContainer.supplierRow.removeAllViews()
        drawableResIds.forEach { resId ->
            ImageView(this).apply {
                setImageResource(resId)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
                )
                binding.splashContainer.supplierRow.addView(this)
                if (resId != drawableResIds.last()) {
                    binding.splashContainer.supplierRow.addView(Space(context).apply {
                        layoutParams = LinearLayout.LayoutParams(24.dp, 0)
                    })
                }
            }
        }
    }
}


