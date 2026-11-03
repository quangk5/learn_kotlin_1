package com.example.learn_kotlin.api

import org.json.JSONObject
import retrofit2.HttpException
import java.io.IOException

fun Throwable.toUserMessage(): String = when (this) {
    is HttpException -> {
        val body = response()?.errorBody()?.string()
        val serverMessage = try {
            body?.let { JSONObject(it).optString("message").takeIf(String::isNotBlank) }
        } catch (_: Exception) {
            null
        }
        serverMessage ?: when (code()) {
            401 -> "Email hoặc mật khẩu không đúng."
            409 -> "Email này đã được đăng ký."
            else -> "Máy chủ trả về lỗi (${code()})."
        }
    }
    is IOException -> "Không kết nối được máy chủ. Kiểm tra mạng rồi thử lại."
    is IllegalStateException -> message?.takeIf(String::isNotBlank) ?: "Yêu cầu không thành công."
    else -> "Có lỗi xảy ra. Vui lòng thử lại."
}