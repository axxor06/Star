package com.star.menugenerator.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.star.menugenerator.R
import com.star.menugenerator.domain.EntryResult
import com.star.menugenerator.domain.EntryStatus
import com.star.menugenerator.domain.formatPaise
import com.star.menugenerator.domain.parseItems
import com.star.menugenerator.ui.theme.StarDeep
import com.star.menugenerator.ui.theme.StarOk
import com.star.menugenerator.ui.theme.StarPurple
import com.star.menugenerator.ui.theme.StarWarn
import com.star.menugenerator.ui.theme.StarYellow

const val LOGO_ASPECT = 2139f / 735f

/** The official STAR Catering & Events logo (full wordmark). */
@Composable
fun StarLogo(modifier: Modifier = Modifier, height: Dp = 56.dp) {
    Image(
        painter = painterResource(R.drawable.star_logo),
        contentDescription = "STAR Catering & Events",
        modifier = modifier.height(height).aspectRatio(LOGO_ASPECT),
        contentScale = ContentScale.Fit,
    )
}

/** White top bar with the small logo, a back arrow and a title. */
@Composable
fun StarHeader(title: String, onBack: (() -> Unit)?, actions: @Composable RowScope.() -> Unit = {}) {
    Surface(color = Color.White, shadowElevation = 2.dp) {
        Row(
            Modifier.statusBarsPadding().fillMaxWidth().height(56.dp).padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = StarPurple)
                }
            } else {
                Spacer(Modifier.width(12.dp))
            }
            Text(
                title, style = MaterialTheme.typography.titleLarge, color = StarDeep,
                maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f),
            )
            actions()
            StarLogo(height = 28.dp, modifier = Modifier.padding(end = 8.dp))
        }
    }
}

/** Create menu -> Template -> Preview & export. [step] is 1..3. */
@Composable
fun StepIndicator(step: Int) {
    val labels = listOf("Menu", "Template", "Preview")
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        labels.forEachIndexed { i, label ->
            val n = i + 1
            val active = n == step
            val done = n < step
            Box(
                Modifier.size(24.dp).clip(CircleShape)
                    .background(if (active || done) StarPurple else MaterialTheme.colorScheme.outlineVariant),
                contentAlignment = Alignment.Center,
            ) {
                Text(n.toString(), color = if (active || done) Color.White else StarPurple, style = MaterialTheme.typography.labelMedium)
            }
            Spacer(Modifier.width(6.dp))
            Text(
                label, style = MaterialTheme.typography.labelLarge,
                color = if (active) StarDeep else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
            )
            if (i < labels.lastIndex) {
                Box(Modifier.weight(1f).padding(horizontal = 8.dp).height(2.dp).background(MaterialTheme.colorScheme.outlineVariant))
            }
        }
    }
}

@Composable
fun StarCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = Color.White,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shadowElevation = 1.dp,
    ) { content() }
}

@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: ImageVector? = null, enabled: Boolean = true) {
    Button(
        onClick = onClick, enabled = enabled, modifier = modifier.height(50.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = StarPurple, contentColor = Color.White),
        contentPadding = PaddingValues(horizontal = 20.dp),
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun AccentButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: ImageVector? = null, enabled: Boolean = true) {
    Button(
        onClick = onClick, enabled = enabled, modifier = modifier.height(50.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = StarYellow, contentColor = StarDeep),
        contentPadding = PaddingValues(horizontal = 20.dp),
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun TypeBadge(text: String, color: Color = StarPurple) {
    Surface(shape = RoundedCornerShape(50), color = color.copy(alpha = 0.12f)) {
        Text(text, Modifier.padding(horizontal = 10.dp, vertical = 3.dp), color = color, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun ConfirmDialog(
    title: String, message: String, confirmLabel: String = "Delete",
    onConfirm: () -> Unit, onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = { onConfirm(); onDismiss() }) {
                Text(confirmLabel, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        containerColor = Color.White,
    )
}

/** Outlined text field with the original project's error / hint behaviour. */
@Composable
fun StarField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier.fillMaxWidth(),
    error: String? = null,
    hint: String? = null,
    placeholder: String? = null,
    keyboard: KeyboardType = KeyboardType.Text,
    capitalization: KeyboardCapitalization = KeyboardCapitalization.None,
    singleLine: Boolean = true,
    minLines: Int = 1,
) {
    val support = error ?: hint
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        label = { Text(label) },
        placeholder = if (placeholder != null) { { Text(placeholder) } } else null,
        isError = error != null,
        supportingText = if (support != null) { { Text(support) } } else null,
        singleLine = singleLine,
        minLines = minLines,
        shape = RoundedCornerShape(12.dp),
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboard,
            capitalization = capitalization,
            imeAction = if (singleLine) ImeAction.Next else ImeAction.Default,
        ),
    )
}

/** "Total: ₹500" or a coloured reason why the entry is not counted yet. */
@Composable
fun TotalLine(result: EntryResult, label: String) {
    if (result.status == EntryStatus.OK) {
        Text(
            buildAnnotatedString {
                append("$label ")
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(formatPaise(result.paise)) }
            },
            color = StarOk, style = MaterialTheme.typography.bodyMedium,
        )
    } else {
        val reason = result.reason.ifEmpty { "Not counted in the total yet" }
        Text(
            "$reason — not counted in the total",
            color = if (result.status == EntryStatus.INVALID) MaterialTheme.colorScheme.error else StarWarn,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

/** Removable chips for a comma-separated item list (same rejoining rule as the original). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ItemChips(text: String, onChange: (String) -> Unit) {
    val items = parseItems(text)
    if (items.isEmpty()) return
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        items.forEachIndexed { i, item ->
            Surface(
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.clickable { onChange(items.filterIndexed { j, _ -> j != i }.joinToString(", ")) },
            ) {
                Row(Modifier.padding(start = 12.dp, end = 6.dp, top = 4.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(item, style = MaterialTheme.typography.bodySmall, color = StarDeep)
                    Spacer(Modifier.width(4.dp))
                    Icon(Icons.Filled.Close, contentDescription = "Remove $item", modifier = Modifier.size(16.dp), tint = StarPurple)
                }
            }
        }
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(text, modifier, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = StarDeep)
}

/** Small helper used by lists of tiles. */
@Composable
fun IconBadge(icon: ImageVector, tint: Color = StarPurple) {
    Box(
        Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(tint.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, contentDescription = null, tint = tint) }
}
