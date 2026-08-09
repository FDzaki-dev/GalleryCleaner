package com.example.gallerycleaner.ui.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.gallerycleaner.ui.theme.SkeuoMidnightTheme

/**
 * §3 Implementasi Modifier Kustom (Low-Level Canvas), dari
 * "Panduan Lengkap: Desain Visual Skeuomorphism-Dark (Midnight Blue
 * Edition)". Logic 100% copy dari spec — satu-satunya perubahan adalah
 * package (spec asli `com.example.skeuomorphism.ui.modifier`, project
 * ini menaruh modifier bersama komponen di `ui/components/`, konsisten
 * dengan struktur `SkeuoModifier.kt` versi lama sebelum dihapus batch
 * ini) dan import token diarahkan ke `SkeuoMidnightTheme` (objek),
 * bukan top-level `val` seperti spec generik sebelumnya — sengaja
 * dinamespace supaya tidak bentrok nama dengan token tema lain.
 */

/** Modifier untuk komponen TIMBUL (Raised / Convex) */
fun Modifier.skeuoMidnightRaised(
    cornerRadius: Dp = 16.dp,
    elevation: Dp = 8.dp
) = this.then(
    Modifier.drawBehind {
        val cornerRadiusPx = cornerRadius.toPx()
        val elevationPx = elevation.toPx()

        drawIntoCanvas { canvas ->
            val paint = Paint()
            val frameworkPaint = paint.asFrameworkPaint()

            // 1. Drop Shadow Gelap (Bawah-Kanan)
            frameworkPaint.color = android.graphics.Color.TRANSPARENT
            frameworkPaint.setShadowLayer(
                elevationPx,
                elevationPx / 2f,
                elevationPx / 2f,
                SkeuoMidnightTheme.DarkShadow.copy(alpha = 0.9f).toArgb()
            )
            canvas.drawRoundRect(
                left = 0f, top = 0f, right = size.width, bottom = size.height,
                radiusX = cornerRadiusPx, radiusY = cornerRadiusPx,
                paint = paint
            )

            // 2. Specular Light Highlight (Atas-Kiri)
            frameworkPaint.setShadowLayer(
                elevationPx * 0.75f,
                -elevationPx / 2f,
                -elevationPx / 2f,
                SkeuoMidnightTheme.LightHighlight.copy(alpha = 0.85f).toArgb()
            )
            canvas.drawRoundRect(
                left = 0f, top = 0f, right = size.width, bottom = size.height,
                radiusX = cornerRadiusPx, radiusY = cornerRadiusPx,
                paint = paint
            )
        }
    }
)

/** Modifier untuk komponen CEKUNG / INSET (Debossed / Concave) */
fun Modifier.skeuoMidnightDebossed(
    cornerRadius: Dp = 16.dp,
    depth: Dp = 6.dp
) = this.then(
    Modifier.drawBehind {
        val cornerRadiusPx = cornerRadius.toPx()
        val depthPx = depth.toPx()

        drawIntoCanvas { canvas ->
            val paint = Paint()
            val frameworkPaint = paint.asFrameworkPaint()

            // 1. Inner Dark Shadow (Simulasi Cekungan Atas-Kiri)
            frameworkPaint.color = android.graphics.Color.TRANSPARENT
            frameworkPaint.setShadowLayer(
                depthPx,
                depthPx / 3f,
                depthPx / 3f,
                SkeuoMidnightTheme.InnerShadowDark.copy(alpha = 0.95f).toArgb()
            )
            canvas.drawRoundRect(
                left = 0f, top = 0f, right = size.width, bottom = size.height,
                radiusX = cornerRadiusPx, radiusY = cornerRadiusPx,
                paint = paint
            )

            // 2. Inner Light Edge (Simulasi Refleksi Bawah-Kanan)
            frameworkPaint.setShadowLayer(
                depthPx * 0.5f,
                -depthPx / 3f,
                -depthPx / 3f,
                SkeuoMidnightTheme.InnerShadowLight.copy(alpha = 0.6f).toArgb()
            )
            canvas.drawRoundRect(
                left = 0f, top = 0f, right = size.width, bottom = size.height,
                radiusX = cornerRadiusPx, radiusY = cornerRadiusPx,
                paint = paint
            )
        }
    }
)
