package com.bruce.liquidglass.data

import org.json.JSONArray
import org.json.JSONObject

enum class WallpaperKind { LIQUID, AURORA, OCEAN, SUNSET, NIGHT, PHOTO }
enum class IconStyle { DEFAULT, DARK, CLEAR, TINTED }
enum class IconShape { SQUIRCLE, ROUNDED, CIRCLE }
enum class DockStyle { GLASS, CLEAR, NONE }
enum class ClockStyle { THIN, REGULAR, BOLD }
enum class SwipeDownAction { SEARCH, NOTIFICATIONS }

/** Every user-tweakable option of the launcher. Persisted as one JSON blob. */
data class LauncherSettings(
    // Wallpaper
    val wallpaper: WallpaperKind = WallpaperKind.LIQUID,
    val wallpaperDim: Float = 0f,
    val animateWallpaper: Boolean = true,
    val wallpaperSpeed: Float = 1f,
    // Icons
    val iconStyle: IconStyle = IconStyle.DEFAULT,
    val iconShape: IconShape = IconShape.SQUIRCLE,
    val iconTint: Int = 0xFFFFFFFF.toInt(),
    val iconSheen: Boolean = true,
    val iconSize: Int = 60,
    val showLabels: Boolean = true,
    val labelSize: Float = 11.5f,
    // Home grid
    val columns: Int = 4,
    val rows: Int = 6,
    val showWidget: Boolean = true,
    val clockStyle: ClockStyle = ClockStyle.THIN,
    val showBattery: Boolean = true,
    val searchPill: Boolean = true,
    // Dock
    val dockSize: Int = 4,
    val dockStyle: DockStyle = DockStyle.GLASS,
    // Glass
    val glassTint: Float = 0.10f,
    val glassRim: Boolean = true,
    // Motion & feel
    val haptics: Boolean = true,
    val returnAnimation: Boolean = true,
    val reducedMotion: Boolean = false,
    val highRefresh: Boolean = true,
    val swipeDown: SwipeDownAction = SwipeDownAction.SEARCH,
    // Apps
    val hidden: Set<String> = emptySet(),
) {
    fun toJson(): String = JSONObject().apply {
        put("wallpaper", wallpaper.name); put("wallpaperDim", wallpaperDim.toDouble())
        put("animateWallpaper", animateWallpaper); put("wallpaperSpeed", wallpaperSpeed.toDouble())
        put("iconStyle", iconStyle.name); put("iconShape", iconShape.name); put("iconTint", iconTint)
        put("iconSheen", iconSheen); put("iconSize", iconSize); put("showLabels", showLabels)
        put("labelSize", labelSize.toDouble())
        put("columns", columns); put("rows", rows); put("showWidget", showWidget)
        put("clockStyle", clockStyle.name); put("showBattery", showBattery); put("searchPill", searchPill)
        put("dockSize", dockSize); put("dockStyle", dockStyle.name)
        put("glassTint", glassTint.toDouble()); put("glassRim", glassRim)
        put("haptics", haptics); put("returnAnimation", returnAnimation); put("reducedMotion", reducedMotion)
        put("highRefresh", highRefresh); put("swipeDown", swipeDown.name)
        put("hidden", JSONArray(hidden.toList()))
    }.toString()

    companion object {
        private inline fun <reified E : Enum<E>> JSONObject.enumOr(key: String, def: E): E =
            runCatching { enumValueOf<E>(getString(key)) }.getOrDefault(def)

        fun fromJson(raw: String?): LauncherSettings {
            val d = LauncherSettings()
            if (raw.isNullOrBlank()) return d
            val o = runCatching { JSONObject(raw) }.getOrNull() ?: return d
            val hiddenArr = o.optJSONArray("hidden")
            val hidden = buildSet { if (hiddenArr != null) for (i in 0 until hiddenArr.length()) add(hiddenArr.getString(i)) }
            return LauncherSettings(
                wallpaper = o.enumOr("wallpaper", d.wallpaper),
                wallpaperDim = o.optDouble("wallpaperDim", d.wallpaperDim.toDouble()).toFloat(),
                animateWallpaper = o.optBoolean("animateWallpaper", d.animateWallpaper),
                wallpaperSpeed = o.optDouble("wallpaperSpeed", d.wallpaperSpeed.toDouble()).toFloat(),
                iconStyle = o.enumOr("iconStyle", d.iconStyle),
                iconShape = o.enumOr("iconShape", d.iconShape),
                iconTint = o.optInt("iconTint", d.iconTint),
                iconSheen = o.optBoolean("iconSheen", d.iconSheen),
                iconSize = o.optInt("iconSize", d.iconSize),
                showLabels = o.optBoolean("showLabels", d.showLabels),
                labelSize = o.optDouble("labelSize", d.labelSize.toDouble()).toFloat(),
                columns = o.optInt("columns", d.columns),
                rows = o.optInt("rows", d.rows),
                showWidget = o.optBoolean("showWidget", d.showWidget),
                clockStyle = o.enumOr("clockStyle", d.clockStyle),
                showBattery = o.optBoolean("showBattery", d.showBattery),
                searchPill = o.optBoolean("searchPill", d.searchPill),
                dockSize = o.optInt("dockSize", d.dockSize),
                dockStyle = o.enumOr("dockStyle", d.dockStyle),
                glassTint = o.optDouble("glassTint", d.glassTint.toDouble()).toFloat(),
                glassRim = o.optBoolean("glassRim", d.glassRim),
                haptics = o.optBoolean("haptics", d.haptics),
                returnAnimation = o.optBoolean("returnAnimation", d.returnAnimation),
                reducedMotion = o.optBoolean("reducedMotion", d.reducedMotion),
                highRefresh = o.optBoolean("highRefresh", d.highRefresh),
                swipeDown = o.enumOr("swipeDown", d.swipeDown),
                hidden = hidden,
            )
        }
    }
}

/** The subset of settings that changes how icon bitmaps are baked. */
data class IconConfig(
    val style: IconStyle,
    val shape: IconShape,
    val tint: Int,
    val sheen: Boolean,
)

val LauncherSettings.iconConfig get() = IconConfig(iconStyle, iconShape, iconTint, iconSheen)
