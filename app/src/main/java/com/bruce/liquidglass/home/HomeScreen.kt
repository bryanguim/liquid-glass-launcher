package com.bruce.liquidglass.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bruce.liquidglass.LauncherViewModel
import com.bruce.liquidglass.MenuTarget
import com.bruce.liquidglass.SearchMode
import com.bruce.liquidglass.data.DockStyle
import com.bruce.liquidglass.data.SwipeDownAction
import com.bruce.liquidglass.data.HomeLayout
import com.bruce.liquidglass.data.WidgetKey
import com.bruce.liquidglass.ui.CoordsRef
import com.bruce.liquidglass.ui.Glass
import com.bruce.liquidglass.ui.LocalBackdrop
import com.bruce.liquidglass.ui.LocalScreenSize
import com.bruce.liquidglass.ui.Motion
import com.bruce.liquidglass.ui.drawBackdrop
import com.bruce.liquidglass.ui.thud
import com.bruce.liquidglass.ui.tick
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.hypot

@Composable
fun HomeScreen(vm: LauncherViewModel) {
    val view = LocalView.current
    val actions = LocalActions.current
    val s = vm.settings

    val homeCount = vm.homePages.size
    val extraPage = if (vm.editing) 1 else 0
    val pageCount = homeCount + extraPage + 1 // + App Library
    val pagerState = rememberPagerState(pageCount = { pageCount })

    // Icons "zoom back" into place, like iOS when you leave an app.
    val appear = remember { Animatable(0f) }
    LaunchedEffect(Unit) { appear.animateTo(1f, tween(800, easing = FastOutSlowInEasing)) }
    LaunchedEffect(Unit) {
        vm.returnedHome.collect {
            if (vm.settings.returnAnimation && !vm.settings.reducedMotion) {
                appear.snapTo(0.25f)
                appear.animateTo(1f, tween(480, easing = FastOutSlowInEasing))
            }
        }
    }
    LaunchedEffect(Unit) {
        vm.homePressed.collect {
            if (!vm.closeOverlays()) pagerState.animateScrollToPage(0)
        }
    }

    LaunchedEffect(pagerState.currentPage) { vm.currentPage = pagerState.currentPage.coerceAtMost(homeCount - 1).coerceAtLeast(0) }

    val overlayOpen = vm.widgetMenu != null || vm.widgetPickerOpen || vm.menu != null || vm.search != null || vm.openCategory != null || vm.settingsOpen
    val overlay by animateFloatAsState(if (overlayOpen) 1f else 0f, Motion.soft(), label = "overlay")

    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val sc = 1f - 0.06f * overlay
                    scaleX = sc; scaleY = sc
                }
                .pointerInput(Unit) {
                    var startX = 0f
                    var startY = 0f
                    var total = 0f
                    detectVerticalDragGestures(
                        onDragStart = { startX = it.x; startY = it.y; total = 0f },
                        onDragEnd = {
                            if (total > 80.dp.toPx() && !vm.editing) {
                                val fromTop = startY < size.height * 0.10f
                                when {
                                    fromTop && startX >= size.width / 2f -> actions.expandQuickSettings()
                                    fromTop || vm.settings.swipeDown == SwipeDownAction.NOTIFICATIONS -> actions.expandNotifications()
                                    else -> { view.tick(); vm.search = SearchMode.SPOTLIGHT }
                                }
                            }
                        },
                        onVerticalDrag = { change, dy -> total += dy; change.consume() },
                    )
                }
        ) {
            Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
                EditBar(vm)
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    beyondViewportPageCount = 1,
                ) { page ->
                    when {
                        page < homeCount -> HomePage(vm, page, vm.homePages.getOrNull(page) ?: emptyList()) { appear.value }
                        vm.editing && page == homeCount -> HomePage(vm, page, emptyList()) { appear.value }
                        else -> AppLibrary(vm)
                    }
                }
                PillOrDots(vm, pagerState, pageCount)
                Spacer(Modifier.height(10.dp))
                Dock(vm) { appear.value }
                Spacer(Modifier.height(8.dp))
            }
        }

        // Frosted glass between the home screen and any overlay (cheap: one bitmap draw).
        if (overlay > 0.01f) FrostScrim(overlay)

        // ---- overlays ----
        ContextMenuOverlay(vm)
        WidgetMenuOverlay(vm)
        WidgetPicker(vm)
        CategoryOverlay(vm)
        SearchOverlay(vm)
        SettingsSheet(vm)
    }
}

@Composable
fun FrostScrim(progress: Float) {
    val ref = remember { CoordsRef() }
    val backdrop = LocalBackdrop.current
    val screen = LocalScreenSize.current
    Canvas(
        Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = progress.coerceIn(0f, 1f) }
    ) {
        drawBackdrop(ref, backdrop, screen)
        drawRect(Color.Black.copy(alpha = 0.18f))
    }
}

@Composable
private fun EditBar(vm: LauncherViewModel) {
    val view = LocalView.current
    AnimatedVisibility(
        visible = vm.editing,
        enter = fadeIn(tween(220)) + slideInVertically(Motion.snappy()) { -it },
        exit = fadeOut(tween(160)) + slideOutVertically(Motion.snappy()) { -it },
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                GlassButton(text = "Widgets", icon = { Icon(Icons.Rounded.Add, null, tint = Color.White, modifier = Modifier.size(18.dp)) }) {
                    view.tick(); vm.widgetPickerOpen = true
                }
                GlassButton(text = "Réglages", icon = { Icon(Icons.Rounded.Tune, null, tint = Color.White, modifier = Modifier.size(16.dp)) }) {
                    view.tick(); vm.settingsOpen = true
                }
                Spacer(Modifier.weight(1f))
                GlassButton(text = "OK", bold = true) {
                    view.tick(); vm.editing = false; vm.selectedForMove = null
                }
            }
            val hint = if (vm.selectedForMove != null) "Touche un élément pour échanger, ou un vide pour déplacer"
            else "Touche 2 éléments pour les échanger"
            Text(
                hint, color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp,
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
    }
}

@Composable
fun GlassButton(
    text: String,
    bold: Boolean = false,
    icon: (@Composable () -> Unit)? = null,
    onClick: () -> Unit,
) {
    var pressed by remember { mutableStateOf(false) }
    val sc by animateFloatAsState(if (pressed) 0.9f else 1f, Motion.snappy(), label = "btn")
    Glass(
        Modifier
            .graphicsLayer { scaleX = sc; scaleY = sc }
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = { pressed = true; tryAwaitRelease(); pressed = false },
                    onTap = { onClick() },
                )
            },
        shape = RoundedCornerShape(50),
    ) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) { icon(); Spacer(Modifier.width(6.dp)) }
            Text(text, color = Color.White, fontSize = 15.sp, fontWeight = if (bold) FontWeight.Bold else FontWeight.Medium)
        }
    }
}

@Composable
private fun HomePage(vm: LauncherViewModel, page: Int, keys: List<String>, appear: () -> Float) {
    val view = LocalView.current
    val s = vm.settings
    val cols = s.columns
    val rows = s.rows
    BoxWithConstraints(Modifier.fillMaxSize().padding(horizontal = 10.dp, vertical = 6.dp)) {
        val rowH = maxHeight / rows
        val cellW = maxWidth / cols
        val totalRows = rows.toFloat()
        val iconSize = minOf(s.iconSize.dp, cellW - 14.dp)
        val clock = page == 0 && s.showWidget
        val firstRow = if (clock) 2 else 0

        // Background: taps / long-press on empty space. A sibling (not a parent) so widgets keep their own touches.
        Box(
            Modifier
                .matchParentSize()
                .pointerInput(page) {
                    detectTapGestures(
                        onTap = {
                            if (vm.editing) {
                                if (vm.selectedForMove != null) { view.tick(); vm.moveSelectedToPage(page) }
                                else { vm.editing = false }
                            }
                        },
                        onLongPress = {
                            if (!vm.editing) { view.thud(); vm.editing = true }
                        },
                    )
                }
        )

        if (clock) {
            ClockWidget(
                s,
                Modifier
                    .fillMaxWidth()
                    .height(rowH * 2 - 14.dp)
                    .padding(horizontal = 8.dp)
                    .appearIn(appear, 0f)
            )
        }

        val placements = remember(keys, cols) { HomeLayout.place(keys, cols).first }
        placements.forEach { pl ->
            key(pl.key) {
                val y = rowH * (firstRow + pl.row)
                val half = (cols - 1) / 2f
                val dx = if (half > 0f) (pl.col - half) / half else 0f
                val dy = ((firstRow + pl.row) - (totalRows - 1) / 2f) / ((totalRows - 1) / 2f)
                val d = (hypot(dx, dy) / 1.42f).coerceIn(0f, 1f) * 0.35f
                if (WidgetKey.isWidget(pl.key)) {
                    HomeWidget(
                        vm = vm,
                        key = pl.key,
                        modifier = Modifier
                            .offset(x = 0.dp, y = y)
                            .padding(horizontal = 8.dp, vertical = 5.dp)
                            .fillMaxWidth()
                            .height(rowH * pl.spanRows - 10.dp)
                            .appearIn(appear, d),
                    )
                } else {
                    val app = vm.appsByKey[pl.key]
                    if (app != null) {
                        Box(
                            Modifier
                                .offset(x = cellW * pl.col, y = y)
                                .width(cellW)
                                .height(rowH)
                                .padding(top = 6.dp),
                            contentAlignment = Alignment.TopCenter,
                        ) {
                            AppIcon(
                                app = app,
                                iconSize = iconSize,
                                showLabel = s.showLabels,
                                editing = vm.editing,
                                selected = vm.selectedForMove == app.key,
                                seed = abs(app.key.hashCode()),
                                onTap = { b -> if (vm.editing) vm.onEditTap(app.key) else vm.launch(app, b, view) },
                                onLongPress = { b ->
                                    if (vm.editing) vm.onEditTap(app.key)
                                    else vm.menu = MenuTarget(app, b, inDock = false, onHome = true)
                                },
                                onRemove = { vm.removeFromHome(app.key) },
                                modifier = Modifier.appearIn(appear, d),
                            )
                        }
                    }
                }
            }
        }

        if (keys.isEmpty() && page != 0) {
            Box(Modifier.matchParentSize(), contentAlignment = Alignment.Center) {
                Text(
                    if (vm.selectedForMove != null) "Touche ici pour déplacer l'élément sur cette page" else "Nouvelle page",
                    color = Color.White.copy(alpha = 0.6f), fontSize = 14.sp,
                )
            }
        }
    }
}

/** Scale + fade in, driven by a 0..1 progress with a per-item delay. */
fun Modifier.appearIn(progress: () -> Float, delay: Float): Modifier = this.graphicsLayer {
    val g = progress()
    if (g >= 1f) return@graphicsLayer
    val p = ((g - delay) / (1f - 0.35f)).coerceIn(0f, 1f)
    val e = 1f - (1f - p) * (1f - p) * (1f - p) // ease-out cubic
    val sc = 1.18f - 0.18f * e
    scaleX = sc; scaleY = sc
    alpha = e
}

@Composable
private fun PillOrDots(vm: LauncherViewModel, pager: PagerState, pageCount: Int) {
    val view = LocalView.current
    var showDots by remember { mutableStateOf(false) }
    LaunchedEffect(pager.isScrollInProgress) {
        if (pager.isScrollInProgress) showDots = true else { delay(900); showDots = false }
    }
    val scope = rememberCoroutineScope()
    val dots = showDots || vm.editing || !vm.settings.searchPill
    Box(Modifier.fillMaxWidth().padding(top = 6.dp), contentAlignment = Alignment.Center) {
        Glass(
            Modifier
                .animateContentSize(Motion.snappy())
                .pointerInput(Unit) {
                    detectTapGestures {
                        view.tick()
                        vm.search = if (pager.currentPage == pageCount - 1) SearchMode.LIBRARY else SearchMode.SPOTLIGHT
                    }
                },
            shape = RoundedCornerShape(50),
        ) {
            AnimatedContent(
                targetState = dots,
                transitionSpec = { (fadeIn(tween(180)) + scaleIn(initialScale = 0.9f)) togetherWith fadeOut(tween(120)) },
                label = "pill",
            ) { showingDots ->
                if (showingDots) {
                    Row(
                        Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        for (i in 0 until pageCount) {
                            val active = pager.currentPage == i
                            Box(
                                Modifier
                                    .size(7.dp)
                                    .background(Color.White.copy(alpha = if (active) 1f else 0.35f), CircleShape)
                                    .pointerInput(i) { detectTapGestures { scope.launch { pager.animateScrollToPage(i) } } }
                            )
                        }
                    }
                } else {
                    Row(
                        Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Rounded.Search, null, tint = Color.White, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(5.dp))
                        Text("Rechercher", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun Dock(vm: LauncherViewModel, appear: () -> Float) {
    val view = LocalView.current
    val s = vm.settings
    val iconSize = minOf(s.iconSize.dp, if (s.dockSize >= 5) 56.dp else 64.dp)
    val content: @Composable () -> Unit = {
        if (vm.dock.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    if (vm.editing) "Sélectionne une app puis touche le dock" else "Dock vide — appui long sur une app",
                    color = Color.White.copy(alpha = 0.6f), fontSize = 13.sp,
                )
            }
        }
        Row(
            Modifier.fillMaxSize().padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            vm.dock.forEach { key ->
                val app = vm.appsByKey[key] ?: return@forEach
                AppIcon(
                    app = app,
                    iconSize = iconSize,
                    showLabel = false,
                    editing = vm.editing,
                    selected = vm.selectedForMove == key,
                    seed = abs(key.hashCode()),
                    onTap = { b -> if (vm.editing) vm.onEditTap(key) else vm.launch(app, b, view) },
                    onLongPress = { b ->
                        if (vm.editing) vm.onEditTap(key)
                        else vm.menu = MenuTarget(app, b, inDock = true, onHome = true)
                    },
                    onRemove = { vm.removeFromDock(key) },
                )
            }
        }
    }
    val mod = Modifier
        .padding(horizontal = 10.dp)
        .fillMaxWidth()
        .height(iconSize + 34.dp)
        .appearIn(appear, 0.1f)
        .pointerInput(Unit) {
            detectTapGestures {
                if (vm.editing && vm.selectedForMove != null) { view.tick(); vm.moveSelectedToDock() }
            }
        }
    when (s.dockStyle) {
        DockStyle.GLASS -> Glass(mod, shape = RoundedCornerShape(38.dp)) { content() }
        DockStyle.CLEAR -> Glass(mod, shape = RoundedCornerShape(38.dp), tint = Color.Transparent, rim = true) { content() }
        DockStyle.NONE -> Box(mod) { content() }
    }
}
