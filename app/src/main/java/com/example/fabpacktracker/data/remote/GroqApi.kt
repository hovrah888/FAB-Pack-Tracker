package com.example.fabpacktracker.data.remote

import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import okhttp3.MediaType.Companion.toMediaType
import kotlinx.serialization.json.Json
import retrofit2.http.Body
import retrofit2.http.POST

interface GroqApi {
    @POST("openai/v1/chat/completions")
    suspend fun createChat(@Body request: ChatRequest): GroqResponse
}