package com.pageturn.core.network.api

import kotlinx.serialization.Serializable
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PATCH
import retrofit2.http.Query

@Serializable
data class RegisterRequest(
    val email: String,
    val password: String,
    val displayName: String
)

@Serializable
data class LoginRequest(
    val email: String,
    val password: String
)

@Serializable
data class RefreshRequest(
    val refreshToken: String
)

@Serializable
data class LogoutRequest(
    val refreshToken: String
)

@Serializable
data class ApiResponse<T>(
    val success: Boolean,
    val status: Int? = null,
    val message: String? = null,
    val data: T? = null
)

@Serializable
data class AuthResponse(
    val accessToken: String,
    val refreshToken: String,
    val user: UserDto? = null
)

@Serializable
data class UserDto(
    val id: Int? = null,
    val email: String,
    val displayName: String? = null
)

@Serializable
data class UpdateProfileRequest(
    val email: String,
    val displayName: String
)

interface AuthService {
    @POST("api/auth/register")
    suspend fun register(@Body request: RegisterRequest): ApiResponse<AuthResponse>

    @POST("api/v1/auth/login")
    suspend fun login(@Body request: LoginRequest): ApiResponse<AuthResponse>

    @POST("api/auth/refresh")
    suspend fun refresh(@Body request: RefreshRequest): ApiResponse<AuthResponse>

    @POST("api/auth/logout")
    suspend fun logout(
        @Header("Authorization") token: String,
        @Body request: LogoutRequest
    )

    @GET("api/auth/me")
    suspend fun me(@Header("Authorization") token: String): UserDto

    @GET("api/users/me")
    suspend fun getCurrentUser(@Header("Authorization") token: String): UserDto

    @PATCH("api/users/me")
    suspend fun updateProfile(
        @Header("Authorization") token: String,
        @Body request: UpdateProfileRequest
    ): UserDto

    @GET("api/users/search")
    suspend fun searchUser(
        @Header("Authorization") token: String,
        @Query("email") email: String
    ): List<UserDto>
}
