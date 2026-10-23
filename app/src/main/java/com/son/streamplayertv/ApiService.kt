package com.son.streamplayertv

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET

interface ApiService {
    @GET("channels.json") // Nombre del archivo en tu repositorio
    suspend fun getChannels(): List<ChannelDto>
}

// Objeto DTO para mapear el JSON
data class ChannelDto(
        val id: Int,
        val name: String,
        val categories: List<String>,
        val streamUrl: String
)

object RetrofitClient {
    // Reemplaza con la URL base de tu usuario y repositorio Raw de GitHub
    private const val BASE_URL = "https://raw.githubusercontent.com/byte-a-byte/TransmisionTvLibre/main/"

    val apiService: ApiService by lazy {
        Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(ApiService::class.java)
    }
}