package com.bruce.liquidglass.home

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Shop
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bruce.liquidglass.LauncherViewModel
import com.bruce.liquidglass.SearchMode
import com.bruce.liquidglass.data.AppInfo
import com.bruce.liquidglass.ui.Glass
import com.bruce.liquidglass.ui.Motion
import com.bruce.liquidglass.ui.tick
import kotlin.math.abs

private fun rank(apps: List<AppInfo>, q: String): List<AppInfo> {
    val query = q.trim().lowercase()
    if (query.isEmpty()) return emptyList()
    return apps.mapNotNull { a ->
        val l = a.label.lowercase()
        val score = when {
            l == query -> 0
            l.startsWith(query) -> 1
            l.split(' ').any { it.startsWith(query) } -> 2
            l.contains(query) -> 3
            a.packageName.lowercase().contains(query) -> 4
            else -> null
        }
        score?.let { it to a }
    }.sortedWith(compareBy({ it.first }, { it.second.label.lowercase() })).map { it.second }
}

@Composable
fun SearchOverlay(vm: LauncherViewModel) {
    val mode = vm.search
    var lastMode by remember { mutableStateOf(SearchMode.SPOTLIGHT) }
    if (mode != null) lastMode = mode
    BackHandler(enabled = mode != null) { vm.search = null }

    AnimatedVisibility(visible = mode != null, enter = fadeIn(tween(200)), exit = fadeOut(tween(180))) {
        val avScope = this
        val slide = with(avScope) {
            Modifier.animateEnterExit(
                enter = slideInVertically(Motion.snappy()) { it / 3 },
                exit = slideOutVertically(Motion.snappy()) { it / 3 },
            )
        }
        SearchContent(vm, lastMode, slide)
    }
}

@Composable
private fun SearchContent(vm: LauncherViewModel, mode: SearchMode, slide: Modifier) {
    val view = LocalView.current
    val actions = LocalActions.current
    val keyboard = LocalSoftwareKeyboardController.current
    var query by remember { mutableStateOf("") }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }

    val results = remember(query, vm.apps, vm.settings.hidden) { rank(vm.visibleApps, query) }
    fun close() { keyboard?.hide(); vm.search = null }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.30f))
            .pointerInput(Unit) {
                detectTapGestures { close() }
            }
            .pointerInput(Unit) {
                var total = 0f
                detectVerticalDragGestures(
                    onDragStart = { total = 0f },
                    onDragEnd = { if (total > 140f) close() },
                    onVerticalDrag = { _, dy -> total += dy },
                )
            }
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .then(slide)
        ) {
            // ---------- results ----------
            Box(Modifier.weight(1f).fillMaxWidth()) {
                LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    reverseLayout = false,
                ) {
                    if (query.isBlank()) {
                        if (mode == SearchMode.SPOTLIGHT) {
                            val sugg = vm.recents.mapNotNull { vm.appsByKey[it] }.take(8)
                                .ifEmpty { vm.visibleApps.filter { !it.isSystem }.take(8) }
                            item { SectionTitle("Suggestions") }
                            item { IconGridCard(vm, sugg) { vm.search = null } }
                        } else {
                            val grouped = vm.visibleApps.groupBy { it.label.firstOrNull()?.uppercaseChar()?.takeIf { c -> c.isLetter() } ?: '#' }
                                .toSortedMap()
                            grouped.forEach { (letter, list) ->
                                item(key = "h$letter") { SectionTitle(letter.toString()) }
                                item(key = "g$letter") { AppListCard(vm, list) { vm.search = null } }
                            }
                        }
                    } else {
                        if (results.isNotEmpty()) {
                            item { SectionTitle("Apps") }
                            item { IconGridCard(vm, results.take(8)) { vm.search = null } }
                            if (results.size > 8) item { AppListCard(vm, results.drop(8)) { vm.search = null } }
                        }
                        item { SectionTitle("Plus") }
                        item {
                            Glass(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp)) {
                                Column {
                                    ActionRow(Icons.Rounded.Public, "Rechercher « $query » sur le Web") {
                                        actions.openUrl("https://www.google.com/search?q=" + Uri.encode(query)); close()
                                    }
                                    ActionRow(Icons.Rounded.Shop, "Chercher « $query » dans le Play Store") {
                                        actions.openUrl("market://search?q=" + Uri.encode(query)); close()
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ---------- search field (at the bottom, like iOS 26) ----------
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Glass(Modifier.weight(1f).height(50.dp), shape = RoundedCornerShape(50), blur = 30.dp) {
                    Row(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Search, null, tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Box(Modifier.weight(1f)) {
                            if (query.isEmpty()) Text(
                                if (mode == SearchMode.LIBRARY) "Bibliothèque d'apps" else "Rechercher",
                                color = Color.White.copy(alpha = 0.55f), fontSize = 17.sp,
                            )
                            BasicTextField(
                                value = query,
                                onValueChange = { query = it },
                                singleLine = true,
                                textStyle = TextStyle(color = Color.White, fontSize = 17.sp),
                                cursorBrush = SolidColor(Color.White),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                keyboardActions = KeyboardActions(onSearch = {
                                    val first = results.firstOrNull()
                                    if (first != null) { vm.launch(first, null, null); close() }
                                    else if (query.isNotBlank()) {
                                        actions.openUrl("https://www.google.com/search?q=" + Uri.encode(query)); close()
                                    }
                                }),
                                modifier = Modifier.fillMaxWidth().focusRequester(focus),
                            )
                        }
                    }
                }
                Spacer(Modifier.width(10.dp))
                Glass(
                    Modifier.size(50.dp).pointerInput(Unit) { detectTapGestures { view.tick(); close() } },
                    shape = CircleShape,
                ) {
                    Icon(Icons.Rounded.Close, null, tint = Color.White, modifier = Modifier.align(Alignment.Center).size(22.dp))
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 6.dp, top = 6.dp),
    )
}

@Composable
private fun IconGridCard(vm: LauncherViewModel, apps: List<AppInfo>, afterLaunch: () -> Unit) {
    val view = LocalView.current
    Glass(Modifier.fillMaxWidth().pointerInput(Unit) { detectTapGestures { } }, shape = RoundedCornerShape(28.dp)) {
        Column(Modifier.padding(vertical = 14.dp)) {
            apps.chunked(4).forEach { row ->
                Row(Modifier.fillMaxWidth().height(92.dp)) {
                    for (c in 0 until 4) {
                        Box(Modifier.weight(1f), contentAlignment = Alignment.TopCenter) {
                            val app = row.getOrNull(c)
                            if (app != null) AppIcon(
                                app = app, iconSize = 56.dp, showLabel = true,
                                editing = false, selected = false, seed = abs(app.key.hashCode()),
                                onTap = { b -> vm.launch(app, b, view); afterLaunch() },
                                onLongPress = { b ->
                                    vm.menu = com.bruce.liquidglass.MenuTarget(app, b, app.key in vm.dock, vm.isOnHome(app.key))
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

@Composable
private fun AppListCard(vm: LauncherViewModel, apps: List<AppInfo>, afterLaunch: () -> Unit) {
    val view = LocalView.current
    Glass(Modifier.fillMaxWidth().pointerInput(Unit) { detectTapGestures { } }, shape = RoundedCornerShape(24.dp)) {
        Column(Modifier.padding(vertical = 4.dp)) {
            apps.forEach { app ->
                val ref = remember { com.bruce.liquidglass.ui.CoordsRef() }
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .onPlaced { ref.coords = it }
                        .pointerInput(app.key) {
                            detectTapGestures(
                                onTap = { vm.launch(app, ref.windowBounds(), view); afterLaunch() },
                                onLongPress = {
                                    view.tick()
                                    vm.menu = com.bruce.liquidglass.MenuTarget(app, ref.windowBounds(), app.key in vm.dock, vm.isOnHome(app.key))
                                },
                            )
                        }
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconImage(app, 38.dp)
                    Spacer(Modifier.width(14.dp))
                    Text(app.label, color = Color.White, fontSize = 16.sp, maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun ActionRow(icon: ImageVector, label: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(54.dp)
            .pointerInput(label) { detectTapGestures { onClick() } }
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = Color.White, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(14.dp))
        Text(label, color = Color.White, fontSize = 15.sp, maxLines = 1)
    }
}
