package com.example.fabpacktracker

import com.example.fabpacktracker.data.remote.ChatRequest
import com.example.fabpacktracker.data.remote.GroqApi
import com.example.fabpacktracker.data.remote.GroqResponse

// отдельный класс для фейка GroqApi (используеться в GroqRepositoryTest и )
class FakeGroqApi(
    private val behavior: suspend (ChatRequest) -> GroqResponse
) : GroqApi {
    var lastRequest: ChatRequest? = null // запомнить, что прислал репозиторий

    var callCount = 0
        private set // только изменения внутри класса и чтения из вне


    override suspend fun createChat(request: ChatRequest): GroqResponse {
        lastRequest = request
        callCount++
        return behavior(request)
    }
}