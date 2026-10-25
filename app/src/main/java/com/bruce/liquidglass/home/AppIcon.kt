package com.bruce.liquidglass.home

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bruce.liquidglass.data.AppInfo
import com.bruce.liquidglass.data.IconStyle
import com.bruce.liquidglass.ui.Glass
import com.bruce.liquidglass.ui.Motion
import com.bruce.liquidglass.ui.Squircle
import com.bruce.liquidglass.ui.tick

private val grayscale = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })

/** Just the squircle icon image, in the chosen style. */
@Composable
fun IconImage(app: AppInfo, size: Dp, style: IconStyle, modifier: Modifier = Modifier) {
    if (style == IconStyle.CLEAR) {
        Glass(modifier.size(size), shape = Squircle, blur = 18.dp) {
            Image(
                bitmap = app.icon,
                contentDescription = app.label,
                contentScale = ContentScale.Crop,
                colorFilter = grayscale,
                alpha = 0.78f,
                modifier = Modifier.size(size).clip(Squircle),
            )
        }
    } else {
        Image(
            bitmap = app.icon,
            contentDescription = app.label,
            contentScale = ContentScale.Crop,
            modifier = modifier
                .size(size)
                .clip(Squircle)
                .border(0.6.dp, Color.White.copy(alpha = 0.18f), Squircle),
        )
    }
}

@Composable
private fun jiggleRotation(seed: Int): Float {
    val t = rememberInfiniteTransition(label = "jiggle")
    val dur = 118 + (seed % 5) * 9
    val r by t.animateFloat(
        initialValue = if (seed % 2 == 0) -1.8f else 1.8f,
        targetValue = if (seed % 2 == 0) 1.8f else -1.8f,
        animationSpec = infiniteRepeatable(tween(dur, easing = LinearEasing), RepeatMode.Reverse),
        label = "rot",
    )
    return r
}

/**
 * A home-screen icon with press-shrink, long-press, launch bounds, jiggle (edit mode)
 * and the "−" badge.
 */
@Composable
fun AppIcon(
    app: AppInfo,
    iconSize: Dp,
    style: IconStyle,
    showLabel: Boolean,
    editing: Boolean,
    selected: Boolean,
    seed: Int,
    onTap: (Rect) -> Unit,
    onLongPress: (Rect) -> Unit,
    onRemove: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val view = LocalView.current
    val tapAction by rememberUpdatedState(onTap)
    val longAction by rememberUpdatedState(onLongPress)
    val removeAction by rememberUpdatedState(onRemove)
    var pressed by remember { mutableStateOf(false) }
    var bounds by remember { mutableStateOf(Rect.Zero) }
    val scale by animateFloatAsState(
        targetValue = when {
            selected -> 1.14f
            pressed -> 0.88f
            else -> 1f
        },
        animationSpec = Motion.snappy(),
        label = "iconScale",
    )
    val rotation = if (editing && !selected) jiggleRotation(seed) else 0f

    Column(modifier.width(iconSize + 18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box {
            Box(
                Modifier
                    .onGloballyPositioned { bounds = it.boundsInWindow() }
                    .graphicsLayer {
                        scaleX = scale; scaleY = scale; rotationZ = rotation
                        alpha = if (pressed && !editing) 0.85f else 1f
                    }
                    .pointerInput(app.key, editing) {
                        detectTapGestures(
                            onPress = {
                                pressed = true
                                tryAwaitRelease()
                                pressed = false
                            },
                            onTap = { tapAction(bounds) },
                            onLongPress = {
                                pressed = false
                                view.tick()
                                longAction(bounds)
                            },
                        )
                    }
            ) {
                IconImage(app, iconSize, style)
            }
            val badge by animateFloatAsState(
                if (editing && onRemove != null) 1f else 0f, Motion.bouncy(), label = "badge"
            )
            if (badge > 0.01f) {
                Glass(
                    Modifier
                        .offset((-6).dp, (-6).dp)
                        .graphicsLayer { scaleX = badge; scaleY = badge; rotationZ = rotation }
                        .size(22.dp)
                        .pointerInput(app.key) { detectTapGestures { view.tick(); removeAction?.invoke() } },
                    shape = CircleShape,
                    tint = Color(0x66000000),
                ) {
                    Icon(
                        Icons.Rounded.Remove, null, tint = Color.White,
                        modifier = Modifier.align(Alignment.Center).size(16.dp),
                    )
                }
            }
        }
        if (showLabel) {
            Spacer(Modifier.height(6.dp))
            Text(
                app.label,
                style = LabelStyle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
        }
    }
}

val LabelStyle = TextStyle(
    color = Color.White,
    fontSize = 11.5.sp,
    fontWeight = FontWeight.Medium,
    letterSpacing = 0.1.sp,
    shadow = Shadow(Color.Black.copy(alpha = 0.55f), blurRadius = 6f),
)
