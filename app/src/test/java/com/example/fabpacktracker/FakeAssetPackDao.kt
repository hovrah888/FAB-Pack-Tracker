package com.example.fabpacktracker

import com.example.fabpacktracker.data.local.AssetPack
import com.example.fabpacktracker.data.local.AssetPackDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import java.util.Collections.emptyList


class FakeAssetPackDao(initialPacks: List<AssetPack> = emptyList()) : AssetPackDao {

    // тут храняться данные (сортировка по дате идет!)
    private val _packsFlow = MutableStateFlow<List<AssetPack>>(initialPacks.sortedByDescending { it.publishedDate })

    val packs: List<AssetPack>
        get() = _packsFlow.value

    override fun getAllPacks(): Flow<List<AssetPack>> {
        return _packsFlow // озвращаем StateFlow, который является наследником Flow
    }

    override suspend fun getPackById(packId: Int): AssetPack? {
        return _packsFlow.value.find { it.id == packId }
    }

    override suspend fun insertPack(pack: AssetPack): Long {

        // Room при 0 айди (новый пак) выдает новый свободный
        val newId = if (pack.id == 0) {
            // нужно учитывать и null!
            (_packsFlow.value.maxOfOrNull { it.id } ?: 0 ) + 1
        } else {
            pack.id
        }

        if (_packsFlow.value.any { it.id == newId }) {
            throw IllegalStateException("Pack already exists")
        }

        //если пришёл id = 0, в список кладётся копия с выданным номером, а если номер уже задан, пак кладётся как есть.
        val packToInsert = if (pack.id == 0) {
            pack.copy(id = newId)
        } else {
            pack
        }

        _packsFlow.value = (_packsFlow.value + packToInsert)
            .sortedByDescending { it.publishedDate } // сортировка как в dao

        return pack.id.toLong()
    }

    override suspend fun updatePack(pack: AssetPack) {
        _packsFlow.value = _packsFlow.value.map {
            if (it.id == pack.id) pack else it
        }.sortedByDescending { it.publishedDate }
    }

    override suspend fun deletePack(pack: AssetPack) {
        _packsFlow.value = _packsFlow.value.filter {
            it.id != pack.id
        }
    }
}
