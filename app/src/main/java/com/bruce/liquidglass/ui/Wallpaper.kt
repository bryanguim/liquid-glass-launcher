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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.bruce.liquidglass.data.WallpaperKind
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin

@Immutable
data class WallpaperSpec(val kind: WallpaperKind, val photo: ImageBitmap?, val dim: Float = 0f)

val LocalWallpaper = staticCompositionLocalOf { WallpaperSpec(WallpaperKind.LIQUID, null) }
val LocalWallTime = staticCompositionLocalOf<State<Float>> { mutableFloatStateOf(0f) }
val LocalScreenSize = staticCompositionLocalOf { Size(1080f, 2400f) }

/** One shared clock so the full-screen wallpaper and the blurred glass backdrop stay in sync. */
object WallClock {
    val epoch: Long = System.nanoTime()
    const val STATIC_T = 6f
    fun at(nanos: Long, speed: Float): Float = (nanos - epoch) / 1_000_000_000f * 0.5f * speed
}

class Blob(
    val color: Int,                    // ARGB
    val cx: Float, val cy: Float,      // base center, fraction of screen
    val ax: Float, val ay: Float,      // amplitude, fraction of screen
    val fx: Float, val fy: Float,      // frequency
    val phase: Float,
    val radius: Float,                 // fraction of the longest side
) {
    fun x(t: Float, w: Float) = (cx + ax * sin(t * fx + phase)) * w
    fun y(t: Float, h: Float) = (cy + ay * cos(t * fy + phase * 1.3f)) * h
    fun r(t: Float, longest: Float) = radius * longest * (1f + 0.08f * sin(t * 0.3f + phase))
}

private fun argb(a: Float, rgb: Long): Int = ((a * 255).roundToInt() shl 24) or (rgb and 0xFFFFFF).toInt()

object WallpaperModel {
    fun base(kind: WallpaperKind): Int = when (kind) {
        WallpaperKind.AURORA -> 0xFF05060F.toInt()
        WallpaperKind.OCEAN -> 0xFF020814.toInt()
        WallpaperKind.SUNSET -> 0xFF120508.toInt()
        else -> 0xFF000000.toInt()
    }

    /** Extra darkening applied on top of a preset (on top of the user's dim). */
    fun shade(kind: WallpaperKind): Float = when (kind) {
        WallpaperKind.AURORA, WallpaperKind.SUNSET -> 0.18f
        WallpaperKind.OCEAN -> 0.10f
        WallpaperKind.PHOTO -> 0.10f
        else -> 0f
    }

    fun blobs(kind: WallpaperKind): List<Blob> = when (kind) {
        WallpaperKind.LIQUID, WallpaperKind.PHOTO -> liquid
        WallpaperKind.AURORA -> aurora
        WallpaperKind.OCEAN -> ocean
        WallpaperKind.SUNSET -> sunset
        WallpaperKind.NIGHT -> night
    }

    private val liquid = listOf(
        Blob(argb(0.30f, 0xFFFFFF), 0.20f, 0.18f, 0.18f, 0.10f, 0.21f, 0.17f, 0.0f, 0.55f),
        Blob(argb(0.28f, 0x9A9A9F), 0.85f, 0.42f, 0.15f, 0.14f, 0.16f, 0.23f, 1.7f, 0.60f),
        Blob(argb(0.16f, 0xFFFFFF), 0.30f, 0.82f, 0.20f, 0.10f, 0.13f, 0.19f, 3.1f, 0.55f),
        Blob(argb(0.55f, 0x3A3A3C), 0.65f, 0.95f, 0.18f, 0.08f, 0.19f, 0.11f, 4.4f, 0.50f),
        Blob(argb(0.14f, 0xD1D1D6), 0.55f, 0.55f, 0.25f, 0.20f, 0.09f, 0.14f, 2.2f, 0.35f),
    )
    private val aurora = listOf(
        Blob(argb(0.75f, 0x2E6BFF), 0.15f, 0.20f, 0.15f, 0.10f, 0.20f, 0.15f, 0.0f, 0.60f),
        Blob(argb(0.70f, 0x8A3FFC), 0.85f, 0.35f, 0.14f, 0.12f, 0.15f, 0.21f, 1.3f, 0.55f),
        Blob(argb(0.60f, 0xFF4FA3), 0.70f, 0.78f, 0.18f, 0.10f, 0.12f, 0.17f, 2.7f, 0.55f),
        Blob(argb(0.50f, 0xFF9F43), 0.20f, 0.90f, 0.16f, 0.08f, 0.17f, 0.10f, 3.9f, 0.45f),
        Blob(argb(0.35f, 0x2EE6D6), 0.50f, 0.50f, 0.25f, 0.18f, 0.08f, 0.13f, 5.0f, 0.35f),
    )
    private val ocean = listOf(
        Blob(argb(0.70f, 0x0A84FF), 0.20f, 0.15f, 0.16f, 0.10f, 0.18f, 0.14f, 0.0f, 0.60f),
        Blob(argb(0.60f, 0x30D5C8), 0.85f, 0.45f, 0.14f, 0.13f, 0.14f, 0.20f, 1.5f, 0.55f),
        Blob(argb(0.65f, 0x1B3A8C), 0.40f, 0.85f, 0.20f, 0.10f, 0.11f, 0.16f, 2.9f, 0.60f),
        Blob(argb(0.30f, 0x64D2FF), 0.55f, 0.50f, 0.25f, 0.20f, 0.09f, 0.12f, 4.2f, 0.35f),
    )
    private val sunset = listOf(
        Blob(argb(0.75f, 0xFF6B35), 0.25f, 0.20f, 0.15f, 0.10f, 0.19f, 0.15f, 0.0f, 0.60f),
        Blob(argb(0.70f, 0xFF2D55), 0.85f, 0.40f, 0.14f, 0.12f, 0.15f, 0.20f, 1.2f, 0.55f),
        Blob(argb(0.60f, 0xFFCC00), 0.30f, 0.80f, 0.18f, 0.10f, 0.12f, 0.17f, 2.6f, 0.45f),
        Blob(argb(0.55f, 0x6E1E78), 0.75f, 0.92f, 0.16f, 0.08f, 0.17f, 0.10f, 3.8f, 0.55f),
    )
    private val night = listOf(
        Blob(argb(0.55f, 0x2C2C2E), 0.25f, 0.15f, 0.08f, 0.05f, 0.10f, 0.08f, 0.0f, 0.55f),
        Blob(argb(0.70f, 0x1C1C1E), 0.80f, 0.85f, 0.08f, 0.05f, 0.07f, 0.09f, 2.0f, 0.60f),
    )
}

/** Draws the full-resolution wallpaper. */
fun DrawScope.drawWallpaper(spec: WallpaperSpec, t: Float, screen: Size) {
    val w = screen.width
    val h = screen.height
    val img = spec.photo
    if (spec.kind == WallpaperKind.PHOTO && img != null) {
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
    } else {
        drawRect(Color(WallpaperModel.base(spec.kind)), size = screen)
        val longest = max(w, h)
        for (b in WallpaperModel.blobs(spec.kind)) {
            val c = Offset(b.x(t, w), b.y(t, h))
            val r = b.r(t, longest)
            val col = Color(b.color)
            drawCircle(
                brush = Brush.radialGradient(
                    0f to col,
                    0.55f to col.copy(alpha = col.alpha * 0.45f),
                    1f to Color.Transparent,
                    center = c,
                    radius = r,
                ),
                radius = r,
                center = c,
            )
        }
    }
    val shade = WallpaperModel.shade(spec.kind) + spec.dim
    if (shade > 0f) drawRect(Color.Black.copy(alpha = shade.coerceIn(0f, 0.85f)), size = screen)
}

@Composable
fun WallpaperLayer(modifier: Modifier = Modifier) {
    val spec = LocalWallpaper.current
    val time = LocalWallTime.current
    val screen = LocalScreenSize.current
    // Own layer: redrawing the animated wallpaper never re-records the home screen above it.
    Canvas(modifier.graphicsLayer()) { drawWallpaper(spec, time.value, screen) }
}
