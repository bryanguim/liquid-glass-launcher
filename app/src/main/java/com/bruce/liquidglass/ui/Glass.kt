package com.bruce.liquidglass.ui

import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sign
import kotlin.math.sin

/** Global look of every glass surface — driven by the settings. */
@Immutable
data class GlassLook(val tint: Float = 0.10f, val rim: Boolean = true)

val LocalGlassLook = staticCompositionLocalOf { GlassLook() }

/** Holds layout coordinates without triggering recomposition. */
class CoordsRef { var coords: LayoutCoordinates? = null }

/** Draws the pre-blurred wallpaper aligned with the real one, behind this element. */
fun DrawScope.drawBackdrop(ref: CoordsRef, backdrop: BackdropState, screen: Size) {
    val img = backdrop.image ?: return
    val c = ref.coords
    val pos = if (c != null && c.isAttached) c.positionInRoot() else Offset.Zero
    drawImage(
        image = img,
        srcOffset = IntOffset.Zero,
        srcSize = IntSize(img.width, img.height),
        dstOffset = IntOffset(-pos.x.roundToInt(), -pos.y.roundToInt()),
        dstSize = IntSize(screen.width.roundToInt(), screen.height.roundToInt()),
        filterQuality = FilterQuality.Low,
    )
}

/**
 * Liquid Glass surface: blurred wallpaper + tint + specular gradient + rim light.
 * Costs a couple of bitmap/gradient draws — no GPU blur — so it stays smooth at 120 Hz.
 */
@Composable
fun Glass(
    modifier: Modifier = Modifier,
    shape: Shape,
    tint: Color? = null,
    rim: Boolean = true,
    @Suppress("UNUSED_PARAMETER") blur: Dp = 0.dp,
    content: @Composable BoxScope.() -> Unit = {},
) {
    val ref = remember { CoordsRef() }
    val backdrop = LocalBackdrop.current
    val screen = LocalScreenSize.current
    val look = LocalGlassLook.current
    val fill = tint ?: Color.White.copy(alpha = look.tint)
    val showRim = rim && look.rim

    Box(
        modifier
            .onPlaced { ref.coords = it }
            .clip(shape)
            .drawWithCache {
                val outline = shape.createOutline(size, layoutDirection, this)
                val sheen = Brush.verticalGradient(
                    0f to Color.White.copy(alpha = 0.16f),
                    0.45f to Color.White.copy(alpha = 0.03f),
                    1f to Color.White.copy(alpha = 0.08f),
                )
                val rimBrush = Brush.linearGradient(
                    0f to Color.White.copy(alpha = 0.62f),
                    0.35f to Color.White.copy(alpha = 0.10f),
                    0.65f to Color.White.copy(alpha = 0.06f),
                    1f to Color.White.copy(alpha = 0.36f),
                    start = Offset.Zero,
                    end = Offset(size.width, size.height),
                )
                val stroke = Stroke(1.dp.toPx() * 1.2f)
                onDrawBehind {
                    drawBackdrop(ref, backdrop, screen)
                    drawRect(fill)
                    drawRect(sheen)
                    if (showRim) drawOutline(outline, rimBrush, style = stroke)
                }
            },
        content = content,
    )
}

/** iOS-like continuous-corner shape (superellipse). */
class SquircleShape(private val n: Float = 5f) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val path = Path()
        val a = size.width / 2f
        val b = size.height / 2f
        val e = 2.0 / n
        val steps = 96
        for (i in 0..steps) {
            val t = 2.0 * PI * i / steps
            val c = cos(t)
            val s = sin(t)
            val x = (a + a * sign(c) * abs(c).pow(e)).toFloat()
            val y = (b + b * sign(s) * abs(s).pow(e)).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        return Outline.Generic(path)
    }
}

val Squircle = SquircleShape()

object Motion {
    @Volatile var reduced: Boolean = false
    fun <T> soft(): SpringSpec<T> = if (reduced) spring(1f, 1400f) else spring(dampingRatio = 0.86f, stiffness = 300f)
    fun <T> snappy(): SpringSpec<T> = if (reduced) spring(1f, 1600f) else spring(dampingRatio = 0.80f, stiffness = 600f)
    fun <T> bouncy(): SpringSpec<T> = if (reduced) spring(1f, 1400f) else spring(dampingRatio = 0.64f, stiffness = Spring.StiffnessMediumLow)
}

object Haptics { @Volatile var enabled: Boolean = true }

fun View.tick() { if (Haptics.enabled) performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK) }
fun View.thud() { if (Haptics.enabled) performHapticFeedback(HapticFeedbackConstants.LONG_PRESS) }
fun View.confirm() {
    if (!Haptics.enabled) return
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) performHapticFeedback(HapticFeedbackConstants.CONFIRM)
    else performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
}
