package com.b.a2.utils

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log

/**
 * URL 拦截处理器
 * 负责处理 WebView 中的特殊链接，包括：
 * - 第三方应用链接（Telegram、WhatsApp、Facebook 等）
 * - 自定义协议（browser://、intent:// 等）
 * - 系统协议（mailto:、tel: 等）
 * - Deep Link（paytmmp:// 等）
 *
 * 使用方式：
 * ```kotlin
 * val handled = UrlHandler.handle(url, context)
 * if (handled) {
 *     // URL 已被处理，WebView 不应继续加载
 * }
 * ```
 */
object UrlHandler {
    private const val TAG = "UrlHandler"

    // URL 模式常量
    private object Patterns {
        const val TELEGRAM_HTTPS = "https://t.me/"
        const val TELEGRAM_HTTP = "http://telegram.me/"
        const val WHATSAPP_SCHEME = "whatsapp://"
        const val WHATSAPP_WA_ME = "https://wa.me/"
        const val WHATSAPP_API = "https://api.whatsapp.com/"
        const val WHATSAPP_CHAT = "https://chat.whatsapp.com/"
        const val BROWSER_SCHEME = "browser://"
        const val INTENT_SCHEME = "intent://"
        const val MAILTO_SCHEME = "mailto:"
        const val TEL_SCHEME = "tel:"
        const val FB_SCHEME = "fb://"
        const val FACEBOOK_HTTPS = "https://www.facebook.com/"
        const val FACEBOOK_M = "https://m.facebook.com/"
        const val FACEBOOK_FB = "https://fb.com/"
        const val PAYTMMP_SCHEME = "paytmmp://"
        const val PHONE_PE_SCHEME = "phonepe://"
    }

    // 应用包名常量
    private object Packages {
        const val TELEGRAM = "org.telegram.messenger"
        const val WHATSAPP = "com.whatsapp"
        const val FACEBOOK = "com.facebook.katana"
        const val PAYTM_MONEY = "net.one97.paytm"
        const val PHONE_PE = "com.phonepe.app"
    }

    /**
     * 处理 URL
     *
     * @param url 要处理的 URL
     * @param context 上下文对象
     * @return true 表示已处理，WebView 不应加载此 URL；false 表示由 WebView 处理
     */
    fun handle(url: String, context: Context): Boolean {
        if (url.isBlank()) {
            Log.w(TAG, "Empty URL provided")
            return false
        }

        Log.d(TAG, "Handling URL: $url")

        return when {
            isTelegram(url) -> handleTelegram(url, context)
            isWhatsApp(url) -> handleWhatsApp(url, context)
            isFacebook(url) -> handleFacebook(url, context)
            isBrowserScheme(url) -> handleBrowserScheme(url, context)
            isIntentScheme(url) -> handleIntentScheme(url, context)
            isSystemScheme(url) -> handleSystemScheme(url, context)
            isPaytmMoney(url) -> handlePaytmMoney(url, context)
            isPhonePe(url) -> handlePhonePe(url, context)
            else -> false // 让 WebView 处理其他 URL
        }
    }


    // ==================== URL 类型判断 ====================

    private fun isTelegram(url: String): Boolean {
        return url.startsWith(Patterns.TELEGRAM_HTTPS, ignoreCase = true) ||
                url.startsWith(Patterns.TELEGRAM_HTTP, ignoreCase = true)
    }

    private fun isWhatsApp(url: String): Boolean {
        return url.startsWith(Patterns.WHATSAPP_SCHEME, ignoreCase = true) ||
                url.startsWith(Patterns.WHATSAPP_WA_ME, ignoreCase = true) ||
                url.startsWith(Patterns.WHATSAPP_API, ignoreCase = true) ||
                url.startsWith(Patterns.WHATSAPP_CHAT, ignoreCase = true)
    }

    private fun isBrowserScheme(url: String): Boolean {
        return url.startsWith(Patterns.BROWSER_SCHEME, ignoreCase = true)
    }

    private fun isIntentScheme(url: String): Boolean {
        return url.startsWith(Patterns.INTENT_SCHEME, ignoreCase = true)
    }

    private fun isFacebook(url: String): Boolean {
        return url.startsWith(Patterns.FB_SCHEME, ignoreCase = true) ||
                url.startsWith(Patterns.FACEBOOK_HTTPS, ignoreCase = true) ||
                url.startsWith(Patterns.FACEBOOK_M, ignoreCase = true) ||
                url.startsWith(Patterns.FACEBOOK_FB, ignoreCase = true)
    }

    private fun isSystemScheme(url: String): Boolean {
        return url.startsWith(Patterns.MAILTO_SCHEME, ignoreCase = true) ||
                url.startsWith(Patterns.TEL_SCHEME, ignoreCase = true)
    }

    private fun isPaytmMoney(url: String): Boolean {
        return url.startsWith(Patterns.PAYTMMP_SCHEME, ignoreCase = true)
    }

    private fun isPhonePe(url: String): Boolean {
        return url.startsWith(Patterns.PHONE_PE_SCHEME, ignoreCase = true)
    }

    // ==================== 链接处理实现 ====================

    /**
     * 处理 Telegram 链接
     * 优先使用 Telegram 应用打开，如果未安装或无法处理则使用浏览器打开
     */
    private fun handleTelegram(url: String, context: Context): Boolean {
        return try {
            if (!isAppInstalled(context, Packages.TELEGRAM)) {
                openInBrowser(url, context)
                Log.d(TAG, "Telegram not installed, opened in browser: $url")
                return true
            }

            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                setPackage(Packages.TELEGRAM)
            }

            // 检查应用是否能处理该 Intent（特别是对于 HTTP/HTTPS 链接）
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
                Log.d(TAG, "Opened Telegram link in app: $url")
            } else {
                // 应用无法处理该 URL，使用浏览器打开
                openInBrowser(url, context)
                Log.d(TAG, "Telegram cannot handle URL, opened in browser: $url")
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error handling Telegram link: $url", e)
            openInBrowser(url, context)
            true
        }
    }

    /**
     * 处理 WhatsApp 链接
     * 优先使用 WhatsApp 应用打开，如果未安装或无法处理则使用浏览器打开
     */
    private fun handleWhatsApp(url: String, context: Context): Boolean {
        return try {
            if (!isAppInstalled(context, Packages.WHATSAPP)) {
                openInBrowser(url, context)
                Log.d(TAG, "WhatsApp not installed, opened in browser: $url")
                return true
            }

            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                setPackage(Packages.WHATSAPP)
            }

            // 检查应用是否能处理该 Intent（特别是对于 HTTP/HTTPS 链接）
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
                Log.d(TAG, "Opened WhatsApp link in app: $url")
            } else {
                // 应用无法处理该 URL，使用浏览器打开
                openInBrowser(url, context)
                Log.d(TAG, "WhatsApp cannot handle URL, opened in browser: $url")
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error handling WhatsApp link: $url", e)
            openInBrowser(url, context)
            true
        }
    }

    /**
     * 处理 Facebook 链接
     * 优先使用 Facebook 应用打开，如果未安装或无法处理则使用浏览器打开
     */
    private fun handleFacebook(url: String, context: Context): Boolean {
        return try {
            if (!isAppInstalled(context, Packages.FACEBOOK)) {
                openInBrowser(url, context)
                Log.d(TAG, "Facebook not installed, opened in browser: $url")
                return true
            }

            // 如果是 HTTP/HTTPS 链接，尝试转换为 fb:// deep link
            val deepLinkUrl = when {
                url.startsWith(Patterns.FACEBOOK_HTTPS, ignoreCase = true) -> {
                    url.replace(Patterns.FACEBOOK_HTTPS, "fb://", ignoreCase = true)
                }
                url.startsWith(Patterns.FACEBOOK_M, ignoreCase = true) -> {
                    url.replace(Patterns.FACEBOOK_M, "fb://", ignoreCase = true)
                }
                url.startsWith(Patterns.FACEBOOK_FB, ignoreCase = true) -> {
                    url.replace(Patterns.FACEBOOK_FB, "fb://", ignoreCase = true)
                }
                else -> url
            }

            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(deepLinkUrl)).apply {
                setPackage(Packages.FACEBOOK)
            }

            // 检查应用是否能处理该 Intent
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
                Log.d(TAG, "Opened Facebook link in app: $url")
            } else {
                // 应用无法处理该 URL，使用浏览器打开原始 URL
                openInBrowser(url, context)
                Log.d(TAG, "Facebook cannot handle URL, opened in browser: $url")
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error handling Facebook link: $url", e)
            openInBrowser(url, context)
            true
        }
    }

    /**
     * 处理自定义 browser:// 协议
     * 将 browser:// 转换为 https:// 并在浏览器中打开
     */
    private fun handleBrowserScheme(url: String, context: Context): Boolean {
        return try {
            val httpsUrl = url.replace(Patterns.BROWSER_SCHEME, "https://", ignoreCase = true)
            openInBrowser(httpsUrl, context)
            Log.d(TAG, "Converted browser:// to https:// and opened: $httpsUrl")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error handling browser scheme: $url", e)
            false
        }
    }

    /**
     * 处理 Intent 协议
     * 解析 Intent URI 并启动对应的 Activity
     */
    private fun handleIntentScheme(url: String, context: Context): Boolean {
        return try {
            val intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME)
            context.startActivity(intent)
            Log.d(TAG, "Handled intent scheme: $url")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing intent scheme: $url", e)
            false
        }
    }

    /**
     * 处理系统协议（mailto:、tel: 等）
     * 使用系统默认应用打开
     */
    private fun handleSystemScheme(url: String, context: Context): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            context.startActivity(intent)
            Log.d(TAG, "Handled system scheme: $url")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error handling system scheme: $url", e)
            false
        }
    }

    /**
     * 处理 Paytm Money Deep Link
     * 如果应用已安装则打开应用，否则跳转到 Play Store
     */
    private fun handlePaytmMoney(url: String, context: Context): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }

            if (isAppInstalled(context, Packages.PAYTM_MONEY)) {
                context.startActivity(intent)
                Log.d(TAG, "Opened Paytm Money link in app: $url")
                return true
            }

            openPlayStore(context, Packages.PAYTM_MONEY)
            Log.d(TAG, "Paytm Money not installed, opened Play Store")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error handling Paytm Money link: $url", e)
            false
        }
    }

    /**
     * 处理 PhonePe Deep Link Link
     * 如果应用已安装则打开应用，否则跳转到 Play Store
     */
    private fun handlePhonePe(url: String, context: Context): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }

            if (isAppInstalled(context, Packages.PHONE_PE)) {
                context.startActivity(intent)
                Log.d(TAG, "Opened PhonePe link in app: $url")
                return true
            }

            openPlayStore(context, Packages.PHONE_PE)
            Log.d(TAG, "PhonePe not installed, opened Play Store")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error handling PhonePe link: $url", e)
            false
        }
    }


    // ==================== 工具方法 ====================

    /**
     * 检查应用是否已安装
     */
    private fun isAppInstalled(context: Context, packageName: String): Boolean {
        return try {
            context.packageManager.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        } catch (e: Exception) {
            Log.e(TAG, "Error checking app installation: $packageName", e)
            false
        }
    }

    /**
     * 在浏览器中打开 URL
     */
    private fun openInBrowser(url: String, context: Context) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Error opening URL in browser: $url", e)
        }
    }

    /**
     * 打开 Play Store 应用详情页
     * 优先使用 market:// 协议，如果不可用则使用 HTTPS 链接
     */
    private fun openPlayStore(context: Context, packageName: String) {
        try {
            // 尝试使用 market:// 协议
            val marketIntent =
                Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName")).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }

            if (marketIntent.resolveActivity(context.packageManager) != null) {
                context.startActivity(marketIntent)
            } else {
                // 备用方案：使用浏览器打开 Play Store 网页版
                val webIntent = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/apps/details?id=$packageName")
                )
                context.startActivity(webIntent)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error opening Play Store: $packageName", e)
        }
    }
}
