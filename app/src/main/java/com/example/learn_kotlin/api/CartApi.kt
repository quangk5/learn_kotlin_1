package com.example.learn_kotlin.api

import com.google.gson.JsonElement
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface CartApi {
    @GET("api/v1/cart")
    suspend fun getCart(): ApiResponse<CartResponse>

    @POST("api/v1/cart/items")
    suspend fun addItem(@Body request: CartItemRequest): ApiResponse<CartResponse>

    @PUT("api/v1/cart/items")
    suspend fun updateItemQuantity(@Body request: CartItemRequest): ApiResponse<CartResponse>

    @DELETE("api/v1/cart/items/{productId}")
    suspend fun removeItem(@Path("productId") productId: String): ApiResponse<CartResponse>

    @DELETE("api/v1/cart/clear")
    suspend fun clearCart(): ApiResponse<JsonElement>
}
