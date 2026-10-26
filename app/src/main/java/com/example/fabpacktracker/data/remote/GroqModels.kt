package com.example.fabpacktracker.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

//        { <- GroqResponse
//            "id": "chatcmpl-…",
//            "object": "chat.completion",
//            "model": "openai/gpt-oss-20b",
//            "choices": [ <- GroqChoice
//            {
//                "index": 0,
//                "message": { <- GroqMessage
    //                "role": "assistant",
//                    "content": "{"name": string, "description": string}" <- GroqContent
//                 },
//                "finish_reason": "stop"
//            }
//            ],
//            "usage": { … }
//        }


@Serializable
data class GroqMessage(
    val role : String,
    val content : String)

@Serializable
data class GroqContent( // как раз тот вывод, который отправляет groq, согласно промпту
    @SerialName("missing_info") val missingInfo : List<String> = emptyList(),
    val name : String,
    val description : String
)
@Serializable
data class GroqChoice(
    val message: GroqMessage
)
@Serializable
data class GroqResponse(
   val choices : List<GroqChoice>
)


@Serializable
data class ResponseFormat(
    val type : String
)
@Serializable
data class ChatRequest(
    val model: String,
    val messages: List<GroqMessage>,
    val temperature: Double,
    @SerialName("response_format") val responseFormat: ResponseFormat
)

