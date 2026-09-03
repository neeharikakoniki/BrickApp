package com.brick.earthquaketracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun MagnitudeBadge(magnitude: Double?, modifier: Modifier = Modifier) {
    val display = magnitude?.let { "%.1f".format(it) } ?: "?"
    val color = magnitudeColor(magnitude)
    val a11yLabel = magnitude?.let { "Magnitude $display" } ?: "Unknown magnitude"

    Box(
        modifier = modifier
            .widthIn(min = 48.dp)
            .heightIn(min = 32.dp)
            .background(color, RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .semantics { contentDescription = a11yLabel },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = display,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.Center,
        )
    }
}


fun magnitudeColor(magnitude: Double?): Color = when {
    magnitude == null -> Color(0xFF757575) // grey        — 4.6:1
    magnitude >= 7.0 -> Color(0xFFD32F2F)  // red         — 4.6:1
    magnitude >= 5.0 -> Color(0xFFE65100)  // deep orange — 4.5:1
    magnitude >= 4.0 -> Color(0xFFEF6C00)  // dark amber  — 3.3:1 (was #FBC02D / 1.8:1)
    magnitude >= 3.0 -> Color(0xFF2E7D32)  // green       — 4.3:1
    else -> Color(0xFF1565C0)              // blue        — 5.5:1
}
