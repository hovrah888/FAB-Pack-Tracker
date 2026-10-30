package com.example.fabpacktracker.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.fabpacktracker.data.local.AssetPack
import com.example.fabpacktracker.data.local.AssetPackDao
import com.example.fabpacktracker.data.model.ListingSuggestion
import com.example.fabpacktracker.data.repository.GroqRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/*
ViewModel ДЛЯ ПОЛЯ СОХРАНЕНИЯ/РЕДАКТИРОВАНИЯ ФОРМЫ
 */

const val NEW_PACK_ID = -1 // айди, которое будет для нового пака, у котрого айди еще нету

// отвечает на вопрос «что сейчас набрано в полях».
// Всё строками, потому что пользователь печатает текст.
// Умеет сам себя проверять (isValid) и разбирать цену (priceValue).
data class FormState(
    val name : String = "",
    val price : String = "",
    val description : String = "",
    val url : String = "",
    val salesCount : String = ""

){
    val isValid: Boolean
        get() = name.isNotBlank() && priceValue != null && salesCountValue != null

    val priceValue: Double?
        get() = price.replace(',', '.').toDoubleOrNull()?.takeIf { it >= 0 }

    val salesCountValue: Int?
        get() = if(salesCount.isBlank()) 0 else salesCount.toIntOrNull()?.takeIf { it >= 0 }
}

//твечает на вопрос «что загружено из базы»: Loading, New (режим создания),
// Found (оригинал на руках), NotFound.
sealed class PackState{
    data object Loading : PackState()
    data object New : PackState()
    data class Found(val pack: AssetPack) : PackState()
    data class NotFound(val message: String) : PackState()
}

// что с подсказочкой ии
sealed class ListingSuggestionState{
    data object EmptyRequest : ListingSuggestionState()
    data object SendingRequest : ListingSuggestionState()
    data class SuccessRequest(val suggestion: ListingSuggestion) : ListingSuggestionState()
    data class FailedRequest(val message: String) : ListingSuggestionState()
}

//это одноразовые сообщения экрану.
sealed class SaveEvent {
    data object Saved : SaveEvent()

    // как я понимаю, тут дальше будут еще объекты (для ошибки)
}

// сам view model
class AddEditPackViewModel(
    private val dao : AssetPackDao,
    private val id : Int, // айдишник пака под редактирование
    private val groqRepo : GroqRepository
) : ViewModel(){

    private val _pack = MutableStateFlow<PackState>(PackState.Loading)
    val pack : StateFlow<PackState> = _pack.asStateFlow()

    private val _form = MutableStateFlow<FormState>(FormState())
    val form : StateFlow<FormState> = _form.asStateFlow()

    private val _suggestion = MutableStateFlow<ListingSuggestionState>(ListingSuggestionState.EmptyRequest)
    val suggestion : StateFlow<ListingSuggestionState> = _suggestion.asStateFlow()

    // для недостающей информации из подсказки ии
    private val _missingInfo = MutableStateFlow<List<String>>(emptyList())
    val missingInfo : StateFlow<List<String>> = _missingInfo.asStateFlow()

    private val _events = Channel<SaveEvent>() // канал (чтото вродее сигнала), который будет отправлятся при сохранении и читаться в UI
    val events = _events.receiveAsFlow()

    init { // подгрузка значений в форму на начале (init)
        viewModelScope.launch {

            if (id == NEW_PACK_ID){ // если пак будет новым, то ничего не подгружаем, а создаем
                _pack.value = PackState.New
            }else{
                val loadedPack = dao.getPackById(id)

                if (loadedPack != null){
                    _pack.value = PackState.Found(loadedPack)
                    _form.value = FormState(
                        name = loadedPack.name,
                        price = loadedPack.price.toString(),
                        description = loadedPack.description,
                        url = loadedPack.url,
                        salesCount = loadedPack.salesCount.toString()
                    )
                }else{
                    _pack.value = PackState.NotFound("Not found")
                }
            }
        }
    }

    fun savePack(){ // функция сохранения значений
        val form = _form.value
        val price = form.priceValue ?: return // подмена запятой в значении цены(double) на точку
        val salesCount = form.salesCountValue ?: return
        viewModelScope.launch {
            when(val state = _pack.value){ // smart cast (обязательно)
                PackState.New -> { // при новом паке вызывается insertPack у dao
                    dao.insertPack(
                        AssetPack(
                            name = form.name,
                            price = price,
                            url = form.url,
                            description = form.description,
                            salesCount = salesCount
                        )
                    )
                    _events.send(SaveEvent.Saved) // отправка канала
                }

                is PackState.Found -> { // развлетвение для перезаписи существующего пака
                    dao.updatePack(state.pack.copy(
                        name = form.name,
                        price = price,
                        url = form.url,
                        description = form.description,
                        salesCount = salesCount
                    ))
                    _events.send(SaveEvent.Saved)
                }


                else -> {
                    // _events.send(SaveEvent.NotSaved) // условно
                }
            }
        }
    }

    fun requestSuggestion(){
        // name и description будут браться с _form
        val name = _form.value.name
        val description = _form.value.description

        if (name.isBlank()) return
        if (_suggestion.value == ListingSuggestionState.SendingRequest) {
            return
        }
        _suggestion.value = ListingSuggestionState.SendingRequest

        viewModelScope.launch {
            val result = groqRepo.improveListing(
                name = name,
                description = description
            )
            result
                .onSuccess { suggestion ->
                    _suggestion.value = ListingSuggestionState.SuccessRequest(suggestion)
                }
                .onFailure { suggestion ->
                    _suggestion.value = ListingSuggestionState.FailedRequest("Some error has occurred :(")
                    Log.e("GroqSuggestion", "${suggestion.message}")
                }

        }
    }

    fun applySuggestion(){

        when(val state = _suggestion.value){
            is ListingSuggestionState.SuccessRequest -> {
                _form.update { it.copy(name = state.suggestion.name, description = state.suggestion.description) }

                _suggestion.value = ListingSuggestionState.EmptyRequest // сброс предложения после применения
                _missingInfo.value = state.suggestion.missing

            }

            else -> {}
        }

    }

    fun denySuggestion(){
        _suggestion.value = ListingSuggestionState.EmptyRequest
    }

    fun dismissMissingInfo(){
        _missingInfo.value = emptyList()
    }

    // колбеки (реализация state hoisting)
    fun onNameChange(newName: String) {
        _form.update { it.copy(name = newName) }
    }
    fun onPriceChange(newPrice: String) {
        _form.update { it.copy(price = newPrice) }
    }
    fun onDescriptionChange(newDescription: String) {
        _form.update { it.copy(description = newDescription) }
    }
    fun onUrlChange(newUrl: String) {
        _form.update { it.copy(url = newUrl) }
    }
    fun onSalesCountChange(newSalesCount: String){
        _form.update { it.copy(salesCount = newSalesCount) }
    }

}

// фабрика, ведь во view model есть параметры
class AddEditPackViewModelFactory (
    private val dao : AssetPackDao,
    private val id : Int,
    private val groqRepo : GroqRepository
) : ViewModelProvider.Factory{
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        if (modelClass.isAssignableFrom(AddEditPackViewModel::class.java)){
            @Suppress("UNCHECKED_CAST")
            return AddEditPackViewModel(dao, id, groqRepo) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")

    }
}
