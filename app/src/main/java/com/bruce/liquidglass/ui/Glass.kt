package com.bruce.liquidglass.ui

import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sign
import kotlin.math.sin

val canBlur: Boolean get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

/**
 * Liquid Glass surface: the wallpaper behind it is re-drawn, blurred, then lit with
 * a specular gradient and a bright rim — like iOS 26 materials.
 */
@Composable
fun Glass(
    modifier: Modifier = Modifier,
    shape: Shape,
    blur: Dp = 28.dp,
    tint: Color = Color.White.copy(alpha = 0.08f),
    rim: Boolean = true,
    content: @Composable BoxScope.() -> Unit = {},
) {
    var origin by remember { mutableStateOf(Offset.Zero) }
    val spec = LocalWallpaper.current
    val time = LocalWallTime.current
    val screen = LocalScreenSize.current

    Box(
        modifier
            .onGloballyPositioned { origin = it.positionInRoot() }
            .clip(shape)
    ) {
        // 1. Blurred backdrop
        Box(
            Modifier
                .matchParentSize()
                .then(if (canBlur) Modifier.blur(blur, BlurredEdgeTreatment.Rectangle) else Modifier)
                .drawBehind {
                    val o = origin
                    translate(-o.x, -o.y) { drawWallpaper(spec, time.value, screen) }
                    if (!canBlur) drawRect(Color.Black.copy(alpha = 0.45f))
                }
        )
        // 2. Tint + specular highlight + rim light
        Box(
            Modifier
                .matchParentSize()
                .background(tint)
                .background(
                    Brush.verticalGradient(
                        0f to Color.White.copy(alpha = 0.16f),
                        0.45f to Color.White.copy(alpha = 0.03f),
                        1f to Color.White.copy(alpha = 0.07f),
                    )
                )
                .then(
                    if (rim) Modifier.border(
                        1.dp,
                        Brush.linearGradient(
                            0f to Color.White.copy(alpha = 0.60f),
                            0.35f to Color.White.copy(alpha = 0.10f),
                            0.65f to Color.White.copy(alpha = 0.06f),
                            1f to Color.White.copy(alpha = 0.35f),
                        ),
                        shape,
                    ) else Modifier
                )
        )
        content()
    }
}

/** iOS-like continuous-corner shape (superellipse). */
class SquircleShape(private val n: Float = 5f) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val path = Path()
        val a = size.width / 2f
        val b = size.height / 2f
        val e = 2.0 / n
        val steps = 120
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
    fun <T> soft(): SpringSpec<T> = spring(dampingRatio = 0.86f, stiffness = 260f)
    fun <T> snappy(): SpringSpec<T> = spring(dampingRatio = 0.78f, stiffness = 520f)
    fun <T> bouncy(): SpringSpec<T> = spring(dampingRatio = 0.62f, stiffness = Spring.StiffnessMediumLow)
}

fun View.tick() { performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK) }
fun View.thud() { performHapticFeedback(HapticFeedbackConstants.LONG_PRESS) }
fun View.confirm() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) performHapticFeedback(HapticFeedbackConstants.CONFIRM)
    else performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
}
