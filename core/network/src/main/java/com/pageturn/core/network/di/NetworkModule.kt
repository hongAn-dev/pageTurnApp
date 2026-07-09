package com.pageturn.core.network.di

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.pageturn.core.common.preferences.UserPreferencesDataSource
import com.pageturn.core.network.PageTurnNetworkApi
import com.pageturn.core.network.RetrofitPageTurnNetworkApi
import com.pageturn.core.network.api.AuthService
import com.pageturn.core.network.api.BackendSyncService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import android.util.Log
import okio.Buffer
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private const val BASE_URL = "https://pageturn.ddns.net/"

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        encodeDefaults = true
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        preferences: UserPreferencesDataSource,
        authServiceLazy: dagger.Lazy<AuthService>
    ): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val authInterceptor = Interceptor { chain ->
            val token = runBlocking { preferences.accessToken.firstOrNull() }
            val requestBuilder = chain.request().newBuilder()
            if (!token.isNullOrEmpty()) {
                requestBuilder.header("Authorization", "Bearer $token")
            }
            chain.proceed(requestBuilder.build())
        }

        val curlInterceptor = Interceptor { chain ->
            val request = chain.request()
            val curlCmd = StringBuilder("curl -X ${request.method}")
            val headers = request.headers
            for (i in 0 until headers.size) {
                curlCmd.append(" -H \"${headers.name(i)}: ${headers.value(i)}\"")
            }
            request.body?.let { body ->
                val buffer = Buffer()
                body.writeTo(buffer)
                val bodyString = buffer.readUtf8()
                if (bodyString.isNotEmpty()) {
                    curlCmd.append(" -d '${bodyString.replace("'", "'\\''")}'")
                }
            }
            curlCmd.append(" \"${request.url}\"")
            Log.d("OkHttpCurl", curlCmd.toString())
            chain.proceed(request)
        }

        val authenticator = okhttp3.Authenticator { _, response ->
            val refreshToken = runBlocking { preferences.refreshToken.firstOrNull() }
            if (refreshToken.isNullOrEmpty() || refreshToken == "anonymous_refresh") {
                return@Authenticator null
            }
            if (response.request.url.encodedPath.contains("auth/refresh")) {
                return@Authenticator null
            }
            synchronized(this) {
                val currentToken = runBlocking { preferences.accessToken.firstOrNull() }
                val requestHeaderToken = response.request.header("Authorization")?.removePrefix("Bearer ")
                val newAccessToken = if (currentToken != requestHeaderToken && !currentToken.isNullOrEmpty()) {
                    currentToken
                } else {
                    try {
                        val authService = authServiceLazy.get()
                        val refreshResponse = runBlocking { authService.refresh(com.pageturn.core.network.api.RefreshRequest(refreshToken)) }
                        if (refreshResponse.success && refreshResponse.data != null) {
                            val data = refreshResponse.data
                            runBlocking {
                                preferences.saveTokens(data.accessToken, data.refreshToken)
                            }
                            data.accessToken
                        } else {
                            null
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        null
                    }
                }
                if (newAccessToken != null) {
                    response.request.newBuilder()
                        .header("Authorization", "Bearer $newAccessToken")
                        .build()
                } else {
                    null
                }
            }
        }

        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(curlInterceptor)
            .addInterceptor(loggingInterceptor)
            .authenticator(authenticator)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(
        okHttpClient: OkHttpClient,
        json: Json
    ): Retrofit {
        val contentType = "application/json".toMediaType()
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
    }

    @Provides
    @Singleton
    fun provideAuthService(retrofit: Retrofit): AuthService {
        return retrofit.create(AuthService::class.java)
    }

    @Provides
    @Singleton
    fun provideBackendSyncService(retrofit: Retrofit): BackendSyncService {
        return retrofit.create(BackendSyncService::class.java)
    }

    @Provides
    @Singleton
    fun providePageTurnNetworkApi(
        impl: RetrofitPageTurnNetworkApi
    ): PageTurnNetworkApi {
        return impl
    }
}
