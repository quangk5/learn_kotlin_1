package com.example.learn_kotlin.api

import com.google.gson.JsonElement
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface ProfileApi {
    @GET("api/v1/profile/me")
    suspend fun getProfile(): ApiResponse<UserProfileResponse>

    @PUT("api/v1/profile/me")
    suspend fun updateProfile(@Body request: ProfileUpdateRequest): ApiResponse<UserProfileResponse>

    @POST("api/v1/profile/change-password")
    suspend fun changePassword(@Body request: ChangePasswordRequest): ApiResponse<JsonElement>

    @GET("api/v1/profile/addresses")
    suspend fun getAddresses(): ApiResponse<List<AddressResponse>>

    @POST("api/v1/profile/addresses")
    suspend fun addAddress(@Body request: AddressRequest): ApiResponse<AddressResponse>

    @PUT("api/v1/profile/addresses/{addressId}")
    suspend fun updateAddress(
        @Path("addressId") addressId: String,
        @Body request: AddressRequest
    ): ApiResponse<AddressResponse>

    @DELETE("api/v1/profile/addresses/{addressId}")
    suspend fun deleteAddress(@Path("addressId") addressId: String): ApiResponse<JsonElement>

    @PUT("api/v1/profile/addresses/{addressId}/default")
    suspend fun setDefaultAddress(@Path("addressId") addressId: String): ApiResponse<JsonElement>
}
