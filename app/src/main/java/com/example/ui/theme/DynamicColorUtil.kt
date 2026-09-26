package com.example.ui.theme

import android.graphics.Bitmap
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.drawable.toBitmap
import androidx.palette.graphics.Palette
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val dynamicColorCache = mutableMapOf<String, Color>()

@Composable
fun rememberDominantColor(
    imageUrl: String?,
    defaultColor: Color = LocalVaultPalette.current.cardBg
): Color {
    val context = LocalContext.current
    var dominantColor by remember(imageUrl) {
        mutableStateOf(imageUrl?.let { dynamicColorCache[it] } ?: defaultColor)
    }

    LaunchedEffect(imageUrl) {
        if (imageUrl.isNullOrBlank()) return@LaunchedEffect
        val cached = dynamicColorCache[imageUrl]
        if (cached != null) {
            dominantColor = cached
            return@LaunchedEffect
        }
        withContext(Dispatchers.IO) {
            try {
                val loader = ImageLoader(context)
                val request = ImageRequest.Builder(context)
                    .data(imageUrl)
                    .allowHardware(false)
                    .size(80, 80)
                    .build()
                val result = loader.execute(request)
                if (result is SuccessResult) {
                    val bitmap = result.drawable.toBitmap(80, 80, Bitmap.Config.ARGB_8888)
                    val palette = Palette.from(bitmap).generate()
                    val swatch = palette.dominantSwatch
                        ?: palette.vibrantSwatch
                        ?: palette.mutedSwatch
                        ?: palette.darkVibrantSwatch
                    if (swatch != null) {
                        val color = Color(swatch.rgb)
                        dynamicColorCache[imageUrl] = color
                        withContext(Dispatchers.Main) {
                            dominantColor = color
                        }
                    }
                }
            } catch (_: Exception) {
                // Keep default color on network or parsing failure
            }
        }
    }

    return dominantColor
}
