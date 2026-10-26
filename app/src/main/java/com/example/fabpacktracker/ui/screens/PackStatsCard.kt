package com.example.fabpacktracker.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fabpacktracker.data.local.AssetPackDao
import com.example.fabpacktracker.viewmodel.PackStatsViewModel
import com.example.fabpacktracker.viewmodel.PackStatsViewModelFactory

@Composable
fun PackStatsCard(
    packsCount : Int,
    estRevenue : Int,
    modifier: Modifier = Modifier
){
    Column(modifier = modifier) {
        Text(
            text = "Your stats",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary
        )
        Card(modifier = Modifier.padding(16.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Total Packs Count:",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
                Text(
                    text = "$packsCount",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }

        }
        Card(modifier = Modifier.padding(16.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "est. Revenue:",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
                Text(
                    text = "$$estRevenue",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }

        }
    }
}

@Composable
fun PackStatsScreen(
    viewmodel: PackStatsViewModel,
    modifier: Modifier = Modifier
){
    val packsCount by viewmodel.packsCount.collectAsState()
    val estRevenue by viewmodel.totalRevenue.collectAsState()

    //PackStatsCard(packsCount = packsCount, estRevenue = estRevenue, modifier = modifier)
}