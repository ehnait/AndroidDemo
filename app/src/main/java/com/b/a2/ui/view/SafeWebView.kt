package com.b.a2.ui.view

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Message
import android.util.AttributeSet
import android.util.Log
import android.view.Gravity
import android.view.View
import android.content.res.ColorStateList
import android.view.ViewGroup
import android.webkit.ValueCallback
import androidx.core.content.ContextCompat
import android.widget.FrameLayout
import android.widget.ProgressBar
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import com.b.a2.R
import com.b.a2.utils.UrlHandler

/**
 * 安全WebView控件，集成常用安全设置、JSBridge、特殊链接处理。
 * 用于XML布局和代码中安全加载网页。
 * 
 * 功能特性：
 * - 基础安全配置（禁止文件访问、内容访问等）
 * - JSBridge 支持（通过 "Android" 接口）
 * - 特殊链接处理（Telegram、WhatsApp、自定义协议等）
 * - 多窗口支持
 * - 文件选择器支持（需配合 Activity 实现）
 * 
 * 使用示例：
 * ```xml
 * <com.beans.a23.ui.view.SafeWebView
 *     android:id="@+id/webview"
 *     android:layout_width="match_parent"
 *     android:layout_height="match_parent"/>
 * ```
 * 
 * 文件选择器使用：
 * 在 Activity 的 onActivityResult 中调用 webView.onFileChooseResult(uris)
 * 
 * ```kotlin
 * companion object {
 *     private const val FILE_CHOOSER_REQUEST_CODE = 1001
 * }
 * 
 * override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
 *     super.onActivityResult(requestCode, resultCode, data)
 *     if (requestCode == FILE_CHOOSER_REQUEST_CODE) {
 *         val uris = if (resultCode == RESULT_OK && data?.data != null) {
 *             arrayOf(data.data!!)
 *         } else {
 *             null
 *         }
 *         binding.webview.onFileChooseResult(uris)
 *     }
 * }
 * ```
 */
class SafeWebView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : WebView(context, attrs, defStyleAttr) {

    companion object {
        private const val TAG = "SafeWebView"
        const val FILE_CHOOSER_REQUEST_CODE = 1001
    }

    private var mFilePathCallback: ValueCallback<Array<Uri>>? = null

    /** 首次加载时的 loading 遮罩，避免白屏过久 */
    private var loadingOverlay: View? = null
    /** 是否尚未完成过首次加载，仅此时显示 loading */
    private var isFirstLoad = true

    init {
        configureWebViewSettings()
        setLayerType(View.LAYER_TYPE_HARDWARE, null)
        setBackgroundColor(0xFF1E232F.toInt())
        // 注入JSBridge
        addJavascriptInterface(WebAppJsBridge(context), "Android")
        // 设置WebViewClient
        webViewClient = createWebViewClient()
        // 设置WebChromeClient
        webChromeClient = createWebChromeClient()
        // 创建并添加 loading 遮罩（首次加载时显示，防止白屏）
        setupLoadingOverlay()
    }

    /**
     * 创建覆盖在 WebView 上的 loading 遮罩层
     * 在 onPageStarted 时显示，onPageFinished 时隐藏
     */
    private fun setupLoadingOverlay() {
        val overlay = FrameLayout(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(0xFF1E232F.toInt())
            visibility = View.GONE
        }
        val progressBar = ProgressBar(context).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { gravity = Gravity.CENTER }
            indeterminateTintList = ColorStateList.valueOf(
                ContextCompat.getColor(context, R.color.primary)
            )
        }
        overlay.addView(progressBar)
        addView(overlay)
        loadingOverlay = overlay
    }

    /**
     * 配置 WebView 基础安全设置
     * 参考 CustomWebView 的配置，但采用更严格的安全策略
     */
    @SuppressLint("SetJavaScriptEnabled")
    private fun configureWebViewSettings() {
        settings.apply {
            // 基础功能设置（与 CustomWebView 一致）
            javaScriptEnabled = true
            domStorageEnabled = true
            useWideViewPort = true
            loadWithOverviewMode = true
            setNeedInitialFocus(true)
            setSupportMultipleWindows(true)
            javaScriptCanOpenWindowsAutomatically = true
            cacheMode = WebSettings.LOAD_DEFAULT
            
            // 安全设置（SafeWebView 采用更严格的策略）
            allowFileAccess = false // 禁止文件访问，防止本地文件泄露
            allowContentAccess = false // 禁止内容访问

        }
    }

    /**
     * 统一处理 URL 加载拦截
     *
     * @param url 要处理的 URL
     * @return true 表示已处理，WebView 不应加载此 URL；false 表示由 WebView 处理
     */
    private fun handleUrlLoading(url: String?): Boolean {
        if (url.isNullOrBlank()) {
            return false
        }
        return try {
            UrlHandler.handle(url, context)
        } catch (e: Exception) {
            Log.e(TAG, "Error handling URL: $url", e)
            false
        }
    }

    /**
     * 创建 WebViewClient，处理页面加载和 URL 拦截
     */
    private fun createWebViewClient(): WebViewClient {
        return object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                Log.d(TAG, "Page started: $url")
                if (isFirstLoad) loadingOverlay?.visibility = View.VISIBLE
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                Log.d(TAG, "Page finished: $url")
                loadingOverlay?.visibility = View.GONE
                isFirstLoad = false
            }

            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val url = request?.url?.toString()
                return handleUrlLoading(url)
            }

            @Deprecated("Use shouldOverrideUrlLoading(WebView, WebResourceRequest)")
            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                return handleUrlLoading(url)
            }

            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: android.webkit.WebResourceError?
            ) {
                super.onReceivedError(view, request, error)
                Log.e(TAG, "Page error: ${error?.description}, URL: ${request?.url}")
                loadingOverlay?.visibility = View.GONE
                isFirstLoad = false
            }
        }
    }

    /**
     * 创建子窗口的 WebViewClient（用于 onCreateWindow）
     * 使用统一的 handleSpecialLink 处理所有特殊链接
     */
    private fun createChildWebViewClient(): WebViewClient {
        return object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val url = request?.url?.toString()
                return handleUrlLoading(url)
            }

            @Deprecated("Use shouldOverrideUrlLoading(WebView, WebResourceRequest)")
            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                return handleUrlLoading(url)
            }
        }
    }

    /**
     * 创建 WebChromeClient，处理多窗口、文件选择等
     */
    private fun createWebChromeClient(): WebChromeClient {
        return object : WebChromeClient() {
            override fun onCreateWindow(
                view: WebView?,
                isDialog: Boolean,
                isUserGesture: Boolean,
                resultMsg: Message?
            ): Boolean {
                return try {
                    val newWebView = WebView(context)
                    // 为新窗口创建 WebViewClient，使用统一的 URL 处理逻辑
                    newWebView.webViewClient = createChildWebViewClient()
                    val transport = resultMsg?.obj as? WebView.WebViewTransport
                    transport?.webView = newWebView
                    resultMsg?.sendToTarget()
                    Log.d(TAG, "Created new window")
                    true
                } catch (e: Exception) {
                    Log.e(TAG, "Error creating window", e)
                    false
                }
            }

            override fun onShowFileChooser(
                webView: WebView?,
                filePathCallback: ValueCallback<Array<Uri>>?,
                fileChooserParams: FileChooserParams?
            ): Boolean {
                // 保存回调，等待 Activity 处理文件选择
                mFilePathCallback = filePathCallback
                
                // 创建文件选择 Intent（参考 CustomWebView 的实现）
                val intent = fileChooserParams?.createIntent()
                    ?: Intent(Intent.ACTION_GET_CONTENT).apply {
                        type = "image/*"
                    }
                
                // 检查 context 是否是 Activity
                val activity = context as? Activity
                if (activity == null) {
                    Log.w(TAG, "Context is not an Activity, cannot launch file chooser")
                    mFilePathCallback?.onReceiveValue(null)
                    mFilePathCallback = null
                    return false
                }
                
                // 启动文件选择器（标准实现方式）
                try {
                    activity.startActivityForResult(
                        Intent.createChooser(intent, "选择文件"),
                        FILE_CHOOSER_REQUEST_CODE
                    )
                    Log.d(TAG, "File chooser launched")
                    return true
                } catch (e: Exception) {
                    Log.e(TAG, "Error launching file chooser", e)
                    // 如果出错，返回 null 表示取消
                    mFilePathCallback?.onReceiveValue(null)
                    mFilePathCallback = null
                    return false
                }
            }

            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                super.onProgressChanged(view, newProgress)
                // 可以在这里添加进度条更新逻辑
            }
        }
    }

    /**
     * 提供给 Activity/Fragment 回调文件选择结果
     * 
     * @param uris 选择的文件 URI 数组，如果用户取消选择则为 null
     * 
     * 使用示例（在 Activity 中）：
     * ```kotlin
     * override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
     *     super.onActivityResult(requestCode, resultCode, data)
     *     if (requestCode == FILE_CHOOSER_REQUEST_CODE) {
     *         val uris = if (resultCode == RESULT_OK && data?.data != null) {
     *             arrayOf(data.data!!)
     *         } else {
     *             null
     *         }
     *         webView.onFileChooseResult(uris)
     *     }
     * }
     * ```
     */
    fun onFileChooseResult(uris: Array<Uri>?) {
        try {
            mFilePathCallback?.onReceiveValue(uris)
            Log.d(TAG, "File choose result: ${uris?.size ?: 0} files")
        } catch (e: Exception) {
            Log.e(TAG, "Error handling file choose result", e)
        } finally {
            mFilePathCallback = null
        }
    }

    /**
     * 安全销毁，防止内存泄漏
     * 在 Activity/Fragment 销毁时调用，确保清理所有资源
     */
    override fun destroy() {
        try {
            // 清理文件选择回调
            mFilePathCallback?.onReceiveValue(null)
            mFilePathCallback = null
            // 移除 JS 接口
            removeJavascriptInterface("Android")
            // 停止加载
            stopLoading()
            // 清理历史记录
            clearHistory()
            // 从父容器移除
            (parent as? android.view.ViewGroup)?.removeView(this)
        } catch (e: Exception) {
            Log.e(TAG, "Error during destroy", e)
        } finally {
            super.destroy()
        }
    }
}