package com.example.learn_kotlin.api

import com.google.gson.JsonElement
import retrofit2.http.Body
import retrofit2.http.PATCH
import retrofit2.http.Path

interface AdminUserApi {
    @PATCH("api/v1/admin/users/{userId}/status")
    suspend fun updateUserStatus(
        @Path("userId") userId: String,
        @Body status: Map<String, Boolean>
    ): ApiResponse<Map<String, JsonElement>>

    @PATCH("api/v1/admin/users/{userId}/roles")
    suspend fun updateUserRoles(
        @Path("userId") userId: String,
        @Body roles: Set<String>
    ): ApiResponse<Map<String, JsonElement>>
}
