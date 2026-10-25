package com.bruce.liquidglass

import android.app.role.RoleManager
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color as AndroidColor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.lifecycle.Lifecycle
import com.bruce.liquidglass.home.HomeScreen
import com.bruce.liquidglass.home.LauncherActions
import com.bruce.liquidglass.home.LocalActions
import com.bruce.liquidglass.onboarding.Onboarding
import com.bruce.liquidglass.home.LocalSettings
import com.bruce.liquidglass.ui.GlassLook
import com.bruce.liquidglass.ui.LocalBackdrop
import com.bruce.liquidglass.ui.LocalGlassLook
import com.bruce.liquidglass.ui.LocalScreenSize
import com.bruce.liquidglass.ui.WallClock
import com.bruce.liquidglass.ui.rememberBackdrop
import com.bruce.liquidglass.ui.LocalWallTime
import com.bruce.liquidglass.ui.LocalWallpaper
import com.bruce.liquidglass.ui.WallpaperLayer
import com.bruce.liquidglass.ui.WallpaperSpec

class MainActivity : ComponentActivity(), LauncherActions {

    private val vm: LauncherViewModel by viewModels()
    private lateinit var photoPicker: ActivityResultLauncher<PickVisualMediaRequest>
    private lateinit var roleRequest: ActivityResultLauncher<Intent>
    private var leftForApp = false

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)

        photoPicker = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) vm.setPhoto(uri)
        }
        roleRequest = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (!isDefaultHome()) openHomeSettings()
        }

        applyRefreshRate(vm.settings.highRefresh)
        setContent {
            val high = vm.settings.highRefresh
            androidx.compose.runtime.LaunchedEffect(high) { applyRefreshRate(high) }
            Root(vm, this)
        }
    }

    /** Ask for the panel's fastest mode (120 Hz on Honor) — MagicOS often keeps apps at 60 Hz otherwise. */
    private fun applyRefreshRate(high: Boolean) {
        runCatching {
            @Suppress("DEPRECATION")
            val disp = (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) this.display else windowManager.defaultDisplay) ?: return
            val current = disp.mode
            val best = disp.supportedModes
                .filter { it.physicalWidth == current.physicalWidth && it.physicalHeight == current.physicalHeight }
                .maxByOrNull { it.refreshRate }
            val attrs = window.attributes
            attrs.preferredDisplayModeId = if (high && best != null) best.modeId else 0
            if (high && best != null) attrs.preferredRefreshRate = best.refreshRate
            window.attributes = attrs
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Home pressed while we're already in front: close overlays / go back to page 1.
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) vm.homePressed.tryEmit(Unit)
    }

    override fun onStop() {
        super.onStop()
        leftForApp = true
    }

    override fun onStart() {
        super.onStart()
        if (leftForApp) {
            leftForApp = false
            vm.menu = null
            vm.search = null
            vm.openCategory = null
            vm.returnedHome.tryEmit(Unit)
        }
    }

    // ---------------- LauncherActions ----------------

    override fun isDefaultHome(): Boolean {
        val i = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val res = packageManager.resolveActivity(i, PackageManager.MATCH_DEFAULT_ONLY)
        return res?.activityInfo?.packageName == packageName
    }

    override fun requestDefaultHome() {
        if (isDefaultHome()) { openHomeSettings(); return }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val rm = getSystemService(RoleManager::class.java)
            if (rm != null && rm.isRoleAvailable(RoleManager.ROLE_HOME)) {
                runCatching { roleRequest.launch(rm.createRequestRoleIntent(RoleManager.ROLE_HOME)) }
                    .onFailure { openHomeSettings() }
                return
            }
        }
        openHomeSettings()
    }

    private fun openHomeSettings() {
        val ok = runCatching {
            startActivity(Intent(Settings.ACTION_HOME_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }.isSuccess
        if (!ok) runCatching {
            startActivity(Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }.onFailure {
            runCatching { startActivity(Intent(Settings.ACTION_SETTINGS)) }
        }
    }

    override fun pickPhoto() {
        photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    private fun statusBar(method: String) {
        runCatching {
            val service = getSystemService("statusbar")
            Class.forName("android.app.StatusBarManager").getMethod(method).invoke(service)
        }
    }

    override fun expandNotifications() = statusBar("expandNotificationsPanel")
    override fun expandQuickSettings() = statusBar("expandSettingsPanel")

    override fun openUrl(url: String) {
        val ok = runCatching {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }.isSuccess
        if (!ok && url.startsWith("market://")) {
            openUrl(url.replace("market://", "https://play.google.com/store/apps/"))
        }
    }
}

@Composable
private fun Root(vm: LauncherViewModel, actions: LauncherActions) {
    // Registered first = lowest priority: overlays' own back handlers win. A launcher never finishes on back.
    BackHandler(enabled = true) { vm.closeOverlays() }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val screen = Size(constraints.maxWidth.toFloat(), constraints.maxHeight.toFloat())
        val time = remember { mutableFloatStateOf(0f) }
        val s = vm.settings
        LaunchedEffect(s.animateWallpaper, s.wallpaperSpeed) {
            if (!s.animateWallpaper) { time.floatValue = WallClock.STATIC_T; return@LaunchedEffect }
            while (true) {
                withFrameNanos { now -> time.floatValue = WallClock.at(now, s.wallpaperSpeed) }
            }
        }
        val spec = remember(s.wallpaper, vm.photo, s.wallpaperDim) { WallpaperSpec(s.wallpaper, vm.photo, s.wallpaperDim) }
        val backdrop = rememberBackdrop(spec, s.animateWallpaper, s.wallpaperSpeed, screen)
        val look = remember(s.glassTint, s.glassRim) { GlassLook(s.glassTint, s.glassRim) }
        CompositionLocalProvider(
            LocalWallpaper provides spec,
            LocalWallTime provides time,
            LocalScreenSize provides screen,
            LocalActions provides actions,
            LocalBackdrop provides backdrop,
            LocalGlassLook provides look,
            LocalSettings provides s,
        ) {
            WallpaperLayer(Modifier.fillMaxSize())
            if (vm.loaded) HomeScreen(vm)
            AnimatedVisibility(
                visible = !vm.onboarded,
                enter = fadeIn(tween(300)),
                exit = fadeOut(tween(450)) + scaleOut(tween(550), targetScale = 1.15f),
            ) {
                Onboarding(vm)
            }
        }
    }
}
