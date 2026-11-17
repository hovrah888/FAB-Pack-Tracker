package com.example.fabpacktracker

import com.example.fabpacktracker.data.local.AssetPack
import com.example.fabpacktracker.viewmodel.PackListViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test


class PackListViewModelTest {
    @get:Rule val mainRule = MainDispatcherRule()

    private fun TestScope.collectPacks(viewModel: PackListViewModel){ // ф-ция для создания подписчика
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.packs.collect{} // пока нет подписчика у packs, то он будет равен emptyList()  (изза SharingStarted.WhileSubscribed)
            // поэтому здесь нужна подписка
        }
    }

    private val samplePack = AssetPack(
        id = 1,
        name = "Sci-Fi Weapons",
        price = 19.99,
        description = "Cool 3D models",
        salesCount = 5,
        publishedDate = 1_000L // старее
    )

    private val newSamplePack = AssetPack(
        id = 2,
        name = "Sci-Fi Buildings",
        price = 39.99,
        description = "Cool 3D models",
        salesCount = 7,
        publishedDate = 2_000L // новее
    )

    val dao = FakeAssetPackDao(listOf(samplePack))

    // при отсутствии подписчиков список паков будет пуст (особенность с работой stateIn с SharingStarted.WhileSubscribed)
    @Test
    fun `pack list without collectors is empty`() = runTest {
        val viewModel = PackListViewModel(dao)
        assertTrue(viewModel.packs.value.isEmpty()) // viewModel.packs пуст
        assertEquals(1, dao.packs.size) // пока dao имеет в себе пак
    }

    // при наличии подписчика список паков будет отображаться
    @Test
    fun `pack list with collectors is visible`() = runTest {
        val viewModel = PackListViewModel(dao)
        collectPacks(viewModel)
        assertEquals(listOf(samplePack), viewModel.packs.value)
    }

    // тест на реактивность: при добавлении нового элемента непосредственно в базу packs обновиться сам
    @Test
    fun `when a new item is added to dao, it visible in pack list`() = runTest {
        val viewModel = PackListViewModel(dao)
        collectPacks(viewModel)
        assertEquals(listOf(samplePack), viewModel.packs.value) // 1 пак
        dao.insertPack(newSamplePack)
        assertEquals(listOf(newSamplePack, samplePack), viewModel.packs.value) // 2 пака
        // именно DAO обеспечивает порядок элементов в списке!
    }

    // тест на реактивность: при удалении из базы элемента packs подхватит изменения сам
    @Test
    fun `when an item is deleted from dao, it disappears from pack list`() = runTest {
        val viewModel = PackListViewModel(dao)
        dao.insertPack(newSamplePack)
        collectPacks(viewModel)
        assertEquals(listOf(newSamplePack, samplePack), viewModel.packs.value) // 2 пака
        dao.deletePack(samplePack)
        assertEquals(listOf(newSamplePack), viewModel.packs.value) // 1 пак
    }

    // добавление через ViewModel проверять смысла нет, ведь оно нигде не используется, как оказывается (уже его и нету в самом viewmodel)

    // удаления пака через ViewModel
    @Test
    fun `when deletePack is called on viewModel, it disappears from pack list`() = runTest {
        val viewModel = PackListViewModel(dao)
        dao.insertPack(newSamplePack)
        collectPacks(viewModel)

        viewModel.deletePack(samplePack)

        assertEquals(listOf(newSamplePack), viewModel.packs.value)
    }

}