package com.brick.earthquaketracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun MagnitudeBadge(magnitude: Double?, modifier: Modifier = Modifier) {
    val display = magnitude?.let { "%.1f".format(it) } ?: "?"
    val color = magnitudeColor(magnitude)

    Box(
        modifier = modifier
            .widthIn(min = 48.dp)
            .background(color, RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
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

private fun magnitudeColor(magnitude: Double?): Color = when {
    magnitude == null -> Color(0xFF9E9E9E)
    magnitude >= 7.0 -> Color(0xFFD32F2F)
    magnitude >= 5.0 -> Color(0xFFF57C00)
    magnitude >= 4.0 -> Color(0xFFFBC02D)
    magnitude >= 3.0 -> Color(0xFF388E3C)
    else -> Color(0xFF1976D2)
}
