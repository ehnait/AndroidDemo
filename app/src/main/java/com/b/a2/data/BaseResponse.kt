package com.b.a2.data

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

@Keep
data class BaseResponse<T>(
    @SerializedName("data")
    val data: T,

    @SerializedName("message")
    val message: String,

    @SerializedName("result")
    val result: Int
)

