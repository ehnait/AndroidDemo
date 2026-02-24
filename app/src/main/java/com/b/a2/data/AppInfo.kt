package com.b.a2.data

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

@Keep
data class AppInfo(
    @SerializedName("apkurl")
    val apkUrl: String,

    @SerializedName("apkversion")
    val apkVersion: String,

    @SerializedName("apkCode")
    val apkCode: Int,

    @SerializedName("resourceUrl")
    val resourceUrl: String?,

    @SerializedName("status")
    val status: Boolean
)

