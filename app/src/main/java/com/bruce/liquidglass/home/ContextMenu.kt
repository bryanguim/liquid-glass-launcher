package com.bruce.liquidglass.home

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddCircleOutline
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.RemoveCircleOutline
import androidx.compose.material.icons.rounded.SpaceDashboard
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bruce.liquidglass.LauncherViewModel
import com.bruce.liquidglass.MenuTarget
import com.bruce.liquidglass.ui.Glass
import com.bruce.liquidglass.ui.LocalScreenSize
import com.bruce.liquidglass.ui.thud
import com.bruce.liquidglass.ui.tick
import kotlin.math.roundToInt

private class MenuItem(val label: String, val icon: ImageVector, val destructive: Boolean = false, val action: () -> Unit)

@Composable
fun ContextMenuOverlay(vm: LauncherViewModel) {
    val view = LocalView.current
    val density = LocalDensity.current
    val screen = LocalScreenSize.current
    val target = vm.menu
    var last by remember { mutableStateOf<MenuTarget?>(null) }
    if (target != null) last = target
    BackHandler(enabled = target != null) { vm.menu = null }

    val progress by animateFloatAsState(
        if (target != null) 1f else 0f,
        spring(dampingRatio = 0.72f, stiffness = 420f),
        label = "menu",
    )
    val t = last ?: return
    if (target == null && progress < 0.01f) return

    val items = buildList {
        if (t.onHome) add(MenuItem("Modifier l'écran d'accueil", Icons.Rounded.Apps) {
            vm.menu = null; view.thud(); vm.editing = true
        })
        if (t.inDock) add(MenuItem("Retirer du dock", Icons.Rounded.RemoveCircleOutline) {
            vm.menu = null; vm.removeFromDock(t.app.key)
        }) else if (vm.dock.size < vm.dockCapacity) add(MenuItem("Ajouter au dock", Icons.Rounded.SpaceDashboard) {
            vm.menu = null; vm.addToDock(t.app.key)
        })
        if (!t.onHome) add(MenuItem("Ajouter à l'écran d'accueil", Icons.Rounded.AddCircleOutline) {
            vm.menu = null; vm.addToHome(t.app.key)
        })
        add(MenuItem("Infos sur l'app", Icons.Rounded.Info) { vm.menu = null; vm.openAppInfo(t.app) })
        if (t.onHome && !t.inDock) add(MenuItem("Retirer de l'écran d'accueil", Icons.Rounded.RemoveCircleOutline) {
            vm.menu = null; vm.removeFromHome(t.app.key)
        })
        add(MenuItem("Masquer l'app", Icons.Rounded.VisibilityOff) { vm.menu = null; vm.hideApp(t.app.key) })
        add(MenuItem("Supprimer l'app", Icons.Rounded.DeleteOutline, destructive = true) {
            vm.menu = null; vm.uninstall(t.app)
        })
    }

    val p = progress.coerceIn(0f, 1.2f)
    val a = progress.coerceIn(0f, 1f)

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.22f * a))
            .pointerInput(Unit) { detectTapGestures { vm.menu = null } }
    ) {
        // Lifted icon
        val iconDp = with(density) { t.bounds.width.toDp() }
        Box(
            Modifier
                .offset { IntOffset(t.bounds.left.roundToInt(), t.bounds.top.roundToInt()) }
                .graphicsLayer {
                    val s = 1f + 0.12f * p
                    scaleX = s; scaleY = s
                    shadowElevation = 18f * a
                }
        ) {
            IconImage(t.app, iconDp)
        }

        // Menu card, morphing out of the icon
        val menuW = with(density) { 262.dp.toPx() }
        val itemH = with(density) { 50.dp.toPx() }
        val gap = with(density) { 16.dp.toPx() }
        val margin = with(density) { 14.dp.toPx() }
        val menuH = itemH * items.size + with(density) { 12.dp.toPx() }
        val below = t.bounds.bottom + gap + menuH < screen.height - with(density) { 48.dp.toPx() }
        val x = t.bounds.left.coerceIn(margin, (screen.width - menuW - margin).coerceAtLeast(margin))
        val y = if (below) t.bounds.bottom + gap else t.bounds.top - gap - menuH
        val originX = ((t.bounds.center.x - x) / menuW).coerceIn(0f, 1f)

        Glass(
            Modifier
                .offset { IntOffset(x.roundToInt(), y.roundToInt()) }
                .width(262.dp)
                .graphicsLayer {
                    transformOrigin = TransformOrigin(originX, if (below) 0f else 1f)
                    scaleX = 0.4f + 0.6f * p
                    scaleY = 0.2f + 0.8f * p
                    alpha = a
                }
                .pointerInput(Unit) { detectTapGestures { } },
            shape = RoundedCornerShape(26.dp),
            blur = 36.dp,
            tint = Color(0x33000000),
        ) {
            Column(Modifier.padding(vertical = 6.dp)) {
                items.forEachIndexed { i, item ->
                    MenuRow(item) { view.tick(); item.action() }
                    if (i < items.lastIndex) Box(
                        Modifier.padding(horizontal = 18.dp).fillMaxWidth().height(0.5.dp)
                            .background(Color.White.copy(alpha = 0.12f))
                    )
                }
            }
        }
    }
}

@Composable
private fun MenuRow(item: MenuItem, onClick: () -> Unit) {
    var pressed by remember { mutableStateOf(false) }
    val color = if (item.destructive) Color(0xFFFF453A) else Color.White
    Row(
        Modifier
            .fillMaxWidth()
            .height(50.dp)
            .background(if (pressed) Color.White.copy(alpha = 0.10f) else Color.Transparent)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = { pressed = true; tryAwaitRelease(); pressed = false },
                    onTap = { onClick() },
                )
            }
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(item.label, color = color, fontSize = 16.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
        Spacer(Modifier.width(10.dp))
        Icon(item.icon, null, tint = color, modifier = Modifier.size(20.dp))
    }
}
