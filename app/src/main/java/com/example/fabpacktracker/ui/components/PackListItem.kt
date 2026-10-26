package com.example.fabpacktracker.ui.components

import android.graphics.Color.pack
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextOverflow
import com.example.fabpacktracker.data.local.AssetPack
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.Instant
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete



fun formatDate(date: Long) : String
{
    return Instant.ofEpochMilli(date)
        .atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))

}
@Composable
fun PackListItem(pack : AssetPack, onClick: () -> Unit, onDeleteClick: () -> Unit, modifier: Modifier = Modifier) { // элемент пака для отрисовки в compose
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(20.dp), // modifier с параметра указывать только в КОРНЕ (тоеесть в card)
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface) ,
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        onClick = onClick,
    )
    {
        Column(
            modifier = Modifier
                .padding(20.dp)
        ) {
            Text(
                text = pack.name,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(
                modifier = Modifier.height(4.dp)
            )

            Text(
                text = pack.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = pack.url,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary,
            )

            Text(
                text = formatDate(pack.publishedDate),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary,
            )

            Spacer(
                modifier = Modifier.height(4.dp)
            )

            Text(
                text = "Sales: ${pack.salesCount}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary,
            )
            Text(
                text = "Est. Revenue: $ ${"%.2f".format(pack.revenue)}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(
                modifier = Modifier.height(4.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
                ) {
                Text(
                    text = "$ ${"%.2f".format(pack.price)}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.secondary,
                )

                IconButton(onClick = onDeleteClick) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "delete this pack"
                    )
                }
            }

        }
    }
}