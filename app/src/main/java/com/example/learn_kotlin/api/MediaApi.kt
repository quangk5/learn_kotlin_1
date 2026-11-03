package com.example.learn_kotlin.api

import okhttp3.MultipartBody
import retrofit2.http.Multipart
import retrofit2.http.Part
import retrofit2.http.POST

interface MediaApi {
    @Multipart
    @POST("api/v1/media/upload")
    suspend fun uploadFile(@Part file: MultipartBody.Part): ApiResponse<MediaUploadResponse>

    @Multipart
    @POST("api/v1/media/upload-multiple")
    suspend fun uploadFiles(@Part files: List<MultipartBody.Part>): ApiResponse<List<MediaUploadResponse>>
}
