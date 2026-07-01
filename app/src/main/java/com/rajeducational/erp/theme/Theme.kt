package com.rajeducational.erp.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object AppColors {
    val Student = Color(0xFF378ADD)
    val Teacher = Color(0xFF1D9E75)
    val Guest = Color(0xFFBA7517)
    val Admin = Color(0xFFD85A30)
    val Director = Color(0xFF8B5CF6)
    val Navy = Color(0xFF1A3A5C)
    val Background = Color(0xFFF0F2F5)
    val CardBg = Color.White
    val TextPrimary = Color(0xFF333333)
    val TextSecondary = Color(0xFF888888)
    val Success = Color(0xFF2E7D32)
    val Error = Color(0xFFE53935)
    val Warning = Color(0xFFF57C00)
    val StarYellow = Color(0xFFF59E0B)
}

private val LightColorScheme = lightColorScheme(
    primary = AppColors.Navy,
    secondary = AppColors.Student,
    background = AppColors.Background,
    surface = AppColors.CardBg,
    error = AppColors.Error,
)

@Composable
fun ERPTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        content = content
    )
}
