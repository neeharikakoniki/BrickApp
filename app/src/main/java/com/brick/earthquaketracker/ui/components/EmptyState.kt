package com.brick.earthquaketracker.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.FilterAlt
import androidx.compose.material.icons.outlined.Landscape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.brick.earthquaketracker.ui.list.EmptyReason

@Composable
fun EmptyState(
    reason: EmptyReason,
    modifier: Modifier = Modifier,
    onClearFilter: (() -> Unit)? = null,
    onRetry: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val (icon, title, subtitle) = when (reason) {
            EmptyReason.NO_DATA -> Triple(
                Icons.Outlined.Landscape,
                "No earthquakes recorded",
                "Pull to refresh to check again.",
            )
            EmptyReason.NO_RESULTS_FOR_FILTER -> Triple(
                Icons.Outlined.FilterAlt,
                "No results",
                "Try adjusting your filter or sort settings.",
            )
            EmptyReason.NO_CACHE_OFFLINE -> Triple(
                Icons.Outlined.CloudOff,
                "You're offline",
                "Connect to the internet and pull to refresh.",
            )
        }

        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        if (reason == EmptyReason.NO_RESULTS_FOR_FILTER && onClearFilter != null) {
            Spacer(Modifier.height(16.dp))
            TextButton(onClick = onClearFilter) {
                Text("Clear filter")
            }
        }

        if (reason == EmptyReason.NO_CACHE_OFFLINE && onRetry != null) {
            Spacer(Modifier.height(16.dp))
            TextButton(onClick = onRetry) {
                Text("Retry")
            }
        }
    }
}
