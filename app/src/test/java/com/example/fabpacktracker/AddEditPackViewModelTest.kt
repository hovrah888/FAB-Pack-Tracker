package com.example.fabpacktracker

import com.example.fabpacktracker.data.local.AssetPack
import com.example.fabpacktracker.data.model.ListingSuggestion
import com.example.fabpacktracker.data.remote.AppJson
import com.example.fabpacktracker.data.remote.GroqChoice
import com.example.fabpacktracker.data.remote.GroqMessage
import com.example.fabpacktracker.data.remote.GroqResponse
import com.example.fabpacktracker.data.repository.GroqRepository
import com.example.fabpacktracker.viewmodel.AddEditPackViewModel
import com.example.fabpacktracker.viewmodel.FormState
import com.example.fabpacktracker.viewmodel.ListingSuggestionState
import com.example.fabpacktracker.viewmodel.NEW_PACK_ID
import com.example.fabpacktracker.viewmodel.PackState
import com.example.fabpacktracker.viewmodel.SaveEvent
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import okio.IOException
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description

// этот класс - JUnit 4 правило (Rule),
// которое автоматически подменяет главный Android-диспетчер корутин (Dispatchers.Main)
// на тестовый (TestDispatcher) перед началом каждого теста и сбрасывает его обратно после завершения.
@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(
    private val dispatcher: TestDispatcher = UnconfinedTestDispatcher()
) : TestWatcher() {
    override fun starting(description: Description) = Dispatchers.setMain(dispatcher)
    override fun finished(description: Description) = Dispatchers.resetMain()
}

class AddEditPackViewModelTest{
    @get:Rule val mainRule = MainDispatcherRule()

    // Фейковый GroqRepository для передачи в ViewModel
    private val fakeGroqRepo = GroqRepository(
        api = FakeGroqApi { GroqResponse(emptyList()) },
        json = AppJson.json
    )

    // Тестовый пак для проверок
    private val samplePack = AssetPack(
        id = 1,
        name = "Sci-Fi Weapons",
        price = 19.99,
        description = "Cool 3D models",
        salesCount = 5,
        publishedDate = 1_000L
    )


    private fun TestScope.collectEvents(viewModel: AddEditPackViewModel): List<SaveEvent> {
        // savePack отправляет event, который приостанавливает корутину,
        // поэтому, чтобы этого не произошло в тесте получаем этот event
        val events = mutableListOf<SaveEvent>()
        //                                VVV без этого дистпечера assertEquals(listOf(SaveEvent.Saved), events) выполнялось бы быстрее, чем принялся сигнал SaveEvent.Saved, и поэтому, тест падал бы
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { // backgroundScope существует внутри runTest
            viewModel.events.toList(events) // подписка на events через toList
        }
        return events
    }

    // --- ТЕСТЫ ПОД init{} ---

    // при новом паке обязано быть состояние пака New и состояние формы пустым
    @Test
    fun `init with NEW_PACK_ID sets state to New and keeps empty form`() {
        val dao = FakeAssetPackDao()
        val viewModel = AddEditPackViewModel(
            dao = dao,
            id = NEW_PACK_ID, // новый айди (-1)
            groqRepo = fakeGroqRepo
        )

        assertEquals(PackState.New, viewModel.pack.value)
        assertEquals(FormState(), viewModel.form.value)
    }

    // при существующем паке обязано быть состояние пака Found и состояние формы с ожидаемыми данными
    @Test
    fun `init with existing id sets state to Found and populates form`(){
        val dao = FakeAssetPackDao(listOf(samplePack))
        val viewModel = AddEditPackViewModel(
            dao = dao,
            id = 1, // существующий айди
            groqRepo = fakeGroqRepo
        )

        val expectedFormState = FormState(
            name = "Sci-Fi Weapons",
            price = "19.99",
            description = "Cool 3D models",
            salesCount = "5"
        )

        assertEquals(PackState.Found(samplePack), viewModel.pack.value)
        assertEquals(expectedFormState, viewModel.form.value)
    }

    // при несуществующем паке обязано быть состояние пака NotFound и состояние формы с ожидаемыми данными
    @Test
    fun `init with non-existing id sets state to NotFound`(){
        val dao = FakeAssetPackDao(listOf(samplePack))
        val viewModel = AddEditPackViewModel(
            dao = dao,
            id = 999, // несуществующий айди
            groqRepo = fakeGroqRepo
        )


        //assertEquals(PackState.NotFound("Not found"), viewModel.pack.value)
        assertTrue(viewModel.pack.value is PackState.NotFound)
        assertEquals(FormState(), viewModel.form.value)
    }

    // --- ТЕСТЫ ПОД savePack() ---

    // при сохранении нового валидного пака приходит SaveEvent.Saved и сохраняется в фейк-базе один валидный пак
    @Test
    fun `savePack for new pack inserts item to dao and emits Saved event`() = runTest {

        val dao = FakeAssetPackDao()
        val viewModel = AddEditPackViewModel(dao = dao, id = NEW_PACK_ID, groqRepo = fakeGroqRepo)

        val events = collectEvents(viewModel)

        viewModel.onNameChange("New Pack")
        viewModel.onPriceChange("15.5")

        viewModel.savePack()
        assertEquals(listOf(SaveEvent.Saved), events)

        assertEquals(1, dao.packs.size) // появился один пак
        assertEquals("New Pack", dao.packs.first().name)
        assertEquals(15.5, dao.packs.first().price, 0.0)
        assertTrue(dao.packs.first().id > 0) // айди нового сохраненного пака не должно равняться нулю
    }

    // при существующем паке ОБНОВЛЯЕТСЯ существующий пак и ничего другого в dao не добавляется
    @Test
    fun `savePack for existing pack updates item in dao and emits Saved event`() = runTest {

        val dao = FakeAssetPackDao(listOf(samplePack))
        val viewModel = AddEditPackViewModel(dao = dao, id = 1, groqRepo = fakeGroqRepo)

        val events = collectEvents(viewModel)

        viewModel.onNameChange("Updated Pack")
        viewModel.onPriceChange("20")

        viewModel.savePack()
        assertEquals(listOf(SaveEvent.Saved), events)

        assertEquals(1, dao.packs.size) // остается только один единственный пак
        assertEquals("Updated Pack", dao.packs.first().name)
        assertEquals(20.0, dao.packs.first().price, 0.0)
        assertEquals(1, dao.packs.first().id)
        assertEquals(1_000L, dao.packs.first().publishedDate)
        assertEquals("Cool 3D models", dao.packs.first().description)
        assertEquals(5, dao.packs.first().salesCount)
    }

    // при сохранении нового невалидного пака (неправильная цена) не приходит SaveEvent.Saved
    @Test
    fun `savePack for new pack with invalid price does not save and does not emit event`() = runTest {

        val dao = FakeAssetPackDao()
        val viewModel = AddEditPackViewModel(dao = dao, id = NEW_PACK_ID, groqRepo = fakeGroqRepo)

        val events = collectEvents(viewModel)

        viewModel.onNameChange("New Pack")
        viewModel.onPriceChange("qwert")

        viewModel.savePack()
        assertTrue(events.isEmpty())
        assertTrue(dao.packs.isEmpty())

        // никаких новых паков появиться не должно!

    }

    // при невалидном обновлении пака не приходит SaveEvent.Saved
    @Test
    fun `savePack for existing pack with invalid price does not save and does not emit event`() = runTest {

        val dao = FakeAssetPackDao(listOf(samplePack))
        val viewModel = AddEditPackViewModel(dao = dao, id = 1, groqRepo = fakeGroqRepo)

        val events = collectEvents(viewModel)

        viewModel.onNameChange("Updated Pack")
        viewModel.onPriceChange("qwert")

        viewModel.savePack()
        assertTrue(events.isEmpty())
        assertEquals(samplePack, dao.packs.first()) // пак остался неизменным
        assertEquals(1, dao.packs.size)

        // никаких новых паков появиться не должно!

    }

    // при сохранении нового невалидного (пустое название) пака не приходит SaveEvent.Saved
    @Test
    fun `savePack for new pack with empty name does not save and does not emit event`() = runTest {

        val dao = FakeAssetPackDao()
        val viewModel = AddEditPackViewModel(dao = dao, id = NEW_PACK_ID, groqRepo = fakeGroqRepo)

        val events = collectEvents(viewModel)

        viewModel.onNameChange("  ")
        viewModel.onPriceChange("12")

        viewModel.savePack()
        assertTrue(events.isEmpty())
        assertTrue(dao.packs.isEmpty())

        // никаких новых паков появиться не должно!

    }

    // при невалидном (неправильное имя) обновлении пака не приходит SaveEvent.Saved
    @Test
    fun `savePack for existing pack with empty name does not update dao`() = runTest {
        val dao = FakeAssetPackDao(listOf(samplePack))
        val viewModel = AddEditPackViewModel(dao, 1, fakeGroqRepo)

        val events = collectEvents(viewModel)

        viewModel.onNameChange(" ")
        viewModel.onPriceChange("20")

        viewModel.savePack()
        assertTrue(events.isEmpty())
        assertEquals(samplePack, dao.packs.first())
    }


    // --- ТЕСТЫ ПОД requestSuggestion, applySuggestion, denySuggestion, dismissMissingInfo ---

    // собирает ответ сервера вокруг текста, который "сочинила модель"
    private fun responseWith(content: String) = GroqResponse(
        choices = listOf(GroqChoice(GroqMessage(role = "assistant", content = content)))
    )



    // при успешном запросе будет ожидаемый валидный результат
    @Test
    fun `requestSuggestion shows loading and then success`() = runTest {
        // Результат с подделки приходит мгновенно, пропуская состояние SendingRequest,
        // и поэтому это состояние без некой задержки проверить нельзя.
        // Нужно, чтобы подделка ждала, пока тест её не отпустит. В корутинах есть
        // некая коробочка с результатом CompletableDeferred, которая может заполняться потом

        val gate = CompletableDeferred<GroqResponse>() // пустая коробочка
        val fakeGroqApi = FakeGroqApi{ gate.await() } // подделка ждет результат с коробочки и получает его через await()
        val repo = GroqRepository(api = fakeGroqApi, json = AppJson.json)
        val viewModel = AddEditPackViewModel(FakeAssetPackDao(), NEW_PACK_ID, repo)

        viewModel.onNameChange("Pack")
        viewModel.requestSuggestion()

        assertEquals(ListingSuggestionState.SendingRequest, viewModel.suggestion.value) // ловится промежуточное состояние!

        gate.complete(responseWith("""{"name": "New Pack", "description": "Description", "missing_info": []}"""))

        assertEquals(
            ListingSuggestionState.SuccessRequest(suggestion = ListingSuggestion(name = "New Pack", description = "Description")),
            viewModel.suggestion.value
        )

    }

    // если сервер вернул ошибку, то FailedRequest(...)
    @Test
    fun `requestSuggestion throws exception and then failed`() = runTest{
        val gate = CompletableDeferred<GroqResponse>()
        val fakeGroqApi = FakeGroqApi{ gate.await() }
        val repo = GroqRepository(api = fakeGroqApi, json = AppJson.json)
        val viewModel = AddEditPackViewModel(FakeAssetPackDao(), NEW_PACK_ID, repo)

        viewModel.onNameChange("Pack")
        viewModel.requestSuggestion()
        assertEquals(ListingSuggestionState.SendingRequest, viewModel.suggestion.value) // ловится промежуточное состояние!

        gate.completeExceptionally(exception = IOException("no network"))

        assertTrue(viewModel.suggestion.value is ListingSuggestionState.FailedRequest)
    }

    // при двойном вызове обязан вызваться только один запрос
    @Test
    fun `only one is expected when two requests are called`() = runTest {


        val gate = CompletableDeferred<GroqResponse>()
        val fakeGroqApi = FakeGroqApi{ gate.await() }
        val repo = GroqRepository(api = fakeGroqApi, json = AppJson.json)
        val viewModel = AddEditPackViewModel(FakeAssetPackDao(), NEW_PACK_ID, repo)

        viewModel.onNameChange("Pack")

        // Двойной одновременный вызов
        viewModel.requestSuggestion()
        viewModel.requestSuggestion()

        assertEquals(ListingSuggestionState.SendingRequest, viewModel.suggestion.value) // ловится промежуточное состояние!

        gate.complete(responseWith("""{"name": "New Pack", "description": "Description", "missing_info": []}"""))

        assertTrue(viewModel.suggestion.value is ListingSuggestionState.SuccessRequest)
        assertEquals(1, fakeGroqApi.callCount) // Вызов fakeGroqApi должен быть ТОЛЬКО ОДИН
    }

    // при второй попытке после ошибки результат должен быть нормальным
    @Test
    fun `second attempt after error succeeds`() = runTest {

        // тут можно и без "коробочки"
        lateinit var fakeGroqApi: FakeGroqApi
        fakeGroqApi = FakeGroqApi {
            if (fakeGroqApi.callCount == 1) throw IOException("no network") // при первом обращении кинуть ошибку
            responseWith("""{"name": "New Pack", "description": "Description", "missing_info": []}""")
        }
        val repo = GroqRepository(api = fakeGroqApi, json = AppJson.json)
        val viewModel = AddEditPackViewModel(FakeAssetPackDao(), NEW_PACK_ID, repo)
        viewModel.onNameChange("Pack")

        viewModel.requestSuggestion()
        assertTrue(viewModel.suggestion.value is ListingSuggestionState.FailedRequest) // 1й вызов - ошибка

        viewModel.requestSuggestion()
        assertTrue(viewModel.suggestion.value is ListingSuggestionState.SuccessRequest) // 2й - все нормально
        assertEquals(2, fakeGroqApi.callCount) // будет 2 вызова
    }


    private fun viewModelWithSuggestion(): AddEditPackViewModel {
        val api = FakeGroqApi { responseWith("""{"name": "New Pack", "description": "New Description", "missing_info": ["file formats"]}""") }
        val viewModel = AddEditPackViewModel(FakeAssetPackDao(), NEW_PACK_ID, GroqRepository(api, AppJson.json))
        viewModel.onNameChange("Pack")
        viewModel.requestSuggestion()
        return viewModel
    }

    // при применении подсказки НЕ МЕНЯЕТСЯ ничего другого кроме имени и описания
    @Test
    fun `applySuggestion updates only name and description while price, url and salesCount remain unchanged`() = runTest {

        val viewModel = viewModelWithSuggestion()

        viewModel.onPriceChange("10.0")
        viewModel.onDescriptionChange("Description")
        viewModel.onUrlChange("https://example.com")
        viewModel.onSalesCountChange("10")

        assertTrue(viewModel.suggestion.value is ListingSuggestionState.SuccessRequest)

        // Применяем подсказку
        viewModel.applySuggestion()
        assertTrue(viewModel.suggestion.value is ListingSuggestionState.EmptyRequest)

        val form = viewModel.form.value
        val missingInfo = viewModel.missingInfo.value
        assertEquals("New Pack", form.name)              // Изменилось
        assertEquals("New Description", form.description)// Изменилось
        assertEquals("10.0", form.price)               // Осталось прежним
        assertEquals("https://example.com", form.url) // Осталось прежним
        assertEquals("10", form.salesCount) // Осталось прежним
        assertEquals(listOf("file formats"), missingInfo) // список появился

    }

    // в состояниях EmptyRequest, SendingRequest и FailedRequest форма не меняется
    @Test
    fun `applySuggestion in non-success states does not modify form`() = runTest {
        val gate = CompletableDeferred<GroqResponse>()
        val api = FakeGroqApi {gate.await() }
        val viewModel = AddEditPackViewModel(FakeAssetPackDao(), NEW_PACK_ID, GroqRepository(api, AppJson.json))
        viewModel.onNameChange("Pack")
        val initialForm = viewModel.form.value

        assertEquals(ListingSuggestionState.EmptyRequest, viewModel.suggestion.value)
        viewModel.applySuggestion()
        assertEquals(ListingSuggestionState.EmptyRequest, viewModel.suggestion.value) // состояние должно быть тем же
        assertEquals(initialForm, viewModel.form.value) // форма не изменилась
        assertTrue(viewModel.missingInfo.value.isEmpty())

        viewModel.requestSuggestion()
        assertEquals(ListingSuggestionState.SendingRequest, viewModel.suggestion.value)
        viewModel.applySuggestion()
        assertEquals(ListingSuggestionState.SendingRequest, viewModel.suggestion.value) // состояние должно быть тем же
        assertEquals(initialForm, viewModel.form.value) // форма не изменилась
        assertTrue(viewModel.missingInfo.value.isEmpty())

        gate.completeExceptionally(IOException("err"))
        assertTrue(viewModel.suggestion.value is ListingSuggestionState.FailedRequest)
        viewModel.applySuggestion()
        assertTrue(viewModel.suggestion.value is ListingSuggestionState.FailedRequest) // состояние должно быть тем же
        assertEquals(initialForm, viewModel.form.value) // форма не изменилась
        assertTrue(viewModel.missingInfo.value.isEmpty())
    }

    // При отклонении подсказки в состоянии SuccessRequest состояние сбрасывается в EmptyRequest, а форма остается неизменной
    @Test
    fun `denySuggestion in SuccessRequest state resets suggestion to EmptyRequest without modifying form`() = runTest {
        val viewModel = viewModelWithSuggestion()
        val initialForm = viewModel.form.value

        assertTrue(viewModel.suggestion.value is ListingSuggestionState.SuccessRequest)

        viewModel.denySuggestion()

        assertEquals(ListingSuggestionState.EmptyRequest, viewModel.suggestion.value)
        assertEquals(initialForm, viewModel.form.value)
    }

    // При отклонении подсказки в состоянии SendingRequest состояние сбрасывается в EmptyRequest
    @Test
    fun `denySuggestion in SendingRequest state resets suggestion to EmptyRequest`() = runTest {
        val gate = CompletableDeferred<GroqResponse>()
        val fakeGroqApi = FakeGroqApi { gate.await() }
        val repo = GroqRepository(api = fakeGroqApi, json = AppJson.json)
        val viewModel = AddEditPackViewModel(FakeAssetPackDao(), NEW_PACK_ID, repo)

        viewModel.onNameChange("Pack")
        viewModel.requestSuggestion()

        assertEquals(ListingSuggestionState.SendingRequest, viewModel.suggestion.value)

        viewModel.denySuggestion()

        assertEquals(ListingSuggestionState.EmptyRequest, viewModel.suggestion.value)

        // сервер отвечает после того, как пользователь отклонил запрос в состоянии SendingRequest
        gate.complete(responseWith("""{"name": "New Pack", "description": "Description", "missing_info": []}"""))

        // Когда сервер ответит, корутина завершится и обновит состояние на SuccessRequest
        assertEquals(
            ListingSuggestionState.SuccessRequest(suggestion = ListingSuggestion(name = "New Pack", description = "Description")),
            viewModel.suggestion.value
        )
    }

    // При вызове dismissMissingInfo список недостающей информации очищается
    @Test
    fun `dismissMissingInfo clears missingInfo list`() = runTest {
        val viewModel = viewModelWithSuggestion()

        viewModel.applySuggestion()
        assertEquals(listOf("file formats"), viewModel.missingInfo.value)

        viewModel.dismissMissingInfo()

        assertTrue(viewModel.missingInfo.value.isEmpty())
    }
}