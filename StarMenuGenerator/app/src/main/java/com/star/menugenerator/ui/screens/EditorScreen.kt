package com.star.menugenerator.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.star.menugenerator.domain.DEFAULT_MEALS
import com.star.menugenerator.domain.Entry
import com.star.menugenerator.domain.EntryType
import com.star.menugenerator.domain.Meal
import com.star.menugenerator.domain.blankEntry
import com.star.menugenerator.domain.blankMeal
import com.star.menugenerator.domain.hasContent
import com.star.menugenerator.domain.newId
import com.star.menugenerator.domain.summarize
import com.star.menugenerator.domain.swapped
import com.star.menugenerator.domain.toApi
import com.star.menugenerator.domain.validateMenu
import com.star.menugenerator.domain.withNewIds
import com.star.menugenerator.ui.AppViewModel
import com.star.menugenerator.ui.SaveStatus
import com.star.menugenerator.ui.Screen
import com.star.menugenerator.ui.components.ConfirmDialog
import com.star.menugenerator.ui.components.EntryEditor
import com.star.menugenerator.ui.components.EntryShell
import com.star.menugenerator.ui.components.StarCard
import com.star.menugenerator.ui.components.StarField
import com.star.menugenerator.ui.components.StarHeader
import com.star.menugenerator.ui.components.StepIndicator
import com.star.menugenerator.ui.components.SummaryBar
import com.star.menugenerator.ui.theme.StarDeep
import com.star.menugenerator.ui.theme.StarOk
import com.star.menugenerator.ui.theme.StarPurple
import com.star.menugenerator.ui.theme.StarWarn
import kotlinx.coroutines.launch

private data class EntryChoice(val type: EntryType, val title: String, val hint: String)

private val ENTRY_CHOICES = listOf(
    EntryChoice(EntryType.INDIVIDUAL, "Individual Item", "Counted and priced one by one — Dosa, Tea, Juice"),
    EntryChoice(EntryType.PACKAGE, "Package / Sadhya", "A named combination of items with one price per plate"),
    EntryChoice(EntryType.BUFFET, "Buffet", "Several sections of dishes with one price per plate"),
)

@Composable
private fun SaveStatusLine(status: SaveStatus, onRetry: () -> Unit) {
    when (status) {
        SaveStatus.Idle -> Text("Saved on this phone automatically", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        SaveStatus.Saving -> Text("Saving…", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        SaveStatus.Saved -> Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = StarOk, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(4.dp))
            Text("All changes saved", style = MaterialTheme.typography.labelSmall, color = StarOk)
        }
        SaveStatus.Invalid -> Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Error, contentDescription = null, tint = StarWarn, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(4.dp))
            Text("Not saved yet — fix the highlighted fields", style = MaterialTheme.typography.labelSmall, color = StarWarn)
        }
        is SaveStatus.Failed -> Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Could not save: ${status.message}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
            TextButton(onClick = onRetry) { Text("Retry") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddEntrySheet(onAdd: (EntryType) -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color.White,
    ) {
        Column(Modifier.padding(horizontal = 16.dp).padding(bottom = 24.dp).navigationBarsPadding(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("What would you like to add?", style = MaterialTheme.typography.titleLarge, color = StarDeep)
            ENTRY_CHOICES.forEach { c ->
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth().clickable { onAdd(c.type); onDismiss() },
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Text(c.title, fontWeight = FontWeight.Bold, color = StarDeep)
                        Text(c.hint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun MealCard(
    meal: Meal, index: Int, total: Int, errs: Map<String, String>,
    onChange: (Meal) -> Unit, onDelete: () -> Unit, onMove: (Int) -> Unit, onDup: () -> Unit,
) {
    var open by rememberSaveable(meal.id) { mutableStateOf(meal.entries.isNotEmpty() || index == 0) }
    var confirmDelete by remember { mutableStateOf(false) }
    var showAdd by remember { mutableStateOf(false) }

    if (confirmDelete) {
        ConfirmDialog(
            title = "Delete this meal?",
            message = "“${meal.name.ifBlank { "Untitled meal" }}” and its ${meal.entries.size} entr${if (meal.entries.size == 1) "y" else "ies"} will be removed.",
            onConfirm = onDelete, onDismiss = { confirmDelete = false },
        )
    }
    if (showAdd) {
        AddEntrySheet(
            onAdd = { type -> onChange(meal.copy(entries = meal.entries + blankEntry(type))); open = true },
            onDismiss = { showAdd = false },
        )
    }

    StarCard {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { open = !open }, modifier = Modifier.size(36.dp)) {
                    Icon(if (open) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore, contentDescription = if (open) "Collapse meal" else "Expand meal", tint = StarPurple)
                }
                Column(Modifier.weight(1f)) {
                    Text(meal.name.ifBlank { "Untitled meal" }, fontWeight = FontWeight.Bold, color = StarDeep, maxLines = 1)
                    Text("${meal.entries.size} entr${if (meal.entries.size == 1) "y" else "ies"}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = { onMove(-1) }, enabled = index > 0, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Filled.ArrowUpward, contentDescription = "Move meal up", modifier = Modifier.size(20.dp))
                }
                IconButton(onClick = { onMove(1) }, enabled = index < total - 1, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Filled.ArrowDownward, contentDescription = "Move meal down", modifier = Modifier.size(20.dp))
                }
                IconButton(onClick = onDup, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Filled.ContentCopy, contentDescription = "Duplicate meal", modifier = Modifier.size(20.dp))
                }
                IconButton(onClick = { if (meal.entries.isEmpty()) onDelete() else confirmDelete = true }, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Filled.Delete, contentDescription = "Delete meal", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                }
            }
            if (open) {
                StarField(
                    "Meal name", meal.name, { onChange(meal.copy(name = it)) },
                    error = errs["${meal.id}:name"], placeholder = "e.g. Lunch", capitalization = KeyboardCapitalization.Words,
                )
                meal.entries.forEachIndexed { i, e ->
                    EntryShell(
                        entry = e, first = i == 0, last = i == meal.entries.lastIndex,
                        onMove = { d -> onChange(meal.copy(entries = meal.entries.swapped(i, d))) },
                        onDup = {
                            val c = meal.entries.toMutableList()
                            c.add(i + 1, e.withNewIds())
                            onChange(meal.copy(entries = c))
                        },
                        onDelete = { onChange(meal.copy(entries = meal.entries.filterIndexed { x, _ -> x != i })) },
                    ) {
                        EntryEditor(e, errs) { upd: Entry -> onChange(meal.copy(entries = meal.entries.map { if (it.id == e.id) upd else it })) }
                    }
                }
                OutlinedButton(onClick = { showAdd = true }, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Add Menu Entry")
                }
            }
        }
    }
}

/**
 * Step 1 of 3: menu title, meals, entries and live totals.
 * Workflow: Menu Title -> Add Meals -> Add Menu Entries -> Calculate Totals -> (next) Select Template.
 */
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(vm: AppViewModel) {
    val menu = vm.menu
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val errs = remember(menu, vm.templateId) {
        validateMenu(toApi(menu, vm.templateId)).errors.associate { "${it.id}:${it.field}" to it.message }
    }
    val summary = remember(menu) { summarize(menu) }
    val imeVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0

    fun next() {
        val message = when {
            !menu.hasContent() -> "Please add a meal with at least one named entry."
            errs.isNotEmpty() -> "Please fix the highlighted fields first."
            else -> null
        }
        if (message != null) scope.launch { snackbar.showSnackbar(message) } else vm.go(Screen.Templates)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Column {
                StarHeader("Create your menu", onBack = { vm.back() })
                Surface(color = Color.White) { StepIndicator(1) }
            }
        },
        bottomBar = { if (!imeVisible) SummaryBar(summary, onContinue = ::next) },
        snackbarHost = { SnackbarHost(snackbar) },
        modifier = Modifier.imePadding(),
    ) { padding ->
        LazyColumn(
            Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item(key = "title") {
                StarCard {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        StarField(
                            "Menu name", menu.title, { v -> vm.editMenu { it.copy(title = v) } },
                            error = errs["menu:title"], capitalization = KeyboardCapitalization.Words,
                            hint = "For your saved list — not printed on the menu",
                        )
                        SaveStatusLine(vm.saveStatus, onRetry = { vm.retrySave() })
                    }
                }
            }
            if (menu.meals.isEmpty()) {
                item(key = "empty") {
                    StarCard {
                        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Start by adding a meal", style = MaterialTheme.typography.titleMedium, color = StarDeep, fontWeight = FontWeight.Bold)
                            Text("Each meal can hold individual items, packages and buffets together.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            itemsIndexed(menu.meals, key = { _, m -> m.id }) { i, meal ->
                MealCard(
                    meal = meal, index = i, total = menu.meals.size, errs = errs,
                    onChange = { n -> vm.editMenu { m -> m.copy(meals = m.meals.map { if (it.id == meal.id) n else it }) } },
                    onDelete = { vm.editMenu { m -> m.copy(meals = m.meals.filter { it.id != meal.id }) } },
                    onMove = { d -> vm.editMenu { m -> m.copy(meals = m.meals.swapped(i, d)) } },
                    onDup = {
                        vm.editMenu { m ->
                            val c = m.meals.toMutableList()
                            c.add(i + 1, meal.withNewIds())
                            m.copy(meals = c)
                        }
                    },
                )
            }
            item(key = "add-meal") {
                StarCard {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Add another meal", fontWeight = FontWeight.Bold, color = StarDeep)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            DEFAULT_MEALS.forEach { name ->
                                AssistChip(
                                    onClick = { vm.editMenu { m -> m.copy(meals = m.meals + blankMeal(name)) } },
                                    label = { Text(name) },
                                    leadingIcon = { Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp)) },
                                )
                            }
                            AssistChip(
                                onClick = { vm.editMenu { m -> m.copy(meals = m.meals + blankMeal("")) } },
                                label = { Text("Custom meal") },
                                leadingIcon = { Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp)) },
                            )
                        }
                    }
                }
            }
        }
    }
}
