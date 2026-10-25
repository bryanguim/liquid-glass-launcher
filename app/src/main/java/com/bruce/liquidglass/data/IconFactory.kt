package com.bruce.liquidglass.data

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.PorterDuffXfermode
import android.graphics.RadialGradient
import android.graphics.Shader
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sign
import kotlin.math.sin

/**
 * Bakes real app icons into iOS 26-style "Liquid Glass" icons: masked to the chosen shape,
 * restyled (default / dark / clear glass / tinted), then lit with a specular sheen and a rim light.
 * Everything is done once, off the main thread, so drawing an icon on screen is a plain bitmap blit.
 */
object IconFactory {

    fun shapePath(shape: IconShape, size: Float, inset: Float = 0f): Path {
        val p = Path()
        val s = size - inset * 2
        when (shape) {
            IconShape.CIRCLE -> p.addCircle(size / 2f, size / 2f, s / 2f, Path.Direction.CW)
            IconShape.ROUNDED -> p.addRoundRect(inset, inset, size - inset, size - inset, s * 0.24f, s * 0.24f, Path.Direction.CW)
            IconShape.SQUIRCLE -> {
                val a = s / 2.0
                val e = 2.0 / 5.0
                val steps = 160
                for (i in 0..steps) {
                    val t = 2.0 * PI * i / steps
                    val c = cos(t)
                    val sn = sin(t)
                    val x = (inset + a + a * sign(c) * abs(c).pow(e)).toFloat()
                    val y = (inset + a + a * sign(sn) * abs(sn).pow(e)).toFloat()
                    if (i == 0) p.moveTo(x, y) else p.lineTo(x, y)
                }
                p.close()
            }
        }
        return p
    }

    private val maskCache = HashMap<Pair<IconShape, Int>, Bitmap>()

    @Synchronized
    private fun mask(shape: IconShape, px: Int): Bitmap = maskCache.getOrPut(shape to px) {
        val m = Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
        Canvas(m).drawPath(shapePath(shape, px.toFloat()), paint)
        m
    }

    private fun clearMatrix(): ColorMatrix = ColorMatrix(
        floatArrayOf(
            0.30f, 0.59f, 0.11f, 0f, 95f,
            0.30f, 0.59f, 0.11f, 0f, 95f,
            0.30f, 0.59f, 0.11f, 0f, 95f,
            0f, 0f, 0f, 0.92f, 0f,
        )
    )

    private fun tintMatrix(tint: Int): ColorMatrix {
        val r = Color.red(tint) / 255f * 1.35f
        val g = Color.green(tint) / 255f * 1.35f
        val b = Color.blue(tint) / 255f * 1.35f
        return ColorMatrix(
            floatArrayOf(
                0.30f * r, 0.59f * r, 0.11f * r, 0f, 18f * r,
                0.30f * g, 0.59f * g, 0.11f * g, 0f, 18f * g,
                0.30f * b, 0.59f * b, 0.11f * b, 0f, 18f * b,
                0f, 0f, 0f, 1f, 0f,
            )
        )
    }

    private fun drawLayer(c: Canvas, d: Drawable?, px: Int, filter: android.graphics.ColorFilter?) {
        if (d == null) return
        val inset = px / 4
        d.setBounds(-inset, -inset, px + inset, px + inset)
        val old = d.colorFilter
        d.colorFilter = filter
        d.draw(c)
        d.colorFilter = old
    }

    private fun drawLegacy(c: Canvas, d: Drawable, px: Int, pad: Float, filter: android.graphics.ColorFilter?) {
        val p = (px * pad).toInt()
        d.setBounds(p, p, px - p, px - p)
        val old = d.colorFilter
        d.colorFilter = filter
        d.draw(c)
        d.colorFilter = old
    }

    private fun darkFill(c: Canvas, px: Int) {
        val paint = Paint().apply {
            shader = LinearGradient(0f, 0f, 0f, px.toFloat(),
                Color.rgb(46, 46, 50), Color.rgb(10, 10, 12), Shader.TileMode.CLAMP)
        }
        c.drawRect(0f, 0f, px.toFloat(), px.toFloat(), paint)
    }

    fun bake(d: Drawable, cfg: IconConfig, px: Int): Bitmap {
        val out = Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888)
        val c = Canvas(out)
        val adaptive = d as? AdaptiveIconDrawable
        val mono: Drawable? = if (Build.VERSION.SDK_INT >= 33) adaptive?.monochrome else null

        when (cfg.style) {
            IconStyle.DEFAULT -> {
                if (adaptive != null) {
                    drawLayer(c, adaptive.background, px, null)
                    drawLayer(c, adaptive.foreground, px, null)
                } else {
                    c.drawColor(Color.WHITE)
                    drawLegacy(c, d, px, 0.14f, null)
                }
            }
            IconStyle.DARK -> {
                darkFill(c, px)
                if (adaptive != null) drawLayer(c, adaptive.foreground, px, null)
                else drawLegacy(c, d, px, 0.16f, null)
            }
            IconStyle.CLEAR -> {
                // frosted pane — the live blurred wallpaper shows through behind it
                val frost = Paint().apply {
                    shader = LinearGradient(0f, 0f, px.toFloat(), px.toFloat(),
                        Color.argb(70, 255, 255, 255), Color.argb(26, 255, 255, 255), Shader.TileMode.CLAMP)
                }
                c.drawRect(0f, 0f, px.toFloat(), px.toFloat(), frost)
                when {
                    mono != null -> drawLayer(c, mono, px, PorterDuffColorFilter(Color.argb(235, 255, 255, 255), PorterDuff.Mode.SRC_IN))
                    adaptive != null -> drawLayer(c, adaptive.foreground, px, ColorMatrixColorFilter(clearMatrix()))
                    else -> drawLegacy(c, d, px, 0.16f, ColorMatrixColorFilter(clearMatrix()))
                }
            }
            IconStyle.TINTED -> {
                darkFill(c, px)
                when {
                    mono != null -> drawLayer(c, mono, px, PorterDuffColorFilter(cfg.tint, PorterDuff.Mode.SRC_IN))
                    adaptive != null -> drawLayer(c, adaptive.foreground, px, ColorMatrixColorFilter(tintMatrix(cfg.tint)))
                    else -> drawLegacy(c, d, px, 0.16f, ColorMatrixColorFilter(tintMatrix(cfg.tint)))
                }
            }
        }

        // Cut to shape (anti-aliased mask).
        val maskPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
            xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
        }
        c.drawBitmap(mask(cfg.shape, px), 0f, 0f, maskPaint)

        if (cfg.sheen) applyGlass(c, cfg, px)
        return out
    }

    /** Specular highlight + inner glow + rim light, only where the icon already has pixels. */
    private fun applyGlass(c: Canvas, cfg: IconConfig, px: Int) {
        val f = px.toFloat()
        val atop = PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP)
        val strong = cfg.style == IconStyle.CLEAR

        // top specular sheen
        c.drawRect(0f, 0f, f, f, Paint().apply {
            xfermode = atop
            shader = LinearGradient(0f, 0f, 0f, f * 0.62f,
                intArrayOf(Color.argb(if (strong) 95 else 70, 255, 255, 255), Color.argb(14, 255, 255, 255), Color.TRANSPARENT),
                floatArrayOf(0f, 0.55f, 1f), Shader.TileMode.CLAMP)
        })
        // soft light pooled at the bottom (refraction)
        c.drawRect(0f, 0f, f, f, Paint().apply {
            xfermode = atop
            shader = RadialGradient(f * 0.5f, f * 1.05f, f * 0.55f,
                Color.argb(if (strong) 60 else 34, 255, 255, 255), Color.TRANSPARENT, Shader.TileMode.CLAMP)
        })
        // rim light
        val stroke = f * 0.022f
        val rim = shapePath(cfg.shape, f, stroke / 2f)
        c.drawPath(rim, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = stroke
            shader = LinearGradient(0f, 0f, f, f,
                intArrayOf(Color.argb(200, 255, 255, 255), Color.argb(30, 255, 255, 255), Color.argb(20, 255, 255, 255), Color.argb(130, 255, 255, 255)),
                floatArrayOf(0f, 0.35f, 0.65f, 1f), Shader.TileMode.CLAMP)
        })
    }
}
