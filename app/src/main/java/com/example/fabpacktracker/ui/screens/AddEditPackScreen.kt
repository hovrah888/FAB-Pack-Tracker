package com.example.fabpacktracker.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.fabpacktracker.viewmodel.AddEditPackViewModel
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.fabpacktracker.viewmodel.FormState
import com.example.fabpacktracker.viewmodel.ListingSuggestionState
import com.example.fabpacktracker.viewmodel.PackState
import com.example.fabpacktracker.viewmodel.SaveEvent

@Composable
fun SuggestionDialog( // диалог подтверждения новой информации от ии
    name: String,
    description : String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AlertDialog(
        modifier = modifier,

        onDismissRequest = onDismiss, // Пользователь смахнул или кликнул вне окна
        title = {
            Text(text = "Apply AI edit")
        },
        text = {
            Column(modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())) // для скролла длинного текста
            {
                Text(
                    text = "Do you want to apply new data?",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(8.dp))
                Text(
                    text = "New name:",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = name,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Text(
                    text = "New description:",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Apply")
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
@Composable
fun AddEditPackContent(
    form: FormState,
    suggestion: ListingSuggestionState,
    missingInfo: List<String>,
    onNameChange: (String) -> Unit,
    onPriceChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onUrlChange: (String) -> Unit,
    onSalesCountChange: (String) -> Unit,
    onRequestSuggestion: () -> Unit,
    onApplySuggestion: () -> Unit,
    onDenySuggestion: () -> Unit,
    onDismissMissingInfo: () -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier
        .fillMaxWidth()
        .verticalScroll(rememberScrollState())
    )
    {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        )
        {
            Text(
                text = "Create or Edit pack",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            )
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ){
            Column(modifier = Modifier.padding(16.dp)) {

                Button(
                    onClick = onRequestSuggestion,
                    enabled = form.name.isNotBlank() && suggestion !is ListingSuggestionState.SendingRequest
                ) {
                    if (suggestion is ListingSuggestionState.SendingRequest) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Improving...")
                    } else {
                        Text("Improve with AI")
                    }
                }

                if (suggestion is ListingSuggestionState.FailedRequest) { // сообщение при ошибке
                    Text(
                        text = suggestion.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier
                            .padding(20.dp)
                    )
                }

                if (suggestion is ListingSuggestionState.SuccessRequest){
                    SuggestionDialog(
                        name = suggestion.suggestion.name,
                        description = suggestion.suggestion.description,
                        onConfirm = onApplySuggestion,
                        onDismiss = onDenySuggestion
                        )
                }

                OutlinedTextField(
                    value = form.name,
                    onValueChange = onNameChange,
                    label = { Text(text = "Name") }
                )
                OutlinedTextField(
                    value = form.url,
                    onValueChange = onUrlChange,
                    label = { Text(text = "URL") }
                )
                OutlinedTextField(
                    value = form.description,
                    onValueChange = onDescriptionChange,
                    label = { Text(text = "Description") }
                )
                OutlinedTextField(
                    value = form.price,
                    onValueChange = onPriceChange,
                    label = { Text(text = "Price") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                OutlinedTextField(
                    value = form.salesCount,
                    onValueChange = onSalesCountChange,
                    label = { Text(text = "Sales Count") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                Button(onClick = onSave, enabled = form.isValid) {
                    Text(text = "Save pack")
                }
            }
            }

        if (missingInfo.isNotEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiary),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            )
            {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Missing Information:",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onTertiary
                            )
                        }
                        IconButton(onClick = onDismissMissingInfo) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "dismiss missing info"
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    missingInfo.forEach { item ->
                        Text(
                            text = "- $item",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onTertiary,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }

}


@Composable
fun AddEditPackScreen(
    viewModel: AddEditPackViewModel,
    onSaved: () -> Unit,
    modifier: Modifier = Modifier,
)
{
    val form by viewModel.form.collectAsStateWithLifecycle()
    val pack by viewModel.pack.collectAsStateWithLifecycle()
    val suggestion by viewModel.suggestion.collectAsStateWithLifecycle()
    val missingInfo by viewModel.missingInfo.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                SaveEvent.Saved -> onSaved()
            }
        }
    }
    Box(modifier = modifier.fillMaxSize()) {
        when (pack) {
            is PackState.Loading -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }

            is PackState.New, is PackState.Found -> {
                AddEditPackContent(
                    onSave = viewModel::savePack,
                    form = form,
                    suggestion = suggestion,
                    missingInfo = missingInfo,
                    onNameChange = viewModel::onNameChange,
                    onPriceChange = viewModel::onPriceChange,
                    onDescriptionChange = viewModel::onDescriptionChange,
                    onUrlChange = viewModel::onUrlChange,
                    onSalesCountChange = viewModel::onSalesCountChange,
                    onRequestSuggestion = viewModel::requestSuggestion,
                    onApplySuggestion = viewModel::applySuggestion,
                    onDenySuggestion = viewModel::denySuggestion,
                    onDismissMissingInfo = viewModel::dismissMissingInfo,
                    modifier = modifier,

                )
            }

            is PackState.NotFound -> {
                Text(
                    text = "Pack not found",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                )
            }
        }
    }


}