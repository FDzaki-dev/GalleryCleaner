package com.example.gallerycleaner.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gallerycleaner.ui.theme.SkeuoMidnightTheme

/**
 * §4A Tombol Timbul Interaktif, dari "Panduan Lengkap: Desain Visual
 * Skeuomorphism-Dark (Midnight Blue Edition)". Logic 100% copy dari
 * spec, hanya package yang diadaptasi.
 */
@Composable
fun MidnightSkeuoButton(
    text: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // Animasi elevasi saat ditekan (simulasi sakelar/tombol fisik)
    val currentElevation = if (isPressed) 2.dp else 8.dp

    Box(
        modifier = modifier
            .padding(12.dp)
            .height(56.dp)
            .skeuoMidnightRaised(
                cornerRadius = 16.dp,
                elevation = currentElevation
            )
            .background(
                brush = SkeuoMidnightTheme.RaisedGradient,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null, // Mengabaikan ripple bawaan Material agar rasa taktil fisik terasa murni
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (isPressed) SkeuoMidnightTheme.ElectricCyan else SkeuoMidnightTheme.TextMuted,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.5.sp
        )
    }
}
