package com.bruce.liquidglass.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.SwipeDown
import androidx.compose.material.icons.rounded.SwipeLeft
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bruce.liquidglass.LauncherViewModel
import com.bruce.liquidglass.data.AppInfo
import com.bruce.liquidglass.data.WallpaperKind
import com.bruce.liquidglass.home.IconImage
import com.bruce.liquidglass.home.LocalActions
import com.bruce.liquidglass.ui.Glass
import com.bruce.liquidglass.ui.LocalWallpaper
import com.bruce.liquidglass.ui.Motion
import com.bruce.liquidglass.ui.WallpaperLayer
import com.bruce.liquidglass.ui.WallpaperSpec
import com.bruce.liquidglass.ui.confirm
import com.bruce.liquidglass.ui.tick
import kotlinx.coroutines.delay
import kotlin.math.abs

private val greetings = listOf("Bonjour", "Hello", "Hola", "Ciao", "Hallo", "Olá", "こんにちは", "Привет", "مرحبا")

@Composable
fun Onboarding(vm: LauncherViewModel) {
    // The guide always uses the colourful aurora backdrop, like an Apple setup screen.
    CompositionLocalProvider(LocalWallpaper provides WallpaperSpec(WallpaperKind.AURORA, null)) {
        var step by remember { mutableIntStateOf(0) }
        val view = LocalView.current
        BackHandler(enabled = step > 0) { step-- }

        fun next() { view.tick(); step++ }
        fun finish() { view.confirm(); vm.finishOnboarding(); vm.returnedHome.tryEmit(Unit) }

        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black)
                .pointerInput(Unit) { detectTapGestures { } } // swallow touches
        ) {
            WallpaperLayer(Modifier.fillMaxSize())
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.25f)))

            AnimatedContent(
                targetState = step,
                transitionSpec = {
                    val forward = targetState > initialState
                    (slideInHorizontally(Motion.soft()) { if (forward) it / 3 else -it / 3 } +
                        fadeIn(tween(380, delayMillis = 80)) + scaleIn(Motion.soft(), initialScale = 0.96f)) togetherWith
                        (slideOutHorizontally(Motion.soft()) { if (forward) -it / 3 else it / 3 } +
                            fadeOut(tween(220)) + scaleOut(Motion.soft(), targetScale = 1.04f))
                },
                label = "steps",
                modifier = Modifier.fillMaxSize(),
            ) { s ->
                Box(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
                    when (s) {
                        0 -> HelloStep { next() }
                        1 -> IntroStep { next() }
                        2 -> GesturesStep { next() }
                        3 -> AppsStep(vm) { next() }
                        else -> DefaultStep { finish() }
                    }
                }
            }

            if (step in 1..3) {
                Text(
                    "Passer",
                    color = Color.White.copy(alpha = 0.75f),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .statusBarsPadding()
                        .padding(20.dp)
                        .pointerInput(Unit) { detectTapGestures { view.tick(); step = 4 } },
                )
            }
            if (step >= 1) StepDots(step, Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 28.dp))
        }
    }
}

@Composable
private fun StepDots(step: Int, modifier: Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        for (i in 1..4) {
            val w by animateFloatAsState(if (i == step) 22f else 7f, Motion.snappy(), label = "sd")
            Box(
                Modifier
                    .size(w.dp, 7.dp)
                    .background(Color.White.copy(alpha = if (i <= step) 0.95f else 0.3f), RoundedCornerShape(50))
            )
        }
    }
}

@Composable
fun PrimaryButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    var pressed by remember { mutableStateOf(false) }
    val s by animateFloatAsState(if (pressed) 0.95f else 1f, Motion.snappy(), label = "pb")
    Box(
        modifier
            .fillMaxWidth()
            .height(56.dp)
            .graphicsLayer { scaleX = s; scaleY = s }
            .clip(RoundedCornerShape(50))
            .background(Color.White)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = { pressed = true; tryAwaitRelease(); pressed = false },
                    onTap = { onClick() },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = Color.Black, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun SecondaryButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Glass(
        modifier
            .fillMaxWidth()
            .height(56.dp)
            .pointerInput(Unit) { detectTapGestures { onClick() } },
        shape = RoundedCornerShape(50),
    ) {
        Text(
            text, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.SemiBold,
            modifier = Modifier.align(Alignment.Center),
        )
    }
}

// ---------------------------------------------------------------- 0. Hello

@Composable
private fun HelloStep(onNext: () -> Unit) {
    var index by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) { delay(1900); index = (index + 1) % greetings.size }
    }
    val pulse = rememberInfiniteTransition(label = "pulse")
    val a by pulse.animateFloat(0.35f, 0.95f, infiniteRepeatable(tween(1300, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "a")
    val bob by pulse.animateFloat(0f, -8f, infiniteRepeatable(tween(1300, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "b")

    Box(
        Modifier
            .fillMaxSize()
            .pointerInput(Unit) { detectTapGestures { onNext() } },
        contentAlignment = Alignment.Center,
    ) {
        AnimatedContent(
            targetState = index,
            transitionSpec = {
                (fadeIn(tween(700, delayMillis = 200)) + scaleIn(tween(900, easing = FastOutSlowInEasing), initialScale = 1.12f)) togetherWith
                    (fadeOut(tween(450)) + scaleOut(tween(500), targetScale = 0.92f))
            },
            label = "hello",
        ) { i ->
            Text(
                greetings[i],
                color = Color.White,
                fontSize = 68.sp,
                fontWeight = FontWeight.Light,
                letterSpacing = (-1).sp,
            )
        }
        Column(
            Modifier.align(Alignment.BottomCenter).padding(bottom = 48.dp).offset(y = bob.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("Touche pour commencer", color = Color.White.copy(alpha = a), fontSize = 16.sp, fontWeight = FontWeight.Medium)
        }
    }
}

// ---------------------------------------------------------------- 1. Intro

@Composable
private fun IntroStep(onNext: () -> Unit) {
    val float = rememberInfiniteTransition(label = "orb")
    val y by float.animateFloat(-10f, 10f, infiniteRepeatable(tween(2600, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "y")
    val rot by float.animateFloat(-4f, 4f, infiniteRepeatable(tween(3400, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "r")

    Column(
        Modifier.fillMaxSize().padding(horizontal = 28.dp).padding(top = 80.dp, bottom = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.weight(1f))
        Glass(
            Modifier
                .size(170.dp)
                .graphicsLayer { translationY = y * density; rotationZ = rot },
            shape = CircleShape,
            blur = 22.dp,
            tint = Color.White.copy(alpha = 0.06f),
        ) {
            Glass(
                Modifier.size(84.dp).align(Alignment.Center),
                shape = com.bruce.liquidglass.ui.Squircle,
                blur = 10.dp,
            ) {
                Icon(Icons.Rounded.Apps, null, tint = Color.White, modifier = Modifier.size(40.dp).align(Alignment.Center))
            }
        }
        Spacer(Modifier.height(44.dp))
        Text("Liquid Glass", color = Color.White, fontSize = 42.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp)
        Spacer(Modifier.height(12.dp))
        Text(
            "Ton nouvel écran d'accueil, inspiré d'iOS 26.\nDu verre, de la lumière et des animations fluides.",
            color = Color.White.copy(alpha = 0.75f), fontSize = 17.sp, textAlign = TextAlign.Center, lineHeight = 24.sp,
        )
        Spacer(Modifier.weight(1.2f))
        PrimaryButton("Continuer", onClick = onNext)
    }
}

// ---------------------------------------------------------------- 2. Gestures

private class Gesture(val icon: ImageVector, val title: String, val text: String)

private val gestures = listOf(
    Gesture(Icons.Rounded.SwipeDown, "Glisse vers le bas", "Recherche instantanée dans tes apps et sur le Web."),
    Gesture(Icons.Rounded.SwipeLeft, "Va jusqu'à la dernière page", "La bibliothèque d'apps, rangée par catégories."),
    Gesture(Icons.Rounded.TouchApp, "Appui long sur une app", "Menu rapide : dock, infos, retirer, supprimer."),
    Gesture(Icons.Rounded.Apps, "Appui long sur le fond", "Les icônes bougent : réorganise ton écran."),
    Gesture(Icons.Rounded.Notifications, "Glisse depuis tout en haut", "Notifications à gauche, réglages rapides à droite."),
)

@Composable
private fun GesturesStep(onNext: () -> Unit) {
    var shown by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        delay(250)
        while (shown < gestures.size) { shown++; delay(110) }
    }
    Column(
        Modifier.fillMaxSize().padding(horizontal = 22.dp).padding(top = 78.dp, bottom = 28.dp),
    ) {
        Text("Prends-le en main", color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Text("Les gestes à connaître.", color = Color.White.copy(alpha = 0.7f), fontSize = 17.sp)
        Spacer(Modifier.height(22.dp))
        gestures.forEachIndexed { i, g ->
            val p by animateFloatAsState(if (i < shown) 1f else 0f, Motion.bouncy(), label = "g$i")
            Glass(
                Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp)
                    .graphicsLayer {
                        alpha = p.coerceIn(0f, 1f)
                        translationY = (1f - p) * 40f * density
                        val sc = 0.94f + 0.06f * p
                        scaleX = sc; scaleY = sc
                    },
                shape = RoundedCornerShape(24.dp),
            ) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(44.dp).background(Color.White.copy(alpha = 0.16f), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) { Icon(g.icon, null, tint = Color.White, modifier = Modifier.size(24.dp)) }
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text(g.title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        Text(g.text, color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp)
                    }
                }
            }
        }
        Spacer(Modifier.weight(1f))
        PrimaryButton("Continuer", onClick = onNext)
    }
}

// ---------------------------------------------------------------- 3. Our apps

@Composable
private fun AppsStep(vm: LauncherViewModel, onNext: () -> Unit) {
    val pager = rememberPagerState(pageCount = { showcaseApps.size })
    Column(
        Modifier.fillMaxSize().padding(top = 78.dp, bottom = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Fait maison", color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Text(
            "Les apps qu'on a créées ensemble.",
            color = Color.White.copy(alpha = 0.7f), fontSize = 17.sp,
        )
        Spacer(Modifier.height(22.dp))
        HorizontalPager(
            state = pager,
            contentPadding = PaddingValues(horizontal = 44.dp),
            pageSpacing = 14.dp,
            modifier = Modifier.weight(1f),
        ) { page ->
            val offset = (pager.currentPage - page) + pager.currentPageOffsetFraction
            val o = abs(offset).coerceIn(0f, 1f)
            ShowcaseCard(
                vm, showcaseApps[page],
                Modifier.graphicsLayer {
                    val sc = 1f - 0.12f * o
                    scaleX = sc; scaleY = sc
                    alpha = 1f - 0.45f * o
                    rotationY = offset.coerceIn(-1f, 1f) * 14f
                    cameraDistance = 14f * density
                },
            )
        }
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            repeat(showcaseApps.size) { i ->
                val active = pager.currentPage == i
                val w by animateFloatAsState(if (active) 20f else 7f, Motion.snappy(), label = "pd")
                Box(
                    Modifier.size(w.dp, 7.dp)
                        .background(Color.White.copy(alpha = if (active) 1f else 0.35f), RoundedCornerShape(50))
                )
            }
        }
        Spacer(Modifier.height(18.dp))
        PrimaryButton("Continuer", Modifier.padding(horizontal = 28.dp), onClick = onNext)
    }
}

private fun findInstalled(vm: LauncherViewModel, app: ShowcaseApp): AppInfo? =
    vm.apps.firstOrNull { it.label.trim().lowercase() == app.match }
        ?: vm.apps.firstOrNull { it.label.trim().lowercase().startsWith(app.match) }

@Composable
private fun ShowcaseCard(vm: LauncherViewModel, app: ShowcaseApp, modifier: Modifier) {
    val actions = LocalActions.current
    val view = LocalView.current
    val installed = findInstalled(vm, app)
    Glass(modifier.fillMaxSize(), shape = RoundedCornerShape(38.dp), blur = 32.dp) {
        Column(
            Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.weight(0.6f))
            if (installed != null) IconImage(installed, 104.dp, vm.iconStyle)
            else ShowcaseArtwork(app.art, 104.dp)
            Spacer(Modifier.height(20.dp))
            Text(app.name, color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text(app.tagline, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center)
            Spacer(Modifier.height(12.dp))
            Text(
                app.description, color = Color.White.copy(alpha = 0.68f), fontSize = 14.sp,
                textAlign = TextAlign.Center, lineHeight = 20.sp,
            )
            Spacer(Modifier.weight(1f))
            // status chip
            Row(
                Modifier
                    .background(Color.White.copy(alpha = 0.12f), RoundedCornerShape(50))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (installed != null) {
                    Icon(Icons.Rounded.CheckCircle, null, tint = Color(0xFF30D158), modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(5.dp))
                    Text("Installée sur ce téléphone", color = Color.White, fontSize = 13.sp)
                } else {
                    Text("Pas encore installée", color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp)
                }
            }
            Spacer(Modifier.height(14.dp))
            when {
                installed != null -> SecondaryButton("Ouvrir") { view.tick(); vm.launch(installed, null, null) }
                app.repoUrl != null -> SecondaryButton("Voir sur GitHub") { view.tick(); actions.openUrl(app.repoUrl) }
                else -> Spacer(Modifier.height(56.dp))
            }
        }
    }
}

// ---------------------------------------------------------------- 4. Default launcher

@Composable
private fun DefaultStep(onDone: () -> Unit) {
    val actions = LocalActions.current
    val view = LocalView.current
    var isDefault by remember { mutableStateOf(actions.isDefaultHome()) }
    LaunchedEffect(Unit) {
        while (true) { isDefault = actions.isDefaultHome(); delay(800) }
    }
    val check by animateFloatAsState(if (isDefault) 1f else 0f, Motion.bouncy(), label = "chk")

    Column(
        Modifier.fillMaxSize().padding(horizontal = 28.dp).padding(top = 80.dp, bottom = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.weight(1f))
        Box(contentAlignment = Alignment.Center) {
            Glass(Modifier.size(150.dp), shape = CircleShape) {
                Icon(Icons.Rounded.Home, null, tint = Color.White, modifier = Modifier.size(64.dp).align(Alignment.Center))
            }
            Icon(
                Icons.Rounded.CheckCircle, null, tint = Color(0xFF30D158),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(46.dp)
                    .graphicsLayer { scaleX = check; scaleY = check; alpha = check.coerceIn(0f, 1f) }
                    .background(Color.Black, CircleShape),
            )
        }
        Spacer(Modifier.height(36.dp))
        Text(
            if (isDefault) "Tout est prêt" else "Dernière étape",
            color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            if (isDefault) "Liquid Glass est ton écran d'accueil. Appuie sur Accueil quand tu veux y revenir."
            else "Choisis Liquid Glass comme écran d'accueil pour qu'il s'ouvre à chaque appui sur Accueil.",
            color = Color.White.copy(alpha = 0.75f), fontSize = 17.sp, textAlign = TextAlign.Center, lineHeight = 24.sp,
        )
        Spacer(Modifier.weight(1.2f))
        if (!isDefault) {
            PrimaryButton("Définir par défaut") { view.tick(); actions.requestDefaultHome() }
            Spacer(Modifier.height(12.dp))
            SecondaryButton("Plus tard", onClick = onDone)
        } else {
            PrimaryButton("Commencer", onClick = onDone)
        }
    }
}
