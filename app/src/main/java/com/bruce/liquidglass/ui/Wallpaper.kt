package com.bruce.liquidglass.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.bruce.liquidglass.data.WallpaperKind
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin

@Immutable
data class WallpaperSpec(val kind: WallpaperKind, val photo: ImageBitmap?)

val LocalWallpaper = staticCompositionLocalOf { WallpaperSpec(WallpaperKind.LIQUID, null) }
val LocalWallTime = staticCompositionLocalOf<State<Float>> { mutableFloatStateOf(0f) }
val LocalScreenSize = staticCompositionLocalOf { Size(1080f, 2400f) }

private class Blob(
    val color: Color,
    val cx: Float, val cy: Float,      // base center, fraction of screen
    val ax: Float, val ay: Float,      // amplitude, fraction of screen
    val fx: Float, val fy: Float,      // frequency
    val phase: Float,
    val radius: Float,                 // fraction of the longest side
)

private val liquidBlobs = listOf(
    Blob(Color.White.copy(alpha = 0.30f), 0.20f, 0.18f, 0.18f, 0.10f, 0.21f, 0.17f, 0.0f, 0.55f),
    Blob(Color(0xFF9A9A9F).copy(alpha = 0.28f), 0.85f, 0.42f, 0.15f, 0.14f, 0.16f, 0.23f, 1.7f, 0.60f),
    Blob(Color.White.copy(alpha = 0.16f), 0.30f, 0.82f, 0.20f, 0.10f, 0.13f, 0.19f, 3.1f, 0.55f),
    Blob(Color(0xFF3A3A3C).copy(alpha = 0.55f), 0.65f, 0.95f, 0.18f, 0.08f, 0.19f, 0.11f, 4.4f, 0.50f),
    Blob(Color(0xFFD1D1D6).copy(alpha = 0.14f), 0.55f, 0.55f, 0.25f, 0.20f, 0.09f, 0.14f, 2.2f, 0.35f),
)

private val auroraBlobs = listOf(
    Blob(Color(0xFF2E6BFF).copy(alpha = 0.75f), 0.15f, 0.20f, 0.15f, 0.10f, 0.20f, 0.15f, 0.0f, 0.60f),
    Blob(Color(0xFF8A3FFC).copy(alpha = 0.70f), 0.85f, 0.35f, 0.14f, 0.12f, 0.15f, 0.21f, 1.3f, 0.55f),
    Blob(Color(0xFFFF4FA3).copy(alpha = 0.60f), 0.70f, 0.78f, 0.18f, 0.10f, 0.12f, 0.17f, 2.7f, 0.55f),
    Blob(Color(0xFFFF9F43).copy(alpha = 0.50f), 0.20f, 0.90f, 0.16f, 0.08f, 0.17f, 0.10f, 3.9f, 0.45f),
    Blob(Color(0xFF2EE6D6).copy(alpha = 0.35f), 0.50f, 0.50f, 0.25f, 0.18f, 0.08f, 0.13f, 5.0f, 0.35f),
)

private val nightBlobs = listOf(
    Blob(Color(0xFF2C2C2E).copy(alpha = 0.55f), 0.25f, 0.15f, 0.08f, 0.05f, 0.10f, 0.08f, 0.0f, 0.55f),
    Blob(Color(0xFF1C1C1E).copy(alpha = 0.70f), 0.80f, 0.85f, 0.08f, 0.05f, 0.07f, 0.09f, 2.0f, 0.60f),
)

/** Draws the wallpaper for a given time. Used full-screen AND inside every glass panel (as its blurred backdrop). */
fun DrawScope.drawWallpaper(spec: WallpaperSpec, t: Float, screen: Size) {
    val w = screen.width
    val h = screen.height
    when (spec.kind) {
        WallpaperKind.PHOTO -> {
            val img = spec.photo
            if (img == null) {
                drawRect(Color.Black, size = screen)
                drawBlobs(liquidBlobs, t, w, h)
            } else {
                // center-crop
                val scale = max(w / img.width, h / img.height)
                val dw = img.width * scale
                val dh = img.height * scale
                drawImage(
                    image = img,
                    srcOffset = IntOffset.Zero,
                    srcSize = IntSize(img.width, img.height),
                    dstOffset = IntOffset(((w - dw) / 2f).roundToInt(), ((h - dh) / 2f).roundToInt()),
                    dstSize = IntSize(dw.roundToInt(), dh.roundToInt()),
                )
                drawRect(Color.Black.copy(alpha = 0.12f), size = screen)
            }
        }
        WallpaperKind.LIQUID -> {
            drawRect(Color.Black, size = screen)
            drawBlobs(liquidBlobs, t, w, h)
        }
        WallpaperKind.AURORA -> {
            drawRect(Color(0xFF05060F), size = screen)
            drawBlobs(auroraBlobs, t, w, h)
            drawRect(Color.Black.copy(alpha = 0.18f), size = screen)
        }
        WallpaperKind.NIGHT -> {
            drawRect(Color.Black, size = screen)
            drawBlobs(nightBlobs, t, w, h)
        }
    }
}

private fun DrawScope.drawBlobs(blobs: List<Blob>, t: Float, w: Float, h: Float) {
    val longest = max(w, h)
    for (b in blobs) {
        val c = Offset(
            (b.cx + b.ax * sin(t * b.fx + b.phase)) * w,
            (b.cy + b.ay * cos(t * b.fy + b.phase * 1.3f)) * h,
        )
        val r = b.radius * longest * (1f + 0.08f * sin(t * 0.3f + b.phase))
        drawCircle(
            brush = Brush.radialGradient(
                0f to b.color,
                0.55f to b.color.copy(alpha = b.color.alpha * 0.45f),
                1f to Color.Transparent,
                center = c,
                radius = r,
            ),
            radius = r,
            center = c,
        )
    }
}

@Composable
fun WallpaperLayer(modifier: Modifier = Modifier) {
    val spec = LocalWallpaper.current
    val time = LocalWallTime.current
    val screen = LocalScreenSize.current
    Canvas(modifier) { drawWallpaper(spec, time.value, screen) }
}
