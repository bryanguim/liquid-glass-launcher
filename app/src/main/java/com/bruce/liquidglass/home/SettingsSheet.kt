package com.bruce.liquidglass.home

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Lens
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
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
import com.bruce.liquidglass.data.ClockStyle
import com.bruce.liquidglass.data.DockStyle
import com.bruce.liquidglass.data.IconShape
import com.bruce.liquidglass.data.IconStyle
import com.bruce.liquidglass.data.LauncherSettings
import com.bruce.liquidglass.data.SwipeDownAction
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

private enum class Page(val title: String) {
    ROOT("Réglages"), WALLPAPER("Fond d'écran"), ICONS("Icônes"), HOME("Écran d'accueil"),
    DOCK("Dock"), GLASS("Verre liquide"), MOTION("Animations et fluidité"), HIDDEN("Apps masquées"),
}

@Composable
fun SettingsSheet(vm: LauncherViewModel) {
    var page by remember { mutableStateOf(Page.ROOT) }
    BackHandler(enabled = vm.settingsOpen) {
        if (page != Page.ROOT) page = Page.ROOT else vm.settingsOpen = false
    }
    AnimatedVisibility(visible = vm.settingsOpen, enter = fadeIn(), exit = fadeOut()) {
        val avScope = this
        var drag by remember { mutableFloatStateOf(0f) }
        Box(
            Modifier
                .fillMaxSize()
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
                    .padding(top = 40.dp)
                    .fillMaxWidth()
                    .offset { IntOffset(0, drag.roundToInt().coerceAtLeast(0)) }
                    .pointerInput(Unit) { detectTapGestures { } },
                shape = RoundedCornerShape(topStart = 38.dp, topEnd = 38.dp),
                tint = Color(0x66000000),
            ) {
                Column(Modifier.navigationBarsPadding()) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(28.dp)
                            .pointerInput(Unit) {
                                detectVerticalDragGestures(
                                    onDragEnd = {
                                        if (drag > 160f) { vm.settingsOpen = false; page = Page.ROOT }
                                        drag = 0f
                                    },
                                    onVerticalDrag = { _, dy -> drag += dy },
                                )
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(Modifier.size(40.dp, 5.dp).background(Color.White.copy(alpha = 0.4f), RoundedCornerShape(50)))
                    }
                    AnimatedContent(
                        targetState = page,
                        transitionSpec = {
                            val forward = targetState != Page.ROOT
                            (slideInHorizontally(Motion.snappy()) { if (forward) it else -it / 3 } + fadeIn(tween(200))) togetherWith
                                (slideOutHorizontally(Motion.snappy()) { if (forward) -it / 3 else it } + fadeOut(tween(150)))
                        },
                        label = "settingsPage",
                    ) { p ->
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = 18.dp)
                                .padding(bottom = 28.dp)
                        ) {
                            if (p == Page.ROOT) {
                                Text("Réglages", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                            } else {
                                Row(
                                    Modifier
                                        .pointerInput(Unit) { detectTapGestures { page = Page.ROOT } }
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Icon(Icons.Rounded.ChevronLeft, null, tint = Color.White, modifier = Modifier.size(26.dp))
                                    Text("Réglages", color = Color.White, fontSize = 16.sp)
                                }
                                Spacer(Modifier.height(4.dp))
                                Text(p.title, color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(Modifier.height(18.dp))
                            when (p) {
                                Page.ROOT -> RootPage(vm) { page = it }
                                Page.WALLPAPER -> WallpaperPage(vm)
                                Page.ICONS -> IconsPage(vm)
                                Page.HOME -> HomePageSettings(vm)
                                Page.DOCK -> DockPage(vm)
                                Page.GLASS -> GlassPage(vm)
                                Page.MOTION -> MotionPage(vm)
                                Page.HIDDEN -> HiddenPage(vm)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ------------------------------------------------------------------ pages

@Composable
private fun RootPage(vm: LauncherViewModel, open: (Page) -> Unit) {
    val actions = LocalActions.current
    Group(null) {
        NavRow(Icons.Rounded.Wallpaper, Color(0xFF0A84FF), "Fond d'écran") { open(Page.WALLPAPER) }
        Divider()
        NavRow(Icons.Rounded.AutoAwesome, Color(0xFFBF5AF2), "Icônes") { open(Page.ICONS) }
        Divider()
        NavRow(Icons.Rounded.GridView, Color(0xFF30D158), "Écran d'accueil et widgets") { open(Page.HOME) }
        Divider()
        NavRow(Icons.Rounded.Dashboard, Color(0xFFFF9F0A), "Dock") { open(Page.DOCK) }
        Divider()
        NavRow(Icons.Rounded.Lens, Color(0xFF64D2FF), "Verre liquide") { open(Page.GLASS) }
        Divider()
        NavRow(Icons.Rounded.Speed, Color(0xFFFF375F), "Animations et fluidité") { open(Page.MOTION) }
        Divider()
        NavRow(Icons.Rounded.VisibilityOff, Color(0xFF8E8E93), "Apps masquées",
            trailing = vm.settings.hidden.size.takeIf { it > 0 }?.toString()) { open(Page.HIDDEN) }
    }
    Group(null) {
        NavRow(Icons.Rounded.Home, Color(0xFF34C759), "Définir comme écran d'accueil",
            trailing = if (actions.isDefaultHome()) "Actif" else null) { actions.requestDefaultHome() }
        Divider()
        NavRow(Icons.Rounded.PlayCircle, Color(0xFF5E5CE6), "Revoir le guide de bienvenue") { vm.replayOnboarding() }
        Divider()
        NavRow(Icons.Rounded.RestartAlt, Color(0xFFFF453A), "Réinitialiser l'écran d'accueil") {
            vm.resetLayout(); vm.settingsOpen = false
        }
    }
    Text(
        "Liquid Glass 2.1 — fait maison par Bruce",
        color = Color.White.copy(alpha = 0.45f), fontSize = 12.sp,
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
    )
}

@Composable
private fun WallpaperPage(vm: LauncherViewModel) {
    val s = vm.settings
    val actions = LocalActions.current
    Group("Choisis un fond") {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                WallpaperChoice(vm, WallpaperKind.LIQUID, "Liquide")
                WallpaperChoice(vm, WallpaperKind.AURORA, "Aurore")
                WallpaperChoice(vm, WallpaperKind.OCEAN, "Océan")
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                WallpaperChoice(vm, WallpaperKind.SUNSET, "Couchant")
                WallpaperChoice(vm, WallpaperKind.NIGHT, "Nuit")
                WallpaperChoice(vm, WallpaperKind.PHOTO, "Photo")
            }
        }
        if (vm.photo != null) {
            Divider()
            ActionRow("Changer de photo") { actions.pickPhoto() }
        }
    }
    Group("Réglages du fond") {
        SliderRow("Assombrir", s.wallpaperDim, 0f..0.6f, { "${(it * 100).roundToInt()} %" }) { v -> vm.update { it.copy(wallpaperDim = v) } }
        Divider()
        ToggleRow("Fond animé", s.animateWallpaper) { v -> vm.update { it.copy(animateWallpaper = v) } }
        if (s.animateWallpaper) {
            Divider()
            SegmentedRow("Vitesse", listOf("Lente", "Normale", "Rapide"),
                when { s.wallpaperSpeed < 0.75f -> 0; s.wallpaperSpeed > 1.5f -> 2; else -> 1 }) { i ->
                vm.update { it.copy(wallpaperSpeed = listOf(0.5f, 1f, 2.2f)[i]) }
            }
        }
    }
}

@Composable
private fun IconsPage(vm: LauncherViewModel) {
    val s = vm.settings
    // live preview with the user's own apps
    val preview = remember(vm.apps, vm.dock) {
        (vm.dock.mapNotNull { vm.appsByKey[it] } + vm.visibleApps).distinctBy { it.key }.take(4)
    }
    Glass(Modifier.fillMaxWidth().height(118.dp), shape = RoundedCornerShape(26.dp)) {
        Row(
            Modifier.fillMaxSize().padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            preview.forEach { app ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconImage(app, s.iconSize.dp.coerceAtMost(64.dp))
                    if (s.showLabels) {
                        Spacer(Modifier.height(5.dp))
                        Text(app.label, style = LabelStyle.copy(fontSize = s.labelSize.sp), maxLines = 1,
                            modifier = Modifier.width(72.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    }
                }
            }
        }
        if (vm.rebaking) {
            Text("Mise à jour…", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp,
                modifier = Modifier.align(Alignment.TopEnd).padding(10.dp))
        }
    }
    Spacer(Modifier.height(22.dp))
    Group("Style") {
        StyleChooser(s) { st -> vm.update { it.copy(iconStyle = st) } }
        if (s.iconStyle == IconStyle.TINTED) {
            Divider()
            ColorRow(s.iconTint) { c -> vm.update { it.copy(iconTint = c) } }
        }
        Divider()
        ToggleRow("Reflets Liquid Glass", s.iconSheen) { v -> vm.update { it.copy(iconSheen = v) } }
    }
    Group("Forme") {
        SegmentedRow(null, listOf("iOS", "Arrondie", "Ronde"), s.iconShape.ordinal) { i ->
            vm.update { it.copy(iconShape = IconShape.entries[i]) }
        }
    }
    Group("Taille") {
        SliderRow("Icônes", s.iconSize.toFloat(), 46f..74f, { "${it.roundToInt()}" }) { v -> vm.update { it.copy(iconSize = v.roundToInt()) } }
        Divider()
        ToggleRow("Noms des apps", s.showLabels) { v -> vm.update { it.copy(showLabels = v) } }
        if (s.showLabels) {
            Divider()
            SliderRow("Texte", s.labelSize, 9f..14f, { String.format("%.1f", it) }) { v -> vm.update { it.copy(labelSize = v) } }
        }
    }
}

@Composable
private fun HomePageSettings(vm: LauncherViewModel) {
    val s = vm.settings
    Group("Grille") {
        SegmentedRow("Colonnes", listOf("3", "4", "5"), s.columns - 3) { i -> vm.update { it.copy(columns = i + 3) } }
        Divider()
        SegmentedRow("Lignes", listOf("5", "6", "7"), s.rows - 5) { i -> vm.update { it.copy(rows = i + 5) } }
    }
    Group("Widget horloge") {
        ToggleRow("Afficher le widget", s.showWidget) { v -> vm.update { it.copy(showWidget = v) } }
        if (s.showWidget) {
            Divider()
            SegmentedRow("Chiffres", listOf("Fins", "Normaux", "Gras"), s.clockStyle.ordinal) { i ->
                vm.update { it.copy(clockStyle = ClockStyle.entries[i]) }
            }
            Divider()
            ToggleRow("Batterie", s.showBattery) { v -> vm.update { it.copy(showBattery = v) } }
        }
    }
    Group("Widgets") {
        NavRowPlain("Ajouter un widget") { vm.settingsOpen = false; vm.widgetPickerOpen = true }
        Divider()
        ToggleRow("Fond en verre derrière les widgets", s.widgetGlass) { v -> vm.update { it.copy(widgetGlass = v) } }
    }
    Group("Navigation") {
        ToggleRow("Bouton « Rechercher »", s.searchPill) { v -> vm.update { it.copy(searchPill = v) } }
        Divider()
        SegmentedRow("Glisser vers le bas", listOf("Recherche", "Notifications"), s.swipeDown.ordinal) { i ->
            vm.update { it.copy(swipeDown = SwipeDownAction.entries[i]) }
        }
    }
}

@Composable
private fun DockPage(vm: LauncherViewModel) {
    val s = vm.settings
    Group("Dock") {
        SegmentedRow("Nombre d'apps", listOf("3", "4", "5"), s.dockSize - 3) { i -> vm.update { it.copy(dockSize = i + 3) } }
        Divider()
        SegmentedRow("Style", listOf("Verre", "Transparent", "Aucun"), s.dockStyle.ordinal) { i ->
            vm.update { it.copy(dockStyle = DockStyle.entries[i]) }
        }
    }
}

@Composable
private fun GlassPage(vm: LauncherViewModel) {
    val s = vm.settings
    Glass(Modifier.fillMaxWidth().height(110.dp), shape = RoundedCornerShape(30.dp)) {
        Text("Aperçu du verre", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold,
            modifier = Modifier.align(Alignment.Center))
    }
    Spacer(Modifier.height(22.dp))
    Group("Matière") {
        SliderRow("Opacité", s.glassTint, 0f..0.32f, { "${(it * 100 / 0.32f).roundToInt()} %" }) { v -> vm.update { it.copy(glassTint = v) } }
        Divider()
        ToggleRow("Contour lumineux", s.glassRim) { v -> vm.update { it.copy(glassRim = v) } }
    }
}

@Composable
private fun MotionPage(vm: LauncherViewModel) {
    val s = vm.settings
    Group("Fluidité") {
        ToggleRow("Fréquence max (120 Hz)", s.highRefresh) { v -> vm.update { it.copy(highRefresh = v) } }
        Divider()
        ToggleRow("Fond animé", s.animateWallpaper) { v -> vm.update { it.copy(animateWallpaper = v) } }
    }
    Group("Animations") {
        ToggleRow("Zoom au retour sur l'accueil", s.returnAnimation) { v -> vm.update { it.copy(returnAnimation = v) } }
        Divider()
        ToggleRow("Animations réduites", s.reducedMotion) { v -> vm.update { it.copy(reducedMotion = v) } }
        Divider()
        ToggleRow("Vibrations", s.haptics) { v -> vm.update { it.copy(haptics = v) } }
    }
    Text(
        "Astuce : pour une fluidité maximale, coupe le fond animé et garde le 120 Hz.",
        color = Color.White.copy(alpha = 0.55f), fontSize = 13.sp, modifier = Modifier.padding(horizontal = 14.dp),
    )
}

@Composable
private fun HiddenPage(vm: LauncherViewModel) {
    val hidden = vm.settings.hidden.mapNotNull { vm.appsByKey[it] }.sortedBy { it.label.lowercase() }
    if (hidden.isEmpty()) {
        Text(
            "Aucune app masquée. Fais un appui long sur une app puis « Masquer l'app ».",
            color = Color.White.copy(alpha = 0.6f), fontSize = 15.sp, modifier = Modifier.padding(horizontal = 6.dp),
        )
        return
    }
    Group(null) {
        hidden.forEachIndexed { i, app ->
            Row(
                Modifier.fillMaxWidth().height(60.dp).padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconImage(app, 40.dp)
                Spacer(Modifier.width(12.dp))
                Text(app.label, color = Color.White, fontSize = 16.sp, modifier = Modifier.weight(1f), maxLines = 1)
                Text(
                    "Afficher", color = Color(0xFF0A84FF), fontSize = 16.sp, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.pointerInput(app.key) { detectTapGestures { vm.unhideApp(app.key) } },
                )
            }
            if (i < hidden.lastIndex) Divider()
        }
    }
}

// ------------------------------------------------------------------ building blocks

@Composable
private fun Group(title: String?, content: @Composable () -> Unit) {
    if (title != null) Text(
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
    val selected = vm.settings.wallpaper == kind
    val sc by animateFloatAsState(if (selected) 1f else 0.94f, Motion.bouncy(), label = "wp")
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .graphicsLayer { scaleX = sc; scaleY = sc }
                .size(78.dp, 140.dp)
                .clip(RoundedCornerShape(16.dp))
                .border(
                    if (selected) 2.5.dp else 0.6.dp,
                    if (selected) Color.White else Color.White.copy(alpha = 0.25f),
                    RoundedCornerShape(16.dp),
                )
                .pointerInput(kind) {
                    detectTapGestures {
                        view.tick()
                        if (kind == WallpaperKind.PHOTO && photo == null) actions.pickPhoto() else vm.setWallpaperKind(kind)
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
private fun StyleChooser(s: LauncherSettings, onPick: (IconStyle) -> Unit) {
    val view = LocalView.current
    val labels = listOf("Par défaut", "Sombre", "Verre", "Teinté")
    Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        IconStyle.entries.forEachIndexed { i, st ->
            val selected = s.iconStyle == st
            val sc by animateFloatAsState(if (selected) 1f else 0.92f, Motion.bouncy(), label = "st$i")
            Column(
                Modifier
                    .graphicsLayer { scaleX = sc; scaleY = sc }
                    .pointerInput(st) { detectTapGestures { view.tick(); onPick(st) } },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                StyleSwatch(st, s.iconTint, selected)
                Spacer(Modifier.height(6.dp))
                Text(labels[i], color = Color.White, fontSize = 12.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
            }
        }
    }
}

/** Tiny drawing of what each icon style looks like. */
@Composable
private fun StyleSwatch(style: IconStyle, tint: Int, selected: Boolean) {
    val shape = com.bruce.liquidglass.ui.Squircle
    val bg = when (style) {
        IconStyle.DEFAULT -> androidx.compose.ui.graphics.Brush.linearGradient(listOf(Color(0xFF5AC8FA), Color(0xFF007AFF)))
        IconStyle.DARK, IconStyle.TINTED -> androidx.compose.ui.graphics.Brush.verticalGradient(listOf(Color(0xFF2E2E32), Color(0xFF0A0A0C)))
        IconStyle.CLEAR -> androidx.compose.ui.graphics.Brush.linearGradient(listOf(Color.White.copy(alpha = 0.32f), Color.White.copy(alpha = 0.10f)))
    }
    val glyph = when (style) {
        IconStyle.DEFAULT -> Color.White
        IconStyle.DARK -> Color(0xFF0A84FF)
        IconStyle.CLEAR -> Color.White.copy(alpha = 0.9f)
        IconStyle.TINTED -> Color(tint)
    }
    Box(
        Modifier
            .size(58.dp)
            .clip(shape)
            .background(bg)
            .border(if (selected) 2.5.dp else 0.8.dp, Color.White.copy(alpha = if (selected) 1f else 0.35f), shape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Rounded.Apps, null, tint = glyph, modifier = Modifier.size(28.dp))
    }
}

private val tintChoices = listOf(
    0xFFFFFFFF, 0xFF0A84FF, 0xFF64D2FF, 0xFF30D158, 0xFFFFD60A, 0xFFFF9F0A, 0xFFFF375F, 0xFFBF5AF2,
).map { it.toInt() }

@Composable
private fun ColorRow(selected: Int, onPick: (Int) -> Unit) {
    val view = LocalView.current
    Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        tintChoices.forEach { c ->
            val on = c == selected
            val sc by animateFloatAsState(if (on) 1.15f else 1f, Motion.bouncy(), label = "c")
            Box(
                Modifier
                    .graphicsLayer { scaleX = sc; scaleY = sc }
                    .size(30.dp)
                    .background(Color(c), CircleShape)
                    .border(if (on) 2.5.dp else 0.dp, if (on) Color.White else Color.Transparent, CircleShape)
                    .pointerInput(c) { detectTapGestures { view.tick(); onPick(c) } }
            )
        }
    }
}

@Composable
private fun ToggleRow(label: String, value: Boolean, onChange: (Boolean) -> Unit) {
    val view = LocalView.current
    val current by rememberUpdatedState(value)
    val action by rememberUpdatedState(onChange)
    Row(
        Modifier
            .fillMaxWidth()
            .height(54.dp)
            .pointerInput(Unit) { detectTapGestures { view.tick(); action(!current) } }
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = Color.White, fontSize = 16.sp, modifier = Modifier.weight(1f))
        IosSwitch(value)
    }
}

@Composable
private fun NavRowPlain(label: String, onClick: () -> Unit) {
    val view = LocalView.current
    Row(
        Modifier
            .fillMaxWidth()
            .height(52.dp)
            .pointerInput(label) { detectTapGestures { view.tick(); onClick() } }
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = Color(0xFF0A84FF), fontSize = 16.sp, modifier = Modifier.weight(1f))
        Icon(Icons.Rounded.ChevronRight, null, tint = Color.White.copy(alpha = 0.35f))
    }
}

@Composable
private fun ActionRow(label: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(50.dp)
            .pointerInput(label) { detectTapGestures { onClick() } }
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = Color(0xFF0A84FF), fontSize = 16.sp)
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
                .background(Color.White, CircleShape)
        )
    }
}

@Composable
private fun NavRow(icon: ImageVector, color: Color, label: String, trailing: String? = null, onClick: () -> Unit) {
    val view = LocalView.current
    var pressed by remember { mutableStateOf(false) }
    val action by rememberUpdatedState(onClick)
    Row(
        Modifier
            .fillMaxWidth()
            .height(54.dp)
            .background(if (pressed) Color.White.copy(alpha = 0.08f) else Color.Transparent)
            .pointerInput(label) {
                detectTapGestures(
                    onPress = { pressed = true; tryAwaitRelease(); pressed = false },
                    onTap = { view.tick(); action() },
                )
            }
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(30.dp).background(color, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, null, tint = Color.White, modifier = Modifier.size(18.dp)) }
        Spacer(Modifier.width(12.dp))
        Text(label, color = Color.White, fontSize = 16.sp, modifier = Modifier.weight(1f))
        if (trailing != null) Text(trailing, color = Color.White.copy(alpha = 0.5f), fontSize = 15.sp)
        Icon(Icons.Rounded.ChevronRight, null, tint = Color.White.copy(alpha = 0.35f))
    }
}

@Composable
private fun SegmentedRow(label: String?, options: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    val view = LocalView.current
    Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp)) {
        if (label != null) {
            Text(label, color = Color.White, fontSize = 16.sp)
            Spacer(Modifier.height(10.dp))
        }
        Segmented(options, selected.coerceIn(0, options.lastIndex)) { view.tick(); onSelect(it) }
    }
}

@Composable
fun Segmented(options: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    val action by rememberUpdatedState(onSelect)
    Box(
        Modifier
            .fillMaxWidth()
            .height(36.dp)
            .background(Color.White.copy(alpha = 0.10f), RoundedCornerShape(50))
            .padding(3.dp)
    ) {
        val fraction by animateFloatAsState(selected.toFloat(), Motion.snappy(), label = "seg")
        BoxWithConstraints(Modifier.fillMaxSize()) {
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
                            .pointerInput(i) { detectTapGestures { action(i) } },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(label, color = Color.White, fontSize = 13.sp, maxLines = 1,
                            fontWeight = if (i == selected) FontWeight.SemiBold else FontWeight.Normal)
                    }
                }
            }
        }
    }
}

@Composable
private fun SliderRow(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    format: (Float) -> String,
    onChange: (Float) -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, color = Color.White, fontSize = 16.sp, modifier = Modifier.weight(1f))
            Text(format(value), color = Color.White.copy(alpha = 0.6f), fontSize = 15.sp)
        }
        Spacer(Modifier.height(12.dp))
        GlassSlider(value, range, onChange)
    }
}

/** iOS-style slider: thin track, white fill, big white knob that grows while dragging. */
@Composable
fun GlassSlider(value: Float, range: ClosedFloatingPointRange<Float>, onChange: (Float) -> Unit) {
    val action by rememberUpdatedState(onChange)
    var dragging by remember { mutableStateOf(false) }
    val knob by animateFloatAsState(if (dragging) 1.25f else 1f, Motion.bouncy(), label = "knob")
    val frac = ((value - range.start) / (range.endInclusive - range.start)).coerceIn(0f, 1f)
    BoxWithConstraints(
        Modifier
            .fillMaxWidth()
            .height(30.dp)
            .pointerInput(range) {
                fun set(x: Float) {
                    val f = (x / size.width).coerceIn(0f, 1f)
                    action(range.start + f * (range.endInclusive - range.start))
                }
                detectTapGestures(onTap = { set(it.x) })
            }
            .pointerInput(range) {
                fun set(x: Float) {
                    val f = (x / size.width).coerceIn(0f, 1f)
                    action(range.start + f * (range.endInclusive - range.start))
                }
                detectHorizontalDragGestures(
                    onDragStart = { dragging = true; set(it.x) },
                    onDragEnd = { dragging = false },
                    onDragCancel = { dragging = false },
                    onHorizontalDrag = { change, _ -> change.consume(); set(change.position.x) },
                )
            },
        contentAlignment = Alignment.CenterStart,
    ) {
        val knobSize = 26.dp
        Box(Modifier.fillMaxWidth().height(6.dp).background(Color.White.copy(alpha = 0.18f), RoundedCornerShape(50)))
        Box(Modifier.width(maxWidth * frac).height(6.dp).background(Color.White, RoundedCornerShape(50)))
        Box(
            Modifier
                .offset(x = (maxWidth - knobSize) * frac)
                .size(knobSize)
                .graphicsLayer { scaleX = knob; scaleY = knob }
                .background(Color.White, CircleShape)
        )
    }
}
