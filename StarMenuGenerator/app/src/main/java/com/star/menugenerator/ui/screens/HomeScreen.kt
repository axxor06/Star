package com.star.menugenerator.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.star.menugenerator.domain.formatPaise
import com.star.menugenerator.ui.AppViewModel
import com.star.menugenerator.ui.Screen
import com.star.menugenerator.ui.components.AccentButton
import com.star.menugenerator.ui.components.IconBadge
import com.star.menugenerator.ui.components.StarCard
import com.star.menugenerator.ui.components.StarLogo
import com.star.menugenerator.ui.theme.StarDeep
import com.star.menugenerator.ui.theme.StarPurple

@Composable
private fun ActionTile(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    StarCard(modifier.clickable(onClick = onClick)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            IconBadge(icon)
            Column {
                Text(title, fontWeight = FontWeight.Bold, color = StarDeep)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun Feature(icon: ImageVector, text: String, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Icon(icon, contentDescription = null, tint = StarPurple, modifier = Modifier.size(22.dp))
        Text(text, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Dashboard: brand header, primary action, shortcuts and the most recent menus. */
@Composable
fun HomeScreen(vm: AppViewModel, onError: (String) -> Unit) {
    val count by vm.menuCount.collectAsState(initial = 0)
    val recent by vm.recentMenus.collectAsState(initial = emptyList())

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).verticalScroll(rememberScrollState())) {
        // brand header
        Box(
            Modifier.fillMaxWidth()
                .background(Brush.verticalGradient(listOf(StarDeep, StarPurple)))
                .statusBarsPadding().padding(horizontal = 20.dp, vertical = 24.dp),
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Surface(shape = MaterialTheme.shapes.large, color = Color.White, shadowElevation = 6.dp) {
                    StarLogo(height = 64.dp, modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp))
                }
                Spacer(Modifier.height(18.dp))
                Text("Beautiful catering menus, in minutes.", style = MaterialTheme.typography.headlineSmall, color = Color.White, textAlign = TextAlign.Center)
                Spacer(Modifier.height(6.dp))
                Text(
                    "Type your dishes in English or Malayalam, pick a design, and share a print-ready menu.",
                    style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.85f), textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(18.dp))
                AccentButton("Create New Menu", onClick = { vm.newMenu() }, icon = Icons.Filled.Add, modifier = Modifier.fillMaxWidth())
            }
        }

        Column(Modifier.padding(16.dp).navigationBarsPadding(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ActionTile(
                    Icons.AutoMirrored.Filled.MenuBook, "My Menus",
                    if (count == 1) "1 saved menu" else "$count saved menus",
                    onClick = { vm.go(Screen.MyMenus) }, modifier = Modifier.weight(1f),
                )
                ActionTile(
                    Icons.Filled.Backup, "Backup & Restore", "Keep your menus safe",
                    onClick = { vm.go(Screen.Backup) }, modifier = Modifier.weight(1f),
                )
            }

            if (recent.isNotEmpty()) {
                Text("Recent menus", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = StarDeep)
                recent.forEach { m ->
                    StarCard(Modifier.clickable { vm.openMenu(m.id, onError) }) {
                        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(m.title, fontWeight = FontWeight.SemiBold, color = StarDeep, maxLines = 1)
                                Text(
                                    "${m.entryCount} entr${if (m.entryCount == 1) "y" else "ies"}" +
                                        (if (m.totalPaise > 0) " · ${formatPaise(m.totalPaise)}" else "") +
                                        (if (m.entryCount > 0 && !m.isComplete) " · incomplete total" else ""),
                                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Text("Open", color = StarPurple, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            StarCard {
                Row(Modifier.padding(vertical = 14.dp, horizontal = 8.dp)) {
                    Feature(Icons.Filled.WifiOff, "Works fully offline", Modifier.weight(1f))
                    Feature(Icons.Filled.Palette, "Two designed templates", Modifier.weight(1f))
                    Feature(Icons.Filled.PictureAsPdf, "PDF & PNG export", Modifier.weight(1f))
                }
            }
            Spacer(Modifier.width(1.dp))
        }
    }
}
