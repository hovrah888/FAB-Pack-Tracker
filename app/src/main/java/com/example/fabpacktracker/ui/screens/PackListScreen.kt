package com.example.fabpacktracker.ui.screens

import android.app.AlertDialog
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.fabpacktracker.data.local.AssetPack
import com.example.fabpacktracker.ui.components.PackListItem
import kotlinx.coroutines.*
import androidx.lifecycle.viewModelScope


// отдельная composable функция под диалоговое окно при удалении пака
@Composable
fun DeletePackDialog(
    packName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss, // Пользователь смахнул или кликнул вне окна
        title = {
            Text(text = "Delete pack")
        },
        text = {
            Text(text = "Do you want to delete $packName ?")
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Delete", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(24.dp),
        containerColor = MaterialTheme.colorScheme.surfaceVariant
    )
}

// есть проблема с UX: пользователь не видит, какие поля обязательные!
@Composable
fun PackListScreen(packs : List<AssetPack>, onAddClick: () -> Unit, onPackClick: (Int) -> Unit, onDeletePack: (AssetPack) -> Unit, modifier: Modifier = Modifier){

    var packToDeleteId by rememberSaveable { mutableStateOf<Int?>(null) }

    val packForDialog = packs.find { it.id == packToDeleteId }
    if (packForDialog != null) {
        DeletePackDialog(
            packName = packForDialog.name,
            onConfirm = {
                onDeletePack(packForDialog)
                packToDeleteId = null
                        },
            onDismiss = { packToDeleteId = null }
        )
    }

    Column(modifier = modifier) {
        Text(
            text = "Your Packs: ",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
               // .windowInsetsPadding(WindowInsets.statusBars)
                /*
                если у есть Scaffold (в setContent()) с применённым innerPadding,
                 не добавлять windowInsetsPadding вручную внутри контента!
                */
                .padding(16.dp)
        )
        Button(onClick = onAddClick, modifier = Modifier.padding(16.dp)) {
            Text(text = "Add pack",
                style = MaterialTheme.typography.titleMedium)
        }

        LazyColumn {
            // билдер контента LazyColumn
            items(
                packs,
                key = { pack -> pack.id }) {// используем pack id как индентификатор элемента в LazyColum
                PackListItem(pack = it, onClick = {onPackClick(it.id)} , onDeleteClick = {packToDeleteId = it.id} )
            }

        }
    }
}