package com.example.learn_kotlin.api

import android.content.Context
import android.util.Log
import com.example.learn_kotlin.activity.TokenStore
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {
    @Volatile private var retrofitInstance: Retrofit? = null

    fun authApi(context: Context): AuthApi = retrofit(context).create(AuthApi::class.java)

    fun productApi(context: Context): ProductApi = retrofit(context).create(ProductApi::class.java)

    fun categoryApi(context: Context): CategoryApi = retrofit(context).create(CategoryApi::class.java)

    fun brandApi(context: Context): BrandApi = retrofit(context).create(BrandApi::class.java)

    fun cartApi(context: Context): CartApi = retrofit(context).create(CartApi::class.java)

    fun orderApi(context: Context): OrderApi = retrofit(context).create(OrderApi::class.java)

    fun profileApi(context: Context): ProfileApi = retrofit(context).create(ProfileApi::class.java)

    fun mediaApi(context: Context): MediaApi = retrofit(context).create(MediaApi::class.java)

    fun adminUserApi(context: Context): AdminUserApi = retrofit(context).create(AdminUserApi::class.java)

    fun healthApi(context: Context): HealthApi = retrofit(context).create(HealthApi::class.java)

    private fun retrofit(context: Context): Retrofit = retrofitInstance ?: synchronized(this) {
        retrofitInstance ?: buildRetrofit(context.applicationContext).also { retrofitInstance = it }
    }

    private fun buildRetrofit(context: Context): Retrofit {
        val tokenStore = TokenStore(context)
        val loggingInterceptor = HttpLoggingInterceptor { message ->
            Log.d("API_HTTP", message)
        }.apply {
            level = HttpLoggingInterceptor.Level.BASIC
            redactHeader("Authorization")
        }

        val httpClient = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(100, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .callTimeout(110, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val request = chain.request()
                val requestPath = request.url.encodedPath
                val isPublicCall = requestPath.startsWith("/api/v1/public/") ||
                    requestPath == "/health" ||
                    requestPath.endsWith("/auth/login") ||
                    requestPath.endsWith("/auth/register") ||
                    requestPath.endsWith("/auth/refresh-token")

                val token = if (isPublicCall) null else tokenStore.accessToken()
                val authenticatedRequest = request.newBuilder().apply {
                    if (!token.isNullOrBlank()) {
                        header("Authorization", "Bearer $token")
                    }
                }.build()
                chain.proceed(authenticatedRequest)
            }
            .authenticator(TokenAuthenticator(context))

        // Keep request URLs and timing logs out of release builds.
        val isDebuggable = context.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE != 0
        if (isDebuggable) httpClient.addInterceptor(loggingInterceptor)

        return Retrofit.Builder()
            .baseUrl(API_BASE_URL)
            .client(httpClient.build())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
}

internal const val API_BASE_URL = "https://ecommerce-backend-q1wt.onrender.com/"
