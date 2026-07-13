package com.otpbox.ui.theme

import androidx.compose.ui.graphics.Color

val BrandPrimary = Color(0xFF5B5BD6)
val BrandPrimaryDark = Color(0xFFBCB9FF)
val BrandSecondary = Color(0xFF5D5D72)
val BrandTertiary = Color(0xFF7A5368)

val LightBackground = Color(0xFFFBF8FF)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceVariant = Color(0xFFE9E7F1)

val DarkBackground = Color(0xFF121218)
val DarkSurface = Color(0xFF1C1B22)
val DarkSurfaceVariant = Color(0xFF47464F)

val CodeGreen = Color(0xFF2E7D32)
val CodeGreenDark = Color(0xFF7BD88F)
val WarnAmber = Color(0xFFE08A00)

val AvatarColors = listOf(
    Color(0xFF5B5BD6),
    Color(0xFF00897B),
    Color(0xFFD81B60),
    Color(0xFF3949AB),
    Color(0xFF00838F),
    Color(0xFF6D4C41),
    Color(0xFF8E24AA),
    Color(0xFFEF6C00),
    Color(0xFF43A047),
    Color(0xFF1E88E5)
)

fun avatarColorFor(key: String): Color {
    if (key.isEmpty()) return AvatarColors.first()
    val index = (key.hashCode() and 0x7FFFFFFF) % AvatarColors.size
    return AvatarColors[index]
}
