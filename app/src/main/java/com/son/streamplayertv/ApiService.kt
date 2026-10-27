package com.son.streamplayertv

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET

interface ApiService {
    @GET("channels.json")
    suspend fun getChannels(): List<ChannelDto>

    @GET("catalog.json")
    suspend fun getCatalog(): CatalogDto
}

data class ChannelDto(
        val id: Int,
        val name: String,
        val categories: List<String>,
        val streamUrl: String
)

object RetrofitClient {
    private const val BASE_URL = "https://raw.githubusercontent.com/byte-a-byte/TransmisionTvLibre/main/"

    val apiService: ApiService by lazy {
        Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(ApiService::class.java)
    }
}
