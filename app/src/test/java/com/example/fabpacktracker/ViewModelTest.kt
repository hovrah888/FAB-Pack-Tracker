package com.example.fabpacktracker

import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fabpacktracker.data.local.AssetPack
import com.example.fabpacktracker.data.remote.AppJson
import com.example.fabpacktracker.data.remote.GroqResponse
import com.example.fabpacktracker.data.repository.GroqRepository
import com.example.fabpacktracker.viewmodel.AddEditPackViewModel
import com.example.fabpacktracker.viewmodel.FormState
import com.example.fabpacktracker.viewmodel.NEW_PACK_ID
import com.example.fabpacktracker.viewmodel.PackState
import com.example.fabpacktracker.viewmodel.SaveEvent
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNull
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
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
}