package com.b.a2.ui.view

import android.content.Context
import android.webkit.JavascriptInterface

class WebAppJsBridge(val context: Context) {

    @JavascriptInterface
    fun onPageLoaded() {
        // Implement if needed
    }

}