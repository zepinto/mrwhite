package com.zepinto.mrwhite

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Colours of the party look. Black stays pure black wherever a secret could leak. */
object Palette {
    val Ink = Color(0xFF140B2E)
    val Violet = Color(0xFF2D1B69)
    val Grape = Color(0xFF4B2A9B)
    val Pink = Color(0xFFFF5C8A)
    val Aqua = Color(0xFF5CE1E6)
    val Sun = Color(0xFFFFD166)
    val Mint = Color(0xFF6EE7B7)
    val Lilac = Color(0xFFB69CFF)
    val Coral = Color(0xFFFF8A5C)

    val avatars = listOf(Pink, Aqua, Sun, Mint, Lilac, Coral)
    fun avatar(index: Int) = avatars[index % avatars.size]
}

/** Deep indigo gradient with soft colour blobs, used behind every non-secret screen. */
fun Modifier.partyBackground(): Modifier = this
    .background(Brush.verticalGradient(listOf(Palette.Ink, Palette.Violet)))
    .drawBehind {
        drawCircle(Palette.Pink.copy(alpha = 0.18f), radius = size.width * 0.55f, center = Offset(size.width * 0.95f, size.height * 0.05f))
        drawCircle(Palette.Aqua.copy(alpha = 0.12f), radius = size.width * 0.6f, center = Offset(size.width * 0.0f, size.height * 0.98f))
    }

@Composable
fun Avatar(name: String, index: Int, size: Dp = 44.dp) {
    Box(Modifier.size(size).background(Palette.avatar(index), CircleShape), contentAlignment = Alignment.Center) {
        Text(
            name.firstOrNull()?.uppercase() ?: "?",
            color = Palette.Ink,
            fontWeight = FontWeight.Black,
            fontSize = (size.value * 0.46f).sp,
        )
    }
}

/** The Mr White mascot: a bobbing ghost. */
@Composable
fun Ghost(modifier: Modifier = Modifier, bob: Boolean = true, body: Color = Color.White, eyes: Color = Palette.Ink) {
    val transition = rememberInfiniteTransition(label = "ghost")
    val lift by transition.animateFloat(
        initialValue = 0f,
        targetValue = if (bob) 1f else 0f,
        animationSpec = infiniteRepeatable(tween(1300, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "lift",
    )
    Canvas(modifier.aspectRatio(1f)) {
        val s = size.minDimension
        translate(top = -lift * s * 0.05f) {
            val path = Path().apply {
                moveTo(s * 0.2f, s * 0.4f)
                arcTo(Rect(s * 0.2f, s * 0.1f, s * 0.8f, s * 0.7f), 180f, 180f, false)
                lineTo(s * 0.8f, s * 0.86f)
                quadraticTo(s * 0.7f, s * 0.98f, s * 0.6f, s * 0.86f)
                quadraticTo(s * 0.5f, s * 0.98f, s * 0.4f, s * 0.86f)
                quadraticTo(s * 0.3f, s * 0.98f, s * 0.2f, s * 0.86f)
                close()
            }
            drawPath(path, body)
            drawCircle(eyes, s * 0.05f, Offset(s * 0.4f, s * 0.42f))
            drawCircle(eyes, s * 0.05f, Offset(s * 0.6f, s * 0.42f))
            drawCircle(eyes, s * 0.03f, Offset(s * 0.5f, s * 0.56f))
            drawCircle(Palette.Pink.copy(alpha = 0.55f), s * 0.045f, Offset(s * 0.31f, s * 0.53f))
            drawCircle(Palette.Pink.copy(alpha = 0.55f), s * 0.045f, Offset(s * 0.69f, s * 0.53f))
        }
    }
}
