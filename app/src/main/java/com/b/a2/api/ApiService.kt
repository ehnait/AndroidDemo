package com.b.a2.api

import com.b.a2.data.AppInfo
import com.b.a2.data.BaseResponse
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Url
import retrofit2.http.Streaming

interface ApiService {
    @GET("xxx")
    suspend fun getAppInfo(): BaseResponse<AppInfo>

    @Streaming // 这个注解很重要，避免大文件下载时OOM
    @GET
    suspend fun downloadFile(@Url fileUrl: String): Response<ResponseBody>
}
