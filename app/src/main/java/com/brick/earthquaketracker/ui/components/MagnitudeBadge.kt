package com.brick.earthquaketracker.ui.components

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.brick.earthquaketracker.ui.navigation.LocalAnimatedVisibilityScope
import com.brick.earthquaketracker.ui.navigation.LocalSharedTransitionScope

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun MagnitudeBadge(
    magnitude: Double?,
    modifier: Modifier = Modifier,
    isRecent: Boolean = false,
    sharedElementKey: String? = null,
) {
    val display = magnitude?.let { "%.1f".format(it) } ?: "?"
    val color = magnitudeColor(magnitude)
    val a11yLabel = buildString {
        append(magnitude?.let { "Magnitude $display" } ?: "Unknown magnitude")
        if (isRecent) append(", recent")
    }

    val sharedModifier = if (sharedElementKey != null) {
        val sharedTransitionScope = LocalSharedTransitionScope.current
        val animatedVisibilityScope = LocalAnimatedVisibilityScope.current
        if (sharedTransitionScope != null && animatedVisibilityScope != null) {
            with(sharedTransitionScope) {
                modifier.sharedElement(
                    rememberSharedContentState(key = sharedElementKey),
                    animatedVisibilityScope = animatedVisibilityScope,
                )
            }
        } else {
            modifier
        }
    } else {
        modifier
    }

    Box(
        modifier = sharedModifier.size(44.dp),
        contentAlignment = Alignment.Center,
    ) {
        // Pulse rings for recent earthquakes
        if (isRecent) {
            val transition = rememberInfiniteTransition(label = "pulse")
            val scale by transition.animateFloat(
                initialValue = 1f,
                targetValue = 1.6f,
                animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Restart),
                label = "pulseScale",
            )
            val alpha by transition.animateFloat(
                initialValue = 0.5f,
                targetValue = 0f,
                animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Restart),
                label = "pulseAlpha",
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .scale(scale)
                    .border(
                        width = 2.dp,
                        color = color.copy(alpha = alpha),
                        shape = CircleShape,
                    ),
            )
        }

        // Main circular badge
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(color, CircleShape)
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
}


fun magnitudeColor(magnitude: Double?): Color = when {
    magnitude == null -> Color(0xFF757575) // grey        — 4.6:1
    magnitude >= 7.0 -> Color(0xFFD32F2F)  // red         — 4.6:1
    magnitude >= 5.0 -> Color(0xFFE65100)  // deep orange — 4.5:1
    magnitude >= 4.0 -> Color(0xFFEF6C00)  // dark amber  — 3.3:1 (was #FBC02D / 1.8:1)
    magnitude >= 3.0 -> Color(0xFF2E7D32)  // green       — 4.3:1
    else -> Color(0xFF1565C0)              // blue        — 5.5:1
}
