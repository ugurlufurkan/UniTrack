package com.unitrack.app.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GoogleLoginRequest(
    val idToken: String
)

@Serializable
data class LogoutRequest(
    val refreshToken: String
)

@Serializable
data class RefreshTokenRequest(
    val refreshToken: String
)

@Serializable
data class AuthResponse(
    val success: Boolean = false,
    val message: String = "",
    val accessToken: String = "",
    val refreshToken: String = "",
    val user: UserDto = UserDto() // Backend'den user objesi hiç gelmezse bile çökmesini engeller
)

@Serializable
data class UserDto(
    @SerialName("id") // Backend'den veri "id" olarak geliyorsa onu bizim "userId" ile eşleştirir
    val userId: String = "", // Veri hiç gelmezse çökmek yerine boş bir ID kabul eder

    val email: String = "",
    val name: String? = null,
    val picture: String? = null
)