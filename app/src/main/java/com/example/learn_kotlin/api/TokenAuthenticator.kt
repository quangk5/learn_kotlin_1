package com.example.learn_kotlin.api

import android.content.Context
import android.util.Log
import com.example.learn_kotlin.activity.TokenStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import okhttp3.Authenticator
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.Route
import java.io.IOException
import java.util.concurrent.TimeUnit

/** Refreshes an expired access token once and retries the original request. */
internal class TokenAuthenticator(context: Context) : Authenticator {
    private val tokenStore = TokenStore(context.applicationContext)
    private val gson = Gson()
    private val refreshLock = Any()
    private val refreshClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()

    override fun authenticate(route: Route?, response: Response): Request? {
        if (responseCount(response) >= 2) {
            tokenStore.clear()
            return null
        }

        val rejectedToken = response.request.header("Authorization")
            ?.removePrefix("Bearer ")
            ?.takeIf(String::isNotBlank)
            ?: return null

        return synchronized(refreshLock) {
            val currentToken = tokenStore.accessToken() ?: return@synchronized null

            // Another concurrent request may already have refreshed the token.
            if (currentToken != rejectedToken) {
                return@synchronized response.request.newBuilder()
                    .header("Authorization", "Bearer $currentToken")
                    .build()
            }

            val refreshToken = tokenStore.refreshToken()
            if (refreshToken.isNullOrBlank()) {
                tokenStore.clear()
                return@synchronized null
            }

            val refreshed = try {
                requestNewTokens(refreshToken)
            } catch (error: IOException) {
                Log.w("API_AUTH", "Token refresh could not reach the server", error)
                null
            }

            if (refreshed == null || refreshed.accessToken.isBlank()) {
                return@synchronized null
            }

            tokenStore.save(refreshed, fallbackRefreshToken = refreshToken)
            response.request.newBuilder()
                .header("Authorization", "Bearer ${refreshed.accessToken}")
                .build()
        }
    }

    private fun requestNewTokens(refreshToken: String): TokenResponse? {
        val body = gson.toJson(RefreshTokenRequest(refreshToken))
            .toRequestBody(JSON_MEDIA_TYPE)
        val request = Request.Builder()
            .url("${API_BASE_URL}api/v1/auth/refresh-token")
            .post(body)
            .build()

        refreshClient.newCall(request).execute().use { response ->
            if (response.code in 400..499 && response.code != 429) {
                tokenStore.clear()
                return null
            }
            if (!response.isSuccessful) return null

            val json = response.body?.string() ?: return null
            val responseType = object : TypeToken<ApiResponse<TokenResponse>>() {}.type
            val envelope = runCatching { gson.fromJson<ApiResponse<TokenResponse>>(json, responseType) }
                .getOrNull()
                ?: return null
            if (!envelope.success) {
                tokenStore.clear()
                return null
            }
            return envelope.data?.takeIf { !it.accessToken.isNullOrBlank() }
        }
    }

    private fun responseCount(response: Response): Int {
        var count = 1
        var prior = response.priorResponse
        while (prior != null) {
            count++
            prior = prior.priorResponse
        }
        return count
    }

    private companion object {
        val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }
}
