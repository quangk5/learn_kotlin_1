package com.example.learn_kotlin.api

import com.google.gson.JsonElement
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface CategoryApi {
    @GET("api/v1/public/categories")
    suspend fun getAllCategories(): ApiResponse<List<CategoryResponse>>

    @GET("api/v1/public/categories/{id}")
    suspend fun getCategoryById(@Path("id") id: String): ApiResponse<CategoryResponse>

    @POST("api/v1/admin/categories")
    suspend fun createCategory(@Body request: CategoryRequest): ApiResponse<CategoryResponse>

    @PUT("api/v1/admin/categories/{id}")
    suspend fun updateCategory(
        @Path("id") id: String,
        @Body request: CategoryRequest
    ): ApiResponse<CategoryResponse>

    @DELETE("api/v1/admin/categories/{id}")
    suspend fun deleteCategory(@Path("id") id: String): ApiResponse<JsonElement>
}
