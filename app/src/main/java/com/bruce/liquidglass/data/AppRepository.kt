package com.bruce.liquidglass.data

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.LauncherActivityInfo
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Process
import android.provider.MediaStore
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap

data class AppInfo(
    val key: String,
    val label: String,
    val packageName: String,
    val component: ComponentName,
    val category: String,
    val installTime: Long,
    val isSystem: Boolean,
    val drawable: Drawable,
    val icon: ImageBitmap,
)

object Categories {
    const val OURS = "Nos apps"
    const val SOCIAL = "Réseaux sociaux"
    const val ENTERTAINMENT = "Divertissement"
    const val GAMES = "Jeux"
    const val SHOPPING = "Shopping"
    const val FINANCE = "Finance"
    const val PRODUCTIVITY = "Productivité"
    const val CREATIVITY = "Photo et vidéo"
    const val TRAVEL = "Voyages et cartes"
    const val NEWS = "Infos et lecture"
    const val UTILITIES = "Utilitaires"

    val order = listOf(OURS, SOCIAL, ENTERTAINMENT, GAMES, SHOPPING, FINANCE, PRODUCTIVITY, CREATIVITY, TRAVEL, NEWS, UTILITIES)
}

/** Labels of the apps Bruce built — they get their own category and are featured in the guide. */
val OUR_APP_LABELS = listOf("left", "liquid lock", "be better", "resell ai")

fun isOurApp(label: String): Boolean {
    val l = label.trim().lowercase()
    return OUR_APP_LABELS.any { l == it || l.startsWith("$it ") }
}

class AppRepository(private val context: Context) {

    private val launcherApps = context.getSystemService(LauncherApps::class.java)
    private val pm = context.packageManager

    fun loadApps(cfg: IconConfig, iconSizePx: Int): List<AppInfo> {
        val list = launcherApps.getActivityList(null, Process.myUserHandle())
        return list
            .filter { it.componentName.packageName != context.packageName }
            .mapNotNull { info -> runCatching { toAppInfo(info, cfg, iconSizePx) }.getOrNull() }
            .distinctBy { it.key }
            .sortedBy { it.label.lowercase() }
    }

    fun rebake(apps: List<AppInfo>, cfg: IconConfig, px: Int): List<AppInfo> =
        apps.map { a -> runCatching { a.copy(icon = IconFactory.bake(a.drawable, cfg, px).asImageBitmap()) }.getOrDefault(a) }

    private fun toAppInfo(info: LauncherActivityInfo, cfg: IconConfig, size: Int): AppInfo {
        val label = info.label?.toString()?.ifBlank { null } ?: info.componentName.packageName
        val ai = info.applicationInfo
        val installTime = runCatching { pm.getPackageInfo(ai.packageName, 0).firstInstallTime }.getOrDefault(0L)
        val drawable = (info.getIcon(0) ?: pm.defaultActivityIcon).mutate()
        return AppInfo(
            key = info.componentName.flattenToShortString(),
            label = label,
            packageName = ai.packageName,
            component = info.componentName,
            category = categorize(label, ai),
            installTime = installTime,
            isSystem = (ai.flags and ApplicationInfo.FLAG_SYSTEM) != 0 &&
                (ai.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) == 0,
            drawable = drawable,
            icon = IconFactory.bake(drawable, cfg, size).asImageBitmap(),
        )
    }

    private fun categorize(label: String, ai: ApplicationInfo): String {
        if (isOurApp(label)) return Categories.OURS
        val s = (ai.packageName + " " + label).lowercase()
        fun has(vararg k: String) = k.any { s.contains(it) }

        if (has("whatsapp", "instagram", "snapchat", "tiktok", "musically", "facebook", "messenger", "telegram",
                "discord", "twitter", "x.android", "threads", "reddit", "linkedin", "pinterest", "bereal", "signal",
                "messages", "mms", "sms")) return Categories.SOCIAL
        if (has("netflix", "youtube", "spotify", "twitch", "deezer", "disney", "primevideo", "prime video",
                "canal", "molotov", "soundcloud", "crunchyroll", "music", "musique", "podcast", "plex", "mycanal"))return Categories.ENTERTAINMENT
        if (has("epicgames", "fortnite", "supercell", "roblox", "mojang", "minecraft", "game", "jeu", "steam",
                "playstation", "xbox", "riot", "activision", "ubisoft")) return Categories.GAMES
        if (has("vinted", "aliexpress", "amazon", "shein", "temu", "leboncoin", "ebay", "wish", "zalando",
                "fnac", "cdiscount", "uber eats", "ubereats", "deliveroo", "mcdo", "shop")) return Categories.SHOPPING
        if (has("bank", "banque", "revolut", "paypal", "lydia", "boursorama", "wallet", "binance", "phantom",
                "coinbase", "crypto", "sumeria", "credit", "crédit", "societegenerale", "bnp", "lcl", "n26", "finance")) return Categories.FINANCE
        if (has("gmail", "mail", "docs", "drive", "notion", "office", "outlook", "calendar", "calendrier", "agenda",
                "teams", "slack", "zoom", "keep", "notes", "note", "claude", "chatgpt", "gemini", "sheets", "word",
                "excel", "lovable", "github")) return Categories.PRODUCTIVITY
        if (has("camera", "caméra", "appareil photo", "photo", "gallery", "galerie", "capcut", "lightroom",
                "snapseed", "canva", "video", "vidéo", "editor", "inshot")) return Categories.CREATIVITY
        if (has("maps", "waze", "uber", "bolt", "sncf", "blablacar", "airbnb", "booking", "citymapper",
                "rtm", "lepilote", "plans", "météo", "weather")) return Categories.TRAVEL
        if (has("news", "actu", "journal", "lemonde", "figaro", "kindle", "book", "livre", "webtoon", "manga")) return Categories.NEWS

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            when (ai.category) {
                ApplicationInfo.CATEGORY_GAME -> return Categories.GAMES
                ApplicationInfo.CATEGORY_SOCIAL -> return Categories.SOCIAL
                ApplicationInfo.CATEGORY_AUDIO, ApplicationInfo.CATEGORY_VIDEO -> return Categories.ENTERTAINMENT
                ApplicationInfo.CATEGORY_IMAGE -> return Categories.CREATIVITY
                ApplicationInfo.CATEGORY_MAPS -> return Categories.TRAVEL
                ApplicationInfo.CATEGORY_NEWS -> return Categories.NEWS
                ApplicationInfo.CATEGORY_PRODUCTIVITY -> return Categories.PRODUCTIVITY
            }
        }
        return Categories.UTILITIES
    }

    /** Packages of the default phone, messages, browser and camera apps — for the first dock. */
    fun defaultDockPackages(): List<String> {
        val intents = listOf(
            Intent(Intent.ACTION_DIAL),
            Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_MESSAGING),
            Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_BROWSER),
            Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA),
        )
        return intents.mapNotNull { intent ->
            runCatching {
                pm.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)?.activityInfo?.packageName
            }.getOrNull()?.takeIf { it != "android" }
        }
    }

    fun registerCallback(onChange: () -> Unit): LauncherApps.Callback {
        val cb = object : LauncherApps.Callback() {
            override fun onPackageRemoved(packageName: String?, user: android.os.UserHandle?) = onChange()
            override fun onPackageAdded(packageName: String?, user: android.os.UserHandle?) = onChange()
            override fun onPackageChanged(packageName: String?, user: android.os.UserHandle?) = onChange()
            override fun onPackagesAvailable(p: Array<out String>?, u: android.os.UserHandle?, r: Boolean) = onChange()
            override fun onPackagesUnavailable(p: Array<out String>?, u: android.os.UserHandle?, r: Boolean) = onChange()
        }
        launcherApps.registerCallback(cb, android.os.Handler(android.os.Looper.getMainLooper()))
        return cb
    }

    fun unregisterCallback(cb: LauncherApps.Callback) = launcherApps.unregisterCallback(cb)

    fun launch(app: AppInfo, bounds: android.graphics.Rect?, opts: android.os.Bundle?) {
        launcherApps.startMainActivity(app.component, Process.myUserHandle(), bounds, opts)
    }

    fun openAppInfo(app: AppInfo) {
        launcherApps.startAppDetailsActivity(app.component, Process.myUserHandle(), null, null)
    }
}
