package com.bruce.liquidglass.home

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bruce.liquidglass.LauncherViewModel
import com.bruce.liquidglass.data.IconStyle
import com.bruce.liquidglass.data.WallpaperKind
import com.bruce.liquidglass.ui.Glass
import com.bruce.liquidglass.ui.LocalScreenSize
import com.bruce.liquidglass.ui.LocalWallTime
import com.bruce.liquidglass.ui.LocalWallpaper
import com.bruce.liquidglass.ui.Motion
import com.bruce.liquidglass.ui.WallpaperSpec
import com.bruce.liquidglass.ui.drawWallpaper
import com.bruce.liquidglass.ui.tick
import kotlin.math.roundToInt

@Composable
fun SettingsSheet(vm: LauncherViewModel) {
    BackHandler(enabled = vm.settingsOpen) { vm.settingsOpen = false }
    AnimatedVisibility(visible = vm.settingsOpen, enter = fadeIn(), exit = fadeOut()) {
        val avScope = this
        var drag by remember { mutableFloatStateOf(0f) }
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.3f))
                .pointerInput(Unit) { detectTapGestures { vm.settingsOpen = false } },
            contentAlignment = Alignment.BottomCenter,
        ) {
            val sheetAnim = with(avScope) {
                Modifier.animateEnterExit(
                    enter = slideInVertically(Motion.snappy()) { it },
                    exit = slideOutVertically(Motion.snappy()) { it },
                )
            }
            Glass(
                sheetAnim
                    .statusBarsPadding()
                    .padding(top = 60.dp)
                    .fillMaxWidth()
                    .offset { IntOffset(0, drag.roundToInt().coerceAtLeast(0)) }
                    .pointerInput(Unit) { detectTapGestures { } },
                shape = RoundedCornerShape(topStart = 38.dp, topEnd = 38.dp),
                blur = 40.dp,
                tint = Color(0x55000000),
            ) {
                Column(Modifier.navigationBarsPadding()) {
                    // grabber (drag to close)
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(30.dp)
                            .pointerInput(Unit) {
                                detectVerticalDragGestures(
                                    onDragEnd = {
                                        if (drag > 160f) vm.settingsOpen = false
                                        drag = 0f
                                    },
                                    onVerticalDrag = { _, dy -> drag += dy },
                                )
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(Modifier.size(40.dp, 5.dp).background(Color.White.copy(alpha = 0.4f), RoundedCornerShape(50)))
                    }
                    SettingsContent(vm)
                }
            }
        }
    }
}

@Composable
private fun SettingsContent(vm: LauncherViewModel) {
    val actions = LocalActions.current
    val view = LocalView.current
    Column(
        Modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp)
            .padding(bottom = 24.dp)
    ) {
        Text("Réglages", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(18.dp))

        Group("Fond d'écran") {
            Row(
                Modifier.fillMaxWidth().padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                WallpaperChoice(vm, WallpaperKind.LIQUID, "Liquide")
                WallpaperChoice(vm, WallpaperKind.AURORA, "Aurore")
                WallpaperChoice(vm, WallpaperKind.NIGHT, "Nuit")
                WallpaperChoice(vm, WallpaperKind.PHOTO, "Photo")
            }
            Divider()
            ToggleRow("Fond animé", vm.animateWallpaper) { vm.setAnimateWallpaperValue(it) }
        }

        Group("Icônes") {
            Row(Modifier.fillMaxWidth().padding(14.dp)) {
                Segmented(
                    options = listOf("Par défaut", "Claires (verre)"),
                    selected = if (vm.iconStyle == IconStyle.DEFAULT) 0 else 1,
                ) { i -> view.tick(); vm.setIconStyleValue(if (i == 0) IconStyle.DEFAULT else IconStyle.CLEAR) }
            }
            Divider()
            ToggleRow("Noms des apps", vm.showLabels) { vm.setShowLabelsValue(it) }
        }

        Group("Général") {
            NavRow(Icons.Rounded.Home, "Définir comme écran d'accueil",
                if (actions.isDefaultHome()) "Actif" else null) { actions.requestDefaultHome() }
            Divider()
            NavRow(Icons.Rounded.PlayCircle, "Revoir le guide de bienvenue") { vm.replayOnboarding() }
            Divider()
            NavRow(Icons.Rounded.RestartAlt, "Réinitialiser l'écran d'accueil") {
                vm.resetLayout(); vm.settingsOpen = false
            }
        }

        Spacer(Modifier.height(10.dp))
        Text(
            "Liquid Glass 1.0 — fait maison par Bruce",
            color = Color.White.copy(alpha = 0.45f), fontSize = 12.sp,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
    }
}

@Composable
private fun Group(title: String, content: @Composable () -> Unit) {
    Text(
        title.uppercase(), color = Color.White.copy(alpha = 0.55f), fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 14.dp, bottom = 6.dp),
    )
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White.copy(alpha = 0.08f))
            .border(0.6.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(24.dp))
    ) {
        Column { content() }
    }
    Spacer(Modifier.height(22.dp))
}

@Composable
private fun Divider() {
    Box(Modifier.padding(start = 16.dp).fillMaxWidth().height(0.5.dp).background(Color.White.copy(alpha = 0.12f)))
}

@Composable
private fun WallpaperChoice(vm: LauncherViewModel, kind: WallpaperKind, label: String) {
    val actions = LocalActions.current
    val view = LocalView.current
    val screen = LocalScreenSize.current
    val time = LocalWallTime.current
    val photo = LocalWallpaper.current.photo
    val selected = vm.wallpaper == kind
    val s by animateFloatAsState(if (selected) 1f else 0.94f, Motion.bouncy(), label = "wp")
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .graphicsLayer { scaleX = s; scaleY = s }
                .size(62.dp, 112.dp)
                .clip(RoundedCornerShape(14.dp))
                .border(
                    if (selected) 2.5.dp else 0.6.dp,
                    if (selected) Color.White else Color.White.copy(alpha = 0.25f),
                    RoundedCornerShape(14.dp),
                )
                .pointerInput(kind) {
                    detectTapGestures {
                        view.tick()
                        if (kind == WallpaperKind.PHOTO) actions.pickPhoto() else vm.setWallpaperKind(kind)
                    }
                }
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val k = size.width / screen.width
                scale(k, k, pivot = Offset.Zero) {
                    drawWallpaper(WallpaperSpec(kind, photo), time.value, screen)
                }
            }
            if (kind == WallpaperKind.PHOTO && photo == null) {
                Icon(Icons.Rounded.Image, null, tint = Color.White, modifier = Modifier.align(Alignment.Center))
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(label, color = Color.White, fontSize = 12.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
    }
}

@Composable
private fun ToggleRow(label: String, value: Boolean, onChange: (Boolean) -> Unit) {
    val view = LocalView.current
    Row(
        Modifier
            .fillMaxWidth()
            .height(54.dp)
            .pointerInput(value) { detectTapGestures { view.tick(); onChange(!value) } }
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = Color.White, fontSize = 16.sp, modifier = Modifier.weight(1f))
        IosSwitch(value)
    }
}

@Composable
fun IosSwitch(on: Boolean) {
    val x by animateDpAsState(if (on) 22.dp else 2.dp, Motion.bouncy(), label = "knob")
    val track by animateColorAsState(if (on) Color(0xFF34C759) else Color.White.copy(alpha = 0.18f), label = "track")
    Box(Modifier.size(51.dp, 31.dp).background(track, RoundedCornerShape(50))) {
        Box(
            Modifier
                .offset(x = x, y = 2.dp)
                .size(27.dp)
                .graphicsLayer { shadowElevation = 6f; shape = CircleShape; clip = true }
                .background(Color.White, CircleShape)
        )
    }
}

@Composable
private fun NavRow(icon: ImageVector, label: String, trailing: String? = null, onClick: () -> Unit) {
    val view = LocalView.current
    var pressed by remember { mutableStateOf(false) }
    Row(
        Modifier
            .fillMaxWidth()
            .height(54.dp)
            .background(if (pressed) Color.White.copy(alpha = 0.08f) else Color.Transparent)
            .pointerInput(label) {
                detectTapGestures(
                    onPress = { pressed = true; tryAwaitRelease(); pressed = false },
                    onTap = { view.tick(); onClick() },
                )
            }
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(30.dp).background(Color.White.copy(alpha = 0.14f), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, null, tint = Color.White, modifier = Modifier.size(18.dp)) }
        Spacer(Modifier.width(12.dp))
        Text(label, color = Color.White, fontSize = 16.sp, modifier = Modifier.weight(1f))
        if (trailing != null) Text(trailing, color = Color.White.copy(alpha = 0.5f), fontSize = 15.sp)
        Icon(Icons.Rounded.ChevronRight, null, tint = Color.White.copy(alpha = 0.35f))
    }
}

@Composable
fun Segmented(options: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(36.dp)
            .background(Color.White.copy(alpha = 0.10f), RoundedCornerShape(50))
            .padding(3.dp)
    ) {
        val fraction by animateFloatAsState(selected.toFloat(), Motion.snappy(), label = "seg")
        androidx.compose.foundation.layout.BoxWithConstraints(Modifier.fillMaxSize()) {
            val w = maxWidth / options.size
            Box(
                Modifier
                    .offset(x = w * fraction)
                    .size(w, maxHeight)
                    .background(Color.White.copy(alpha = 0.28f), RoundedCornerShape(50))
            )
            Row(Modifier.fillMaxSize()) {
                options.forEachIndexed { i, label ->
                    Box(
                        Modifier
                            .weight(1f)
                            .fillMaxSize()
                            .pointerInput(i) { detectTapGestures { onSelect(i) } },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(label, color = Color.White, fontSize = 14.sp,
                            fontWeight = if (i == selected) FontWeight.SemiBold else FontWeight.Normal)
                    }
                }
            }
        }
    }
}
