package com.example.fabpacktracker

import com.example.fabpacktracker.data.remote.AppJson
import com.example.fabpacktracker.data.remote.ChatRequest
import com.example.fabpacktracker.data.remote.GROQ_PROMPT
import com.example.fabpacktracker.data.remote.GroqApi
import com.example.fabpacktracker.data.remote.GroqChoice
import com.example.fabpacktracker.data.remote.GroqMessage
import com.example.fabpacktracker.data.remote.GroqResponse
import com.example.fabpacktracker.data.repository.GroqRepository
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNotNull
import junit.framework.TestCase.assertNull
import junit.framework.TestCase.assertSame
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.SerializationException
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException
import kotlin.collections.emptyList

class GroqRepositoryTest {

    // собирает ответ сервера вокруг текста, который "сочинила модель"
    private fun responseWith(content: String) = GroqResponse(
        choices = listOf(GroqChoice(GroqMessage(role = "assistant", content = content)))
    )


    // ф-ция создание фейкового репозитория
    private fun repositoryReturning(content: String) = GroqRepository(
        api = FakeGroqApi { responseWith(content) }, // ! Обычную функцию можно спокойно вызывать внутри suspend-лямбды.
        json = AppJson.json            // тот самый общий объект, а не копия настроек
    )

    @Test
    // чтобы запускать suspend функции в тестах используют runTest
    fun `valid response is parsed into suggestion`() = runTest {
        // создание фейкового репозитория для этого теста
        val repo = repositoryReturning(
            """{"name": "Test name", "description": "Test description", "missing_info": ["file formats"]}"""
        )

        val result = repo.improveListing("any name", "any description")

        assertTrue(result.isSuccess)
        val suggestion = result.getOrNull()
        assertNotNull(suggestion)
        assertEquals("Test name", suggestion!!.name)
        assertEquals("Test description", suggestion.description)
        assertEquals(listOf("file formats"), suggestion.missing)
    }

    @Test
    fun `network failure is returned as failure`() = runTest {
        val error = IOException("no network")
        val repo = GroqRepository(
            api = FakeGroqApi { throw error }, // фейковый апи кидает ошибку
            json = AppJson.json
        )

        val result = repo.improveListing("any", "any")

        assertTrue(result.isFailure)
        val suggestion = result.getOrNull()
        assertNull(suggestion)
        assertEquals(error, result.exceptionOrNull()) // exceptionOrNull!!!
    }

    @Test
    fun `when content is not json return failure`() = runTest {
        val repo = repositoryReturning("Sorry i cant help")

        val result = repo.improveListing("any", "any")

        assertTrue(result.isFailure)
        val suggestion = result.getOrNull()
        assertNull(suggestion)
        assertTrue(result.exceptionOrNull() is SerializationException) // тут сравнивать с error не надо
        // ведь исключение пришло снаружи, а не внури самого теста
    }

    @Test
    fun `content which doesnt contain name return failure`() = runTest {
        val repo = repositoryReturning(
            """{"description": "Test description", "missing_info": ["file formats"]}"""
        )

        val result = repo.improveListing("any", "any")

        assertTrue(result.isFailure)
        val suggestion = result.getOrNull()
        assertNull(suggestion)
        assertTrue(result.exceptionOrNull() is SerializationException)
    }

    @Test
    fun `empty choices return failure`() = runTest {
        val repo = GroqRepository(
            api = FakeGroqApi { GroqResponse(choices = emptyList()) },
            json = AppJson.json
        )

        val result = repo.improveListing("any", "any")

        assertTrue(result.isFailure)
        val suggestion = result.getOrNull()
        assertNull(suggestion)
        assertTrue(result.exceptionOrNull() is IllegalStateException)
    }

    // ----
    @Test
    fun `http errors are returned as failure with the same code`() = runTest {
        for (code in listOf(401, 429, 500)) {
            val error = HttpException(Response.error<Any>(code, "".toResponseBody()))
            val repo = GroqRepository(api = FakeGroqApi { throw error }, json = AppJson.json)

            val result = repo.improveListing("any", "any")

            assertTrue("code $code: expected failure", result.isFailure)
            assertSame("code $code: another exception returned", error, result.exceptionOrNull())
            //assertSame(error.code(), code) // летит странная ошибка : Expected :401  Actual   :401
            assertEquals(error.code(), code) // тут все в порядке
        }
    }

    // проверка самого ChatRequest
    @Test
    fun `request contains system prompt and user data`() = runTest {
        val fakeApi = FakeGroqApi { responseWith("""{"name": "Test name", "description": "Test description", "missing_info": ["file formats"]}""") }
        val repo = GroqRepository(
            api = fakeApi,
            json = AppJson.json
        )

        val result = repo.improveListing("any name", "any description")

        val request = fakeApi.lastRequest
        assertNotNull(request)
        assertEquals(0.3, request?.temperature)
        assertEquals("json_object", request?.responseFormat?.type)

        assertEquals(GROQ_PROMPT, request?.messages[0]?.content)
        assertEquals("system", request?.messages[0]?.role)

        assertEquals("user", request?.messages[1]?.role)
        assertEquals("""<name>any name</name> <description>any description</description>""", request?.messages[1]?.content)

        assertTrue(result.isSuccess)
    }

    @Test
    fun `when missing_info is absent in json default to empty list`() = runTest {
        // JSON без ключа "missing_info"
        val repo = repositoryReturning(
            """{"name": "Test name", "description": "Test description"}"""
        )

        val result = repo.improveListing("any name", "any description")

        assertTrue(result.isSuccess)
        val suggestion = result.getOrNull()
        assertNotNull(suggestion)
        assertEquals("Test name", suggestion!!.name)
        assertEquals("Test description", suggestion.description)
        assertTrue(suggestion.missing.isEmpty()) // Проверяем, что подставился пустой список
    }

    @Test
    fun `extra json keys are ignored and response is parsed successfully`() = runTest {
        // JSON с лишним ключем
        val repo = repositoryReturning(
            """{
            "name": "Test name", 
            "description": "Test description", 
            "missing_info": ["file formats"],
            "unknown_field": "some extra value"
        }"""
        )

        val result = repo.improveListing("any name", "any description")

        assertTrue(result.isSuccess)
        val suggestion = result.getOrNull()
        assertNotNull(suggestion)
        assertEquals("Test name", suggestion!!.name)
        assertEquals("Test description", suggestion.description)
        assertEquals(listOf("file formats"), suggestion.missing)
    }

    @Test()
    fun `unexpected exception throws RuntimeException`() = runTest {
        val error = RuntimeException("Unexpected")
        val repo = GroqRepository(
            api = FakeGroqApi { throw error },
            json = AppJson.json
        )

        val thrown = try {
            repo.improveListing("any", "any")
            null
        } catch (e: RuntimeException) {
            e
        }

        assertSame(error, thrown)
    }

    @Test
    fun `ai-style hyphen converts into default one`() = runTest {
        val repo = repositoryReturning(
            """{"name": "Test name", "description": "low\u2011poly", "missing_info": ["file formats"]}"""
        )

        val result = repo.improveListing("any name", "any description")

        assertTrue(result.isSuccess)
        val suggestion = result.getOrNull()
        assertNotNull(suggestion)
        assertEquals("Test name", suggestion!!.name)
        assertEquals("low-poly", suggestion.description)
        assertEquals(listOf("file formats"), suggestion.missing)
    }

    @Test
    fun `empty missing info is allowed`() = runTest {
        // создание фейкового репозитория для этого теста
        val repo = repositoryReturning(
            """{"name": "Test name", "description": "Test description", "missing_info": []}"""
        )

        val result = repo.improveListing("any name", "any description")

        assertTrue(result.isSuccess)
        val suggestion = result.getOrNull()
        assertNotNull(suggestion)
        assertEquals("Test name", suggestion!!.name)
        assertEquals("Test description", suggestion.description)
        assertTrue(suggestion.missing.isEmpty())
    }

}

class FakeGroqApi(
    private val behavior: suspend (ChatRequest) -> GroqResponse
) : GroqApi {
    var lastRequest: ChatRequest? = null // запомнить, что прислал репозиторий

    override suspend fun createChat(request: ChatRequest): GroqResponse {
        lastRequest = request
        return behavior(request)
    }
}