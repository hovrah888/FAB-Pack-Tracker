package com.example.fabpacktracker.data.repository

import android.util.Log
import com.example.fabpacktracker.data.model.ListingSuggestion
import com.example.fabpacktracker.data.remote.ChatRequest
import com.example.fabpacktracker.data.remote.GROQ_PROMPT
import com.example.fabpacktracker.data.remote.GroqApi
import com.example.fabpacktracker.data.remote.GroqContent
import com.example.fabpacktracker.data.remote.GroqMessage
import com.example.fabpacktracker.data.remote.ResponseFormat
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import retrofit2.HttpException
import java.io.IOException

private fun String.cleanText(): String =
    this.replace('\u2010', '-')   // обычный «дефис» из набора Unicode
        .replace('\u2011', '-')   // неразрывный дефис
        .replace('\u00A0', ' ')   // неразрывный пробел
        .trim()

class GroqRepository (
    private val api: GroqApi,
    private val json: Json
) {
    suspend fun improveListing(name: String, description: String): Result<ListingSuggestion>
    {

        return try {
            val chat = api.createChat(
                ChatRequest(
                    model = "openai/gpt-oss-120b",
                    messages = listOf(GroqMessage(role = "system", content = GROQ_PROMPT), GroqMessage(role = "user", content = "<name>$name</name> <description>$description</description>")),
                    temperature = 0.3,
                    responseFormat = ResponseFormat("json_object")
                )
            )
            val content = chat.choices.firstOrNull()?.message?.content
                ?: return Result.failure(IllegalStateException("Пустой ответ от модели"))
            val suggestion = json.decodeFromString<GroqContent>(content)

            val listingSuggestion = ListingSuggestion(
                name = suggestion.name.cleanText(),
                description = suggestion.description.cleanText(),
                missing = suggestion.missingInfo.map{ it.cleanText()})

            Result.success(listingSuggestion)
        }
        // ошибка Http
        catch (e : HttpException){
            val errorCode = e.code()
            val errorBody = e.response()?.errorBody()?.string()

            Log.e("ApiTest", "HttpException: $errorCode, $errorBody")
            Result.failure(e)
        }
        // проблемы с сетью
        catch (e : IOException) {

            Log.e("ApiTest", "IOException: ${e.message}")
            Result.failure(e)
        }
        // ошибки json
        catch (e: SerializationException){
            Log.e("ApiTest", "SerializationException: ${e.message}")
            Result.failure(e)
        }

    }
}