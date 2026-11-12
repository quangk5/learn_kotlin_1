package com.example.learn_kotlin.api

import retrofit2.http.GET
import retrofit2.http.Path

interface PaymentApi {
    @GET("api/v1/payment/sepay/qr-info/{orderId}")
    suspend fun getSepayQrInfo(@Path("orderId") orderId: String): ApiResponse<PaymentQrResponse>

    @GET("api/v1/payment/status/{orderId}")
    suspend fun getPaymentStatus(@Path("orderId") orderId: String): ApiResponse<PaymentStatusResponse>
}
