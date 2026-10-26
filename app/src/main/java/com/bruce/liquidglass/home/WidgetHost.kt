package com.bruce.liquidglass.home

import android.annotation.SuppressLint
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import androidx.compose.runtime.staticCompositionLocalOf
import kotlin.math.abs
import kotlin.math.roundToInt

/** Hosted widget view that can report a long-press (to open our glass menu) without breaking the widget's own taps. */
class GlassWidgetView(context: Context) : AppWidgetHostView(context) {

    var onLongPress: (() -> Unit)? = null

    private val slop = ViewConfiguration.get(context).scaledTouchSlop
    private var downX = 0f
    private var downY = 0f
    private var longPressed = false
    private val longPressRunnable = Runnable {
        if (parent != null && isAttachedToWindow) {
            longPressed = true
            performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)
            onLongPress?.invoke()
            // Cancel whatever the widget's children were doing.
            val cancel = MotionEvent.obtain(0, 0, MotionEvent.ACTION_CANCEL, 0f, 0f, 0)
            super.dispatchTouchEvent(cancel)
            cancel.recycle()
        }
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                longPressed = false
                downX = ev.x; downY = ev.y
                removeCallbacks(longPressRunnable)
                postDelayed(longPressRunnable, ViewConfiguration.getLongPressTimeout().toLong())
            }
            MotionEvent.ACTION_MOVE -> {
                if (abs(ev.x - downX) > slop || abs(ev.y - downY) > slop) removeCallbacks(longPressRunnable)
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> removeCallbacks(longPressRunnable)
        }
        return longPressed
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.actionMasked == MotionEvent.ACTION_UP || event.actionMasked == MotionEvent.ACTION_CANCEL) {
            removeCallbacks(longPressRunnable)
        }
        return longPressed || super.onTouchEvent(event)
    }

    override fun onDetachedFromWindow() {
        removeCallbacks(longPressRunnable)
        super.onDetachedFromWindow()
    }

    @Suppress("DEPRECATION")
    fun setSizeDp(widthDp: Int, heightDp: Int) {
        if (widthDp <= 0 || heightDp <= 0) return
        if (lastW == widthDp && lastH == heightDp) return
        lastW = widthDp; lastH = heightDp
        runCatching { updateAppWidgetSize(Bundle(), widthDp, heightDp, widthDp, heightDp) }
    }

    private var lastW = 0
    private var lastH = 0
}

private class GlassHost(context: Context, id: Int) : AppWidgetHost(context, id) {
    override fun onCreateView(context: Context, appWidgetId: Int, appWidget: AppWidgetProviderInfo?): AppWidgetHostView =
        GlassWidgetView(context)
}

/** Owns the AppWidgetHost and keeps one view per widget id (views are reused across recompositions). */
class WidgetController(private val context: Context) {
    val manager: AppWidgetManager = AppWidgetManager.getInstance(context)
    private val host: AppWidgetHost = GlassHost(context.applicationContext, HOST_ID)
    private val views = HashMap<Int, GlassWidgetView>()

    fun start() = runCatching { host.startListening() }
    fun stop() = runCatching { host.stopListening() }

    fun allocate(): Int = host.allocateAppWidgetId()

    fun delete(id: Int) {
        views.remove(id)?.let { (it.parent as? ViewGroup)?.removeView(it) }
        runCatching { host.deleteAppWidgetId(id) }
    }

    /** Opens the widget's own setup screen (works even when it isn't exported). */
    fun startConfigure(activity: android.app.Activity, id: Int, requestCode: Int): Boolean = runCatching {
        host.startAppWidgetConfigureActivityForResult(activity, id, 0, requestCode, null)
    }.isSuccess

    fun info(id: Int): AppWidgetProviderInfo? = runCatching { manager.getAppWidgetInfo(id) }.getOrNull()

    fun view(id: Int): GlassWidgetView? {
        views[id]?.let { v ->
            (v.parent as? ViewGroup)?.removeView(v)
            return v
        }
        val info = info(id) ?: return null
        val v = runCatching { host.createView(context, id, info) as GlassWidgetView }.getOrNull() ?: return null
        v.setPadding(0, 0, 0, 0)
        views[id] = v
        return v
    }

    /** Frees ids the host still holds but the home screen no longer shows (e.g. after a crash mid-add). */
    fun cleanup(keep: Set<Int>) {
        runCatching {
            host.appWidgetIds.filter { it !in keep }.forEach { delete(it) }
        }
    }

    /** Default number of rows for a widget, from its minimum height. */
    fun defaultRows(info: AppWidgetProviderInfo, density: Float): Int {
        val hDp = info.minHeight / density
        return (hDp / 90f).roundToInt().coerceIn(1, 4).let { if (hDp > 150 && it < 2) 2 else it }
    }

    companion object { const val HOST_ID = 1987 }
}

val LocalWidgets = staticCompositionLocalOf<WidgetController?> { null }

@Suppress("unused")
private fun View.detach() = (parent as? ViewGroup)?.removeView(this)
