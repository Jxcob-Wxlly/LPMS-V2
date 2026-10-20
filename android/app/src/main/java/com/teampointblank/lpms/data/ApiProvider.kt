package com.teampointblank.lpms.data

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Builds the Retrofit client for the server address chosen in Settings.
 * The client is rebuilt automatically when the address changes.
 */
class ApiProvider(
    private val settings: SettingsStore,
    private val tokenProvider: () -> String?
) {
    private var cachedUrl: String? = null
    private var cachedApi: LpmsApi? = null

    @Synchronized
    fun api(): LpmsApi {
        val url = settings.current.serverUrl
        val existing = cachedApi
        if (existing != null && url == cachedUrl) return existing
        val created = build(url)
        cachedUrl = url
        cachedApi = created
        return created
    }

    private fun build(baseUrl: String): LpmsApi {
        val client = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(45, TimeUnit.SECONDS) // generous: a sleeping Azure App Service can take a while to wake
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val original = chain.request()
                val builder = original.newBuilder().header("Accept", "application/json")
                val path = original.url.encodedPath
                val anonymous = path.endsWith("/api/auth/login") || path.endsWith("/api/health")
                val token = tokenProvider()
                if (!anonymous && !token.isNullOrBlank()) {
                    builder.header("Authorization", "Bearer $token")
                }
                chain.proceed(builder.build())
            }
            .build()

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(LpmsApi::class.java)
    }
}
