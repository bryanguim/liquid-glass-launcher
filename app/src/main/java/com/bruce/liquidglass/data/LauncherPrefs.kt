package com.bruce.liquidglass.data

import android.content.Context

enum class WallpaperKind { LIQUID, AURORA, NIGHT, PHOTO }
enum class IconStyle { DEFAULT, CLEAR }

/** Small wrapper around SharedPreferences. Everything is stored as plain strings. */
class LauncherPrefs(context: Context) {
    private val sp = context.getSharedPreferences("liquid_glass", Context.MODE_PRIVATE)

    var onboarded: Boolean
        get() = sp.getBoolean("onboarded", false)
        set(v) = sp.edit().putBoolean("onboarded", v).apply()

    var wallpaper: WallpaperKind
        get() = runCatching { WallpaperKind.valueOf(sp.getString("wallpaper", null) ?: "LIQUID") }
            .getOrDefault(WallpaperKind.LIQUID)
        set(v) = sp.edit().putString("wallpaper", v.name).apply()

    var iconStyle: IconStyle
        get() = runCatching { IconStyle.valueOf(sp.getString("icon_style", null) ?: "DEFAULT") }
            .getOrDefault(IconStyle.DEFAULT)
        set(v) = sp.edit().putString("icon_style", v.name).apply()

    var animateWallpaper: Boolean
        get() = sp.getBoolean("animate_wallpaper", true)
        set(v) = sp.edit().putBoolean("animate_wallpaper", v).apply()

    var showLabels: Boolean
        get() = sp.getBoolean("show_labels", true)
        set(v) = sp.edit().putBoolean("show_labels", v).apply()

    /** Pages separated by '\n', items by '|'. Null = never set (first run). */
    var homePages: List<List<String>>?
        get() {
            val raw = sp.getString("home_pages", null) ?: return null
            return raw.split('\n').map { line -> line.split('|').filter { it.isNotBlank() } }
                .filter { it.isNotEmpty() }
        }
        set(v) {
            if (v == null) sp.edit().remove("home_pages").apply()
            else sp.edit().putString("home_pages", v.joinToString("\n") { it.joinToString("|") }).apply()
        }

    var dock: List<String>?
        get() = sp.getString("dock", null)?.split('|')?.filter { it.isNotBlank() }
        set(v) {
            if (v == null) sp.edit().remove("dock").apply()
            else sp.edit().putString("dock", v.joinToString("|")).apply()
        }

    /** Every app key we've already seen, so newly installed apps can be detected. */
    var knownApps: Set<String>
        get() = sp.getStringSet("known_apps", emptySet()) ?: emptySet()
        set(v) = sp.edit().putStringSet("known_apps", HashSet(v)).apply()

    var recents: List<String>
        get() = sp.getString("recents", "")!!.split('|').filter { it.isNotBlank() }
        set(v) = sp.edit().putString("recents", v.take(12).joinToString("|")).apply()
}
