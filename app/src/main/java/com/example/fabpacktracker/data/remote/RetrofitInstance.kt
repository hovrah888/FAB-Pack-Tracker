package com.example.fabpacktracker.data.remote

import java.util.concurrent.TimeUnit
import com.example.fabpacktracker.BuildConfig
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import okhttp3.MediaType.Companion.toMediaType
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient

object RetrofitInstance{
    // потом вернуть private VVV
    /*private*/ val json = Json { ignoreUnknownKeys = true } // игнорируем ненужные ключи

    // HTTP-клиент для сетевого запроса
    private val client = OkHttpClient.Builder()
        .readTimeout(30, TimeUnit.SECONDS) // таймаут нужен, ведь языкова модель может думать подольше
        // отрпавка запроса на сервер
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                // нужна авторизация с api ключем
                .addHeader("Authorization", "Bearer ${BuildConfig.GROQ_API_KEY}")
                .build()
            chain.proceed(request)
        }
        .build()


    // реализация интерфейса GroqApi
    val api: GroqApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.groq.com/")
            .client(client) // подключение
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(GroqApi::class.java)
    }

}