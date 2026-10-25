package com.bruce.liquidglass.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Rect
import android.graphics.Shader
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.bruce.liquidglass.data.WallpaperKind
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * The blurred copy of the wallpaper that every glass surface shows through.
 * It's rendered tiny (≈1/15 of the screen) on a background thread, box-blurred, and simply
 * stretched behind each pane — so glass costs one bitmap draw instead of a GPU blur.
 */
class BackdropState {
    var image by mutableStateOf<ImageBitmap?>(null)
        internal set
}

val LocalBackdrop = staticCompositionLocalOf { BackdropState() }

private const val BACKDROP_W = 72

@Composable
fun rememberBackdrop(spec: WallpaperSpec, animate: Boolean, speed: Float, screen: Size): BackdropState {
    val state = remember { BackdropState() }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(spec, animate, speed, screen) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            withContext(Dispatchers.Default) {
                val w = BACKDROP_W
                val h = max(BACKDROP_W, (BACKDROP_W * screen.height / screen.width).roundToInt())
                val buffers = arrayOf(
                    Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888),
                    Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888),
                )
                val photoSmall = if (spec.kind == WallpaperKind.PHOTO) spec.photo?.let { cropScale(it.asAndroidBitmap(), w, h) } else null
                var index = 0
                val pixels = IntArray(w * h)
                val scratch = IntArray(w * h)
                while (isActive) {
                    val b = buffers[index]
                    index = 1 - index
                    val t = if (animate) WallClock.at(System.nanoTime(), speed) else WallClock.STATIC_T
                    render(b, spec, t, photoSmall)
                    b.getPixels(pixels, 0, w, 0, 0, w, h)
                    repeat(3) { boxBlur(pixels, scratch, w, h, 3) }
                    b.setPixels(pixels, 0, w, 0, 0, w, h)
                    state.image = b.asImageBitmap()
                    if (!animate || spec.kind == WallpaperKind.PHOTO && photoSmall != null) break
                    delay(40)
                }
            }
        }
    }
    return state
}

private fun cropScale(src: Bitmap, w: Int, h: Int): Bitmap {
    val target = w.toFloat() / h
    val srcRatio = src.width.toFloat() / src.height
    val crop = if (srcRatio > target) {
        val cw = (src.height * target).roundToInt()
        Rect((src.width - cw) / 2, 0, (src.width - cw) / 2 + cw, src.height)
    } else {
        val ch = (src.width / target).roundToInt()
        Rect(0, (src.height - ch) / 2, src.width, (src.height - ch) / 2 + ch)
    }
    val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    Canvas(out).drawBitmap(src, crop, Rect(0, 0, w, h), Paint(Paint.FILTER_BITMAP_FLAG))
    return out
}

private fun render(b: Bitmap, spec: WallpaperSpec, t: Float, photo: Bitmap?) {
    val c = Canvas(b)
    val w = b.width.toFloat()
    val h = b.height.toFloat()
    if (spec.kind == WallpaperKind.PHOTO && photo != null) {
        c.drawBitmap(photo, 0f, 0f, null)
    } else {
        c.drawColor(WallpaperModel.base(spec.kind))
        val longest = max(w, h)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        for (blob in WallpaperModel.blobs(spec.kind)) {
            val cx = blob.x(t, w)
            val cy = blob.y(t, h)
            val r = blob.r(t, longest).coerceAtLeast(1f)
            val a = (blob.color ushr 24) and 0xFF
            val rgb = blob.color and 0xFFFFFF
            val mid = ((a * 0.45f).roundToInt() shl 24) or rgb
            paint.shader = RadialGradient(cx, cy, r, intArrayOf(blob.color, mid, 0), floatArrayOf(0f, 0.55f, 1f), Shader.TileMode.CLAMP)
            c.drawCircle(cx, cy, r, paint)
        }
    }
    val shade = (WallpaperModel.shade(spec.kind) + spec.dim).coerceIn(0f, 0.85f)
    if (shade > 0f) c.drawColor(((shade * 255).roundToInt() shl 24))
}

/** Separable box blur on ARGB pixels. */
private fun boxBlur(px: IntArray, tmp: IntArray, w: Int, h: Int, r: Int) {
    val div = r * 2 + 1
    // horizontal
    for (y in 0 until h) {
        var sa = 0; var sr = 0; var sg = 0; var sb = 0
        val row = y * w
        for (i in -r..r) {
            val p = px[row + i.coerceIn(0, w - 1)]
            sa += p ushr 24; sr += (p shr 16) and 0xFF; sg += (p shr 8) and 0xFF; sb += p and 0xFF
        }
        for (x in 0 until w) {
            tmp[row + x] = ((sa / div) shl 24) or ((sr / div) shl 16) or ((sg / div) shl 8) or (sb / div)
            val out = px[row + (x - r).coerceIn(0, w - 1)]
            val inn = px[row + (x + r + 1).coerceIn(0, w - 1)]
            sa += (inn ushr 24) - (out ushr 24)
            sr += ((inn shr 16) and 0xFF) - ((out shr 16) and 0xFF)
            sg += ((inn shr 8) and 0xFF) - ((out shr 8) and 0xFF)
            sb += (inn and 0xFF) - (out and 0xFF)
        }
    }
    // vertical
    for (x in 0 until w) {
        var sa = 0; var sr = 0; var sg = 0; var sb = 0
        for (i in -r..r) {
            val p = tmp[i.coerceIn(0, h - 1) * w + x]
            sa += p ushr 24; sr += (p shr 16) and 0xFF; sg += (p shr 8) and 0xFF; sb += p and 0xFF
        }
        for (y in 0 until h) {
            px[y * w + x] = ((sa / div) shl 24) or ((sr / div) shl 16) or ((sg / div) shl 8) or (sb / div)
            val out = tmp[(y - r).coerceIn(0, h - 1) * w + x]
            val inn = tmp[(y + r + 1).coerceIn(0, h - 1) * w + x]
            sa += (inn ushr 24) - (out ushr 24)
            sr += ((inn shr 16) and 0xFF) - ((out shr 16) and 0xFF)
            sg += ((inn shr 8) and 0xFF) - ((out shr 8) and 0xFF)
            sb += (inn and 0xFF) - (out and 0xFF)
        }
    }
}
