package com.example.fabpacktracker.viewmodel

import androidx.datastore.preferences.protobuf.DoubleValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.fabpacktracker.data.local.AssetPack
import com.example.fabpacktracker.data.local.AssetPackDao
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.count
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PackStatsViewModel(private val dao: AssetPackDao) : ViewModel(){

    val packsCount = dao.getAllPacks()
        .map { packs ->
            packs.size
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)


    val totalRevenue = dao.getAllPacks()
        .map { packs ->
            packs.sumOf { it.revenue }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)


    /* тут типы подтягиваются правильно, но нету map
    val packsCount : Int = dao.getAllPacks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
        .value.size

    val totalRevenue : Double = dao.getAllPacks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
        .value.sumOf { it.price }
     */
}

class PackStatsViewModelFactory(private val dao: AssetPackDao) : ViewModelProvider.Factory{
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PackStatsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PackStatsViewModel(dao) as T
        }
        throw IllegalArgumentException("Unknown class")
    }
}