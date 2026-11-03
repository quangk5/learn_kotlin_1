package com.example.learn_kotlin.api

import com.google.gson.annotations.SerializedName

data class LoginRequest(
    val email: String,
    val password: String
)

data class RegisterRequest(
    val fullName: String,
    val email: String,
    val password: String,
    val phone: String? = null
)

data class RefreshTokenRequest(
    val refreshToken: String
)

data class TokenResponse(
    val accessToken: String,
    val refreshToken: String? = null,
    val tokenType: String? = "Bearer",
    val expiresIn: Long? = null
)

data class ApiResponse<T>(
    val success: Boolean = false,
    val code: Int? = null,
    val message: String? = null,
    val data: T? = null,
    val timestamp: String? = null
)

data class PageResponse<T>(
    val items: List<T>? = emptyList(),
    val pageNo: Int? = 0,
    val pageSize: Int? = 12,
    val totalElements: Long? = 0,
    val totalPages: Int? = 0,
    val last: Boolean? = true
)

data class ProductResponse(
    val id: String? = null,
    val sku: String? = null,
    val name: String? = null,
    val slug: String? = null,
    val price: Double? = null,
    val thumbnailUrl: String? = null,
    val images: List<String>? = emptyList(),
    val description: String? = null,
    val stockQuantity: Int? = 0,
    val active: Boolean? = null,
    val categoryId: String? = null,
    val categoryName: String? = null,
    val brandId: String? = null,
    val brandName: String? = null,
    val createdAt: String? = null
)

data class CategoryResponse(
    val id: String? = null,
    val name: String? = null,
    val slug: String? = null,
    val description: String? = null,
    val createdAt: String? = null
)

data class CartItemRequest(
    val productId: String,
    val quantity: Int
)

data class CartResponse(
    val id: String? = null,
    val userId: String? = null,
    val items: List<CartItemResponse>? = emptyList(),
    val totalItems: Int? = 0,
    val totalAmount: Double? = 0.0
)

data class CartItemResponse(
    val productId: String? = null,
    val productName: String? = null,
    val unitPrice: Double? = null,
    val quantity: Int? = null,
    val subTotal: Double? = null,
    val thumbnailUrl: String? = null,
    val sku: String? = null,
    val categoryName: String? = null,
    val brandName: String? = null
)

data class ProductRequest(
    val sku: String,
    val name: String,
    val slug: String,
    val description: String? = null,
    val price: Double,
    val stockQuantity: Int,
    val thumbnailUrl: String? = null,
    val images: List<String>? = null,
    val categoryId: String,
    val brandId: String? = null
)

data class CategoryRequest(
    val name: String,
    val slug: String,
    val description: String? = null
)

data class BrandRequest(
    val name: String,
    val slug: String,
    val description: String? = null,
    val logoUrl: String? = null
)

data class BrandResponse(
    val id: String? = null,
    val name: String? = null,
    val slug: String? = null,
    val description: String? = null,
    val logoUrl: String? = null,
    val createdAt: String? = null
)

data class ProfileUpdateRequest(
    val fullName: String,
    val phone: String? = null,
    val avatarUrl: String? = null
)

data class AddressRequest(
    val recipientName: String,
    val phone: String,
    val province: String,
    val district: String,
    val ward: String,
    val streetAddress: String,
    @SerializedName("default") val isDefault: Boolean? = false
)

data class AddressResponse(
    val id: String? = null,
    val recipientName: String? = null,
    val phone: String? = null,
    val province: String? = null,
    val district: String? = null,
    val ward: String? = null,
    val streetAddress: String? = null,
    @SerializedName("default") val isDefault: Boolean? = false
)

data class UserProfileResponse(
    val id: String? = null,
    val email: String? = null,
    val fullName: String? = null,
    val phone: String? = null,
    val avatarUrl: String? = null,
    val roles: Set<String>? = emptySet(),
    val addresses: List<AddressResponse>? = emptyList()
)

data class ChangePasswordRequest(val oldPassword: String, val newPassword: String)

data class CheckoutRequest(
    val addressId: String,
    val paymentMethod: String,
    val note: String? = null
)

data class OrderItemResponse(
    val productId: String? = null,
    val sku: String? = null,
    val productName: String? = null,
    val thumbnailUrl: String? = null,
    val brandName: String? = null,
    val price: Double? = null,
    val quantity: Int? = null,
    val subTotal: Double? = null
)

data class OrderResponse(
    val id: String? = null,
    val orderCode: String? = null,
    val userId: String? = null,
    val customerName: String? = null,
    val customerEmail: String? = null,
    val customerPhone: String? = null,
    val shippingAddress: AddressResponse? = null,
    val items: List<OrderItemResponse>? = emptyList(),
    val totalAmount: Double? = null,
    val shippingFee: Double? = null,
    val finalAmount: Double? = null,
    val paymentMethod: String? = null,
    val status: String? = null,
    val note: String? = null,
    val createdAt: String? = null
)

data class UpdateOrderStatusRequest(val status: String)
data class ChangeUserStatusRequest(val enabled: Boolean)

data class MediaUploadResponse(
    val fileName: String? = null,
    val originalName: String? = null,
    val fileUrl: String? = null,
    val contentType: String? = null,
    val size: Long? = null
)

