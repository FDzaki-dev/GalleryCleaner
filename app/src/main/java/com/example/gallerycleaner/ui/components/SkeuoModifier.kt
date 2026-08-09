package com.example.gallerycleaner.ui.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.gallerycleaner.ui.theme.DarkShadow
import com.example.gallerycleaner.ui.theme.LightHighlight

/**
 * §3 Membuat Modifier Skeuomorphic (Ringan & Native) — dari "Panduan
 * Lengkap: Desain Visual 'Skeuomorphism-Dark' pada Native Kotlin".
 * Dirender langsung di Canvas via `drawBehind`/`setShadowLayer` (GPU-
 * accelerated) alih-alih menumpuk banyak layer `Box` dengan blur, supaya
 * tetap ringan (§6.1/§6.2). Logic identik dengan spec — hanya `DarkShadow`/
 * `LightHighlight` diimpor dari [com.example.gallerycleaner.ui.theme]
 * (di spec asli keduanya ada di file yang sama, di sini dipisah per token
 * file agar konsisten dengan struktur `ui/theme/` project).
 */
fun Modifier.skeuomorphicDark(
    cornerRadius: Dp = 12.dp,
    elevation: Dp = 6.dp
) = this.then(
    Modifier.drawBehind {
        val cornerRadiusPx = cornerRadius.toPx()
        val elevationPx = elevation.toPx()

        drawIntoCanvas { canvas ->
            val paint = Paint()
            val frameworkPaint = paint.asFrameworkPaint()

            // 1. Gambar Drop Shadow Gelap (Bawah-Kanan)
            frameworkPaint.color = android.graphics.Color.TRANSPARENT
            frameworkPaint.setShadowLayer(
                elevationPx,
                elevationPx / 2, // Offset X
                elevationPx / 2, // Offset Y
                DarkShadow.copy(alpha = 0.8f).toArgb()
            )
            canvas.drawRoundRect(
                left = 0f, top = 0f, right = size.width, bottom = size.height,
                radiusX = cornerRadiusPx, radiusY = cornerRadiusPx,
                paint = paint
            )

            // 2. Gambar Highlight Terang (Atas-Kiri)
            frameworkPaint.setShadowLayer(
                elevationPx * 0.8f,
                -elevationPx / 2,
                -elevationPx / 2,
                LightHighlight.copy(alpha = 0.9f).toArgb()
            )
            canvas.drawRoundRect(
                left = 0f, top = 0f, right = size.width, bottom = size.height,
                radiusX = cornerRadiusPx, radiusY = cornerRadiusPx,
                paint = paint
            )
        }
    }
)
