package com.example.learn_kotlin.api

import com.google.gson.JsonElement
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/** Product catalog endpoints from the backend OpenAPI document. */
interface ProductApi {
    @GET("api/v1/public/products")
    suspend fun getActiveProducts(
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 12,
        @Query("sortBy") sortBy: String = "createdAt",
        @Query("sortDir") sortDir: String = "desc"
    ): ApiResponse<PageResponse<ProductResponse>>

    @GET("api/v1/public/products/{id}")
    suspend fun getProductById(@Path("id") id: String): ApiResponse<ProductResponse>

    @GET("api/v1/public/products/search")
    suspend fun searchProducts(
        @Query("keyword") keyword: String? = null,
        @Query("categoryId") categoryId: String? = null,
        @Query("brandId") brandId: String? = null,
        @Query("minPrice") minPrice: Double? = null,
        @Query("maxPrice") maxPrice: Double? = null,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 12,
        @Query("sortBy") sortBy: String = "createdAt",
        @Query("sortDir") sortDir: String = "desc"
    ): ApiResponse<PageResponse<ProductResponse>>

    @POST("api/v1/admin/products")
    suspend fun createProduct(@Body request: ProductRequest): ApiResponse<ProductResponse>

    @PUT("api/v1/admin/products/{id}")
    suspend fun updateProduct(
        @Path("id") id: String,
        @Body request: ProductRequest
    ): ApiResponse<ProductResponse>

    @DELETE("api/v1/admin/products/{id}")
    suspend fun deleteProduct(@Path("id") id: String): ApiResponse<JsonElement>
}
