package com.icit.expense.core.font

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

enum class AppFont(val label: String, val fontFamily: FontFamily, val fontStyle: FontStyle = FontStyle.Normal) {
    DEFAULT("Default", FontFamily.Default),
    SERIF("Serif", FontFamily.Serif),
    SANS_SERIF("Sans Serif", FontFamily.SansSerif),
    MONOSPACE("Monospace", FontFamily.Monospace),
    CURSIVE("Cursive", FontFamily.Cursive),
    ITALIC("Italic (Serif)", FontFamily.Serif, FontStyle.Italic)
}

fun getTypography(appFont: AppFont): Typography {
    return Typography(
        bodyLarge = TextStyle(
            fontFamily = appFont.fontFamily,
            fontStyle = appFont.fontStyle,
            fontWeight = FontWeight.Normal,
            fontSize = 16.sp,
            lineHeight = 24.sp,
            letterSpacing = 0.5.sp
        ),
        titleLarge = TextStyle(
            fontFamily = appFont.fontFamily,
            fontStyle = appFont.fontStyle,
            fontWeight = FontWeight.Normal,
            fontSize = 22.sp,
            lineHeight = 28.sp,
            letterSpacing = 0.sp
        ),
        labelSmall = TextStyle(
            fontFamily = appFont.fontFamily,
            fontStyle = appFont.fontStyle,
            fontWeight = FontWeight.Medium,
            fontSize = 11.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.5.sp
        )
    )
}
