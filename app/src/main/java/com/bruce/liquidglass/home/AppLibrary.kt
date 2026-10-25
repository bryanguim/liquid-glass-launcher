package com.bruce.liquidglass.home

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bruce.liquidglass.LauncherViewModel
import com.bruce.liquidglass.MenuTarget
import com.bruce.liquidglass.SearchMode
import com.bruce.liquidglass.data.AppInfo
import com.bruce.liquidglass.data.Categories
import com.bruce.liquidglass.ui.Glass
import com.bruce.liquidglass.ui.Motion
import com.bruce.liquidglass.ui.tick
import kotlin.math.abs

data class LibraryGroup(val title: String, val apps: List<AppInfo>, val allLaunchable: Boolean)

fun libraryGroups(vm: LauncherViewModel): List<LibraryGroup> {
    val apps = vm.visibleApps
    val out = mutableListOf<LibraryGroup>()
    val suggestions = vm.recents.filter { it !in vm.settings.hidden }.mapNotNull { vm.appsByKey[it] }.take(4)
        .ifEmpty { apps.filter { !it.isSystem }.take(4) }
    if (suggestions.isNotEmpty()) out += LibraryGroup("Suggestions", suggestions, true)
    val recent = apps.sortedByDescending { it.installTime }.take(4)
    if (recent.isNotEmpty()) out += LibraryGroup("Ajouts récents", recent, true)
    val byCat = apps.groupBy { it.category }
    for (c in Categories.order) {
        val list = byCat[c] ?: continue
        out += LibraryGroup(c, list, false)
    }
    return out
}

@Composable
fun AppLibrary(vm: LauncherViewModel) {
    val view = LocalView.current
    val groups = remember(vm.apps, vm.recents, vm.settings.hidden) { libraryGroups(vm) }
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 6.dp, bottom = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item(span = { GridItemSpan(2) }) {
            Glass(
                Modifier
                    .fillMaxWidth()
                    .height(42.dp)
                    .pointerInput(Unit) { detectTapGestures { view.tick(); vm.search = SearchMode.LIBRARY } },
                shape = RoundedCornerShape(50),
            ) {
                Row(Modifier.align(Alignment.Center), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Search, null, tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(17.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Bibliothèque d'apps", color = Color.White.copy(alpha = 0.8f), fontSize = 16.sp)
                }
            }
        }
        items(groups, key = { it.title }) { g -> LibraryTile(vm, g) }
    }
}

@Composable
private fun LibraryTile(vm: LauncherViewModel, g: LibraryGroup) {
    val view = LocalView.current
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Glass(Modifier.fillMaxWidth().aspectRatio(1f), shape = RoundedCornerShape(26.dp)) {
            BoxWithConstraints(Modifier.fillMaxSize().padding(13.dp)) {
                val cell = (maxWidth - 12.dp) / 2
                val big = g.allLaunchable || g.apps.size <= 4
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    for (r in 0 until 2) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            for (c in 0 until 2) {
                                val i = r * 2 + c
                                Box(Modifier.size(cell), contentAlignment = Alignment.Center) {
                                    if (i < 3 || big) {
                                        g.apps.getOrNull(i)?.let { app -> LibraryIcon(vm, app, cell) }
                                    } else {
                                        MiniCluster(g.apps.drop(3).take(4), cell) {
                                            view.tick(); vm.openCategory = g.title
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(g.title, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium, maxLines = 1)
    }
}

@Composable
private fun LibraryIcon(vm: LauncherViewModel, app: AppInfo, size: Dp) {
    val view = LocalView.current
    val ref = remember { com.bruce.liquidglass.ui.CoordsRef() }
    var pressed by remember { mutableStateOf(false) }
    val s by animateFloatAsState(if (pressed) 0.86f else 1f, Motion.snappy(), label = "lib")
    IconImage(
        app, size,
        Modifier
            .onPlaced { ref.coords = it }
            .graphicsLayer { scaleX = s; scaleY = s }
            .pointerInput(app.key) {
                detectTapGestures(
                    onPress = { pressed = true; tryAwaitRelease(); pressed = false },
                    onTap = { vm.launch(app, ref.windowBounds(), view) },
                    onLongPress = {
                        view.tick()
                        vm.menu = MenuTarget(app, ref.windowBounds(), inDock = app.key in vm.dock, onHome = vm.isOnHome(app.key))
                    },
                )
            },
    )
}

@Composable
private fun MiniCluster(apps: List<AppInfo>, size: Dp, onClick: () -> Unit) {
    val mini = (size - 6.dp) / 2
    Column(
        Modifier.size(size).pointerInput(Unit) { detectTapGestures { onClick() } },
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        for (r in 0 until 2) Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            for (c in 0 until 2) {
                val app = apps.getOrNull(r * 2 + c)
                Box(Modifier.size(mini)) {
                    if (app != null) IconImage(app, mini)
                }
            }
        }
    }
}

/** Expanded category, like opening a folder on iOS. */
@Composable
fun CategoryOverlay(vm: LauncherViewModel) {
    val view = LocalView.current
    val title = vm.openCategory
    var lastTitle by remember { mutableStateOf<String?>(null) }
    if (title != null) lastTitle = title
    BackHandler(enabled = title != null) { vm.openCategory = null }

    AnimatedVisibility(
        visible = title != null,
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        val avScope = this
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.25f))
                .pointerInput(Unit) { detectTapGestures { vm.openCategory = null } }
                .statusBarsPadding(),
            contentAlignment = Alignment.Center,
        ) {
            val t = lastTitle ?: return@Box
            val apps = remember(t, vm.apps) { libraryGroups(vm).firstOrNull { it.title == t }?.apps ?: emptyList() }
            val enterExit = with(avScope) {
                Modifier.animateEnterExit(
                    enter = scaleIn(Motion.bouncy(), initialScale = 0.7f),
                    exit = scaleOut(Motion.snappy(), targetScale = 0.8f),
                )
            }
            Column(
                enterExit,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(t, color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(16.dp))
                Glass(
                    Modifier
                        .padding(horizontal = 22.dp)
                        .fillMaxWidth()
                        .pointerInput(Unit) { detectTapGestures { } },
                    shape = RoundedCornerShape(36.dp),
                    blur = 34.dp,
                ) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(4),
                        modifier = Modifier.fillMaxWidth().height(
                            (((apps.size + 3) / 4).coerceIn(1, 5) * 96 + 24).dp
                        ),
                        contentPadding = PaddingValues(12.dp),
                    ) {
                        items(apps, key = { it.key }) { app ->
                            Box(Modifier.height(96.dp), contentAlignment = Alignment.TopCenter) {
                                AppIcon(
                                    app = app, iconSize = 56.dp, showLabel = vm.settings.showLabels,
                                    editing = false, selected = false, seed = abs(app.key.hashCode()),
                                    onTap = { b -> vm.launch(app, b, view); vm.openCategory = null },
                                    onLongPress = { b ->
                                        vm.menu = MenuTarget(app, b, inDock = app.key in vm.dock, onHome = vm.isOnHome(app.key))
                                    },
                                    onRemove = null,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

