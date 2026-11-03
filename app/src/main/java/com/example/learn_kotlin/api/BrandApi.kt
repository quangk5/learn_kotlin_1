package com.example.learn_kotlin.api

import com.google.gson.JsonElement
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface BrandApi {
    @GET("api/v1/public/brands")
    suspend fun getAllBrands(): ApiResponse<List<BrandResponse>>

    @GET("api/v1/public/brands/{id}")
    suspend fun getBrandById(@Path("id") id: String): ApiResponse<BrandResponse>

    @POST("api/v1/admin/brands")
    suspend fun createBrand(@Body request: BrandRequest): ApiResponse<BrandResponse>

    @PUT("api/v1/admin/brands/{id}")
    suspend fun updateBrand(
        @Path("id") id: String,
        @Body request: BrandRequest
    ): ApiResponse<BrandResponse>

    @DELETE("api/v1/admin/brands/{id}")
    suspend fun deleteBrand(@Path("id") id: String): ApiResponse<JsonElement>
}
