package com.bruce.liquidglass.onboarding

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import com.bruce.liquidglass.ui.Squircle

enum class ShowcaseArt { LEFT, LOCK, BETTER, RESELL }

data class ShowcaseApp(
    val name: String,
    val match: String,          // lower-case label used to detect the installed app
    val tagline: String,
    val description: String,
    val repoUrl: String?,
    val art: ShowcaseArt,
)

val showcaseApps = listOf(
    ShowcaseApp(
        name = "Left",
        match = "left",
        tagline = "Le temps qu'il te reste, en points.",
        description = "Ta journée, ta semaine et ton année en points qui s'éteignent un à un. Widgets noir et blanc, animés, style Liquid Glass.",
        repoUrl = "https://github.com/bryanguim/-left-android",
        art = ShowcaseArt.LEFT,
    ),
    ShowcaseApp(
        name = "Liquid Lock",
        match = "liquid lock",
        tagline = "Ton écran verrouillé, en verre liquide.",
        description = "Des horloges personnalisables qui remplacent celle de ton écran verrouillé. Polices, tailles, positions : tout se règle.",
        repoUrl = "https://github.com/bryanguim/liquid-lock",
        art = ShowcaseArt.LOCK,
    ),
    ShowcaseApp(
        name = "BE BETTER",
        match = "be better",
        tagline = "Photographie ton assiette. Progresse.",
        description = "Prends ton repas en photo : l'IA estime calories et protéines et suit tes objectifs. Gratuit, sans clé à configurer.",
        repoUrl = null,
        art = ShowcaseArt.BETTER,
    ),
    ShowcaseApp(
        name = "Resell AI",
        match = "resell ai",
        tagline = "Ton business de revente, piloté.",
        description = "Stock, photos, annonces en série et marges réelles pour Vinted. Sur ton PC et ton téléphone, synchronisés.",
        repoUrl = null,
        art = ShowcaseArt.RESELL,
    ),
)

/** Hand-drawn icon used when the app isn't installed on the phone. */
@Composable
fun ShowcaseArtwork(art: ShowcaseArt, size: Dp, modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(size)
            .clip(Squircle)
            .background(
                Brush.linearGradient(listOf(Color(0xFF2C2C2E), Color(0xFF0A0A0A)))
            ),
        contentAlignment = Alignment.Center,
    ) {
        when (art) {
            ShowcaseArt.LEFT -> Canvas(Modifier.fillMaxSize().padding(size * 0.2f)) {
                val n = 5
                val step = this.size.width / n
                val r = step * 0.30f
                for (y in 0 until n) for (x in 0 until n) {
                    val i = y * n + x
                    val c = Offset(step * (x + 0.5f), step * (y + 0.5f))
                    if (i < 17) drawCircle(Color.White, r, c)
                    else drawCircle(Color.White.copy(alpha = 0.35f), r, c, style = Stroke(r * 0.35f))
                }
            }
            ShowcaseArt.LOCK -> Canvas(Modifier.fillMaxSize().padding(size * 0.22f)) {
                val w = this.size.width
                val h = this.size.height
                val stroke = w * 0.075f
                // shackle
                drawArc(
                    Color.White, 180f, 180f, false,
                    topLeft = Offset(w * 0.22f, h * 0.02f), size = Size(w * 0.56f, h * 0.56f),
                    style = Stroke(stroke, cap = StrokeCap.Round),
                )
                // body (glass)
                drawRoundRect(
                    Color.White.copy(alpha = 0.22f), Offset(w * 0.08f, h * 0.36f), Size(w * 0.84f, h * 0.62f),
                    CornerRadius(w * 0.16f),
                )
                drawRoundRect(
                    Color.White, Offset(w * 0.08f, h * 0.36f), Size(w * 0.84f, h * 0.62f),
                    CornerRadius(w * 0.16f), style = Stroke(stroke * 0.6f),
                )
                // clock hands
                val c = Offset(w * 0.5f, h * 0.67f)
                drawLine(Color.White, c, Offset(c.x, c.y - h * 0.16f), stroke * 0.7f, StrokeCap.Round)
                drawLine(Color.White, c, Offset(c.x + w * 0.13f, c.y), stroke * 0.7f, StrokeCap.Round)
            }
            ShowcaseArt.BETTER -> {
                Canvas(Modifier.fillMaxSize().padding(size * 0.16f)) {
                    val s = this.size.width
                    val stroke = s * 0.08f
                    drawArc(Color.White.copy(alpha = 0.18f), 0f, 360f, false,
                        Offset(stroke / 2, stroke / 2), Size(s - stroke, s - stroke), style = Stroke(stroke))
                    drawArc(Color.White, -90f, 260f, false,
                        Offset(stroke / 2, stroke / 2), Size(s - stroke, s - stroke), style = Stroke(stroke, cap = StrokeCap.Round))
                }
                Text("B", color = Color.White, fontSize = (size.value * 0.34f).sp, fontWeight = FontWeight.Black)
            }
            ShowcaseArt.RESELL -> Canvas(Modifier.fillMaxSize().padding(size * 0.2f)) {
                val w = this.size.width
                val h = this.size.height
                val stroke = w * 0.08f
                val path = androidx.compose.ui.graphics.Path().apply {
                    moveTo(0f, h * 0.85f)
                    lineTo(w * 0.32f, h * 0.52f)
                    lineTo(w * 0.55f, h * 0.68f)
                    lineTo(w, h * 0.15f)
                }
                drawPath(path, Color.White, style = Stroke(stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))
                val arrow = androidx.compose.ui.graphics.Path().apply {
                    moveTo(w * 0.70f, h * 0.12f)
                    lineTo(w, h * 0.15f)
                    lineTo(w * 0.97f, h * 0.45f)
                }
                drawPath(arrow, Color.White, style = Stroke(stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))
                drawCircle(Color.White.copy(alpha = 0.3f), w * 0.07f, Offset(w * 0.32f, h * 0.52f))
            }
        }
    }
}
