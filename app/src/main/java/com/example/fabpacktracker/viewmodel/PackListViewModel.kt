package com.example.fabpacktracker.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.fabpacktracker.data.local.AssetPack
import com.example.fabpacktracker.data.local.AssetPackDao
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// view model для списка паков
// источником правды является напосредствнно сама бд, управляемая через интерфейc dao
class PackListViewModel(private val dao: AssetPackDao) : ViewModel() {
    val packs: StateFlow<List<AssetPack>> = dao.getAllPacks()
        // нужно превратить холодный флоу в горячий стейт флоу, чтобы исключить постоянные подписки
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())


    fun deletePack(pack: AssetPack) {
        viewModelScope.launch {
            dao.deletePack(pack)
        }
    }

    fun insertPack(pack: AssetPack) {
        viewModelScope.launch {
            dao.insertPack(pack)
        }
    }
}

class PackListViewModelFactory(private val dao: AssetPackDao) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PackListViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PackListViewModel(dao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}