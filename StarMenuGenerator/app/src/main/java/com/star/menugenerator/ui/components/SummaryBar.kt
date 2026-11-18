package com.star.menugenerator.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.star.menugenerator.domain.Summary
import com.star.menugenerator.domain.formatPaise
import com.star.menugenerator.ui.theme.StarDeep
import com.star.menugenerator.ui.theme.StarPurple
import com.star.menugenerator.ui.theme.StarWarn

@Composable
private fun TotalRow(label: String, paise: Long, bold: Boolean = false) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal, color = StarDeep)
        Text(formatPaise(paise), fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal, color = StarDeep)
    }
}

/**
 * Sticky "Calculate totals" bar: grand total always visible, tap to see the separate Individual /
 * Package / Buffet totals and every entry that is not yet counted (and why).
 */
@Composable
fun SummaryBar(summary: Summary, onContinue: () -> Unit) {
    var open by remember { mutableStateOf(false) }
    Surface(color = Color.White, shadowElevation = 8.dp) {
        Column(Modifier.navigationBarsPadding().padding(horizontal = 16.dp, vertical = 10.dp)) {
            AnimatedVisibility(visible = open) {
                Column(Modifier.padding(bottom = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    TotalRow("Individual items", summary.individual)
                    TotalRow("Packages", summary.packages)
                    TotalRow("Buffets", summary.buffets)
                    HorizontalDivider(Modifier.padding(vertical = 4.dp))
                    TotalRow("Grand total", summary.grand, bold = true)
                    if (summary.issues.isNotEmpty()) {
                        Spacer(Modifier.size(6.dp))
                        Text("Not counted yet", fontWeight = FontWeight.SemiBold, color = StarWarn, style = MaterialTheme.typography.labelLarge)
                        summary.issues.take(6).forEach {
                            Text(
                                "• ${it.name}${if (it.meal.isNotBlank()) " (${it.meal})" else ""}: ${it.reason}",
                                style = MaterialTheme.typography.bodySmall, color = StarWarn,
                            )
                        }
                        if (summary.issues.size > 6) {
                            Text("…and ${summary.issues.size - 6} more", style = MaterialTheme.typography.bodySmall, color = StarWarn)
                        }
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f).clickable { open = !open }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Grand total", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Icon(if (open) Icons.Filled.ExpandMore else Icons.Filled.ExpandLess, contentDescription = "Show totals", modifier = Modifier.size(18.dp), tint = StarPurple)
                    }
                    Text(formatPaise(summary.grand), style = MaterialTheme.typography.titleLarge, color = StarPurple)
                    if (!summary.complete) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Warning, contentDescription = null, tint = StarWarn, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Partial total", color = StarWarn, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
                PrimaryButton("Choose template", onContinue)
            }
        }
    }
}
