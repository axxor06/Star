package com.star.menugenerator.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.star.menugenerator.R
import com.star.menugenerator.domain.TEMPLATES
import com.star.menugenerator.domain.TemplateSpec
import com.star.menugenerator.ui.AppViewModel
import com.star.menugenerator.ui.Screen
import com.star.menugenerator.ui.components.PrimaryButton
import com.star.menugenerator.ui.components.StarHeader
import com.star.menugenerator.ui.components.StepIndicator
import com.star.menugenerator.ui.theme.StarDeep
import com.star.menugenerator.ui.theme.StarPurple
import com.star.menugenerator.ui.theme.StarYellow

fun templateDrawable(spec: TemplateSpec): Int =
    if (spec.id == "green") R.drawable.template_green else R.drawable.template_purple

@Composable
fun TemplateThumb(spec: TemplateSpec, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = MaterialTheme.shapes.large,
        color = Color.White,
        border = BorderStroke(if (selected) 3.dp else 1.dp, if (selected) StarPurple else MaterialTheme.colorScheme.outlineVariant),
        shadowElevation = if (selected) 6.dp else 1.dp,
    ) {
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Image(
                painter = painterResource(templateDrawable(spec)),
                contentDescription = spec.name,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxWidth().aspectRatio(spec.w.toFloat() / spec.h),
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (selected) Icon(Icons.Filled.CheckCircle, contentDescription = "Selected", tint = StarPurple)
                Text(spec.name, fontWeight = FontWeight.Bold, color = StarDeep, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

/** Step 2 of 3: choose one of the two supplied template designs. */
@Composable
fun TemplateScreen(vm: AppViewModel) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Column {
                StarHeader("Select a template", onBack = { vm.back() })
                Surface(color = Color.White) { StepIndicator(2) }
            }
        },
        bottomBar = {
            Surface(color = Color.White, shadowElevation = 8.dp) {
                Row(
                    Modifier.navigationBarsPadding().padding(16.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    OutlinedButton(onClick = { vm.back() }, modifier = Modifier.weight(1f)) { Text("Back") }
                    PrimaryButton("Generate Menu", onClick = { vm.go(Screen.Preview) }, modifier = Modifier.weight(2f))
                }
            }
        },
    ) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text("Your menu will be placed on the template you pick. You can change it later.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TEMPLATES.forEach { t ->
                    TemplateThumb(t, selected = vm.templateId == t.id, onClick = { vm.selectTemplate(t.id) }, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}
