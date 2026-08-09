package com.example.gallerycleaner.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.gallerycleaner.ui.theme.SkeuoMidnightTheme

/**
 * §4B Container Input Cekung, dari "Panduan Lengkap: Desain Visual
 * Skeuomorphism-Dark (Midnight Blue Edition)". Logic 100% copy dari
 * spec, hanya package yang diadaptasi. Dipakai untuk area nilai, card,
 * atau tempat input teks agar terlihat terbenam ke dalam panel.
 */
@Composable
fun MidnightSkeuoSlot(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .padding(12.dp)
            .skeuoMidnightDebossed(
                cornerRadius = 16.dp,
                depth = 6.dp
            )
            .background(
                brush = SkeuoMidnightTheme.InsetGradient,
                shape = RoundedCornerShape(16.dp)
            ),
        content = content
    )
}
