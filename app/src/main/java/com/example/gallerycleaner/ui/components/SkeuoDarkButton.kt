package com.example.gallerycleaner.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gallerycleaner.ui.theme.AccentNeon
import com.example.gallerycleaner.ui.theme.metallicDarkBrush

/**
 * §5 Implementasi: Membuat Tombol Realistis — dari "Panduan Lengkap: Desain
 * Visual 'Skeuomorphism-Dark' pada Native Kotlin". Menggabungkan
 * [Modifier.skeuomorphicDark] (§3, drop-shadow + highlight) dengan
 * [metallicDarkBrush] (§4) jadi tombol yang terlihat seperti objek fisik.
 *
 * Catatan dari spec asli (dipertahankan apa adanya): `isPressed` di sini
 * hanya state placeholder untuk animasi tekan — belum ada `pointerInput`
 * yang mendeteksi `ACTION_DOWN`/`ACTION_UP` sungguhan, jadi elevasi/warna
 * "pressed" tidak akan berubah dari sekadar `.clickable {}` saat ini.
 * Menambahkan deteksi tekan nyata adalah pekerjaan terpisah (lihat
 * PROJECT_STATE), bukan bagian dari override tema 100%-sesuai-spec ini.
 */
@Composable
fun SkeuoDarkButton(
    text: String,
    onClick: () -> Unit
) {
    // State untuk animasi tekanan (opsional namun menambah kesan realistis)
    var isPressed by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .padding(16.dp)
            .size(width = 160.dp, height = 60.dp)
            // Kurangi elevasi saat ditekan untuk efek masuk ke dalam
            .skeuomorphicDark(
                cornerRadius = 16.dp,
                elevation = if (isPressed) 2.dp else 8.dp
            )
            .background(
                brush = metallicDarkBrush,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable { onClick() } // Dalam implementasi nyata, gunakan pointerInput untuk mendeteksi ACTION_DOWN/UP
    ) {
        Text(
            text = text,
            color = if (isPressed) AccentNeon else Color.LightGray, // Warna neon menyala saat ditekan
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.Center)
        )
    }
}
