package com.b.a2.utils

import android.content.res.Resources
import java.util.Locale

fun Long.formatFileSize(): String {
    if (this <= 0) return "0 B"

    // 定义单位数组
    val units = arrayOf("B", "KB", "MB", "GB", "TB")

    // 计算适当的单位索引
    val digitGroups =
        (Math.log10(this.toDouble()) / Math.log10(1024.0)).toInt().coerceAtMost(units.size - 1)

    // 计算文件大小并格式化
    val size = this / Math.pow(1024.0, digitGroups.toDouble())
    return String.format(Locale.ROOT, "%.1f %s", size, units[digitGroups])
}


val Int.dp: Int
    get() = (this * Resources.getSystem().displayMetrics.density).toInt()