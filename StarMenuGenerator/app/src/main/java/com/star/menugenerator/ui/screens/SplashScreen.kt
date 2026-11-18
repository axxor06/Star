package com.star.menugenerator.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.star.menugenerator.ui.components.StarLogo
import com.star.menugenerator.ui.theme.StarDeep
import com.star.menugenerator.ui.theme.StarYellow

/** Brand splash: the full STAR logo on a white card over deep purple. */
@Composable
fun SplashScreen(onFinished: () -> Unit) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        progress.animateTo(1f, tween(durationMillis = 1600))
        onFinished()
    }
    Column(
        Modifier.fillMaxSize().background(StarDeep).padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Surface(shape = MaterialTheme.shapes.large, color = Color.White, shadowElevation = 8.dp) {
            StarLogo(height = 96.dp, modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp))
        }
        Spacer(Modifier.height(28.dp))
        Text("Menu Generator", style = MaterialTheme.typography.headlineSmall, color = Color.White)
        Spacer(Modifier.height(6.dp))
        Text(
            "Beautiful catering menus, in minutes.",
            style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.8f), textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(36.dp))
        LinearProgressIndicator(
            progress = { progress.value },
            modifier = Modifier.width(160.dp).fillMaxWidth(),
            color = StarYellow,
            trackColor = Color.White.copy(alpha = 0.2f),
        )
    }
}
