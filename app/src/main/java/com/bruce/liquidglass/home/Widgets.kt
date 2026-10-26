package com.bruce.liquidglass.home

import android.appwidget.AppWidgetProviderInfo
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.bruce.liquidglass.LauncherViewModel
import com.bruce.liquidglass.WidgetMenuTarget
import com.bruce.liquidglass.data.WidgetKey
import com.bruce.liquidglass.ui.CoordsRef
import com.bruce.liquidglass.ui.Glass
import com.bruce.liquidglass.ui.LocalScreenSize
import com.bruce.liquidglass.ui.Motion
import com.bruce.liquidglass.ui.tick
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.roundToInt

private val WidgetShape = RoundedCornerShape(26.dp)
private val R_TAG_SIZED = com.bruce.liquidglass.R.id.widget_sized

// ====================================================================== on the home screen

@Composable
fun HomeWidget(vm: LauncherViewModel, key: String, modifier: Modifier) {
    val controller = LocalWidgets.current
    val view = LocalView.current
    val density = LocalDensity.current
    val id = WidgetKey.id(key)
    val ref = remember { CoordsRef() }
    val selected = vm.selectedForMove == key
    val scale by animateFloatAsState(if (selected) 1.04f else 1f, Motion.snappy(), label = "wsel")
    val rotation = if (vm.editing && !selected && !vm.settings.reducedMotion) widgetJiggle(abs(key.hashCode())) else 0f
    val glassBehind = vm.settings.widgetGlass

    Box(
        modifier
            .onPlaced { ref.coords = it }
            .graphicsLayer { scaleX = scale; scaleY = scale; rotationZ = rotation }
    ) {
        val content: @Composable () -> Unit = {
            val hostView = remember(id, controller) { controller?.view(id) }
            if (hostView == null) {
                // Provider uninstalled or widget unavailable
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Widget indisponible", color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp)
                }
            } else {
                AndroidView(
                    factory = {
                        (hostView.parent as? android.view.ViewGroup)?.removeView(hostView)
                        if (hostView.getTag(R_TAG_SIZED) == null) {
                            hostView.setTag(R_TAG_SIZED, true)
                            hostView.addOnLayoutChangeListener { lv, l, t, r, b, _, _, _, _ ->
                                (lv as? GlassWidgetView)?.setSizeDp(
                                    ((r - l) / density.density).roundToInt(),
                                    ((b - t) / density.density).roundToInt(),
                                )
                            }
                        }
                        hostView
                    },
                    update = { v ->
                        v.onLongPress = {
                            if (!vm.editing) vm.widgetMenu = WidgetMenuTarget(key, ref.windowBounds())
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        if (glassBehind) {
            Glass(Modifier.fillMaxSize(), shape = WidgetShape) { content() }
        } else {
            Box(Modifier.fillMaxSize().clip(WidgetShape)) { content() }
        }

        // Edit mode: a shield on top so taps select the widget instead of using it.
        if (vm.editing) {
            Box(
                Modifier
                    .fillMaxSize()
                    .clip(WidgetShape)
                    .background(if (selected) Color.White.copy(alpha = 0.10f) else Color.Transparent)
                    .border(if (selected) 2.dp else 0.dp, if (selected) Color.White else Color.Transparent, WidgetShape)
                    .pointerInput(key) { detectTapGestures { view.tick(); vm.onEditTap(key) } }
            )
            Glass(
                Modifier
                    .offset((-6).dp, (-6).dp)
                    .size(24.dp)
                    .pointerInput(key) { detectTapGestures { view.tick(); vm.removeWidget(key) } },
                shape = CircleShape,
                tint = Color(0x66000000),
            ) {
                Icon(Icons.Rounded.Remove, null, tint = Color.White, modifier = Modifier.align(Alignment.Center).size(16.dp))
            }
        }
    }
}

@Composable
private fun widgetJiggle(seed: Int): Float {
    val t = rememberInfiniteTransition(label = "wj")
    val r by t.animateFloat(
        if (seed % 2 == 0) -0.6f else 0.6f,
        if (seed % 2 == 0) 0.6f else -0.6f,
        infiniteRepeatable(tween(150, easing = LinearEasing), RepeatMode.Reverse),
        label = "wr",
    )
    return r
}

// ====================================================================== long-press menu

@Composable
fun WidgetMenuOverlay(vm: LauncherViewModel) {
    val view = LocalView.current
    val density = LocalDensity.current
    val screen = LocalScreenSize.current
    val controller = LocalWidgets.current
    val target = vm.widgetMenu
    var last by remember { mutableStateOf<WidgetMenuTarget?>(null) }
    if (target != null) last = target
    BackHandler(enabled = target != null) { vm.widgetMenu = null }
    val progress by animateFloatAsState(if (target != null) 1f else 0f, spring(0.74f, 420f), label = "wm")
    val t = last ?: return
    if (target == null && progress < 0.01f) return
    val a = progress.coerceIn(0f, 1f)
    val name = remember(t.key) {
        controller?.info(WidgetKey.id(t.key))?.let { info ->
            runCatching { info.loadLabel(view.context.packageManager) }.getOrNull()
        } ?: "Widget"
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.22f * a))
            .pointerInput(Unit) { detectTapGestures { vm.widgetMenu = null } }
    ) {
        val menuW = with(density) { 280.dp.toPx() }
        val menuH = with(density) { 236.dp.toPx() }
        val gap = with(density) { 12.dp.toPx() }
        val margin = with(density) { 14.dp.toPx() }
        val below = t.bounds.bottom + gap + menuH < screen.height - with(density) { 40.dp.toPx() }
        val x = (t.bounds.center.x - menuW / 2f).coerceIn(margin, (screen.width - menuW - margin).coerceAtLeast(margin))
        val y = if (below) t.bounds.bottom + gap else (t.bounds.top - gap - menuH).coerceAtLeast(margin)
        Glass(
            Modifier
                .offset { IntOffset(x.roundToInt(), y.roundToInt()) }
                .width(280.dp)
                .graphicsLayer {
                    transformOrigin = TransformOrigin(0.5f, if (below) 0f else 1f)
                    val p = progress.coerceIn(0f, 1.15f)
                    scaleX = 0.5f + 0.5f * p; scaleY = 0.3f + 0.7f * p
                    alpha = a
                }
                .pointerInput(Unit) { detectTapGestures { } },
            shape = RoundedCornerShape(26.dp),
            tint = Color(0x33000000),
        ) {
            Column(Modifier.padding(vertical = 6.dp)) {
                Text(
                    name, color = Color.White.copy(alpha = 0.6f), fontSize = 13.sp, maxLines = 1,
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp),
                )
                Column(Modifier.padding(horizontal = 14.dp, vertical = 4.dp)) {
                    Text("Taille", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(8.dp))
                    val maxRows = (vm.settings.rows - 1).coerceAtMost(WidgetKey.MAX_ROWS)
                    val labels = listOf("S", "M", "L", "XL").take(maxRows)
                    Segmented(labels, (WidgetKey.rows(t.key) - 1).coerceIn(0, labels.lastIndex)) { i ->
                        view.tick(); vm.resizeWidget(t.key, i + 1)
                    }
                }
                Spacer(Modifier.height(6.dp))
                MenuLine("Modifier l'écran d'accueil", Icons.Rounded.Apps, Color.White) {
                    vm.widgetMenu = null; vm.editing = true
                }
                MenuLine("Supprimer le widget", Icons.Rounded.DeleteOutline, Color(0xFFFF453A)) {
                    vm.widgetMenu = null; vm.removeWidget(t.key)
                }
            }
        }
    }
}

@Composable
private fun MenuLine(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color, onClick: () -> Unit) {
    val view = LocalView.current
    var pressed by remember { mutableStateOf(false) }
    Row(
        Modifier
            .fillMaxWidth()
            .height(50.dp)
            .background(if (pressed) Color.White.copy(alpha = 0.10f) else Color.Transparent)
            .pointerInput(label) {
                detectTapGestures(
                    onPress = { pressed = true; tryAwaitRelease(); pressed = false },
                    onTap = { view.tick(); onClick() },
                )
            }
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = color, fontSize = 16.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
        Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
    }
}

// ====================================================================== picker

private class WidgetEntry(
    val info: AppWidgetProviderInfo,
    val label: String,
    val appLabel: String,
    val sizeText: String,
    val defaultRows: Int,
)

private fun Drawable.toBitmap(maxSide: Int): Bitmap? {
    val w = intrinsicWidth.takeIf { it > 0 } ?: maxSide
    val h = intrinsicHeight.takeIf { it > 0 } ?: maxSide
    val scale = minOf(1f, maxSide.toFloat() / maxOf(w, h))
    val bw = (w * scale).roundToInt().coerceAtLeast(1)
    val bh = (h * scale).roundToInt().coerceAtLeast(1)
    return runCatching {
        val b = Bitmap.createBitmap(bw, bh, Bitmap.Config.ARGB_8888)
        setBounds(0, 0, bw, bh)
        draw(Canvas(b))
        b
    }.getOrNull()
}

/** Pending request from the picker; the activity runs bind + configure, then calls vm.addWidget. */
fun interface WidgetAdder { fun add(info: AppWidgetProviderInfo, rows: Int) }

val LocalWidgetAdder = androidx.compose.runtime.staticCompositionLocalOf<WidgetAdder> { WidgetAdder { _, _ -> } }

@Composable
fun WidgetPicker(vm: LauncherViewModel) {
    BackHandler(enabled = vm.widgetPickerOpen) { vm.widgetPickerOpen = false }
    AnimatedVisibility(visible = vm.widgetPickerOpen, enter = fadeIn(), exit = fadeOut()) {
        val avScope = this
        Box(
            Modifier
                .fillMaxSize()
                .pointerInput(Unit) { detectTapGestures { vm.widgetPickerOpen = false } },
            contentAlignment = Alignment.BottomCenter,
        ) {
            val anim = with(avScope) {
                Modifier.animateEnterExit(
                    enter = slideInVertically(Motion.snappy()) { it },
                    exit = slideOutVertically(Motion.snappy()) { it },
                )
            }
            Glass(
                anim
                    .statusBarsPadding()
                    .padding(top = 40.dp)
                    .fillMaxWidth()
                    .pointerInput(Unit) { detectTapGestures { } },
                shape = RoundedCornerShape(topStart = 38.dp, topEnd = 38.dp),
                tint = Color(0x66000000),
            ) {
                PickerContent(vm)
            }
        }
    }
}

@Composable
private fun PickerContent(vm: LauncherViewModel) {
    val context = LocalContext.current
    val controller = LocalWidgets.current
    val adder = LocalWidgetAdder.current
    val view = LocalView.current
    val density = LocalDensity.current.density
    var query by remember { mutableStateOf("") }

    val entries by produceState<List<WidgetEntry>?>(null) {
        value = withContext(Dispatchers.Default) {
            val pm = context.packageManager
            val providers = runCatching { controller?.manager?.installedProviders }.getOrNull() ?: emptyList()
            providers.mapNotNull { info ->
                runCatching {
                    val appLabel = runCatching {
                        pm.getApplicationLabel(pm.getApplicationInfo(info.provider.packageName, 0)).toString()
                    }.getOrDefault(info.provider.packageName)
                    val wCells = ((info.minWidth / density + 30) / 70).roundToInt().coerceIn(1, 4)
                    val hCells = ((info.minHeight / density + 30) / 70).roundToInt().coerceIn(1, 4)
                    WidgetEntry(
                        info = info,
                        label = runCatching { info.loadLabel(pm) }.getOrNull()?.takeIf { it.isNotBlank() } ?: appLabel,
                        appLabel = appLabel,
                        sizeText = "$wCells × $hCells",
                        defaultRows = controller?.defaultRows(info, density) ?: 2,
                    )
                }.getOrNull()
            }.sortedWith(compareBy({ it.appLabel.lowercase() }, { it.label.lowercase() }))
        }
    }

    Column(Modifier.fillMaxSize().navigationBarsPadding()) {
        Box(Modifier.fillMaxWidth().height(26.dp), contentAlignment = Alignment.Center) {
            Box(Modifier.size(40.dp, 5.dp).background(Color.White.copy(alpha = 0.4f), RoundedCornerShape(50)))
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Widgets", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Glass(
                Modifier.size(38.dp).pointerInput(Unit) { detectTapGestures { view.tick(); vm.widgetPickerOpen = false } },
                shape = CircleShape,
            ) {
                Icon(Icons.Rounded.Close, null, tint = Color.White, modifier = Modifier.align(Alignment.Center).size(20.dp))
            }
        }
        Spacer(Modifier.height(12.dp))
        // search
        Box(
            Modifier
                .padding(horizontal = 18.dp)
                .fillMaxWidth()
                .height(42.dp)
                .background(Color.White.copy(alpha = 0.12f), RoundedCornerShape(50))
                .padding(horizontal = 14.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Search, null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Box {
                    if (query.isEmpty()) Text("Rechercher des widgets", color = Color.White.copy(alpha = 0.5f), fontSize = 16.sp)
                    BasicTextField(
                        value = query, onValueChange = { query = it }, singleLine = true,
                        textStyle = TextStyle(color = Color.White, fontSize = 16.sp),
                        cursorBrush = SolidColor(Color.White),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
        Spacer(Modifier.height(10.dp))

        val list = entries
        when {
            list == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Chargement…", color = Color.White.copy(alpha = 0.6f))
            }
            else -> {
                val q = query.trim().lowercase()
                val filtered = if (q.isEmpty()) list else list.filter {
                    it.label.lowercase().contains(q) || it.appLabel.lowercase().contains(q)
                }
                val groups = filtered.groupBy { it.appLabel }.toList()
                LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 6.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    if (q.isEmpty()) item(key = "builtin") { BuiltInClockCard(vm) }
                    if (groups.isEmpty()) item {
                        Text("Aucun widget trouvé.", color = Color.White.copy(alpha = 0.6f), fontSize = 15.sp,
                            modifier = Modifier.padding(top = 20.dp))
                    }
                    groups.forEach { (app, ws) ->
                        item(key = "h_$app") {
                            Text(app, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 12.dp, start = 4.dp))
                        }
                        items(ws, key = { it.info.provider.flattenToShortString() }) { e ->
                            WidgetCard(e) {
                                view.tick()
                                vm.widgetPickerOpen = false
                                adder.add(e.info, e.defaultRows)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WidgetCard(e: WidgetEntry, onAdd: () -> Unit) {
    val context = LocalContext.current
    val densityDpi = context.resources.displayMetrics.densityDpi
    val preview by produceState<ImageBitmap?>(null, e) {
        value = withContext(Dispatchers.Default) {
            runCatching {
                val d = e.info.loadPreviewImage(context, densityDpi) ?: e.info.loadIcon(context, densityDpi)
                d?.toBitmap(720)?.asImageBitmap()
            }.getOrNull()
        }
    }
    var pressed by remember { mutableStateOf(false) }
    val sc by animateFloatAsState(if (pressed) 0.96f else 1f, Motion.snappy(), label = "wc")
    Box(
        Modifier
            .fillMaxWidth()
            .graphicsLayer { scaleX = sc; scaleY = sc }
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White.copy(alpha = 0.08f))
            .border(0.6.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(24.dp))
            .pointerInput(e) {
                detectTapGestures(
                    onPress = { pressed = true; tryAwaitRelease(); pressed = false },
                    onTap = { onAdd() },
                )
            }
            .padding(14.dp)
    ) {
        Column {
            Box(
                Modifier.fillMaxWidth().heightIn(min = 90.dp, max = 190.dp),
                contentAlignment = Alignment.Center,
            ) {
                val p = preview
                if (p != null) Image(p, null, contentScale = ContentScale.Fit, modifier = Modifier.heightIn(max = 190.dp))
                else Icon(Icons.Rounded.Widgets, null, tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(44.dp))
            }
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(e.label, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                    Text(e.sizeText, color = Color.White.copy(alpha = 0.55f), fontSize = 13.sp)
                }
                Box(
                    Modifier.background(Color.White, RoundedCornerShape(50)).padding(horizontal = 16.dp, vertical = 7.dp)
                ) { Text("Ajouter", color = Color.Black, fontSize = 14.sp, fontWeight = FontWeight.SemiBold) }
            }
        }
    }
}

@Composable
private fun BuiltInClockCard(vm: LauncherViewModel) {
    val view = LocalView.current
    val on = vm.settings.showWidget
    Column {
        Text("Liquid Glass", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 4.dp, bottom = 10.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(Color.White.copy(alpha = 0.08f))
                .border(0.6.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(24.dp))
                .pointerInput(on) {
                    detectTapGestures {
                        view.tick()
                        vm.update { it.copy(showWidget = !on) }
                        if (!on) vm.widgetPickerOpen = false
                    }
                }
                .padding(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Horloge et batterie", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    Text("En haut de la page 1", color = Color.White.copy(alpha = 0.55f), fontSize = 13.sp)
                }
                Box(
                    Modifier
                        .background(if (on) Color.White.copy(alpha = 0.18f) else Color.White, RoundedCornerShape(50))
                        .padding(horizontal = 16.dp, vertical = 7.dp)
                ) {
                    Text(if (on) "Retirer" else "Ajouter", color = if (on) Color.White else Color.Black,
                        fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

