package com.example.learn_kotlin.api

import com.google.gson.JsonElement
import retrofit2.http.GET

interface HealthApi {
    @GET("health")
    suspend fun checkHealth(): Map<String, JsonElement>
}
