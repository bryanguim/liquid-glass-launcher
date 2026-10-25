package com.bruce.liquidglass.home

import androidx.compose.runtime.staticCompositionLocalOf

/** Things only the Activity can do. */
interface LauncherActions {
    fun requestDefaultHome()
    fun isDefaultHome(): Boolean
    fun pickPhoto()
    fun expandNotifications()
    fun expandQuickSettings()
    fun openUrl(url: String)
}

val LocalActions = staticCompositionLocalOf<LauncherActions> { error("No LauncherActions") }
