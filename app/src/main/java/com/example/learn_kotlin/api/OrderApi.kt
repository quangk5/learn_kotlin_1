package com.example.learn_kotlin.api

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface OrderApi {
    @GET("api/v1/orders")
    suspend fun getMyOrders(
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 12
    ): ApiResponse<PageResponse<OrderResponse>>

    @POST("api/v1/orders")
    suspend fun checkout(@Body request: CheckoutRequest): ApiResponse<OrderResponse>

    @GET("api/v1/orders/{id}")
    suspend fun getOrderById(@Path("id") id: String): ApiResponse<OrderResponse>

    @PUT("api/v1/orders/{id}/cancel")
    suspend fun cancelOrder(@Path("id") id: String): ApiResponse<OrderResponse>

    @GET("api/v1/admin/orders")
    suspend fun getAllOrdersForAdmin(
        @Query("status") status: String? = null,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 12
    ): ApiResponse<PageResponse<OrderResponse>>

    @PUT("api/v1/admin/orders/{id}/status")
    suspend fun updateOrderStatus(
        @Path("id") id: String,
        @Body request: UpdateOrderStatusRequest
    ): ApiResponse<OrderResponse>
}
